package com.ccds.water.water.service;

import java.util.List;

import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.water.water.dto.WaterSaveCommand;
import com.ccds.water.water.vo.StationWaterVO;
import com.ccds.water.water.vo.WaterNearbyResultVO;
import com.ccds.water.water.vo.WaterVO;

/**
 * 水源档案：CRUD、附近水源与保留卡补回。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface WaterService {

    /**
     * 本站水源列表。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @return 水源列表
     */
    StationWaterVO getWaters(AuthPrincipal principal, Long stationId);

    /**
     * 全市水源，按账号可见范围返回。
     *
     * @param principal 登录主体
     * @return 水源列表，不会为 null
     */
    List<WaterVO> listCityWaters(AuthPrincipal principal);

    /**
     * 水源详情。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param waterId   水源主键
     * @return 水源
     */
    WaterVO getWater(AuthPrincipal principal, Long stationId, Long waterId);

    /**
     * 新建水源。仅支队/开发者可直写，站级须走审批申请。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param command   水源
     * @return 新建结果
     */
    WaterVO createWater(AuthPrincipal principal, Long stationId, WaterSaveCommand command);

    /**
     * 更新水源。仅支队/开发者可直写，站级须走审批申请。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param waterId   水源主键
     * @param command   水源
     * @return 更新结果
     */
    WaterVO updateWater(AuthPrincipal principal, Long stationId, Long waterId, WaterSaveCommand command);

    /**
     * 删除水源（写删除记录后软删）。仅支队/开发者可直写，站级须走审批申请。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param waterId   水源主键
     */
    void deleteWater(AuthPrincipal principal, Long stationId, Long waterId);

    /**
     * 附近水源：全市好用卡，半径内按距离升序。
     *
     * @param principal 登录主体
     * @param lng       经度
     * @param lat       纬度
     * @param radiusM   半径米，空用默认
     * @param limit     条数，空用默认
     * @return 附近水源
     */
    WaterNearbyResultVO nearby(AuthPrincipal principal, java.math.BigDecimal lng, java.math.BigDecimal lat,
                               Integer radiusM, Integer limit);

    /**
     * 按原编号补回本站「明确要求保留」的卡。
     *
     * @param stationId 站主键
     * @return 补回条数
     */
    int restoreRetained(Long stationId);
}
