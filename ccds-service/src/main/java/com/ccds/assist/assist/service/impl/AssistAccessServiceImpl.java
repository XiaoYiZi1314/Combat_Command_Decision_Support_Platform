package com.ccds.assist.assist.service.impl;

import org.springframework.stereotype.Service;

import com.ccds.assist.assist.constant.AssistRuleConstant;
import com.ccds.assist.assist.service.AssistAccessService;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.enums.AccountRoleEnum;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;

import lombok.RequiredArgsConstructor;

/**
 * 沿花名册可见性，辅助接口登录即可检索，研判需可见该站。
 *
 * @author ccds
 * @since 0.1.0
 */
@Service
@RequiredArgsConstructor
public class AssistAccessServiceImpl implements AssistAccessService {

    private static final String MSG_STATION_MISSING = "单位不存在";

    private final StationMapper stationMapper;

    private final AccountMapper accountMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public AccountDO requireAccount(AuthPrincipal principal) {
        if (principal == null || principal.getAccountId() == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, AssistRuleConstant.MSG_UNAUTHORIZED);
        }
        AccountDO account = accountMapper.selectById(principal.getAccountId());
        if (account == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, AssistRuleConstant.MSG_UNAUTHORIZED);
        }
        return account;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StationDO requireVisibleStation(AccountDO account, Long stationId) {
        StationDO station = stationMapper.selectById(stationId);
        if (station == null) {
            throw new BizException(ErrorCodeConstant.ASSIST_STATION_FORBIDDEN, MSG_STATION_MISSING);
        }
        if (!canView(account, station)) {
            throw new BizException(ErrorCodeConstant.ASSIST_STATION_FORBIDDEN,
                    AssistRuleConstant.MSG_STATION_FORBIDDEN);
        }
        return station;
    }

    private boolean canView(AccountDO account, StationDO station) {
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        if (role == null) {
            return false;
        }
        switch (role) {
            case STATION:
                return account.getStationId() != null && account.getStationId().equals(station.getId());
            case BRIGADE:
                return account.getBrigadeId() != null && account.getBrigadeId().equals(station.getBrigadeId());
            case HQ:
            case DEVELOPER:
                return true;
            default:
                return false;
        }
    }
}
