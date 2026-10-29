package com.ccds.roster.roster.service;

import java.util.List;

import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.roster.roster.dto.ProfileTransferCommand;
import com.ccds.roster.roster.vo.ProfileTransferVO;

/**
 * 人员跨站调动：站级提交，支队审批通过后落档。
 *
 * @author ccds
 * @since 0.1.2
 */
public interface ProfileTransferService {

    /**
     * 提交调动申请。
     *
     * @param principal 登录主体
     * @param command   申请
     * @return 申请视图
     */
    ProfileTransferVO submit(AuthPrincipal principal, ProfileTransferCommand command);

    /**
     * 调动申请列表。
     *
     * @param principal 登录主体
     * @param stationId 站主键，空查可见范围
     * @param status    状态，可空
     * @return 申请列表，不会为 null
     */
    List<ProfileTransferVO> list(AuthPrincipal principal, Long stationId, String status);

    /**
     * 审批。通过则落档：调入站新建档案号，调出站软删并写删除记录。
     *
     * @param principal 登录主体
     * @param transferId 申请主键
     * @param approve   是否通过
     * @param reviewReason 审批意见
     * @return 申请视图
     */
    ProfileTransferVO review(AuthPrincipal principal, Long transferId, Boolean approve, String reviewReason);
}
