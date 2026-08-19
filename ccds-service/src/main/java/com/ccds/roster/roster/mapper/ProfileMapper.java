package com.ccds.roster.roster.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.roster.roster.entity.ProfileDO;

/**
 * 人员档案访问。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface ProfileMapper {

    /**
     * 本站在册档案。
     *
     * @param stationId 站主键
     * @return 档案列表，不会为 null
     */
    List<ProfileDO> selectAliveByStationId(@Param("stationId") Long stationId);

    /**
     * 按主键查在册档案。
     *
     * @param id 主键
     * @return 档案，不存在为 null
     */
    ProfileDO selectAliveById(@Param("id") Long id);

    /**
     * 本站按姓名查在册档案。
     *
     * @param stationId 站主键
     * @param name      姓名
     * @return 档案，不存在为 null
     */
    ProfileDO selectAliveByStationAndName(@Param("stationId") Long stationId, @Param("name") String name);

    /**
     * 本站按 NFC 查在册档案。
     *
     * @param stationId 站主键
     * @param nfcTag    NFC 编号
     * @return 档案，不存在为 null
     */
    ProfileDO selectAliveByStationAndNfc(@Param("stationId") Long stationId, @Param("nfcTag") String nfcTag);

    /**
     * 插入档案。
     *
     * @param profile 档案
     * @return 影响行数
     */
    int insert(ProfileDO profile);

    /**
     * 更新可变字段。
     *
     * @param profile 档案
     * @return 影响行数
     */
    int update(ProfileDO profile);

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
