package com.ccds.assist.assist.service;

import com.ccds.assist.assist.dto.DocumentCommand;
import com.ccds.assist.assist.dto.DocumentResultDTO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * AI 文书生成服务。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface DocumentService {

    /**
     * 按模板生成文书。
     *
     * @param principal 当前身份
     * @param command   入参
     * @return 文书正文
     */
    DocumentResultDTO generate(AuthPrincipal principal, DocumentCommand command);
}
