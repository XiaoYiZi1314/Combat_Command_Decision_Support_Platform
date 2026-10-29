package com.ccds.duty.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 旧系统值班日历，字段名与迁移契约 3.4 一致。
 *
 * @author ccds
 * @since 0.1.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class LegacyDutyPayload {

    /**
     * 值班日期，格式 yyyy-MM-dd。
     */
    public String date;

    /**
     * 主官姓名，用于反查本站活档主键。
     */
    public String mainOfficerName;

    /**
     * 副职姓名，用于反查本站活档主键。
     */
    public String deputyOfficerName;

    /**
     * 备注。
     */
    public String note;
}
