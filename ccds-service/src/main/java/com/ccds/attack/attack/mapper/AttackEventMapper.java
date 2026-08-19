package com.ccds.attack.attack.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.attack.attack.entity.AttackEventDO;

/**
 * 内攻事件访问。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface AttackEventMapper {

    /**
     * 按客户端幂等键查询。
     *
     * @param clientEventId 幂等键
     * @return 事件，不存在为 null
     */
    AttackEventDO selectByClientEventId(@Param("clientEventId") String clientEventId);

    /**
     * 插入事件。
     *
     * @param event 事件
     * @return 影响行数
     */
    int insert(AttackEventDO event);
}
