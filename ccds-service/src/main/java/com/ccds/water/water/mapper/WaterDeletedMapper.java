package com.ccds.water.water.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.water.water.entity.WaterDeletedDO;

/**
 * 已删除水源记录访问。
 *
 * @author ccds
 * @since 0.1.2
 */
@Mapper
public interface WaterDeletedMapper {

    /**
     * 写入删除记录；同站同编号已存在则复活该条（解除时间清空）。
     *
     * @param deleted 删除记录
     * @return 影响行数
     */
    int insertOrRevive(WaterDeletedDO deleted);

    /**
     * 解除删除记录（用户主动新建或审批通过新增时调用）。
     *
     * @param stationId 站主键
     * @param waterNo   水源编号
     * @param releasedBy 解除人账号主键
     * @return 影响行数
     */
    int release(@Param("stationId") Long stationId,
                @Param("waterNo") String waterNo,
                @Param("releasedBy") Long releasedBy);

    /**
     * 本站仍生效的已删除编号。
     *
     * @param stationId 站主键
     * @return 编号列表，不会为 null
     */
    List<String> selectActiveNos(@Param("stationId") Long stationId);

    /**
     * 多个站仍生效的已删除编号。
     *
     * @param stationIds 站主键列表，不得为空
     * @return 删除记录列表，不会为 null
     */
    List<WaterDeletedDO> selectActiveByStationIds(@Param("stationIds") List<Long> stationIds);

    /**
     * 全部仍生效的已删除编号（导入闸门用）。
     *
     * @return 删除记录列表，不会为 null
     */
    List<WaterDeletedDO> selectActiveAll();

    /**
     * 按编号判断是否在删除名单内。
     *
     * @param stationId 站主键
     * @param waterNo   水源编号
     * @return 条数
     */
    int countActiveByNo(@Param("stationId") Long stationId, @Param("waterNo") String waterNo);

    /**
     * 统计删除记录数，供核对。
     *
     * @param stationId 站主键
     * @return 条数
     */
    int countByStation(@Param("stationId") Long stationId);

    /**
     * 清理旧的误伤删除记录（仅 legacy_import 来源可自动清理）。
     *
     * @param before 早于该时间的记录可清理
     * @return 影响行数
     */
    int purgeLegacyBefore(@Param("before") LocalDateTime before);
}
