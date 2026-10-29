package com.ccds.roster.roster.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.roster.roster.entity.ProfileTransferDO;

/**
 * 人员跨站调动申请访问。
 *
 * @author ccds
 * @since 0.1.2
 */
@Mapper
public interface ProfileTransferMapper {

    /**
     * 插入调动申请。
     *
     * @param transfer 申请
     * @return 影响行数
     */
    int insert(ProfileTransferDO transfer);

    /**
     * 按主键查申请。
     *
     * @param id 主键
     * @return 申请，不存在为 null
     */
    ProfileTransferDO selectById(@Param("id") Long id);

    /**
     * 按调出站与客户端请求号查申请（提交幂等）。
     *
     * @param fromStationId   调出站主键
     * @param clientRequestId 客户端请求号
     * @return 申请，不存在为 null
     */
    ProfileTransferDO selectByClientRequestId(@Param("fromStationId") Long fromStationId,
                                              @Param("clientRequestId") String clientRequestId);

    /**
     * 指定站的调动申请列表。
     *
     * @param stationIds 站主键列表，不得为空
     * @param status     状态，可空
     * @return 申请列表，不会为 null
     */
    List<ProfileTransferDO> selectByStations(@Param("stationIds") List<Long> stationIds,
                                             @Param("status") String status);

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
     * 落档后回填新档案号。
     *
     * @param id           主键
     * @param newArchiveNo 新档案号
     * @param targetProfileId 并卡目标人员主键，可空
     * @return 影响行数
     */
    int updateApplied(@Param("id") Long id,
                      @Param("newArchiveNo") String newArchiveNo,
                      @Param("targetProfileId") Long targetProfileId);
}
