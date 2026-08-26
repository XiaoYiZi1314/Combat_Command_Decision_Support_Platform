package com.ccds.assist.assist.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ccds.assist.assist.constant.AssistRuleConstant;
import com.ccds.assist.assist.dto.AttackAdviceCommand;
import com.ccds.assist.assist.dto.AttackAdvicePersonDTO;
import com.ccds.assist.assist.dto.AttackAdviceResultDTO;
import com.ccds.assist.assist.service.AssistAccessService;
import com.ccds.assist.assist.service.AttackAdviceService;
import com.ccds.attack.attack.entity.AttackPersonDO;
import com.ccds.attack.attack.mapper.AttackPersonMapper;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.model.ModelGateway;
import com.ccds.org.org.entity.StationDO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 内攻研判：前端可改白名单人员；空则改用本站未撤出卡片。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttackAdviceServiceImpl implements AttackAdviceService {

    private final AssistAccessService assistAccessService;

    private final AttackPersonMapper attackPersonMapper;

    private final ModelGateway modelGateway;

    /**
     * {@inheritDoc}
     */
    @Override
    public AttackAdviceResultDTO advise(AuthPrincipal principal, AttackAdviceCommand command) {
        AccountDO account = assistAccessService.requireAccount(principal);
        if (command == null || command.getStationId() == null) {
            throw new BizException(ErrorCodeConstant.ASSIST_ATTACK_INVALID, AssistRuleConstant.MSG_ATTACK_INVALID);
        }
        StationDO station = assistAccessService.requireVisibleStation(account, command.getStationId());
        List<AttackAdvicePersonDTO> whitelist = AssistPromptBuilder.sanitizePersons(command.getPersons());
        if (whitelist.isEmpty()) {
            List<AttackPersonDO> cards = attackPersonMapper.selectByStationId(station.getId());
            whitelist = AssistPromptBuilder.whitelistPersons(cards);
        }
        String userPrompt = AssistPromptBuilder.attackUser(
                station.getName(),
                command.getWeatherSummary(),
                command.getSceneDesc(),
                whitelist);
        log.info("内攻研判：stationId={}, personCount={}", station.getId(), whitelist.size());
        try {
            String raw = modelGateway.completeText(AssistPromptBuilder.attackSystem(), userPrompt);
            return AssistModelReplyParser.parseAttack(raw);
        } catch (IllegalStateException ex) {
            log.error("内攻研判模型不可用 stationId={}", station.getId(), ex);
            throw new BizException(ErrorCodeConstant.ASSIST_MODEL_UNAVAILABLE,
                    AssistRuleConstant.MSG_MODEL_UNAVAILABLE, ex);
        }
    }
}
