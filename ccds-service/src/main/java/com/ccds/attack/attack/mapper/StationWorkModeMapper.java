package com.ccds.attack.attack.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.attack.attack.entity.StationWorkModeDO;

/**
 * 站级作业模式访问。
 *
 * @author ccds
 * @since 0.1.1
 */
@Mapper
public interface StationWorkModeMapper {

    /**
     * 按站查作业模式。
     *
     * @param stationId 站主键
     * @return 作业模式，不存在为 null
     */
    StationWorkModeDO selectByStationId(@Param("stationId") Long stationId);

    /**
     * 按站写入作业模式，已存在则覆盖。
     *
     * @param workMode 作业模式
     * @return 影响行数
     */
    int insertOrUpdate(StationWorkModeDO workMode);

    /**
     * 批量查站级作业模式（避免逐站查库）。
     *
     * @param stationIds 站主键列表，不得为空
     * @return 模式列表，不会为 null
     */
    java.util.List<StationWorkModeDO> selectByStationIds(@Param("stationIds") java.util.List<Long> stationIds);
}
