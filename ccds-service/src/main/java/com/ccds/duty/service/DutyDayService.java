package com.ccds.duty.service;

import com.ccds.duty.dto.DutyDayDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 值班日历服务
 *
 * @author system
 * @since 2024
 */
public interface DutyDayService {

    /**
     * 根据消防站和日期查询值班记录
     *
     * @param stationId 消防站ID
     * @param dutyDate  值班日期
     * @return 值班记录
     */
    DutyDayDTO getByStationAndDate(Long stationId, LocalDate dutyDate);

    /**
     * 查询消防站某月的值班记录
     *
     * @param stationId 消防站ID
     * @param year      年份
     * @param month     月份
     * @return 值班记录列表
     */
    List<DutyDayDTO> listByMonth(Long stationId, Integer year, Integer month);

    /**
     * 保存或更新值班记录
     *
     * @param dto 值班记录
     * @return 保存后的值班记录
     */
    DutyDayDTO saveOrUpdate(DutyDayDTO dto);

    /**
     * 删除值班记录
     *
     * @param stationId 消防站ID
     * @param dutyDate  值班日期
     */
    void delete(Long stationId, LocalDate dutyDate);
}
