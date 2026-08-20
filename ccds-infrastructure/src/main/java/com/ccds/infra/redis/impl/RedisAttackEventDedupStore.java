package com.ccds.infra.redis.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.ccds.infra.redis.AttackEventDedupStore;
import com.ccds.infra.redis.RedisKeyConstant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用 Redis SETNX 做补传去重。原文 eventId 不进键名。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAttackEventDedupStore implements AttackEventDedupStore {

    private static final String HASH_ALG = "SHA-256";

    private static final String MARK_VALUE = "1";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tryMark(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return true;
        }
        String key = RedisKeyConstant.ATTACK_EVENT_DEDUP_PREFIX + hashEventId(eventId);
        try {
            Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(
                    key, MARK_VALUE, Duration.ofSeconds(RedisKeyConstant.ATTACK_EVENT_DEDUP_TTL_SECONDS));
            return Boolean.TRUE.equals(first);
        } catch (RuntimeException ex) {
            log.warn("redis event dedup unavailable, fallback to db unique", ex);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        String key = RedisKeyConstant.ATTACK_EVENT_DEDUP_PREFIX + hashEventId(eventId);
        try {
            stringRedisTemplate.delete(key);
        } catch (RuntimeException ex) {
            log.warn("redis event dedup release failed", ex);
        }
    }

    private String hashEventId(String eventId) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALG);
            byte[] hashed = digest.digest(eventId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            log.error("sha-256 missing", ex);
            return Integer.toHexString(eventId.hashCode());
        }
    }
}
