package com.ccds.attack.attack.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ccds.roster.roster.entity.ScbaCalibrationDO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 空呼估算入参。压力为最近一次确认值，由引擎按混合速率衰减。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScbaEstimateQuery {

    /**
     * 最近确认压力。
     */
    private BigDecimal pressure;

    /**
     * 入场 / 待入场初始压力。
     */
    private BigDecimal initPressure;

    /**
     * 气瓶规格。
     */
    private String cylType;

    /**
     * 作业强度。
     */
    private String workLevel;

    /**
     * 作业场景。
     */
    private String scene;

    /**
     * 卡片状态码。
     */
    private String status;

    /**
     * 身高厘米。
     */
    private Integer heightCm;

    /**
     * 体重公斤。
     */
    private Integer weightKg;

    /**
     * 年龄。
     */
    private Integer age;

    /**
     * 高强度标定，新到旧。
     */
    private List<ScbaCalibrationDO> calibrations;

    /**
     * 入场时间。
     */
    private LocalDateTime enteredAt;

    /**
     * 最近确认压力时间。
     */
    private LocalDateTime measuredAt;

    /**
     * 当前时间。
     */
    private LocalDateTime now;
}
