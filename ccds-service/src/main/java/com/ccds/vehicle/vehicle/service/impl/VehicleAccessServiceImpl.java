package com.ccds.vehicle.vehicle.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.enums.AccountRoleEnum;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.org.org.service.OrgQueryService;
import com.ccds.vehicle.vehicle.service.VehicleAccessService;

import lombok.RequiredArgsConstructor;

/**
 * 站级只读并提交申请，支队/开发直接落档并审批。
 *
 * @author ccds
 * @since 0.1.0
 */
@Service
@RequiredArgsConstructor
public class VehicleAccessServiceImpl implements VehicleAccessService {

    private static final String MSG_FORBIDDEN = "无权查看该站车辆";

    private static final String MSG_WRITE_FORBIDDEN = "仅支队账号可直接修改车辆档案";

    private static final String MSG_REQUEST_FORBIDDEN = "仅本站账号可提交车辆变更申请";

    private static final String MSG_REVIEW_FORBIDDEN = "仅支队账号可审批车辆变更";

    private static final String MSG_STATION_MISSING = "单位不存在";

    private final StationMapper stationMapper;

    private final OrgQueryService orgQueryService;

    /**
     * {@inheritDoc}
     */
    @Override
    public StationDO requireVisibleStation(AccountDO account, Long stationId) {
        StationDO station = stationMapper.selectById(stationId);
        if (station == null) {
            throw new BizException(ErrorCodeConstant.VEHICLE_STATION_FORBIDDEN, MSG_STATION_MISSING);
        }
        if (!canView(account, station)) {
            throw new BizException(ErrorCodeConstant.VEHICLE_STATION_FORBIDDEN, MSG_FORBIDDEN);
        }
        return station;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StationDO requireDirectWriteStation(AccountDO account, Long stationId) {
        StationDO station = requireVisibleStation(account, stationId);
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        if (role != AccountRoleEnum.HQ && role != AccountRoleEnum.DEVELOPER) {
            throw new BizException(ErrorCodeConstant.VEHICLE_STATION_FORBIDDEN, MSG_WRITE_FORBIDDEN);
        }
        return station;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StationDO requireRequestStation(AccountDO account, Long stationId) {
        StationDO station = requireVisibleStation(account, stationId);
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        boolean stationSelf = role == AccountRoleEnum.STATION
                && account.getStationId() != null
                && account.getStationId().equals(stationId);
        if (!stationSelf) {
            throw new BizException(ErrorCodeConstant.VEHICLE_STATION_FORBIDDEN, MSG_REQUEST_FORBIDDEN);
        }
        return station;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requireReviewer(AccountDO account) {
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        if (role != AccountRoleEnum.HQ && role != AccountRoleEnum.DEVELOPER) {
            throw new BizException(ErrorCodeConstant.VEHICLE_REQUEST_INVALID, MSG_REVIEW_FORBIDDEN);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> visibleStationIds(AccountDO account) {
        return orgQueryService.listVisibleStationEntities(account).stream()
                .map(StationDO::getId)
                .collect(Collectors.toList());
    }

    private boolean canView(AccountDO account, StationDO station) {
        return orgQueryService.listVisibleStationEntities(account).stream()
                .anyMatch(visible -> visible.getId().equals(station.getId()));
    }
}
