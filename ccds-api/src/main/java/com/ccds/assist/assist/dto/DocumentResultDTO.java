package com.ccds.assist.assist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 文书生成结果。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResultDTO {

    /**
     * 文书正文。
     */
    private String content;

    /**
     * 辅助声明。
     */
    private String disclaimer;
}
