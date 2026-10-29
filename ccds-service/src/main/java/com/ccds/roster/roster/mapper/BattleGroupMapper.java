package com.ccds.roster.roster.mapper;

import java.time.LocalDateTime;
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
     * 本站固定编制卡（指挥员/安全员/通信员）。
     *
     * @param stationId 站主键
     * @return 编组列表，不会为 null
     */
    List<BattleGroupDO> selectFixedByStationId(@Param("stationId") Long stationId);

    /**
     * 按组名查本站编组。
     *
     * @param stationId 站主键
     * @param name      组名
     * @return 编组，不存在为 null
     */
    BattleGroupDO selectByStationAndName(@Param("stationId") Long stationId, @Param("name") String name);

    /**
     * 插入编组。
     *
     * @param group 编组
     * @return 影响行数
     */
    int insert(BattleGroupDO group);

    /**
     * 更新排序号（按 id 增量更新，不再全删全插）。
     *
     * @param id          主键
     * @param sortNo      排序号
     * @param gmtModified 修改时间
     * @return 影响行数
     */
    int updateSortNo(@Param("id") Long id, @Param("sortNo") Integer sortNo,
                     @Param("gmtModified") LocalDateTime gmtModified);

    /**
     * 删除单个非固定编组。
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 删除本站全部非固定编组（固定编制卡永不删除）。
     *
     * @param stationId 站主键
     * @return 影响行数
     */
    int deleteByStationId(@Param("stationId") Long stationId);
}
