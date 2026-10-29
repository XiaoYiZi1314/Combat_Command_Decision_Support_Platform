package com.ccds.water.water.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水源变更申请（新增 / 修改 / 删除）。
 *
 * <p>消防站改卡不再直接写正式目录，而是提交申请；支队审批通过后才落档。
 * 审批表只允许撤回待审批申请，禁止物理删除已通过记录——它是「本月维修数」的统计凭据。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterRequestDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 申请站主键。
     */
    private Long stationId;

    /**
     * 动作：add / modify / delete。
     */
    private String action;

    /**
     * 目标水源主键；新增时为空。
     */
    private Long waterId;

    /**
     * 目标水源编号。
     */
    private String waterNo;

    /**
     * 申请内容快照 JSON。
     */
    private String itemJson;

    /**
     * 变更前快照 JSON。
     */
    private String beforeJson;

    /**
     * 变更前状态码。
     */
    private String fromStatus;

    /**
     * 变更后状态码。
     */
    private String toStatus;

    /**
     * 申请原因（必填）。
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
     * 提交人账号主键。
     */
    private Long createdBy;

    /**
     * 审批人账号主键。
     */
    private Long reviewedBy;

    /**
     * 审批人账号名。
     */
    private String reviewerName;

    /**
     * 审批时间。
     */
    private LocalDateTime reviewedAt;

    /**
     * 客户端请求号，用于提交幂等。
     */
    private String clientRequestId;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
