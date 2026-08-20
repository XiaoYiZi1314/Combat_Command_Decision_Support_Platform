package com.ccds.infra.redis;

/**
 * 补传去重。以客户端 eventId 为准，重复投递不二次处理。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface AttackEventDedupStore {

    /**
     * 尝试占位。已存在则返回 false。
     *
     * @param eventId 客户端幂等键
     * @return true 表示首次见到该键
     */
    boolean tryMark(String eventId);
}
