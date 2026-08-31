package com.ccds.vehicle.vehicle.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 车辆变更申请实体。站级提交，指挥端审批。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequestDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属消防站 ID。
     */
    private Long stationId;

    /**
     * 变更动作：add/modify/delete。
     */
    private String action;

    /**
     * 目标车辆快照 JSON。
     */
    private String itemJson;

    /**
     * 变更前快照 JSON，modify 时使用。
     */
    private String beforeJson;

    /**
     * 状态：pending/approved/rejected。
     */
    private String status;

    /**
     * 提交人账号 ID。
     */
    private Long createdBy;

    /**
     * 审批人账号 ID。
     */
    private Long reviewedBy;

    /**
     * 审批人账号名，冗余展示。
     */
    private String reviewerName;

    /**
     * 审批时间。
     */
    private LocalDateTime reviewedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
