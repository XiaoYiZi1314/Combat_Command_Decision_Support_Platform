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
     * 本站按水源编号查在册水源。
     *
     * @param stationId 站主键
     * @param waterNo   水源编号
     * @return 水源，不存在为 null
     */
    WaterSourceDO selectAliveByStationAndNo(@Param("stationId") Long stationId, @Param("waterNo") String waterNo);

    /**
     * 全市好用卡水源（nearby 扫描用，有数量上限）。
     *
     * @param maxRows 数量上限
     * @return 水源列表，不会为 null
     */
    List<WaterSourceDO> selectActiveAll(@Param("maxRows") int maxRows);

    /**
     * 全市在册水源（listCityWaters 用，无 status/coord 限制）。
     *
     * @return 水源列表，不会为 null
     */
    List<WaterSourceDO> selectAliveAll();

    /**
     * 指定站在册水源（汇总统计用，避免逐站查库）。
     *
     * @param stationIds 站主键列表，不得为空
     * @return 水源列表，不会为 null
     */
    List<WaterSourceDO> selectAliveByStationIds(@Param("stationIds") List<Long> stationIds);

    /**
     * 本站全部水源，含已软删（保留卡补回用）。
     *
     * @param stationId 站主键
     * @return 水源列表，不会为 null
     */
    List<WaterSourceDO> selectAllByStationIncludingDeleted(@Param("stationId") Long stationId);

    /**
     * 本站水源总条数（含已软删），用于生成不重复的水源编号。
     *
     * @param stationId 站主键
     * @return 条数
     */
    Integer countAllByStation(@Param("stationId") Long stationId);

    /**
     * 本站已分配的最大水源编号（按字典序），无则 null。
     *
     * @param stationId 站主键
     * @return 编号
     */
    String selectMaxWaterNo(@Param("stationId") Long stationId);

    /**
     * 插入水源。
     *
     * @param water 水源
     * @return 影响行数
     */
    int insert(WaterSourceDO water);

    /**
     * 批量插入水源（导入用）。
     *
     * @param waters 水源列表
     * @return 影响行数
     */
    int insertBatch(@Param("waters") List<WaterSourceDO> waters);

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

    /**
     * 撤销软删（保留卡按原编号补回用）。
     *
     * @param id          主键
     * @param gmtModified 修改时间
     * @return 影响行数
     */
    int undelete(@Param("id") Long id, @Param("gmtModified") LocalDateTime gmtModified);

    /**
     * 恢复名称（撤销软删后还原被改名前的名称）。
     *
     * @param id          主键
     * @param name        名称
     * @param gmtModified 修改时间
     * @return 影响行数
     */
    int restoreName(@Param("id") Long id, @Param("name") String name,
                    @Param("gmtModified") LocalDateTime gmtModified);
}
