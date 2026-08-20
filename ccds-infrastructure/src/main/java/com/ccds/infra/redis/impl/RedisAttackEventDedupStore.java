package com.ccds.infra.redis.impl;

/**
 * Redis 去重实现放在 bootstrap，避免 infrastructure 引入 spring-data-redis。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class RedisAttackEventDedupStore {

    private RedisAttackEventDedupStore() {
    }
}
