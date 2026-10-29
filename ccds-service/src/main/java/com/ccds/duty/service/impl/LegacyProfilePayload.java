package com.ccds.duty.service.impl;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 旧系统活档人员，字段名与迁移契约 3.4 一致。
 *
 * @author ccds
 * @since 0.1.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class LegacyProfilePayload {

    /**
     * 旧系统档案号。
     */
    public String legacyId;

    /**
     * 姓名。
     */
    public String name;

    /**
     * 职务。
     */
    public String role;

    /**
     * 衔级。
     */
    public String rank;

    /**
     * 电话明文，仅在内存流转。
     */
    public String phone;

    /**
     * 气瓶规格，仅允许 6.8/9。
     */
    public String cylType;

    /**
     * 身高厘米。
     */
    public Integer height;

    /**
     * 体重公斤。
     */
    public Integer weight;

    /**
     * 年龄。
     */
    public Integer age;

    /**
     * NFC 原始编号。
     */
    public String nfcTag;

    /**
     * 早检压力 MPa。
     */
    public BigDecimal morningPressure;
}
