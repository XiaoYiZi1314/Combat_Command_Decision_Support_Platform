package com.ccds.water.water.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.water.water.entity.WaterRequestDO;

/**
 * 水源变更申请访问。
 *
 * @author ccds
 * @since 0.1.2
 */
@Mapper
public interface WaterRequestMapper {

    /**
     * 插入申请。
     *
     * @param request 申请
     * @return 影响行数
     */
    int insert(WaterRequestDO request);

    /**
     * 按主键查申请。
     *
     * @param id 主键
     * @return 申请，不存在为 null
     */
    WaterRequestDO selectById(@Param("id") Long id);

    /**
     * 按站与客户端请求号查申请（提交幂等）。
     *
     * @param stationId       站主键
     * @param clientRequestId 客户端请求号
     * @return 申请，不存在为 null
     */
    WaterRequestDO selectByClientRequestId(@Param("stationId") Long stationId,
                                           @Param("clientRequestId") String clientRequestId);

    /**
     * 指定站的申请列表，status 为空查全部。
     *
     * @param stationIds 站主键列表，不得为空
     * @param status     状态，可空
     * @return 申请列表，不会为 null
     */
    List<WaterRequestDO> selectByStations(@Param("stationIds") List<Long> stationIds,
                                          @Param("status") String status);

    /**
     * 本站待审批水源编号集合（列表标「待审批」用）。
     *
     * @param stationId 站主键
     * @return 待审批申请列表，不会为 null
     */
    List<WaterRequestDO> selectPendingByStation(@Param("stationId") Long stationId);

    /**
     * CAS 审批：仅当状态仍为 pending 时更新。
     *
     * @param id           主键
     * @param fromStatus   期望当前状态
     * @param toStatus     目标状态
     * @param reviewedBy   审批人账号主键
     * @param reviewerName 审批人账号名
     * @param reviewReason 审批意见
     * @param reviewedAt   审批时间
     * @return 影响行数，0 表示已被处理
     */
    int reviewCas(@Param("id") Long id,
                  @Param("fromStatus") String fromStatus,
                  @Param("toStatus") String toStatus,
                  @Param("reviewedBy") Long reviewedBy,
                  @Param("reviewerName") String reviewerName,
                  @Param("reviewReason") String reviewReason,
                  @Param("reviewedAt") LocalDateTime reviewedAt);

    /**
     * 删除待审批申请（撤回）。
     *
     * @param id 主键
     * @return 影响行数
     */
    int deletePending(@Param("id") Long id);

    /**
     * 统计某站某时段审批通过的「报修→在用」维修数。
     *
     * @param stationIds 站主键列表，不得为空
     * @param from       起始时间（含）
     * @param to         结束时间（不含）
     * @return 按站分组的统计行，不会为 null
     */
    List<java.util.Map<String, Object>> countRepairFixedByStations(
            @Param("stationIds") List<Long> stationIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
