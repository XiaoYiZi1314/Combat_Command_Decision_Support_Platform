package com.ccds.duty.mapper;

import com.ccds.duty.entity.DutyDayDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 值班日历Mapper
 *
 * @author system
 * @since 2024
 */
@Mapper
public interface DutyDayMapper {

    /**
     * 插入
     */
    int insert(DutyDayDO dutyDay);

    /**
     * 根据ID更新
     */
    int updateById(DutyDayDO dutyDay);

    /**
     * 根据ID删除
     */
    int deleteById(Long id);

    /**
     * 根据ID查询
     */
    DutyDayDO selectById(Long id);

    /**
     * 根据消防站和日期查询
     *
     * @param stationId 消防站ID
     * @param dutyDate  值班日期
     * @return 值班记录
     */
    DutyDayDO selectByStationAndDate(@Param("stationId") Long stationId,
                                      @Param("dutyDate") LocalDate dutyDate);

    /**
     * 查询消防站某月的值班记录
     *
     * @param stationId 消防站ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 值班记录列表
     */
    List<DutyDayDO> selectByStationAndDateRange(@Param("stationId") Long stationId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    /**
     * 取各站最新一天的值班记录（支队导出战斗编组用）。
     *
     * @param stationIds 站主键列表，不得为空
     * @return 值班记录列表，不会为 null
     */
    List<DutyDayDO> selectLatestByStationIds(@Param("stationIds") List<Long> stationIds);
}
