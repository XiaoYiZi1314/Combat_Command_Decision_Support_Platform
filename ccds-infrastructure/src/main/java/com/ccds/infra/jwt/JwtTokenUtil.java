package com.ccds.infra.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.ccds.iam.identity.constant.AuthRuleConstant;

import lombok.RequiredArgsConstructor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 签发与解析访问 / 刷新令牌。密钥只从配置读取。
 *
 * @author ccds
 * @since 0.1.0
 */
@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties jwtProperties;

    /**
     * 签发访问令牌。
     *
     * @param accountId 账号主键
     * @param role      角色码
     * @param sessionId 会话主键
     * @return 令牌原文
     */
    public String issueAccessToken(Long accountId, String role, Long sessionId) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(accessTtlSeconds());
        return Jwts.builder()
                .issuer(AuthRuleConstant.JWT_ISSUER)
                .subject(AuthRuleConstant.JWT_SUBJECT_ACCESS)
                .id(UUID.randomUUID().toString())
                .claim(AuthRuleConstant.JWT_CLAIM_ACCOUNT_ID, accountId)
                .claim(AuthRuleConstant.JWT_CLAIM_ROLE, role)
                .claim(AuthRuleConstant.JWT_CLAIM_SESSION_ID, sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(accessKey())
                .compact();
    }

    /**
     * 签发刷新令牌。
     *
     * @param accountId 账号主键
     * @param role      角色码
     * @param sessionId 会话主键
     * @return 令牌原文
     */
    public String issueRefreshToken(Long accountId, String role, Long sessionId) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(refreshTtlSeconds());
        return Jwts.builder()
                .issuer(AuthRuleConstant.JWT_ISSUER)
                .subject(AuthRuleConstant.JWT_SUBJECT_REFRESH)
                .id(UUID.randomUUID().toString())
                .claim(AuthRuleConstant.JWT_CLAIM_ACCOUNT_ID, accountId)
                .claim(AuthRuleConstant.JWT_CLAIM_ROLE, role)
                .claim(AuthRuleConstant.JWT_CLAIM_SESSION_ID, sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(refreshKey())
                .compact();
    }

    /**
     * 解析访问令牌。失败抛运行时异常由调用方转业务异常。
     *
     * @param token 令牌原文
     * @return 声明
     */
    public JwtPayload parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(accessKey())
                .requireIssuer(AuthRuleConstant.JWT_ISSUER)
                .requireSubject(AuthRuleConstant.JWT_SUBJECT_ACCESS)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return toPayload(claims);
    }

    /**
     * 解析刷新令牌。
     *
     * @param token 令牌原文
     * @return 声明
     */
    public JwtPayload parseRefreshToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(refreshKey())
                .requireIssuer(AuthRuleConstant.JWT_ISSUER)
                .requireSubject(AuthRuleConstant.JWT_SUBJECT_REFRESH)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return toPayload(claims);
    }

    /**
     * 访问令牌有效秒数。
     *
     * @return 秒
     */
    public long getAccessTtlSeconds() {
        return accessTtlSeconds();
    }

    /**
     * 刷新令牌过期时刻。
     *
     * @return 过期时刻
     */
    public Instant refreshExpireAt() {
        return Instant.now().plusSeconds(refreshTtlSeconds());
    }

    private long accessTtlSeconds() {
        Long configured = jwtProperties.getAccessTtlSeconds();
        if (configured == null || configured <= 0L) {
            return AuthRuleConstant.DEFAULT_ACCESS_TTL_SECONDS;
        }
        return configured;
    }

    private long refreshTtlSeconds() {
        Long configured = jwtProperties.getRefreshTtlSeconds();
        if (configured == null || configured <= 0L) {
            return AuthRuleConstant.DEFAULT_REFRESH_TTL_SECONDS;
        }
        return configured;
    }

    private SecretKey accessKey() {
        return Keys.hmacShaKeyFor(requireSecret(jwtProperties.getAccessSecret(), "CCDS_JWT_ACCESS_SECRET"));
    }

    private SecretKey refreshKey() {
        return Keys.hmacShaKeyFor(requireSecret(jwtProperties.getRefreshSecret(), "CCDS_JWT_REFRESH_SECRET"));
    }

    private JwtPayload toPayload(Claims claims) {
        return JwtPayload.builder()
                .accountId(toLong(claims.get(AuthRuleConstant.JWT_CLAIM_ACCOUNT_ID)))
                .role(claims.get(AuthRuleConstant.JWT_CLAIM_ROLE, String.class))
                .sessionId(toLong(claims.get(AuthRuleConstant.JWT_CLAIM_SESSION_ID)))
                .build();
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        return null;
    }

    private byte[] requireSecret(String secret, String envName) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(envName + " missing");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(envName + " too short");
        }
        return bytes;
    }
}
