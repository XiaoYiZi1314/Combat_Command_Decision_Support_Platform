package com.ccds.duty.service.impl;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 旧系统战斗编组，字段名与迁移契约 3.4 一致。
 *
 * @author ccds
 * @since 0.1.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class LegacyGroupPayload {

    /**
     * 组名。
     */
    public String name;

    /**
     * 展示顺序。
     */
    public Integer sortNo;

    /**
     * 组内成员。
     */
    public List<LegacyGroupMemberPayload> members;
}
