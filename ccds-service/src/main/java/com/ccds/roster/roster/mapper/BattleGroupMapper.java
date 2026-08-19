package com.ccds.roster.roster.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.roster.roster.entity.BattleGroupDO;

/**
 * 战斗编组访问。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface BattleGroupMapper {

    /**
     * 本站编组，按顺序。
     *
     * @param stationId 站主键
     * @return 编组列表，不会为 null
     */
    List<BattleGroupDO> selectByStationId(@Param("stationId") Long stationId);

    /**
     * 插入编组。
     *
     * @param group 编组
     * @return 影响行数
     */
    int insert(BattleGroupDO group);

    /**
     * 删除本站全部编组。
     *
     * @param stationId 站主键
     * @return 影响行数
     */
    int deleteByStationId(@Param("stationId") Long stationId);
}
