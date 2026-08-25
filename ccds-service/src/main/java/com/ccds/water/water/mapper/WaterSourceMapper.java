package com.ccds.water.water.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.water.water.entity.WaterSourceDO;

/**
 * 水源档案访问。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface WaterSourceMapper {

    /**
     * 本站在册水源。
     *
     * @param stationId 站主键
     * @return 水源列表，不会为 null
     */
    List<WaterSourceDO> selectAliveByStationId(@Param("stationId") Long stationId);

    /**
     * 按主键查在册水源。
     *
     * @param id 主键
     * @return 水源，不存在为 null
     */
    WaterSourceDO selectAliveById(@Param("id") Long id);

    /**
     * 本站按名称查在册水源。
     *
     * @param stationId 站主键
     * @param name      名称
     * @return 水源，不存在为 null
     */
    WaterSourceDO selectAliveByStationAndName(@Param("stationId") Long stationId, @Param("name") String name);

    /**
     * 全市在用水源（nearby 扫描用，有数量上限）。
     *
     * @param maxRows 数量上限
     * @return 水源列表，不会为 null
     */
    List<WaterSourceDO> selectActiveAll(@Param("maxRows") int maxRows);

    /**
     * 插入水源。
     *
     * @param water 水源
     * @return 影响行数
     */
    int insert(WaterSourceDO water);

    /**
     * 更新可变字段。
     *
     * @param water 水源
     * @return 影响行数
     */
    int update(WaterSourceDO water);

    /**
     * 软删。
     *
     * @param id          主键
     * @param deletedAt   删除时间
     * @param gmtModified 修改时间
     * @return 影响行数
     */
    int softDelete(@Param("id") Long id,
                   @Param("deletedAt") LocalDateTime deletedAt,
                   @Param("gmtModified") LocalDateTime gmtModified);
}
