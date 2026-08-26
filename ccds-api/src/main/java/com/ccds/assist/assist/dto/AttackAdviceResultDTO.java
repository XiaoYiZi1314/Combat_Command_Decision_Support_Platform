package com.ccds.assist.assist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内攻 AI 研判结果：轮换、撤离、风险提示。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackAdviceResultDTO {

    /**
     * 轮换建议。
     */
    private String rotationAdvice;

    /**
     * 撤离建议。
     */
    private String withdrawAdvice;

    /**
     * 风险提示。
     */
    private String riskHint;

    /**
     * 辅助声明，必须展示。
     */
    private String disclaimer;
}
