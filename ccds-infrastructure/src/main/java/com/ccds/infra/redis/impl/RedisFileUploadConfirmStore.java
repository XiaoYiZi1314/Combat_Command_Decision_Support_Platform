package com.ccds.infra.redis.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.ccds.duty.constant.FileRuleConstant;
import com.ccds.infra.redis.FileUploadConfirmStore;
import com.ccds.infra.redis.RedisKeyConstant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 文件上传确认票据。票据原文不进入 Redis，确认时原子校验并删除。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisFileUploadConfirmStore implements FileUploadConfirmStore {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String VALUE_SEPARATOR = ":";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local v=redis.call('GET',KEYS[1]);"
                    + "if v==ARGV[1] then redis.call('DEL',KEYS[1]); return 1; end; return 0",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    /** {@inheritDoc} */
    @Override
    public String issue(Long fileId, Long accountId, int expireSec) {
        if (fileId == null || accountId == null) {
            throw new IllegalArgumentException("file upload principal invalid");
        }
        byte[] random = new byte[FileRuleConstant.CONFIRM_TICKET_RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(random);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        stringRedisTemplate.opsForValue().set(keyOf(ticket), valueOf(fileId, accountId),
                Duration.ofSeconds(expireSec));
        return ticket;
    }

    /** {@inheritDoc} */
    @Override
    public boolean consume(String ticket, Long fileId, Long accountId) {
        if (ticket == null || ticket.isBlank() || fileId == null || accountId == null) {
            return false;
        }
        try {
            Long result = stringRedisTemplate.execute(
                    CONSUME_SCRIPT, List.of(keyOf(ticket)), valueOf(fileId, accountId));
            return Long.valueOf(1L).equals(result);
        } catch (RuntimeException ex) {
            log.error("file upload confirm ticket consume failed fileId={}", fileId, ex);
            return false;
        }
    }

    private String valueOf(Long fileId, Long accountId) {
        return fileId + VALUE_SEPARATOR + accountId;
    }

    private String keyOf(String ticket) {
        return RedisKeyConstant.FILE_UPLOAD_CONFIRM_PREFIX + hash(ticket);
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
