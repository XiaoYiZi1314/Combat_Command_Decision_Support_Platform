package com.ccds.assist.assist.service;

import com.ccds.assist.assist.dto.SupplyCalcCommand;
import com.ccds.assist.assist.dto.SupplyCalcResultDTO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 供水 AI 研判。本地结果优先，模型失败由前端保留本地计算。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface SupplyAdviceService {

    /**
     * 在本地计算之上做可选增强。
     *
     * @param principal 当前身份
     * @param command   本地计算结果与摘要
     * @return 研判说明
     */
    SupplyCalcResultDTO advise(AuthPrincipal principal, SupplyCalcCommand command);
}
