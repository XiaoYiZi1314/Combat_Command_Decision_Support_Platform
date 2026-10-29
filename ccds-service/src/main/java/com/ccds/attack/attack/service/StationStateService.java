package com.ccds.attack.attack.service;

import com.ccds.attack.attack.model.ScbaThresholds;
import com.ccds.attack.attack.vo.ScbaThresholdVO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 站级作业模式与空呼阈值。
 *
 * <p>模式与阈值都随站点实时状态下发：站上保存后，共享页与指挥端按同一份值算颜色，
 * 指挥端只读，不能替站上改。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
public interface StationStateService {

    /**
     * 读站级作业模式，未配置时按测试模式。
     *
     * @param stationId 站主键
     * @return test / training / dispatch
     */
    String getWorkMode(Long stationId);

    /**
     * 读站级阈值，未配置时按一期默认值。
     *
     * @param stationId 站主键
     * @return 阈值
     */
    ScbaThresholds getThresholds(Long stationId);

    /**
     * 批量读站级作业模式，避免快照列表逐站查库。
     *
     * @param stationIds 站主键列表
     * @return 站主键到模式的映射，未配置的站不在其中
     */
    java.util.Map<Long, String> getWorkModes(java.util.List<Long> stationIds);

    /**
     * 批量读站级阈值视图，避免快照列表逐站查库。
     *
     * @param stationIds 站主键列表
     * @return 站主键到阈值的映射，未配置的站不在其中
     */
    java.util.Map<Long, ScbaThresholdVO> getThresholdVos(java.util.List<Long> stationIds);

    /**
     * 读站级阈值视图。
     *
     * @param stationId 站主键
     * @return 阈值视图
     */
    ScbaThresholdVO getThresholdVo(Long stationId);

    /**
     * 校验登录主体可见该站（读取模式与阈值的轻量校验）。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     */
    void requireVisible(AuthPrincipal principal, Long stationId);

    /**
     * 站级改作业模式。仅站级本站可改，指挥端只读。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param workMode  test / training / dispatch
     * @return 保存后的模式
     */
    String updateWorkMode(AuthPrincipal principal, Long stationId, String workMode);

    /**
     * 站级保存阈值。仅站级本站可改，危险阈值必须低于预警阈值。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param command   阈值
     * @return 保存后的阈值视图
     */
    ScbaThresholdVO saveThresholds(AuthPrincipal principal, Long stationId, ScbaThresholdVO command);
}
