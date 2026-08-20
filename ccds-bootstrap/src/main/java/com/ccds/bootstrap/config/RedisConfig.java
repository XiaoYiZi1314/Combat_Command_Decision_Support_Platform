package com.ccds.bootstrap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.ccds.infra.redis.RedisKeyConstant;

/**
 * Redis 模板与快照通道订阅。
 *
 * @author ccds
 * @since 0.1.0
 */
@Configuration
public class RedisConfig {

    /**
     * 订阅内攻快照通道。
     *
     * @param connectionFactory 连接工厂
     * @return 监听容器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    /**
     * 快照通道主题。
     *
     * @return 主题
     */
    @Bean
    public ChannelTopic attackSnapshotTopic() {
        return new ChannelTopic(RedisKeyConstant.ATTACK_SNAPSHOT_CHANNEL);
    }
}
