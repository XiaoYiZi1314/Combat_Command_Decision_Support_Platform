package com.ccds.assist.assist.service.impl;

import org.springframework.stereotype.Service;

import com.ccds.assist.assist.constant.AssistRuleConstant;
import com.ccds.assist.assist.dto.SupplyCalcCommand;
import com.ccds.assist.assist.dto.SupplyCalcResultDTO;
import com.ccds.assist.assist.service.AssistAccessService;
import com.ccds.assist.assist.service.SupplyAdviceService;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.model.ModelGateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 供水 AI 研判。本地数字原样带入，失败由前端保留本地结果。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplyAdviceServiceImpl implements SupplyAdviceService {

    private final AssistAccessService assistAccessService;

    private final ModelGateway modelGateway;

    /**
     * {@inheritDoc}
     */
    @Override
    public SupplyCalcResultDTO advise(AuthPrincipal principal, SupplyCalcCommand command) {
        assistAccessService.requireAccount(principal);
        if (command == null || command.getRequiredFlow() == null || command.getGap() == null) {
            throw new BizException(ErrorCodeConstant.ASSIST_SUPPLY_INVALID, AssistRuleConstant.MSG_SUPPLY_INVALID);
        }
        log.info("供水研判：fireType={}", command.getFireType());
        try {
            String raw = modelGateway.completeText(
                    AssistPromptBuilder.supplySystem(),
                    AssistPromptBuilder.supplyUser(command));
            return SupplyCalcResultDTO.builder()
                    .enhanced(Boolean.TRUE)
                    .advice(raw == null ? "" : raw.trim())
                    .disclaimer(AssistRuleConstant.AUXILIARY_DISCLAIMER)
                    .build();
        } catch (IllegalStateException ex) {
            log.error("供水研判模型不可用", ex);
            throw new BizException(ErrorCodeConstant.ASSIST_MODEL_UNAVAILABLE,
                    AssistRuleConstant.MSG_MODEL_UNAVAILABLE, ex);
        }
    }
}
