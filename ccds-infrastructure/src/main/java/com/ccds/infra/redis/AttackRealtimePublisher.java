package com.ccds.infra.redis;

import com.ccds.attack.attack.vo.AttackSnapshotVO;

/**
 * 内攻快照实时发布。实现走 Redis Pub/Sub。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface AttackRealtimePublisher {

    /**
     * 发布某站快照。失败只记日志，不打断写事务。
     *
     * @param snapshot 快照，禁止含电话等敏感字段
     */
    void publishSnapshot(AttackSnapshotVO snapshot);
}
