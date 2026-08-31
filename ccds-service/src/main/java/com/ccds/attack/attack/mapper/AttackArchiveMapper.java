package com.ccds.attack.attack.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import com.ccds.attack.attack.entity.AttackArchiveDO;

/**
 * 内攻历史归档 Mapper。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface AttackArchiveMapper {

    /**
     * 插入。
     *
     * @param archive 归档
     * @return 行数
     */
    int insert(AttackArchiveDO archive);

    /**
     * 按主键更新基础信息（事件名称/地点）。
     *
     * @param archive 归档
     * @return 行数
     */
    int updateById(AttackArchiveDO archive);

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
     * @return 归档
     */
    AttackArchiveDO selectById(Long id);

    /**
     * 按站查询。
     *
     * @param stationId 站主键
     * @return 列表，不会为 null
     */
    List<AttackArchiveDO> selectByStation(Long stationId);

    /**
     * 按多站查询，stationIds 为空时返回空列表。
     *
     * @param stationIds 站主键集合
     * @return 列表，不会为 null
     */
    List<AttackArchiveDO> selectByStations(@Param("stationIds") List<Long> stationIds);
}
