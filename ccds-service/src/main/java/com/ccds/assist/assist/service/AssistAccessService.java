package com.ccds.assist.assist.service;

import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.org.org.entity.StationDO;

/**
 * 辅助研判的登录与站级可见。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface AssistAccessService {

    /**
     * 解析当前账号，会话失效则失败。
     *
     * @param principal 当前身份
     * @return 账号
     */
    AccountDO requireAccount(AuthPrincipal principal);

    /**
     * 要求站可见：站级本站，大队本大队，支队与开发者全部。
     *
     * @param account   账号
     * @param stationId 站主键
     * @return 站
     */
    StationDO requireVisibleStation(AccountDO account, Long stationId);
}
