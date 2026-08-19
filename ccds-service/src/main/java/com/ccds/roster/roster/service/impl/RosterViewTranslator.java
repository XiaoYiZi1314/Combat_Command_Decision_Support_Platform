package com.ccds.roster.roster.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ccds.infra.crypto.FieldCipherUtil;
import com.ccds.infra.crypto.SensitiveMaskUtil;
import com.ccds.org.org.entity.StationDO;
import com.ccds.roster.roster.constant.RosterRuleConstant;
import com.ccds.roster.roster.entity.BattleGroupDO;
import com.ccds.roster.roster.entity.BattleGroupMemberDO;
import com.ccds.roster.roster.entity.ProfileDO;
import com.ccds.roster.roster.entity.ScbaCalibrationDO;
import com.ccds.roster.roster.vo.BattleGroupVO;
import com.ccds.roster.roster.vo.GroupMemberVO;
import com.ccds.roster.roster.vo.ProfileVO;
import com.ccds.roster.roster.vo.ScbaCalibrationVO;
import com.ccds.roster.roster.vo.StationRosterVO;

import lombok.RequiredArgsConstructor;

/**
 * 花名册 DO 转 VO。电话明文仅可写账号可见。
 *
 * @author ccds
 * @since 0.1.0
 */
@Component
@RequiredArgsConstructor
public class RosterViewTranslator {

    private final FieldCipherUtil fieldCipherUtil;

    /**
     * 组装本站花名册。
     *
     * @param station   站
     * @param writable  是否可写
     * @param profiles  档案
     * @param groups    编组
     * @param members   成员
     * @return 花名册
     */
    public StationRosterVO toRoster(StationDO station, boolean writable, List<ProfileDO> profiles,
                                    List<BattleGroupDO> groups, List<BattleGroupMemberDO> members) {
        Map<Long, List<BattleGroupMemberDO>> membersByGroup = new LinkedHashMap<Long, List<BattleGroupMemberDO>>();
        for (BattleGroupMemberDO member : members) {
            membersByGroup.computeIfAbsent(member.getGroupId(), key -> new ArrayList<BattleGroupMemberDO>()).add(member);
        }
        List<ProfileVO> profileVos = new ArrayList<ProfileVO>();
        for (ProfileDO profile : profiles) {
            profileVos.add(toProfileVo(profile, station.getName(), writable));
        }
        List<BattleGroupVO> groupVos = new ArrayList<BattleGroupVO>();
        for (BattleGroupDO group : groups) {
            groupVos.add(toGroupVo(group, membersByGroup.getOrDefault(group.getId(), List.of())));
        }
        return StationRosterVO.builder()
                .stationId(station.getId())
                .stationName(station.getName())
                .writable(writable)
                .profiles(profileVos)
                .groups(groupVos)
                .build();
    }

    /**
     * 档案视图。
     *
     * @param profile     档案
     * @param stationName 站名
     * @param exposePhone 是否返回电话明文
     * @return 视图
     */
    public ProfileVO toProfileVo(ProfileDO profile, String stationName, boolean exposePhone) {
        String phone = decryptPhone(profile.getPhoneCipher());
        return ProfileVO.builder()
                .id(profile.getId())
                .stationId(profile.getStationId())
                .stationName(stationName)
                .name(profile.getName())
                .title(profile.getTitle())
                .rankName(profile.getRankName())
                .phone(exposePhone ? phone : null)
                .phoneMasked(SensitiveMaskUtil.maskPhone(phone))
                .cylType(profile.getCylType())
                .nfcTag(profile.getNfcTag())
                .heightCm(profile.getHeightCm())
                .weightKg(profile.getWeightKg())
                .age(profile.getAge())
                .morningPressure(profile.getMorningPressure())
                .calibrationCount(profile.getCalibrationCount() == null ? 0 : profile.getCalibrationCount())
                .gmtModified(profile.getGmtModified())
                .build();
    }

    /**
     * 标定列表。
     *
     * @param records 标定
     * @return 视图，不会为 null
     */
    public List<ScbaCalibrationVO> toCalibrationVos(List<ScbaCalibrationDO> records) {
        List<ScbaCalibrationVO> result = new ArrayList<ScbaCalibrationVO>();
        if (records == null) {
            return result;
        }
        for (ScbaCalibrationDO record : records) {
            result.add(ScbaCalibrationVO.builder()
                    .id(record.getId())
                    .pressure(record.getPressure())
                    .fullTimeSec(record.getFullTimeSec())
                    .cylType(record.getCylType())
                    .source(record.getSource())
                    .measuredAt(record.getMeasuredAt())
                    .build());
        }
        return result;
    }

    private BattleGroupVO toGroupVo(BattleGroupDO group, List<BattleGroupMemberDO> members) {
        List<GroupMemberVO> memberVos = new ArrayList<GroupMemberVO>();
        for (BattleGroupMemberDO member : members) {
            memberVos.add(GroupMemberVO.builder()
                    .profileId(member.getProfileId())
                    .name(member.getProfileName())
                    .roleInGroup(member.getRoleInGroup())
                    .build());
        }
        return BattleGroupVO.builder()
                .id(group.getId())
                .name(group.getName())
                .sortNo(group.getSortNo())
                .commandGroup(isCommandGroupName(group.getName()))
                .members(memberVos)
                .build();
    }

    private String decryptPhone(String cipher) {
        if (cipher == null || cipher.isBlank()) {
            return null;
        }
        return fieldCipherUtil.decrypt(cipher);
    }

    private boolean isCommandGroupName(String name) {
        String trimmed = name == null ? "" : name.trim();
        return RosterRuleConstant.COMMAND_GROUP_NAME.equals(trimmed)
                || RosterRuleConstant.COMMAND_UNIT_NAME.equals(trimmed);
    }
}
