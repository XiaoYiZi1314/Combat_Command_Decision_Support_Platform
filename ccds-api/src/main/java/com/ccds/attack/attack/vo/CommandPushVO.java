package com.ccds.attack.attack.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指挥端 WS 推送载荷。仅含可见站快照。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandPushVO {

    /**
     * 消息类型。snapshot = 单站更新。
     */
    private String type;

    /**
     * 站快照。
     */
    private AttackSnapshotVO snapshot;
}
