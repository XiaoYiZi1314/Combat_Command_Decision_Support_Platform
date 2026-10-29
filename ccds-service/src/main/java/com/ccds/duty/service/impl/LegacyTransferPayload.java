package com.ccds.duty.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 旧系统调档记录，字段名与迁移契约 3.4 一致。
 *
 * @author ccds
 * @since 0.1.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class LegacyTransferPayload {

    /**
     * 调出站名。
     */
    public String fromStation;

    /**
     * 调入站名。
     */
    public String toStation;

    /**
     * 调出方旧档案号。
     */
    public String sourceProfileId;

    /**
     * 姓名。
     */
    public String name;

    /**
     * NFC 原始编号。
     */
    public String nfcTag;

    /**
     * 审批状态。
     */
    public String status;

    /**
     * 发起时间，毫秒。
     */
    public Long createdAt;

    /**
     * 审批时间，毫秒。
     */
    public Long reviewedAt;

    /**
     * 审批人。
     */
    public String reviewer;

    /**
     * 调入后旧档案号。
     */
    public String newProfileId;
}
