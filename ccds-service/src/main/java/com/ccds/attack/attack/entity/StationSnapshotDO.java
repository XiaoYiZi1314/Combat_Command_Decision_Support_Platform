package com.ccds.attack.attack.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站内攻快照。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationSnapshotDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属站。
     */
    private Long stationId;

    /**
     * 安全人数。
     */
    private Integer inCount;

    /**
     * 预警人数。
     */
    private Integer warnCount;

    /**
     * 危险人数。
     */
    private Integer dangerCount;

    /**
     * 已撤出人数。
     */
    private Integer outCount;

    /**
     * 预录入人数。
     */
    private Integer pendingCount;

    /**
     * 有未撤出人员的编组数。
     */
    private Integer groupCount;

    /**
     * 最近事件时间。
     */
    private LocalDateTime lastEventAt;

    /**
     * 快照摘要。
     */
    private String payloadJson;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
