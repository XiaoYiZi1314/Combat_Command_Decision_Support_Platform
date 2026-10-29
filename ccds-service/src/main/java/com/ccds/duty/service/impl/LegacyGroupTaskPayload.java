package com.ccds.duty.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 旧系统编组任务，字段名与迁移契约 3.4 一致。当前 schema 无落点，仅统计条数。
 *
 * @author ccds
 * @since 0.1.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class LegacyGroupTaskPayload {

    /**
     * 组名。
     */
    public String name;

    /**
     * 任务。
     */
    public String task;

    /**
     * 位置。
     */
    public String location;
}
