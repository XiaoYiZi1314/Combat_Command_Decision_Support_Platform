package com.ccds.assist.assist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 危化品视觉/文字研判结果。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HazmatVisionResultDTO {

    /**
     * 可能的物质名称。
     */
    private String possibleName;

    /**
     * 危险性摘要。
     */
    private String hazardSummary;

    /**
     * 处置建议。
     */
    private String advice;

    /**
     * 辅助声明，必须展示。
     */
    private String disclaimer;
}
