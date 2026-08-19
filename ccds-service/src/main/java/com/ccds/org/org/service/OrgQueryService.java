package com.ccds.org.org.service;

import java.util.List;

import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.vo.MeVO;
import com.ccds.iam.identity.vo.OrgTreeVO;
import com.ccds.iam.identity.vo.StationVO;

/**
 * 按账号角色过滤可见编制。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface OrgQueryService {

    /**
     * 组装当前账号与可见站。
     *
     * @param account 账号，调用方保证非空
     * @return 当前账号视图
     */
    MeVO buildMe(AccountDO account);

    /**
     * 可见编制树。
     *
     * @param account 账号，调用方保证非空
     * @return 编制树
     */
    OrgTreeVO buildOrgTree(AccountDO account);

    /**
     * 可见站列表。
     *
     * @param account 账号，调用方保证非空
     * @return 可见站，不会为 null
     */
    List<StationVO> listVisibleStations(AccountDO account);
}
