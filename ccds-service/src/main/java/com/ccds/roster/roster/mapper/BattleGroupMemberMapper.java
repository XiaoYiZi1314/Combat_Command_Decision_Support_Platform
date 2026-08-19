package com.ccds.roster.roster.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.roster.roster.entity.BattleGroupMemberDO;

/**
 * 战斗编组成员访问。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface BattleGroupMemberMapper {

    /**
     * 本站全部成员（含姓名）。
     *
     * @param stationId 站主键
     * @return 成员列表，不会为 null
     */
    List<BattleGroupMemberDO> selectByStationId(@Param("stationId") Long stationId);

    /**
     * 插入成员。
     *
     * @param member 成员
     * @return 影响行数
     */
    int insert(BattleGroupMemberDO member);

    /**
     * 删除本站全部成员。
     *
     * @param stationId 站主键
     * @return 影响行数
     */
    int deleteByStationId(@Param("stationId") Long stationId);

    /**
     * 删除某档案的全部编组关系。
     *
     * @param profileId 档案主键
     * @return 影响行数
     */
    int deleteByProfileId(@Param("profileId") Long profileId);
}
