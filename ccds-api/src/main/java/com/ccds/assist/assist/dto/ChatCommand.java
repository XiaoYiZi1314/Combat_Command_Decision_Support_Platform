package com.ccds.assist.assist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.ccds.assist.assist.constant.AssistRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 通用问答入参。上下文只允许提交经过用户确认的摘要。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCommand {

    /**
     * 当前问题。
     */
    @NotBlank
    @Size(max = AssistRuleConstant.CHAT_QUESTION_MAX_LENGTH)
    private String question;

    /**
     * 可选现场摘要，不包含电话、证件、精确坐标等敏感信息。
     */
    @Size(max = AssistRuleConstant.CHAT_CONTEXT_MAX_LENGTH)
    private String context;
}
