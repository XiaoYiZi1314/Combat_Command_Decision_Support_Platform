package com.ccds.duty.service;

import java.time.LocalDate;
import java.util.List;

import com.ccds.duty.dto.DutyDayDTO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 值班日历。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface DutyDayService {

    /**
     * 按站与日期查询。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param dutyDate  值班日期
     * @return 值班记录，没有则为 null
     */
    DutyDayDTO getByStationAndDate(AuthPrincipal principal, Long stationId, LocalDate dutyDate);

    /**
     * 按月查询。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param year      年
     * @param month     月
     * @return 列表，不会为 null
     */
    List<DutyDayDTO> listByMonth(AuthPrincipal principal, Long stationId, Integer year, Integer month);

    /**
     * 保存或更新。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param dutyDate  路径中的日期
     * @param dto       值班记录
     * @return 保存后的记录
     */
    DutyDayDTO saveOrUpdate(AuthPrincipal principal, Long stationId, LocalDate dutyDate, DutyDayDTO dto);

    /**
     * 删除指定日期值班。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param dutyDate  值班日期
     */
    void delete(AuthPrincipal principal, Long stationId, LocalDate dutyDate);
}
