package com.ccds.vehicle.vehicle.service;

import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.org.org.entity.StationDO;

/**
 * 车辆档案访问控制。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface VehicleAccessService {

    /**
     * 校验当前账号可见该站。
     *
     * @param account   账号
     * @param stationId 站主键
     * @return 站
     */
    StationDO requireVisibleStation(AccountDO account, Long stationId);

    /**
     * 校验当前账号可直接落档该站车辆（仅支队/开发）。
     *
     * @param account   账号
     * @param stationId 站主键
     * @return 站
     */
    StationDO requireDirectWriteStation(AccountDO account, Long stationId);

    /**
     * 校验当前账号可提交该站车辆变更申请（仅站级本站）。
     *
     * @param account   账号
     * @param stationId 站主键
     * @return 站
     */
    StationDO requireRequestStation(AccountDO account, Long stationId);

    /**
     * 校验当前账号可审批车辆变更申请（仅支队/开发）。
     *
     * @param account 账号
     */
    void requireReviewer(AccountDO account);

    /**
     * 当前账号可见的站主键集合。
     *
     * @param account 账号
     * @return 站主键列表，不会为 null
     */
    java.util.List<Long> visibleStationIds(AccountDO account);
}
