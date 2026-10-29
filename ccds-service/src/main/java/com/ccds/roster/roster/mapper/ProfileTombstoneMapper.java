package com.ccds.roster.roster.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.roster.roster.entity.ProfileTombstoneDO;

/**
 * 人员删除记录（墓碑）访问。
 *
 * <p>删除记录只增不删，只有「解除」：按姓名误删只解姓名那一条，档案号与 NFC 记录保持生效。</p>
 *
 * @author ccds
 * @since 0.1.1
 */
@Mapper
public interface ProfileTombstoneMapper {

    /**
     * 写入删除记录，命中唯一键时忽略，保证重复迁移不报错。
     *
     * @param tombstone 删除记录
     * @return 实际写入条数，重复时为 0
     */
    int insertIgnore(ProfileTombstoneDO tombstone);

    /**
     * 本站删除记录条数。
     *
     * @param stationId 站主键
     * @return 条数
     */
    int countByStation(@Param("stationId") Long stationId);

    /**
     * 本站全部删除记录，用于迁移时判断已存在。
     *
     * @param stationId 站主键
     * @return 删除记录列表，不会为 null
     */
    List<ProfileTombstoneDO> selectByStationId(@Param("stationId") Long stationId);

    /**
     * 本站仍生效的删除记录。
     *
     * @param stationId 站主键
     * @return 删除记录列表，不会为 null
     */
    List<ProfileTombstoneDO> selectActiveByStationId(@Param("stationId") Long stationId);

    /**
     * 姓名删除记录是否仍生效。
     *
     * @param stationId 站主键
     * @param nameNorm  姓名归一值
     * @return 条数
     */
    int countActiveByName(@Param("stationId") Long stationId, @Param("nameNorm") String nameNorm);

    /**
     * NFC 删除记录是否仍生效。
     *
     * @param stationId 站主键
     * @param nfcNorm   NFC 归一值
     * @return 条数
     */
    int countActiveByNfc(@Param("stationId") Long stationId, @Param("nfcNorm") String nfcNorm);

    /**
     * 档案号删除记录是否仍生效。
     *
     * @param stationId 站主键
     * @param archiveNo 档案号
     * @return 条数
     */
    int countActiveByArchiveNo(@Param("stationId") Long stationId, @Param("archiveNo") String archiveNo);

    /**
     * 只解除姓名记录。
     *
     * @param stationId  站主键
     * @param nameNorm   姓名归一值
     * @param releasedBy 解除人账号主键
     * @return 影响行数
     */
    int releaseName(@Param("stationId") Long stationId,
                    @Param("nameNorm") String nameNorm,
                    @Param("releasedBy") Long releasedBy);

    /**
     * 只解除 NFC 记录（需用户显式确认）。
     *
     * @param stationId  站主键
     * @param nfcNorm    NFC 归一值
     * @param releasedBy 解除人账号主键
     * @return 影响行数
     */
    int releaseNfc(@Param("stationId") Long stationId,
                   @Param("nfcNorm") String nfcNorm,
                   @Param("releasedBy") Long releasedBy);

    /**
     * 按姓名或 NFC 全部解除（用户主动新建 / 导入显式恢复）。
     *
     * @param stationId  站主键
     * @param nameNorm   姓名归一值
     * @param nfcNorm    NFC 归一值，可空
     * @param releasedBy 解除人账号主键
     * @return 影响行数
     */
    int releaseAll(@Param("stationId") Long stationId,
                   @Param("nameNorm") String nameNorm,
                   @Param("nfcNorm") String nfcNorm,
                   @Param("releasedBy") Long releasedBy);
}
