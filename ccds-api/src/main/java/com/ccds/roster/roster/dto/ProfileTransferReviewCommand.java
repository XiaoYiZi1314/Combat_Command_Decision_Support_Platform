package com.ccds.roster.roster.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批人员调动申请。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileTransferReviewCommand {

    /**
     * 是否通过。
     */
    @NotNull
    private Boolean approve;

    /**
     * 审批意见，可空。
     */
    @Size(max = 255)
    private String reviewReason;
}
