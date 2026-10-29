package com.ccds.water.water.service;

import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.water.water.vo.WaterImportResultVO;
import com.ccds.water.water.vo.WaterStatsVO;

/**
 * 水源检查表导入导出与汇总统计。
 *
 * @author ccds
 * @since 0.1.2
 */
public interface WaterLedgerService {

    /**
     * 导入现网水源检查记录表（23 列）。按「辖区队站 + 地理位置」命中原卡则更新，未命中才新增。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @param fileName  文件名
     * @param bytes     文件字节
     * @return 导入结果
     */
    WaterImportResultVO importWaters(AuthPrincipal principal, Long stationId, String fileName, byte[] bytes);

    /**
     * 导出本站表格（23 列）。
     *
     * @param principal 登录主体
     * @param stationId 站主键
     * @return xlsx
     */
    byte[] exportWaters(AuthPrincipal principal, Long stationId);

    /**
     * 导出空模板（23 列）。
     *
     * @param principal 登录主体
     * @param stationId 站主键，仅用于可见性校验
     * @return xlsx
     */
    byte[] exportTemplate(AuthPrincipal principal, Long stationId);

    /**
     * 水源汇总统计。
     *
     * @param principal 登录主体
     * @param month     月份 yyyy-MM，空用当月
     * @return 统计
     */
    WaterStatsVO summary(AuthPrincipal principal, String month);

    /**
     * 导出汇总统计（多工作表，与屏幕同源）。
     *
     * @param principal 登录主体
     * @param month     月份 yyyy-MM，空用当月
     * @return xlsx
     */
    byte[] exportSummary(AuthPrincipal principal, String month);
}
