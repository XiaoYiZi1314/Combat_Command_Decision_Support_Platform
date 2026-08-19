package com.ccds.org.org.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.org.org.entity.StationDO;

/**
 * 消防站编制查询。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface StationMapper {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 站，不存在为 null
     */
    StationDO selectById(@Param("id") Long id);

    /**
     * 全部站，按展示顺序。编制规模固定，允许一次拉全。
     *
     * @return 站列表，不会为 null
     */
    List<StationDO> selectAll();

    /**
     * 某大队下属站。
     *
     * @param brigadeId 大队主键
     * @return 站列表，不会为 null
     */
    List<StationDO> selectByBrigadeId(@Param("brigadeId") Long brigadeId);
}
