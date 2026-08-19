package com.ccds.roster.roster.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.roster.roster.entity.ScbaCalibrationDO;

/**
 * 空呼标定查询。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface ScbaCalibrationMapper {

    /**
     * 某档案标定，按时间倒序。
     *
     * @param profileId 档案主键
     * @return 标定列表，不会为 null
     */
    List<ScbaCalibrationDO> selectByProfileId(@Param("profileId") Long profileId);
}
