package com.ccds.infra.redis.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.ccds.attack.attack.vo.AttackSnapshotVO;
import com.ccds.infra.redis.AttackRealtimePublisher;
import com.ccds.infra.redis.RedisKeyConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用 Redis Pub/Sub 推内攻快照。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAttackRealtimePublisher implements AttackRealtimePublisher {

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public void publishSnapshot(AttackSnapshotVO snapshot) {
        if (snapshot == null || snapshot.getStationId() == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(snapshot);
            stringRedisTemplate.convertAndSend(RedisKeyConstant.ATTACK_SNAPSHOT_CHANNEL, payload);
        } catch (JsonProcessingException ex) {
            log.error("serialize snapshot failed stationId={}", snapshot.getStationId(), ex);
        } catch (RuntimeException ex) {
            log.error("redis publish snapshot failed stationId={}", snapshot.getStationId(), ex);
        }
    }
}
