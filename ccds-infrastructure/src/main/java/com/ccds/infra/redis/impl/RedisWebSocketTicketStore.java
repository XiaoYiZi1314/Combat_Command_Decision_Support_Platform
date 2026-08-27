package com.ccds.infra.redis.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.ccds.iam.identity.constant.AuthRuleConstant;
import com.ccds.iam.identity.enums.AccountRoleEnum;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.redis.RedisKeyConstant;
import com.ccds.infra.redis.WebSocketTicketStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 指挥 WebSocket 一次性票据。票据原文不进入 Redis 键，消费使用原子 GETDEL。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisWebSocketTicketStore implements WebSocketTicketStore {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DefaultRedisScript<String> CONSUME_SCRIPT =
            new DefaultRedisScript<>("local v=redis.call('GET',KEYS[1]);"
                    + "if v then redis.call('DEL',KEYS[1]); end; return v", String.class);

    private final StringRedisTemplate stringRedisTemplate;

    /** {@inheritDoc} */
    @Override
    public String issue(AuthPrincipal principal) {
        if (principal == null || principal.getAccountId() == null
                || principal.getSessionId() == null || principal.getRole() == null) {
            throw new IllegalArgumentException("principal invalid");
        }
        byte[] random = new byte[AuthRuleConstant.WS_TICKET_RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(random);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String value = principal.getAccountId()
                + AuthRuleConstant.WS_TICKET_VALUE_SEPARATOR + principal.getSessionId()
                + AuthRuleConstant.WS_TICKET_VALUE_SEPARATOR + principal.getRole().getCode();
        stringRedisTemplate.opsForValue().set(keyOf(ticket), value,
                Duration.ofSeconds(AuthRuleConstant.WS_TICKET_TTL_SECONDS));
        return ticket;
    }

    /** {@inheritDoc} */
    @Override
    public AuthPrincipal consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        String value;
        try {
            value = stringRedisTemplate.execute(CONSUME_SCRIPT, java.util.List.of(keyOf(ticket)));
        } catch (RuntimeException ex) {
            log.error("command ws ticket consume failed", ex);
            return null;
        }
        if (value == null || value.isBlank()) {
            return null;
        }
        return parsePrincipal(value);
    }

    private AuthPrincipal parsePrincipal(String value) {
        String[] fields = value.split(AuthRuleConstant.WS_TICKET_VALUE_SEPARATOR, -1);
        if (fields.length != 3) {
            return null;
        }
        try {
            AccountRoleEnum role = AccountRoleEnum.fromCode(fields[2]);
            if (role == null) {
                return null;
            }
            return AuthPrincipal.builder()
                    .accountId(Long.valueOf(fields[0]))
                    .sessionId(Long.valueOf(fields[1]))
                    .role(role)
                    .build();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String keyOf(String ticket) {
        return RedisKeyConstant.COMMAND_WS_TICKET_PREFIX + hash(ticket);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("sha-256 missing", ex);
        }
    }
}
