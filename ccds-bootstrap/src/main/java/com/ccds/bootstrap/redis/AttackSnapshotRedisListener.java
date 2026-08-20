package com.ccds.bootstrap.redis;

import java.io.IOException;

import jakarta.annotation.PostConstruct;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import com.ccds.attack.attack.service.CommandPushService;
import com.ccds.attack.attack.vo.AttackSnapshotVO;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 订阅 Redis 快照通道，转给本机指挥 WS。本进程发布也会收到，用于多实例同步。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttackSnapshotRedisListener implements MessageListener {

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    private final ChannelTopic attackSnapshotTopic;

    private final CommandPushService commandPushService;

    private final ObjectMapper objectMapper;

    /**
     * 绑定通道。
     */
    @PostConstruct
    public void bind() {
        redisMessageListenerContainer.addMessageListener(this, attackSnapshotTopic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null || message.getBody().length == 0) {
            return;
        }
        try {
            AttackSnapshotVO snapshot = objectMapper.readValue(message.getBody(), AttackSnapshotVO.class);
            commandPushService.broadcast(snapshot);
        } catch (IOException ex) {
            log.error("redis snapshot payload invalid", ex);
        } catch (RuntimeException ex) {
            log.error("redis snapshot dispatch failed", ex);
        }
    }
}
