package com.ccds.assist.assist.dto;

import jakarta.validation.constraints.Size;

import com.ccds.assist.assist.constant.AssistRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内攻研判允许带入的人员摘要。禁止电话、住址、完整花名册。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackAdvicePersonDTO {

    /**
     * 展示姓名。
     */
    @Size(max = 10)
    private String displayName;

    /**
     * 状态：pending / in / warn / danger。
     */
    @Size(max = 16)
    private String status;

    /**
     * 当前压力 MPa。
     */
    private Double pressure;

    /**
     * 剩余秒。
     */
    private Integer remainSec;

    /**
     * 编组名。
     */
    @Size(max = AssistRuleConstant.SCENE_DESC_MAX_LENGTH)
    private String groupName;
}
