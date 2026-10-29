package com.ccds.roster.roster.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人员跨站调动申请。
 *
 * <p>通过后才落档：调入站一律新建档案号，绝不把调出站的档案号原样写进调入站；
 * 只有调入站已有同名卡时才并进那张卡，同号不同名禁止合并。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileTransferDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 调出站主键。
     */
    private Long fromStationId;

    /**
     * 调入站主键。
     */
    private Long toStationId;

    /**
     * 调出站人员主键。
     */
    private Long profileId;

    /**
     * 调出站档案号。
     */
    private String sourceArchiveNo;

    /**
     * 并卡时目标人员主键。
     */
    private Long targetProfileId;

    /**
     * 落档后分配的新档案号。
     */
    private String newArchiveNo;

    /**
     * 调动原因。
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
     * 客户端请求号，提交幂等。
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
