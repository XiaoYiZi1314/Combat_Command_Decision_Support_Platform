package com.ccds.roster.roster.service;

import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.org.org.entity.StationDO;

/**
 * 花名册站级访问控制。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface RosterAccessService {

    /**
     * 校验当前账号可见该站。
     *
     * @param account   账号
     * @param stationId 站主键
     * @return 站
     */
    StationDO requireVisibleStation(AccountDO account, Long stationId);

    /**
     * 校验当前账号可写该站。
     *
     * @param account   账号
     * @param stationId 站主键
     * @return 站
     */
    StationDO requireWritableStation(AccountDO account, Long stationId);

    /**
     * 是否可写该站。
     *
     * @param account   账号
     * @param stationId 站主键
     * @return true 表示站级本站
     */
    boolean isWritable(AccountDO account, Long stationId);
}
