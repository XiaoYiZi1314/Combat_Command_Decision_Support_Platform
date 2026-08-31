package com.ccds.assist.assist.service;

import com.ccds.assist.assist.dto.ChatCommand;
import com.ccds.assist.assist.dto.ChatResultDTO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * AI 通用问答服务。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface ChatService {

    /**
     * 生成消防作战辅助回答。
     *
     * @param principal 当前身份
     * @param command   问答入参
     * @return AI 回答
     */
    ChatResultDTO chat(AuthPrincipal principal, ChatCommand command);
}
