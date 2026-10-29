package com.ccds.duty.service.impl;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 旧系统空呼标定，字段名与迁移契约 3.4 一致。
 *
 * @author ccds
 * @since 0.1.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class LegacyCalibrationPayload {

    /**
     * 旧系统档案号。
     */
    public String legacyId;

    /**
     * 标定压力 MPa。
     */
    public BigDecimal pressure;

    /**
     * 完全用尽秒数。
     */
    public Integer fullTimeSec;

    /**
     * 气瓶规格。
     */
    public String cylType;

    /**
     * 标定来源。
     */
    public String source;

    /**
     * 测定时间，毫秒时间戳。
     */
    public Long measuredAt;
}
