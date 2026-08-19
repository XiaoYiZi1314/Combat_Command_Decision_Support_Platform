package com.ccds.roster.roster.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 空呼标定摘要。P2 只读展示，写入在后续空呼页。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScbaCalibrationVO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 标定压力。
     */
    private BigDecimal pressure;

    /**
     * 完全用尽秒数。
     */
    private Integer fullTimeSec;

    /**
     * 气瓶规格。
     */
    private String cylType;

    /**
     * 来源。
     */
    private String source;

    /**
     * 测定时间。
     */
    private LocalDateTime measuredAt;
}
