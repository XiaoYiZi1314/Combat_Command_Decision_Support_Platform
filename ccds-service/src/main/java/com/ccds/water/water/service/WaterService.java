package com.ccds.water.water.service;

import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.water.water.dto.WaterSaveCommand;
import com.ccds.water.water.vo.StationWaterVO;
import com.ccds.water.water.vo.WaterImportResultVO;
import com.ccds.water.water.vo.WaterNearbyResultVO;
import com.ccds.water.water.vo.WaterVO;

/**
 * 水源档案：CRUD、表格导入导出、附近水源。
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
     * 全市水源（登录可读，站级「全市」浏览用）。
     *
     * @param principal 登录主体
     * @return 全市水源，不会为 null
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
     * 新建水源。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param command   水源
     * @return 新建结果
     */
    WaterVO createWater(AuthPrincipal principal, Long stationId, WaterSaveCommand command);

    /**
     * 更新水源。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param waterId   水源主键
     * @param command   水源
     * @return 更新结果
     */
    WaterVO updateWater(AuthPrincipal principal, Long stationId, Long waterId, WaterSaveCommand command);

    /**
     * 软删水源。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param waterId   水源主键
     */
    void deleteWater(AuthPrincipal principal, Long stationId, Long waterId);

    /**
     * 导入现网检查记录表。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param fileName  文件名
     * @param bytes     文件字节
     * @return 导入结果
     */
    WaterImportResultVO importWaters(AuthPrincipal principal, Long stationId, String fileName, byte[] bytes);

    /**
     * 导出本站表格。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @return xlsx
     */
    byte[] exportWaters(AuthPrincipal principal, Long stationId);

    /**
     * 导出空模板。
     *
     * @return xlsx
     */
    byte[] exportTemplate();

    /**
     * 附近水源：全市在用，半径内按距离升序。
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
}
