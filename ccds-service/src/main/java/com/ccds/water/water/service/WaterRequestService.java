package com.ccds.water.water.service;

import java.util.List;

import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.water.water.dto.WaterRequestCommand;
import com.ccds.water.water.dto.WaterRequestReviewCommand;
import com.ccds.water.water.vo.WaterRequestVO;

/**
 * 水源变更审批：站级提交，支队审批通过后落档。
 *
 * @author ccds
 * @since 0.1.2
 */
public interface WaterRequestService {

    /**
     * 提交申请。站级与支队都可提交，审批只能支队/开发者做。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param command   申请
     * @return 申请视图
     */
    WaterRequestVO submit(AuthPrincipal principal, Long stationId, WaterRequestCommand command);

    /**
     * 申请列表。status 为空查全部。
     *
     * @param principal 登录主体
     * @param stationId 站主键，空查可见范围
     * @param status    状态，可空
     * @return 申请列表，不会为 null
     */
    List<WaterRequestVO> list(AuthPrincipal principal, Long stationId, String status);

    /**
     * 审批。通过则落档，驳回则原卡不动。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param requestId 申请主键
     * @param command   审批命令
     * @return 申请视图
     */
    WaterRequestVO review(AuthPrincipal principal, Long stationId, Long requestId,
                          WaterRequestReviewCommand command);

    /**
     * 撤回待审批申请。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param requestId 申请主键
     */
    void withdraw(AuthPrincipal principal, Long stationId, Long requestId);
}
