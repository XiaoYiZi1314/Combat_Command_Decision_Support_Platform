package com.ccds.assist.assist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 供水 AI 研判结果。失败时前端仍展示本地计算。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyCalcResultDTO {

    /**
     * 是否采用了模型增强。
     */
    private Boolean enhanced;

    /**
     * 研判说明。
     */
    private String advice;

    /**
     * 辅助声明，必须展示。
     */
    private String disclaimer;
}
