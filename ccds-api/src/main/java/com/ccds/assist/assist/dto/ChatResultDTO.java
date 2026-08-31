package com.ccds.assist.assist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 通用问答结果。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResultDTO {

    /**
     * 模型回答。
     */
    private String answer;

    /**
     * 辅助声明。
     */
    private String disclaimer;
}
