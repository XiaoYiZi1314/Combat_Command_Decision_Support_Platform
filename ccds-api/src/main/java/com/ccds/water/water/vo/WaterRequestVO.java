package com.ccds.water.water.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水源变更申请视图。
 *
 * <p>不含精确坐标的明文比对信息之外的敏感字段；日志只记 id 与 action。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterRequestVO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 申请站主键。
     */
    private Long stationId;

    /**
     * 申请站名。
     */
    private String stationName;

    /**
     * 动作：add / modify / delete。
     */
    private String action;

    /**
     * 目标水源主键。
     */
    private Long waterId;

    /**
     * 目标水源编号。
     */
    private String waterNo;

    /**
     * 改前内容。
     */
    private WaterVO before;

    /**
     * 改后内容。
     */
    private WaterVO after;

    /**
     * 变更前状态码。
     */
    private String fromStatus;

    /**
     * 变更后状态码。
     */
    private String toStatus;

    /**
     * 申请原因。
     */
    private String reason;

    /**
     * 状态：pending / approved / rejected。
     */
    private String status;

    /**
     * 审批意见。
     */
    private String reviewReason;

    /**
     * 审批人账号名。
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
}
