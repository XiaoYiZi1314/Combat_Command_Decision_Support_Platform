package com.ccds.attack.attack.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站内攻快照。指挥首页读这张。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackSnapshotVO {

    /**
     * 站主键。
     */
    private Long stationId;

    /**
     * 站名。
     */
    private String stationName;

    /**
     * 所属大队主键。
     */
    private Long brigadeId;

    /**
     * 所属大队名。
     */
    private String brigadeName;

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
     * 修改时间。
     */
    private LocalDateTime gmtModified;

    /**
     * 距最近事件的秒数。指挥端用来标琥珀 / 红。
     */
    private Integer staleSec;
}
