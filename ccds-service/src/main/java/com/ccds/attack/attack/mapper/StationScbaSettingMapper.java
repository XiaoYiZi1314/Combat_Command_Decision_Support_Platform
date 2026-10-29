package com.ccds.attack.attack.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.attack.attack.entity.StationScbaSettingDO;

/**
 * 站级空呼阈值访问。
 *
 * @author ccds
 * @since 0.1.1
 */
@Mapper
public interface StationScbaSettingMapper {

    /**
     * 按站查阈值。
     *
     * @param stationId 站主键
     * @return 阈值配置，不存在为 null
     */
    StationScbaSettingDO selectByStationId(@Param("stationId") Long stationId);

    /**
     * 按站写入阈值，已存在则覆盖可变列。
     *
     * @param setting 阈值配置
     * @return 影响行数
     */
    int insertOrUpdate(StationScbaSettingDO setting);

    /**
     * 批量查站级空呼阈值（避免逐站查库）。
     *
     * @param stationIds 站主键列表，不得为空
     * @return 阈值列表，不会为 null
     */
    java.util.List<StationScbaSettingDO> selectByStationIds(@Param("stationIds") java.util.List<Long> stationIds);
}
