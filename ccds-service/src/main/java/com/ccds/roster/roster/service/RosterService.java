package com.ccds.roster.roster.service;

import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.roster.roster.dto.GroupBatchSaveCommand;
import com.ccds.roster.roster.dto.ProfileSaveCommand;
import com.ccds.roster.roster.dto.ScbaCalibrationSaveCommand;
import com.ccds.roster.roster.vo.ProfileVO;
import com.ccds.roster.roster.vo.RosterImportResultVO;
import com.ccds.roster.roster.vo.StationRosterVO;

/**
 * 花名册与战斗编组。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface RosterService {

    /**
     * 本站花名册与编组。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @return 花名册
     */
    StationRosterVO getRoster(AuthPrincipal principal, Long stationId);

    /**
     * 档案详情，含空呼标定。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param profileId 档案主键
     * @return 档案
     */
    ProfileVO getProfile(AuthPrincipal principal, Long stationId, Long profileId);

    /**
     * 新建档案。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param command   档案
     * @return 新建结果
     */
    ProfileVO createProfile(AuthPrincipal principal, Long stationId, ProfileSaveCommand command);

    /**
     * 更新档案。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param profileId 档案主键
     * @param command   档案
     * @return 更新结果
     */
    ProfileVO updateProfile(AuthPrincipal principal, Long stationId, Long profileId, ProfileSaveCommand command);

    /**
     * 软删档案。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param profileId 档案主键
     */
    void deleteProfile(AuthPrincipal principal, Long stationId, Long profileId);

    /**
     * 保存个人空呼高强度标定。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param profileId 档案主键
     * @param command   标定数据
     * @return 更新后的档案详情
     */
    ProfileVO saveScbaCalibration(AuthPrincipal principal, Long stationId, Long profileId,
                                  ScbaCalibrationSaveCommand command);

    /**
     * 整存战斗编组。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param command   编组
     * @return 最新花名册
     */
    StationRosterVO saveGroups(AuthPrincipal principal, Long stationId, GroupBatchSaveCommand command);

    /**
     * 导入现网列模板。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param fileName  文件名
     * @param bytes     文件字节
     * @return 导入结果
     */
    RosterImportResultVO importProfiles(AuthPrincipal principal, Long stationId, String fileName, byte[] bytes);

    /**
     * 导出本站表格。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @return xlsx 字节
     */
    byte[] exportProfiles(AuthPrincipal principal, Long stationId);

    /**
     * 导出空模板。
     *
     * @return xlsx 字节
     */
    byte[] exportTemplate();
}
