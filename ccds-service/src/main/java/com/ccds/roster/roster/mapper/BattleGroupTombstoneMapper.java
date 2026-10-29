package com.ccds.roster.roster.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.roster.roster.entity.BattleGroupTombstoneDO;

/**
 * 战斗编组删除记录访问。
 *
 * @author ccds
 * @since 0.1.2
 */
@Mapper
public interface BattleGroupTombstoneMapper {

    /**
     * 写入删除记录；同站同名已存在则复活（解除时间清空）。
     *
     * @param tombstone 删除记录
     * @return 影响行数
     */
    int insertOrRevive(BattleGroupTombstoneDO tombstone);

    /**
     * 本站仍生效的编组删除记录。
     *
     * @param stationId 站主键
     * @return 删除记录列表，不会为 null
     */
    List<BattleGroupTombstoneDO> selectActiveByStationId(@Param("stationId") Long stationId);

    /**
     * 判断某组名是否仍在删除名单内。
     *
     * @param stationId 站主键
     * @param nameNorm  组名归一值
     * @return 条数
     */
    int countActiveByName(@Param("stationId") Long stationId, @Param("nameNorm") String nameNorm);

    /**
     * 解除某组名的删除记录（用户重新新建同名组时调用）。
     *
     * @param stationId  站主键
     * @param nameNorm   组名归一值
     * @param releasedBy 解除人账号主键
     * @return 影响行数
     */
    int releaseByName(@Param("stationId") Long stationId,
                      @Param("nameNorm") String nameNorm,
                      @Param("releasedBy") Long releasedBy);

    /**
     * 清理旧的误伤记录。只允许清理 legacy_bug 来源，用户删除的记录不得清理。
     *
     * @param stationId 站主键
     * @param before    早于该时间
     * @return 影响行数
     */
    int purgeLegacyBug(@Param("stationId") Long stationId, @Param("before") LocalDateTime before);
}
