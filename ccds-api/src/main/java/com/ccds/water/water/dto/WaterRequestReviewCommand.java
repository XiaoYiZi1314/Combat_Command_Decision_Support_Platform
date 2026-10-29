package com.ccds.water.water.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ccds.water.water.constant.WaterRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批水源变更申请。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterRequestReviewCommand {

    /**
     * 是否通过。
     */
    @NotNull
    private Boolean approve;

    /**
     * 审批意见，可空。
     */
    @Size(max = WaterRuleConstant.REASON_MAX_LENGTH)
    private String reviewReason;
}
