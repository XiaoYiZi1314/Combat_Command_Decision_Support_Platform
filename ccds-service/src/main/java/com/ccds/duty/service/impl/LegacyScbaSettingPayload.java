package com.ccds.duty.service.impl;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 旧系统站级空呼阈值，字段名与迁移契约 3.4 一致。
 *
 * @author ccds
 * @since 0.1.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class LegacyScbaSettingPayload {

    /**
     * 压力预警阈值 MPa。
     */
    public BigDecimal warnPressure;

    /**
     * 压力危险阈值 MPa。
     */
    public BigDecimal dangerPressure;

    /**
     * 时间预警阈值分钟。
     */
    public Integer warnTimeMin;

    /**
     * 时间危险阈值分钟。
     */
    public Integer dangerTimeMin;

    /**
     * 默认作业强度 light/moderate/heavy。
     */
    public String workLevel;

    /**
     * 默认场景 flat/highrise/dark/large/search。
     */
    public String scene;

    /**
     * 语音提醒开关。
     */
    public Boolean voiceEnabled;
}
