package com.ccds.duty.mapper;

import com.ccds.duty.entity.KeyUnitDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 重点单位预案Mapper
 *
 * @author system
 * @since 2024
 */
@Mapper
public interface KeyUnitMapper {

    /**
     * 插入
     */
    int insert(KeyUnitDO keyUnit);

    /**
     * 根据ID更新业务字段。
     *
     * @param keyUnit 重点单位
     * @return 影响行数
     */
    int updateById(KeyUnitDO keyUnit);

    /**
     * 软删除。
     *
     * @param keyUnit 含主键与删除时间
     * @return 影响行数
     */
    int softDeleteById(KeyUnitDO keyUnit);

    /**
     * 根据ID查询
     */
    KeyUnitDO selectById(Long id);

    /**
     * 根据消防站查询重点单位列表
     *
     * @param stationId 消防站ID
     * @return 重点单位列表
     */
    List<KeyUnitDO> selectByStationId(@Param("stationId") Long stationId);

    /**
     * 根据类别查询重点单位列表
     *
     * @param stationId 消防站ID
     * @param category  类别
     * @return 重点单位列表
     */
    List<KeyUnitDO> selectByStationAndCategory(@Param("stationId") Long stationId,
                                                 @Param("category") String category);

    /**
     * 根据关键词搜索重点单位
     *
     * @param stationId 消防站ID
     * @param keyword   关键词
     * @return 重点单位列表
     */
    List<KeyUnitDO> searchByKeyword(@Param("stationId") Long stationId,
                                     @Param("keyword") String keyword);
}
