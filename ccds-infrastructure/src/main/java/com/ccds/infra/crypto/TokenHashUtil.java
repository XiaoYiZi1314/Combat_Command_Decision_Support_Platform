package com.ccds.infra.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.ccds.iam.identity.constant.AuthRuleConstant;

/**
 * 刷新令牌入库哈希。禁止把令牌原文写入数据库或日志。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class TokenHashUtil {

    private TokenHashUtil() {
    }

    /**
     * 对刷新令牌做 SHA-256。
     *
     * @param rawToken 令牌原文
     * @return 十六进制哈希
     */
    public static String sha256(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("token blank");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(AuthRuleConstant.TOKEN_HASH_ALGORITHM);
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("hash algorithm missing", ex);
        }
    }
}
