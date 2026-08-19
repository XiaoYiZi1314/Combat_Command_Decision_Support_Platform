package com.ccds.attack.attack.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.attack.attack.entity.StationSnapshotDO;

/**
 * 站快照访问。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface StationSnapshotMapper {

    /**
     * 按站查询。
     *
     * @param stationId 站主键
     * @return 快照，不存在为 null
     */
    StationSnapshotDO selectByStationId(@Param("stationId") Long stationId);

    /**
     * 插入快照。
     *
     * @param snapshot 快照
     * @return 影响行数
     */
    int insert(StationSnapshotDO snapshot);

    /**
     * 更新快照。
     *
     * @param snapshot 快照
     * @return 影响行数
     */
    int update(StationSnapshotDO snapshot);

    /**
     * 有未撤出内攻人员的站快照。
     *
     * @return 快照列表，不会为 null
     */
    List<StationSnapshotDO> selectActive();
}
