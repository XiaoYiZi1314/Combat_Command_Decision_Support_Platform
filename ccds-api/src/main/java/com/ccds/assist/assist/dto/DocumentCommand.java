package com.ccds.assist.assist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI 文书生成入参。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentCommand {

    /**
     * 文书模板：fire_report/rescue_report/drill_report/inspect_report。
     */
    @NotBlank(message = "文书类型不能为空")
    @Size(max = 50, message = "文书类型最多50字符")
    private String template;

    /**
     * 事件名称，可空。
     */
    @Size(max = 100, message = "事件名称最多100字符")
    private String eventName;

    /**
     * 地点，可空。
     */
    @Size(max = 200, message = "地点最多200字符")
    private String location;

    /**
     * 日期，yyyy-MM-dd，可空则取当天。
     */
    @Size(max = 20, message = "日期最多20字符")
    private String date;

    /**
     * 现场摘要：内攻人员 / 出动车辆 / 天气等，由前端装配。
     */
    @Size(max = 2000, message = "现场摘要最多2000字符")
    private String context;
}
