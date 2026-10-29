package com.ccds.duty.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 旧系统编组成员，字段名与迁移契约 3.4 一致。
 *
 * @author ccds
 * @since 0.1.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class LegacyGroupMemberPayload {

    /**
     * 旧系统档案号，用于映射新档案主键。
     */
    public String legacyId;

    /**
     * 组内角色。
     */
    public String roleInGroup;
}
