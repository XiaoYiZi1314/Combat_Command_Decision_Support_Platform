package com.ccds.water.water.service.impl;

import org.springframework.stereotype.Service;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.enums.AccountRoleEnum;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.water.water.service.WaterAccessService;

import lombok.RequiredArgsConstructor;

/**
 * 沿花名册可见性，写仅站级本站。
 *
 * @author ccds
 * @since 0.1.0
 */
@Service
@RequiredArgsConstructor
public class WaterAccessServiceImpl implements WaterAccessService {

    private static final String MSG_FORBIDDEN = "无权查看该单位水源";

    private static final String MSG_WRITE_FORBIDDEN = "仅本站账号可改水源";

    private static final String MSG_STATION_MISSING = "单位不存在";

    private final StationMapper stationMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public StationDO requireVisibleStation(AccountDO account, Long stationId) {
        StationDO station = stationMapper.selectById(stationId);
        if (station == null) {
            throw new BizException(ErrorCodeConstant.WATER_STATION_FORBIDDEN, MSG_STATION_MISSING);
        }
        if (!canView(account, station)) {
            throw new BizException(ErrorCodeConstant.WATER_STATION_FORBIDDEN, MSG_FORBIDDEN);
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
            throw new BizException(ErrorCodeConstant.WATER_WRITE_FORBIDDEN, MSG_WRITE_FORBIDDEN);
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
