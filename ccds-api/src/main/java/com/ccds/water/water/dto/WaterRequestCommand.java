package com.ccds.water.water.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ccds.water.water.constant.WaterRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交水源变更申请。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterRequestCommand {

    /**
     * 动作：add / modify / delete。
     */
    @NotBlank
    @Pattern(regexp = "^(add|modify|delete)$")
    private String action;

    /**
     * 目标水源主键；新增时为空。
     */
    private Long waterId;

    /**
     * 申请内容；新增与修改必填。
     */
    private WaterSaveCommand item;

    /**
     * 申请原因，必填。
     */
    @NotBlank
    @Size(max = WaterRuleConstant.REASON_MAX_LENGTH)
    private String reason;

    /**
     * 客户端请求号，用于提交幂等，可空。
     */
    @Size(max = 64)
    private String clientRequestId;
}
