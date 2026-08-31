package com.ccds.vehicle.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 车辆变更申请 DTO。站级提交，指挥端审批。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequestDTO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属消防站 ID，以路径为准。
     */
    private Long stationId;

    /**
     * 变更动作：add/modify/delete。
     */
    @NotBlank(message = "变更动作不能为空")
    private String action;

    /**
     * 目标车辆快照。add/modify 为变更后内容，delete 为变更前内容。
     */
    private VehicleDTO item;

    /**
     * modify 时的变更前快照，用于审批端对比。
     */
    private VehicleDTO before;

    /**
     * 状态：pending/approved/rejected。
     */
    private String status;

    /**
     * 提交时间。
     */
    private String createdAt;

    /**
     * 审批时间。
     */
    private String reviewedAt;

    /**
     * 审批人账号名。
     */
    private String reviewer;

    /**
     * 站点名称（列表返回时冗余）。
     */
    private String stationName;
}
