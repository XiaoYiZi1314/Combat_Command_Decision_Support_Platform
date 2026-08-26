package com.ccds.duty.service.impl;

import org.springframework.stereotype.Service;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.service.DutyAccessService;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.enums.AccountRoleEnum;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;

import lombok.RequiredArgsConstructor;

/**
 * 沿花名册可见性，写仅站级本站。
 *
 * @author ccds
 * @since 0.1.0
 */
@Service
@RequiredArgsConstructor
public class DutyAccessServiceImpl implements DutyAccessService {

    private static final String MSG_FORBIDDEN = "无权查看该单位数据";

    private static final String MSG_WRITE_FORBIDDEN = "仅本站账号可改该单位数据";

    private static final String MSG_STATION_MISSING = "单位不存在";

    private static final String MSG_UNAUTHORIZED = "登录已失效，请重新登录";

    private final StationMapper stationMapper;

    private final AccountMapper accountMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public AccountDO requireAccount(AuthPrincipal principal) {
        if (principal == null || principal.getAccountId() == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        AccountDO account = accountMapper.selectById(principal.getAccountId());
        if (account == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
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
            throw new BizException(ErrorCodeConstant.DUTY_STATION_FORBIDDEN, MSG_STATION_MISSING);
        }
        if (!canView(account, station)) {
            throw new BizException(ErrorCodeConstant.DUTY_STATION_FORBIDDEN, MSG_FORBIDDEN);
        }
        return station;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StationDO requireWritableStation(AccountDO account, Long stationId) {
        StationDO station = requireVisibleStation(account, stationId);
        if (!isWritable(account, stationId)) {
            throw new BizException(ErrorCodeConstant.DUTY_WRITE_FORBIDDEN, MSG_WRITE_FORBIDDEN);
        }
        return station;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWritable(AccountDO account, Long stationId) {
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        return role == AccountRoleEnum.STATION
                && account.getStationId() != null
                && account.getStationId().equals(stationId);
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
