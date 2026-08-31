package com.ccds.attack.attack.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 内攻历史归档实体。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttackArchiveDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 消防站 ID。
     */
    private Long stationId;

    /**
     * 类型：drill演练 / dispatch出警。
     */
    private String eventKind;

    /**
     * 事件名称。
     */
    private String eventName;

    /**
     * 地点。
     */
    private String location;

    /**
     * 归档时操作模式。
     */
    private String mode;

    /**
     * 本次内攻开始时间。
     */
    private LocalDateTime startedAt;

    /**
     * 归档完成时间。
     */
    private LocalDateTime finishedAt;

    /**
     * 记录人数。
     */
    private Integer personCount;

    /**
     * 人员快照 JSON。
     */
    private String personSnapshotJson;

    /**
     * 归档人账号 ID。
     */
    private Long createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
