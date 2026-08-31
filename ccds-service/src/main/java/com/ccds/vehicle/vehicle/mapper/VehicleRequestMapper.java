package com.ccds.vehicle.vehicle.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import com.ccds.vehicle.vehicle.entity.VehicleRequestDO;

/**
 * 车辆变更申请 Mapper。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface VehicleRequestMapper {

    /**
     * 插入。
     *
     * @param request 申请
     * @return 行数
     */
    int insert(VehicleRequestDO request);

    /**
     * 按主键更新。
     *
     * @param request 申请
     * @return 行数
     */
    int updateById(VehicleRequestDO request);

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
     * @return 申请
     */
    VehicleRequestDO selectById(Long id);

    /**
     * 按站查询。
     *
     * @param stationId 站主键
     * @return 列表，不会为 null
     */
    List<VehicleRequestDO> selectByStation(Long stationId);

    /**
     * 按多站查询，stationIds 为空时返回空列表。
     *
     * @param stationIds 站主键集合
     * @return 列表，不会为 null
     */
    List<VehicleRequestDO> selectByStations(@Param("stationIds") List<Long> stationIds);
}
