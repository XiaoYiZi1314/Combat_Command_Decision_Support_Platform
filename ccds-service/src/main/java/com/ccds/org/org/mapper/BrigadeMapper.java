package com.ccds.org.org.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.org.org.entity.BrigadeDO;

/**
 * 大队编制查询。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface BrigadeMapper {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 大队，不存在为 null
     */
    BrigadeDO selectById(@Param("id") Long id);

    /**
     * 全部大队，按主键升序。编制规模固定，允许一次拉全。
     *
     * @return 大队列表，不会为 null
     */
    List<BrigadeDO> selectAll();
}
