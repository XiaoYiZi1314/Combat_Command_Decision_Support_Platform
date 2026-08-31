package com.ccds.vehicle.vehicle.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import com.ccds.vehicle.vehicle.entity.VehicleDO;

/**
 * 车辆档案 Mapper。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface VehicleMapper {

    /**
     * 插入。
     *
     * @param vehicle 车辆
     * @return 行数
     */
    int insert(VehicleDO vehicle);

    /**
     * 按主键更新。
     *
     * @param vehicle 车辆
     * @return 行数
     */
    int updateById(VehicleDO vehicle);

    /**
     * 按主键删除。
     *
     * @param id 主键
     * @return 行数
     */
    int deleteById(Long id);

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 车辆
     */
    VehicleDO selectById(Long id);

    /**
     * 按站查询。
     *
     * @param stationId 站主键
     * @return 列表，不会为 null
     */
    List<VehicleDO> selectByStation(Long stationId);

    /**
     * 按多站查询，stationIds 为空时返回空列表。
     *
     * @param stationIds 站主键集合
     * @return 列表，不会为 null
     */
    List<VehicleDO> selectByStations(@Param("stationIds") List<Long> stationIds);

    /**
     * 按号牌查同站车辆，排除指定主键。
     *
     * @param stationId 站主键
     * @param plate     号牌
     * @param excludeId 排除的主键，可空
     * @return 列表，不会为 null
     */
    List<VehicleDO> selectByStationAndPlate(@Param("stationId") Long stationId,
                                            @Param("plate") String plate,
                                            @Param("excludeId") Long excludeId);
}
