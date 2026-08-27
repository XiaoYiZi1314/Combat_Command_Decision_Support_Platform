package com.ccds.infra.redis;

/**
 * Redis 键与通道名。格式 {模块}:{业务}:{操作}:{标识}。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class RedisKeyConstant {

    /**
     * 内攻快照发布通道。
     */
    public static final String ATTACK_SNAPSHOT_CHANNEL = "ccds:attack:snapshot";

    /**
     * 内攻事件补传去重键前缀。完整键为前缀 + eventId 哈希。
     */
    public static final String ATTACK_EVENT_DEDUP_PREFIX = "ccds:attack:event:";

    /**
     * 补传去重键过期秒。
     */
    public static final long ATTACK_EVENT_DEDUP_TTL_SECONDS = 7L * 24L * 3600L;

    /**
     * 指挥 WebSocket 一次性票据键前缀。完整键只包含票据哈希。
     */
    public static final String COMMAND_WS_TICKET_PREFIX = "ccds:command:ws-ticket:";

    /**
     * 文件上传确认票据键前缀。完整键只包含确认票据哈希。
     */
    public static final String FILE_UPLOAD_CONFIRM_PREFIX = "ccds:file:upload-confirm:";

    private RedisKeyConstant() {
    }
}
