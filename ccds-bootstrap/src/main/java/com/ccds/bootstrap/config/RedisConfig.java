package com.ccds.bootstrap.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.ccds.attack.attack.service.CommandPushService;
import com.ccds.attack.attack.vo.AttackSnapshotVO;
import com.ccds.infra.redis.RedisKeyConstant;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis 通道订阅。把快照转给本机指挥 WS。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * 快照通道主题。
     *
     * @return 主题
     */
    @Bean
    public ChannelTopic attackSnapshotTopic() {
        return new ChannelTopic(RedisKeyConstant.ATTACK_SNAPSHOT_CHANNEL);
    }

    /**
     * 订阅内攻快照通道。
     *
     * @param connectionFactory    连接工厂
     * @param attackSnapshotTopic  主题
     * @param commandPushService   指挥推送
     * @param objectMapper         JSON
     * @return 监听容器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChannelTopic attackSnapshotTopic,
            CommandPushService commandPushService,
            ObjectMapper objectMapper) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new SnapshotMessageListener(commandPushService, objectMapper),
                attackSnapshotTopic);
        return container;
    }

    private static final class SnapshotMessageListener implements MessageListener {

        private final CommandPushService commandPushService;

        private final ObjectMapper objectMapper;

        private SnapshotMessageListener(CommandPushService commandPushService, ObjectMapper objectMapper) {
            this.commandPushService = commandPushService;
            this.objectMapper = objectMapper;
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
}
