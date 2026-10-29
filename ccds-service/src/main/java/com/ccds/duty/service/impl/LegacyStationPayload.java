package com.ccds.duty.service.impl;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * roster.ndjson 单站载荷，字段名与迁移契约 3.4 一致。
 *
 * @author ccds
 * @since 0.1.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class LegacyStationPayload {

    /**
     * 旧系统站名。
     */
    public String station;

    /**
     * 旧系统站键。
     */
    public String stationKey;

    /**
     * 数据来源：cloud/bundle。
     */
    public String source;

    /**
     * 旧系统索引更新时间，毫秒。
     */
    public Long indexUpdatedAt;

    /**
     * 活档人员。
     */
    public List<LegacyProfilePayload> profiles;

    /**
     * 旧系统删除键，形如 id:p_3 / nfc:530B2455300001。
     */
    public List<String> deletedKeys;

    /**
     * 战斗编组。
     */
    public List<LegacyGroupPayload> groups;

    /**
     * 空呼标定。
     */
    public List<LegacyCalibrationPayload> calibrations;

    /**
     * 值班日历。
     */
    public List<LegacyDutyPayload> duty;

    /**
     * 调档记录。
     */
    public List<LegacyTransferPayload> transfers;

    /**
     * 站级空呼阈值。
     */
    public LegacyScbaSettingPayload settings;

    /**
     * 作业模式。
     */
    public String operationMode;

    /**
     * 编组任务，当前不落库，仅统计。
     */
    public List<LegacyGroupTaskPayload> groupTasks;
}
