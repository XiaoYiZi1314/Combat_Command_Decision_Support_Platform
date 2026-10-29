package com.ccds.water.water.service;

import java.util.List;

import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.water.water.dto.WaterVideoCommand;
import com.ccds.water.water.vo.WaterVideoVO;

/**
 * 水源出水视频登记与查询。
 *
 * @author ccds
 * @since 0.1.2
 */
public interface WaterVideoService {

    /**
     * 登记视频。超过 10 秒直接拒收。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param waterId   水源主键
     * @param command   登记内容
     * @return 视频视图
     */
    WaterVideoVO submit(AuthPrincipal principal, Long stationId, Long waterId, WaterVideoCommand command);

    /**
     * 某水源的视频列表。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param waterId   水源主键
     * @return 视频列表，不会为 null
     */
    List<WaterVideoVO> list(AuthPrincipal principal, Long stationId, Long waterId);
}
