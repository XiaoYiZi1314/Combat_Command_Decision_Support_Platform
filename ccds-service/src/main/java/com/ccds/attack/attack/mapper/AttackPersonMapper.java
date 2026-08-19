package com.ccds.attack.attack.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.attack.attack.entity.AttackPersonDO;

/**
 * 内攻人员卡片访问。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface AttackPersonMapper {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 卡片，不存在为 null
     */
    AttackPersonDO selectById(@Param("id") Long id);

    /**
     * 按客户端事件幂等键查询。
     *
     * @param clientEventId 幂等键
     * @return 卡片，不存在为 null
     */
    AttackPersonDO selectByClientEventId(@Param("clientEventId") String clientEventId);

    /**
     * 本站全部卡片，未撤出在前。
     *
     * @param stationId 站主键
     * @return 卡片列表，不会为 null
     */
    List<AttackPersonDO> selectByStationId(@Param("stationId") Long stationId);

    /**
     * 本站未撤出且绑定档案的卡片。
     *
     * @param stationId 站主键
     * @param profileId 档案主键
     * @return 卡片，不存在为 null
     */
    AttackPersonDO selectActiveByStationAndProfile(@Param("stationId") Long stationId,
                                                   @Param("profileId") Long profileId);

    /**
     * 本站未撤出且同名的临时人员卡片。
     *
     * @param stationId   站主键
     * @param displayName 展示姓名
     * @return 卡片，不存在为 null
     */
    AttackPersonDO selectActiveTempByStationAndName(@Param("stationId") Long stationId,
                                                    @Param("displayName") String displayName);

    /**
     * 插入卡片。
     *
     * @param person 卡片
     * @return 影响行数
     */
    int insert(AttackPersonDO person);

    /**
     * 更新可变字段。
     *
     * @param person 卡片
     * @return 影响行数
     */
    int update(AttackPersonDO person);

    /**
     * 物理删除预录入卡片。
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
