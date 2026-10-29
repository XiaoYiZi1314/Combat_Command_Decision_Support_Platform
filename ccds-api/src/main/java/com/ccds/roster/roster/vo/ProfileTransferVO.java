package com.ccds.roster.roster.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人员跨站调动申请视图。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileTransferVO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 调出站主键。
     */
    private Long fromStationId;

    /**
     * 调出站名。
     */
    private String fromStationName;

    /**
     * 调入站主键。
     */
    private Long toStationId;

    /**
     * 调入站名。
     */
    private String toStationName;

    /**
     * 人员姓名。
     */
    private String profileName;

    /**
     * 调出站档案号。
     */
    private String sourceArchiveNo;

    /**
     * 落档后新档案号。
     */
    private String newArchiveNo;

    /**
     * 并卡目标人员主键。
     */
    private Long targetProfileId;

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
