package com.ccds.water.water.service;

import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.org.org.entity.StationDO;

/**
 * 水源权限：读允许登录用户看全市，写仅站级本站。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface WaterAccessService {

    /**
     * 要求站可见（沿花名册可见性）。
     *
     * @param account   账号
     * @param stationId 站主键
     * @return 站
     */
    StationDO requireVisibleStation(AccountDO account, Long stationId);

    /**
     * 要求站可写：站级且本站。
     *
     * @param account   账号
     * @param stationId 站主键
     * @return 站
     */
    StationDO requireWritableStation(AccountDO account, Long stationId);

    /**
     * 是否可写（站级本站）。界面「可编辑」标记用。
     *
     * @param account   账号
     * @param stationId 站主键
     * @return 可写为 true
     */
    boolean isWritable(AccountDO account, Long stationId);

    /**
     * 是否可直接改正式目录（支队 / 开发者）。站级一律走审批申请。
     *
     * @param account 账号
     * @return 可直写为 true
     */
    boolean isDirectWriter(AccountDO account);

    /**
     * 要求可提交申请：站级本站或支队/开发者。
     *
     * @param account   账号
     * @param stationId 站主键
     * @return 站
     */
    StationDO requireRequestStation(AccountDO account, Long stationId);

    /**
     * 要求审批人：支队 / 开发者。大队不审批。
     *
     * @param account 账号
     */
    void requireReviewer(AccountDO account);
}
