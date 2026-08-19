package com.ccds.roster.roster.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 空呼标定。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScbaCalibrationDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 档案主键。
     */
    private Long profileId;

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

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
