package com.ccds.assist.assist.service.impl;

import org.springframework.stereotype.Service;

import com.ccds.assist.assist.constant.AssistRuleConstant;
import com.ccds.assist.assist.dto.ChatCommand;
import com.ccds.assist.assist.dto.ChatResultDTO;
import com.ccds.assist.assist.service.AssistAccessService;
import com.ccds.assist.assist.service.ChatService;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.model.ModelGateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 通用问答实现。上下文仅作为有限摘要发送给服务端模型。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String SYSTEM_PROMPT = "你是作战指挥辅助决策平台的消防作战 AI 助手。"
            + "回答应简洁、审慎、可执行；不编造现场不存在的信息。"
            + "涉及人员安全、危险品和灭火救援时，提醒结合现场侦检、指挥员和安全员判断。"
            + "禁止索要或推断电话、证件、住址、精确坐标等敏感信息。"
            + "不要使用 Markdown 表格，必须声明本回答不替代现场指挥。";

    private final AssistAccessService assistAccessService;

    private final ModelGateway modelGateway;

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatResultDTO chat(AuthPrincipal principal, ChatCommand command) {
        assistAccessService.requireAccount(principal);
        if (command == null || command.getQuestion() == null || command.getQuestion().isBlank()) {
            throw new BizException(ErrorCodeConstant.ASSIST_CHAT_INVALID, "请输入问题");
        }
        String prompt = buildUserPrompt(command);
        try {
            String answer = modelGateway.completeText(SYSTEM_PROMPT, prompt);
            return ChatResultDTO.builder()
                    .answer(answer == null ? "" : answer.trim())
                    .disclaimer(AssistRuleConstant.AUXILIARY_DISCLAIMER)
                    .build();
        } catch (IllegalStateException ex) {
            log.error("AI 通用问答模型不可用", ex);
            throw new BizException(ErrorCodeConstant.ASSIST_MODEL_UNAVAILABLE,
                    AssistRuleConstant.MSG_MODEL_UNAVAILABLE, ex);
        }
    }

    private String buildUserPrompt(ChatCommand command) {
        StringBuilder builder = new StringBuilder("用户问题：");
        builder.append(trim(command.getQuestion(), AssistRuleConstant.CHAT_QUESTION_MAX_LENGTH));
        if (command.getContext() != null && !command.getContext().isBlank()) {
            builder.append("\n现场摘要：")
                    .append(trim(command.getContext(), AssistRuleConstant.CHAT_CONTEXT_MAX_LENGTH));
        }
        return builder.toString();
    }

    private String trim(String value, int maxLength) {
        String text = value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
