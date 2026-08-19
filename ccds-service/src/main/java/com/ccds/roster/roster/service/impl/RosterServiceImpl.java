package com.ccds.roster.roster.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.crypto.FieldCipherUtil;
import com.ccds.infra.crypto.SensitiveMaskUtil;
import com.ccds.infra.excel.SpreadsheetCodec;
import com.ccds.infra.excel.SpreadsheetTable;
import com.ccds.org.org.entity.StationDO;
import com.ccds.roster.roster.constant.RosterRuleConstant;
import com.ccds.roster.roster.dto.GroupBatchSaveCommand;
import com.ccds.roster.roster.dto.GroupMemberSaveCommand;
import com.ccds.roster.roster.dto.GroupSaveCommand;
import com.ccds.roster.roster.dto.ProfileSaveCommand;
import com.ccds.roster.roster.entity.BattleGroupDO;
import com.ccds.roster.roster.entity.BattleGroupMemberDO;
import com.ccds.roster.roster.entity.ProfileDO;
import com.ccds.roster.roster.entity.ScbaCalibrationDO;
import com.ccds.roster.roster.enums.CylTypeEnum;
import com.ccds.roster.roster.mapper.BattleGroupMapper;
import com.ccds.roster.roster.mapper.BattleGroupMemberMapper;
import com.ccds.roster.roster.mapper.ProfileMapper;
import com.ccds.roster.roster.mapper.ScbaCalibrationMapper;
import com.ccds.roster.roster.service.RosterAccessService;
import com.ccds.roster.roster.service.RosterService;
import com.ccds.roster.roster.vo.BattleGroupVO;
import com.ccds.roster.roster.vo.GroupMemberVO;
import com.ccds.roster.roster.vo.ProfileVO;
import com.ccds.roster.roster.vo.RosterImportResultVO;
import com.ccds.roster.roster.vo.ScbaCalibrationVO;
import com.ccds.roster.roster.vo.StationRosterVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 本站花名册、编组与现网列导入导出。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RosterServiceImpl implements RosterService {

    private static final String MSG_UNAUTHORIZED = "登录已失效，请重新登录";

    private static final String MSG_PROFILE_MISSING = "人员档案不存在";

    private static final String MSG_NAME_DUPLICATE = "本站已有同名人员";

    private static final String MSG_NFC_DUPLICATE = "本站 NFC 编号已占用";

    private static final String MSG_IMPORT_INVALID = "表格无法解析或没有可导入的数据";

    private static final String MSG_IMPORT_TOO_LARGE = "导入文件过大";

    private static final String MSG_NAME_REQUIRED = "姓名不能为空";

    private static final Pattern NFC_TRIM = Pattern.compile("[\\s:：\\-_]");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final String[] HEADER_NAME = {"姓名"};

    private static final String[] HEADER_TITLE = {"职务/角色", "职务", "角色"};

    private static final String[] HEADER_RANK = {"消防救援衔", "救援衔"};

    private static final String[] HEADER_PHONE = {"电话号", "电话", "手机号", "联系电话"};

    private static final String[] HEADER_CYL = {"气瓶规格", "气瓶"};

    private static final String[] HEADER_NFC = {"NFC标号", "NFC编号", "NFC", "卡号", "卡扣编号"};

    private static final String[] HEADER_GROUP = {"所属战斗编组", "战斗编组", "所属战斗小组", "战斗小组", "小组"};

    private static final String[] HEADER_HEIGHT = {"身高cm", "身高"};

    private static final String[] HEADER_WEIGHT = {"体重kg", "体重"};

    private static final String[] HEADER_AGE = {"年龄"};

    private static final String[] HEADER_MORNING = {"早检查空呼压力", "早检压力", "空呼压力"};

    private static final List<String> EXPORT_HEADERS = List.of(
            "姓名", "职务/角色", "消防救援衔", "电话号", "气瓶规格(6.8/9)", "NFC标号",
            "所属战斗编组", "身高cm", "体重kg", "年龄", "早检查空呼压力(MPa)");

    private static final List<String> TEMPLATE_EXAMPLE = List.of(
            "张三", "消防员", "二级消防士", "13800000000", "6.8", "04A1B2C3D4",
            "灭火1组", "175", "70", "28", "25.0");

    private final AccountMapper accountMapper;

    private final ProfileMapper profileMapper;

    private final BattleGroupMapper battleGroupMapper;

    private final BattleGroupMemberMapper battleGroupMemberMapper;

    private final ScbaCalibrationMapper scbaCalibrationMapper;

    private final RosterAccessService rosterAccessService;

    private final FieldCipherUtil fieldCipherUtil;

    private final SpreadsheetCodec spreadsheetCodec;

    /**
     * {@inheritDoc}
     */
    @Override
    public StationRosterVO getRoster(AuthPrincipal principal, Long stationId) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireVisibleStation(account, stationId);
        return buildRoster(station, rosterAccessService.isWritable(account, stationId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProfileVO getProfile(AuthPrincipal principal, Long stationId, Long profileId) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireVisibleStation(account, stationId);
        ProfileDO profile = requireProfile(stationId, profileId);
        ProfileVO vo = toProfileVo(profile, station.getName());
        vo.setCalibrations(toCalibrationVos(scbaCalibrationMapper.selectByProfileId(profileId)));
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProfileVO createProfile(AuthPrincipal principal, Long stationId, ProfileSaveCommand command) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireWritableStation(account, stationId);
        ProfileDO profile = new ProfileDO();
        profile.setStationId(stationId);
        applyCommand(profile, command, stationId, null, null, null);
        LocalDateTime now = LocalDateTime.now();
        profile.setGmtCreate(now);
        profile.setGmtModified(now);
        profileMapper.insert(profile);
        log.info("profile created stationId={} profileId={}", stationId, profile.getId());
        return toProfileVo(profileMapper.selectAliveById(profile.getId()), station.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProfileVO updateProfile(AuthPrincipal principal, Long stationId, Long profileId,
                                   ProfileSaveCommand command) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireWritableStation(account, stationId);
        ProfileDO profile = requireProfile(stationId, profileId);
        applyCommand(profile, command, stationId, profileId, null, null);
        profile.setGmtModified(LocalDateTime.now());
        profileMapper.update(profile);
        log.info("profile updated stationId={} profileId={}", stationId, profileId);
        return toProfileVo(profileMapper.selectAliveById(profileId), station.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProfile(AuthPrincipal principal, Long stationId, Long profileId) {
        AccountDO account = requireAccount(principal);
        rosterAccessService.requireWritableStation(account, stationId);
        requireProfile(stationId, profileId);
        LocalDateTime now = LocalDateTime.now();
        battleGroupMemberMapper.deleteByProfileId(profileId);
        profileMapper.softDelete(profileId, now, now);
        log.info("profile deleted stationId={} profileId={}", stationId, profileId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StationRosterVO saveGroups(AuthPrincipal principal, Long stationId, GroupBatchSaveCommand command) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireWritableStation(account, stationId);
        List<GroupSaveCommand> incoming = command.getGroups() == null
                ? new ArrayList<GroupSaveCommand>()
                : command.getGroups();
        List<BattleGroupDO> existing = battleGroupMapper.selectByStationId(stationId);
        List<BattleGroupMemberDO> existingMembers = battleGroupMemberMapper.selectByStationId(stationId);
        boolean incomingHasCommand = hasCommandGroup(incoming);
        replaceGroups(stationId, incoming);
        if (!incomingHasCommand) {
            restoreCommandGroups(stationId, existing, existingMembers);
        }
        log.info("groups saved stationId={} groupCount={}", stationId, incoming.size());
        return buildRoster(station, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RosterImportResultVO importProfiles(AuthPrincipal principal, Long stationId, String fileName, byte[] bytes) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireWritableStation(account, stationId);
        if (bytes == null || bytes.length == 0 || bytes.length > RosterRuleConstant.IMPORT_MAX_BYTES) {
            throw new BizException(ErrorCodeConstant.ROSTER_IMPORT_INVALID,
                    bytes != null && bytes.length > RosterRuleConstant.IMPORT_MAX_BYTES
                            ? MSG_IMPORT_TOO_LARGE : MSG_IMPORT_INVALID);
        }
        SpreadsheetTable table;
        try {
            table = spreadsheetCodec.parse(fileName, bytes);
        } catch (RuntimeException ex) {
            log.warn("roster import parse failed stationId={}", stationId);
            throw new BizException(ErrorCodeConstant.ROSTER_IMPORT_INVALID, MSG_IMPORT_INVALID);
        }
        List<List<String>> rows = table.getRows();
        if (rows == null || rows.size() < 2 || rows.size() > RosterRuleConstant.IMPORT_MAX_ROWS) {
            throw new BizException(ErrorCodeConstant.ROSTER_IMPORT_INVALID, MSG_IMPORT_INVALID);
        }
        return applyImportRows(station, rows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] exportProfiles(AuthPrincipal principal, Long stationId) {
        StationRosterVO roster = getRoster(principal, stationId);
        Map<Long, String> groupByProfile = firstGroupNameByProfile(roster.getGroups());
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(new ArrayList<String>(EXPORT_HEADERS));
        for (ProfileVO profile : roster.getProfiles()) {
            rows.add(toExportRow(profile, groupByProfile.get(profile.getId())));
        }
        if (rows.size() == 1) {
            rows.add(new ArrayList<String>(TEMPLATE_EXAMPLE));
        }
        return spreadsheetCodec.writeXlsx(RosterRuleConstant.EXPORT_SHEET_NAME,
                SpreadsheetTable.builder().rows(rows).build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] exportTemplate() {
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(new ArrayList<String>(EXPORT_HEADERS));
        rows.add(new ArrayList<String>(TEMPLATE_EXAMPLE));
        return spreadsheetCodec.writeXlsx(RosterRuleConstant.EXPORT_SHEET_NAME,
                SpreadsheetTable.builder().rows(rows).build());
    }

    private RosterImportResultVO applyImportRows(StationDO station, List<List<String>> rows) {
        Long stationId = station.getId();
        List<String> header = rows.get(0);
        int nameIdx = findColumn(header, HEADER_NAME, 0);
        int titleIdx = findColumn(header, HEADER_TITLE, 1);
        int rankIdx = findColumn(header, HEADER_RANK, 2);
        int phoneIdx = findColumn(header, HEADER_PHONE, -1);
        boolean hasPhone = phoneIdx >= 0;
        int cylIdx = findColumn(header, HEADER_CYL, hasPhone ? 4 : 3);
        int nfcIdx = findColumn(header, HEADER_NFC, hasPhone ? 5 : 4);
        int groupIdx = findColumn(header, HEADER_GROUP, hasPhone ? 6 : 5);
        int heightIdx = findColumn(header, HEADER_HEIGHT, hasPhone ? 7 : 6);
        int weightIdx = findColumn(header, HEADER_WEIGHT, hasPhone ? 8 : 7);
        int ageIdx = findColumn(header, HEADER_AGE, hasPhone ? 9 : 8);
        int morningIdx = findColumn(header, HEADER_MORNING, hasPhone ? 10 : 9);
        int added = 0;
        int updated = 0;
        int skipped = 0;
        Map<String, ProfileDO> profileByName = loadProfileNameMap(stationId);
        Map<String, ProfileDO> profileByNfc = loadProfileNfcMap(profileByName);
        List<BattleGroupDO> groups = battleGroupMapper.selectByStationId(stationId);
        Map<String, BattleGroupDO> groupByName = new LinkedHashMap<String, BattleGroupDO>();
        for (BattleGroupDO group : groups) {
            groupByName.put(group.getName(), group);
        }
        List<BattleGroupMemberDO> members = battleGroupMemberMapper.selectByStationId(stationId);
        int addedGroup = 0;
        boolean importedCommand = false;
        for (int i = 1; i < rows.size(); i++) {
            List<String> cols = rows.get(i);
            String name = cell(cols, nameIdx);
            if (name.isEmpty() || RosterRuleConstant.TEMPLATE_EXAMPLE_NAME.equals(name)) {
                skipped += 1;
                continue;
            }
            ProfileSaveCommand command = ProfileSaveCommand.builder()
                    .name(name)
                    .title(cell(cols, titleIdx))
                    .rankName(cell(cols, rankIdx))
                    .phone(hasPhone ? cell(cols, phoneIdx) : null)
                    .cylType(cell(cols, cylIdx))
                    .nfcTag(cell(cols, nfcIdx))
                    .heightCm(parseInt(cell(cols, heightIdx)))
                    .weightKg(parseInt(cell(cols, weightIdx)))
                    .age(parseInt(cell(cols, ageIdx)))
                    .morningPressure(parseDecimal(cell(cols, morningIdx)))
                    .build();
            ProfileDO existing = profileByName.get(name);
            if (existing == null) {
                ProfileDO created = new ProfileDO();
                created.setStationId(stationId);
                applyCommand(created, command, stationId, null, profileByName, profileByNfc);
                LocalDateTime now = LocalDateTime.now();
                created.setGmtCreate(now);
                created.setGmtModified(now);
                profileMapper.insert(created);
                existing = created;
                profileByName.put(name, created);
                if (created.getNfcTag() != null) {
                    profileByNfc.put(created.getNfcTag(), created);
                }
                added += 1;
            } else {
                mergeImport(existing, command, profileByNfc);
                existing.setGmtModified(LocalDateTime.now());
                profileMapper.update(existing);
                if (existing.getNfcTag() != null) {
                    profileByNfc.put(existing.getNfcTag(), existing);
                }
                updated += 1;
            }
            String groupName = cell(cols, groupIdx);
            if (!groupName.isEmpty()) {
                if (isCommandGroupName(groupName)) {
                    importedCommand = true;
                }
                BattleGroupDO group = groupByName.get(groupName);
                if (group == null) {
                    group = insertGroup(stationId, groupName, groups.size() + 1);
                    groups.add(group);
                    groupByName.put(groupName, group);
                    addedGroup += 1;
                }
                addMemberIfAbsent(group, existing, members);
            }
        }
        log.info("roster imported stationId={} added={} updated={} skipped={} groups={}",
                stationId, added, updated, skipped, addedGroup);
        return RosterImportResultVO.builder()
                .addedCount(added)
                .updatedCount(updated)
                .skippedCount(skipped)
                .addedGroupCount(addedGroup)
                .commandGroupPreserved(!importedCommand && hasExistingCommand(groups))
                .build();
    }

    private void mergeImport(ProfileDO existing, ProfileSaveCommand command, Map<String, ProfileDO> profileByNfc) {
        if (notBlank(command.getTitle())) {
            existing.setTitle(trimToNull(command.getTitle()));
        }
        if (notBlank(command.getRankName())) {
            existing.setRankName(trimToNull(command.getRankName()));
        }
        if (notBlank(command.getPhone())) {
            existing.setPhoneCipher(fieldCipherUtil.encrypt(command.getPhone().trim()));
        }
        if (notBlank(command.getCylType())) {
            existing.setCylType(CylTypeEnum.fromCodeOrDefault(command.getCylType()).getCode());
        }
        String nfc = normalizeNfc(command.getNfcTag());
        if (nfc != null) {
            assertNfcUnique(existing.getStationId(), nfc, existing.getId(), profileByNfc);
            existing.setNfcTag(nfc);
        }
        if (command.getHeightCm() != null && command.getHeightCm() > 0) {
            existing.setHeightCm(command.getHeightCm());
        }
        if (command.getWeightKg() != null && command.getWeightKg() > 0) {
            existing.setWeightKg(command.getWeightKg());
        }
        if (command.getAge() != null && command.getAge() > 0) {
            existing.setAge(command.getAge());
        }
        if (command.getMorningPressure() != null
                && command.getMorningPressure().compareTo(BigDecimal.ZERO) > 0) {
            existing.setMorningPressure(command.getMorningPressure());
        }
    }

    private void applyCommand(ProfileDO profile, ProfileSaveCommand command, Long stationId, Long currentId,
                              Map<String, ProfileDO> profileByName, Map<String, ProfileDO> profileByNfc) {
        String name = command.getName() == null ? "" : command.getName().trim();
        if (name.isEmpty()) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_NAME_REQUIRED);
        }
        ProfileDO sameName = profileByName == null
                ? profileMapper.selectAliveByStationAndName(stationId, name)
                : profileByName.get(name);
        if (sameName != null && (currentId == null || !sameName.getId().equals(currentId))) {
            throw new BizException(ErrorCodeConstant.ROSTER_NAME_DUPLICATE, MSG_NAME_DUPLICATE);
        }
        String nfc = normalizeNfc(command.getNfcTag());
        assertNfcUnique(stationId, nfc, currentId, profileByNfc);
        profile.setName(name);
        profile.setTitle(trimToNull(command.getTitle()));
        profile.setRankName(trimToNull(command.getRankName()));
        profile.setPhoneCipher(fieldCipherUtil.encrypt(trimToNull(command.getPhone())));
        profile.setCylType(CylTypeEnum.fromCodeOrDefault(command.getCylType()).getCode());
        profile.setNfcTag(nfc);
        profile.setHeightCm(zeroToNull(command.getHeightCm()));
        profile.setWeightKg(zeroToNull(command.getWeightKg()));
        profile.setAge(zeroToNull(command.getAge()));
        profile.setMorningPressure(zeroToNull(command.getMorningPressure()));
    }

    private void assertNfcUnique(Long stationId, String nfc, Long currentId, Map<String, ProfileDO> profileByNfc) {
        if (nfc == null) {
            return;
        }
        ProfileDO sameNfc = profileByNfc == null
                ? profileMapper.selectAliveByStationAndNfc(stationId, nfc)
                : profileByNfc.get(nfc);
        if (sameNfc != null && (currentId == null || !sameNfc.getId().equals(currentId))) {
            throw new BizException(ErrorCodeConstant.ROSTER_NFC_DUPLICATE, MSG_NFC_DUPLICATE);
        }
    }

    private void replaceGroups(Long stationId, List<GroupSaveCommand> incoming) {
        battleGroupMemberMapper.deleteByStationId(stationId);
        battleGroupMapper.deleteByStationId(stationId);
        Map<Long, ProfileDO> profiles = loadProfileMap(stationId);
        int sort = 1;
        LocalDateTime now = LocalDateTime.now();
        for (GroupSaveCommand item : incoming) {
            if (item == null || item.getName() == null || item.getName().isBlank()) {
                continue;
            }
            BattleGroupDO group = BattleGroupDO.builder()
                    .stationId(stationId)
                    .name(item.getName().trim())
                    .sortNo(item.getSortNo() == null ? sort : item.getSortNo())
                    .gmtCreate(now)
                    .gmtModified(now)
                    .build();
            battleGroupMapper.insert(group);
            insertMembers(group.getId(), item.getMembers(), profiles, now);
            sort += 1;
        }
    }

    private void restoreCommandGroups(Long stationId, List<BattleGroupDO> existing,
                                      List<BattleGroupMemberDO> existingMembers) {
        Map<Long, List<BattleGroupMemberDO>> byGroup = new LinkedHashMap<Long, List<BattleGroupMemberDO>>();
        for (BattleGroupMemberDO member : existingMembers) {
            byGroup.computeIfAbsent(member.getGroupId(), key -> new ArrayList<BattleGroupMemberDO>()).add(member);
        }
        int sort = battleGroupMapper.selectByStationId(stationId).size() + 1;
        LocalDateTime now = LocalDateTime.now();
        for (BattleGroupDO group : existing) {
            if (!isCommandGroupName(group.getName())) {
                continue;
            }
            BattleGroupDO restored = BattleGroupDO.builder()
                    .stationId(stationId)
                    .name(group.getName())
                    .sortNo(sort)
                    .gmtCreate(now)
                    .gmtModified(now)
                    .build();
            battleGroupMapper.insert(restored);
            List<BattleGroupMemberDO> members = byGroup.getOrDefault(group.getId(), List.of());
            for (BattleGroupMemberDO member : members) {
                battleGroupMemberMapper.insert(BattleGroupMemberDO.builder()
                        .groupId(restored.getId())
                        .profileId(member.getProfileId())
                        .roleInGroup(member.getRoleInGroup())
                        .gmtCreate(now)
                        .gmtModified(now)
                        .build());
            }
            sort += 1;
        }
    }

    private void insertMembers(Long groupId, List<GroupMemberSaveCommand> members,
                               Map<Long, ProfileDO> profiles, LocalDateTime now) {
        if (members == null) {
            return;
        }
        List<Long> seen = new ArrayList<Long>();
        for (GroupMemberSaveCommand member : members) {
            if (member == null || member.getProfileId() == null || seen.contains(member.getProfileId())) {
                continue;
            }
            if (!profiles.containsKey(member.getProfileId())) {
                continue;
            }
            seen.add(member.getProfileId());
            battleGroupMemberMapper.insert(BattleGroupMemberDO.builder()
                    .groupId(groupId)
                    .profileId(member.getProfileId())
                    .roleInGroup(trimToNull(member.getRoleInGroup()))
                    .gmtCreate(now)
                    .gmtModified(now)
                    .build());
        }
    }

    private BattleGroupDO insertGroup(Long stationId, String name, int sortNo) {
        LocalDateTime now = LocalDateTime.now();
        BattleGroupDO group = BattleGroupDO.builder()
                .stationId(stationId)
                .name(name)
                .sortNo(sortNo)
                .gmtCreate(now)
                .gmtModified(now)
                .build();
        battleGroupMapper.insert(group);
        return group;
    }

    private void addMemberIfAbsent(BattleGroupDO group, ProfileDO profile, List<BattleGroupMemberDO> members) {
        for (BattleGroupMemberDO member : members) {
            if (member.getGroupId().equals(group.getId()) && member.getProfileId().equals(profile.getId())) {
                return;
            }
        }
        LocalDateTime now = LocalDateTime.now();
        BattleGroupMemberDO created = BattleGroupMemberDO.builder()
                .groupId(group.getId())
                .profileId(profile.getId())
                .gmtCreate(now)
                .gmtModified(now)
                .build();
        battleGroupMemberMapper.insert(created);
        members.add(created);
    }

    private StationRosterVO buildRoster(StationDO station, boolean writable) {
        List<ProfileDO> profiles = profileMapper.selectAliveByStationId(station.getId());
        List<BattleGroupDO> groups = battleGroupMapper.selectByStationId(station.getId());
        List<BattleGroupMemberDO> members = battleGroupMemberMapper.selectByStationId(station.getId());
        Map<Long, List<BattleGroupMemberDO>> membersByGroup = new LinkedHashMap<Long, List<BattleGroupMemberDO>>();
        for (BattleGroupMemberDO member : members) {
            membersByGroup.computeIfAbsent(member.getGroupId(), key -> new ArrayList<BattleGroupMemberDO>()).add(member);
        }
        List<ProfileVO> profileVos = new ArrayList<ProfileVO>();
        for (ProfileDO profile : profiles) {
            profileVos.add(toProfileVo(profile, station.getName()));
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

    private ProfileVO toProfileVo(ProfileDO profile, String stationName) {
        String phone = decryptPhone(profile.getPhoneCipher());
        return ProfileVO.builder()
                .id(profile.getId())
                .stationId(profile.getStationId())
                .stationName(stationName)
                .name(profile.getName())
                .title(profile.getTitle())
                .rankName(profile.getRankName())
                .phone(phone)
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

    private List<ScbaCalibrationVO> toCalibrationVos(List<ScbaCalibrationDO> records) {
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

    private List<String> toExportRow(ProfileVO profile, String groupName) {
        List<String> row = new ArrayList<String>();
        row.add(nullToEmpty(profile.getName()));
        row.add(nullToEmpty(profile.getTitle()));
        row.add(nullToEmpty(profile.getRankName()));
        row.add(nullToEmpty(profile.getPhone()));
        row.add(nullToEmpty(profile.getCylType()));
        row.add(nullToEmpty(profile.getNfcTag()));
        row.add(nullToEmpty(groupName));
        row.add(profile.getHeightCm() == null ? "" : String.valueOf(profile.getHeightCm()));
        row.add(profile.getWeightKg() == null ? "" : String.valueOf(profile.getWeightKg()));
        row.add(profile.getAge() == null ? "" : String.valueOf(profile.getAge()));
        row.add(profile.getMorningPressure() == null ? "" : profile.getMorningPressure().toPlainString());
        return row;
    }

    private Map<Long, String> firstGroupNameByProfile(List<BattleGroupVO> groups) {
        Map<Long, String> map = new LinkedHashMap<Long, String>();
        if (groups == null) {
            return map;
        }
        for (BattleGroupVO group : groups) {
            if (group.getMembers() == null) {
                continue;
            }
            for (GroupMemberVO member : group.getMembers()) {
                map.putIfAbsent(member.getProfileId(), group.getName());
            }
        }
        return map;
    }

    private Map<Long, ProfileDO> loadProfileMap(Long stationId) {
        Map<Long, ProfileDO> map = new LinkedHashMap<Long, ProfileDO>();
        for (ProfileDO profile : profileMapper.selectAliveByStationId(stationId)) {
            map.put(profile.getId(), profile);
        }
        return map;
    }

    private Map<String, ProfileDO> loadProfileNameMap(Long stationId) {
        Map<String, ProfileDO> map = new LinkedHashMap<String, ProfileDO>();
        for (ProfileDO profile : profileMapper.selectAliveByStationId(stationId)) {
            map.put(profile.getName(), profile);
        }
        return map;
    }

    private Map<String, ProfileDO> loadProfileNfcMap(Map<String, ProfileDO> profileByName) {
        Map<String, ProfileDO> map = new LinkedHashMap<String, ProfileDO>();
        for (ProfileDO profile : profileByName.values()) {
            if (profile.getNfcTag() != null && !profile.getNfcTag().isBlank()) {
                map.put(profile.getNfcTag(), profile);
            }
        }
        return map;
    }

    private AccountDO requireAccount(AuthPrincipal principal) {
        AccountDO account = accountMapper.selectById(principal.getAccountId());
        if (account == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        return account;
    }

    private ProfileDO requireProfile(Long stationId, Long profileId) {
        ProfileDO profile = profileMapper.selectAliveById(profileId);
        if (profile == null || !stationId.equals(profile.getStationId())) {
            throw new BizException(ErrorCodeConstant.ROSTER_PROFILE_NOT_FOUND, MSG_PROFILE_MISSING);
        }
        return profile;
    }

    private String decryptPhone(String cipher) {
        if (cipher == null || cipher.isBlank()) {
            return null;
        }
        return fieldCipherUtil.decrypt(cipher);
    }

    private String normalizeNfc(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = NFC_TRIM.matcher(raw.trim()).replaceAll("").toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isCommandGroupName(String name) {
        String trimmed = name == null ? "" : name.trim();
        return RosterRuleConstant.COMMAND_GROUP_NAME.equals(trimmed)
                || RosterRuleConstant.COMMAND_UNIT_NAME.equals(trimmed);
    }

    private boolean hasCommandGroup(List<GroupSaveCommand> groups) {
        for (GroupSaveCommand group : groups) {
            if (group != null && isCommandGroupName(group.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExistingCommand(List<BattleGroupDO> groups) {
        for (BattleGroupDO group : groups) {
            if (isCommandGroupName(group.getName())) {
                return true;
            }
        }
        return false;
    }

    private int findColumn(List<String> header, String[] names, int fallback) {
        if (header == null) {
            return fallback;
        }
        for (int i = 0; i < header.size(); i++) {
            String cell = compact(header.get(i)).toLowerCase(Locale.ROOT);
            for (String name : names) {
                String key = compact(name).toLowerCase(Locale.ROOT);
                if (cell.equals(key) || cell.contains(key)) {
                    return i;
                }
            }
        }
        return fallback;
    }

    private String cell(List<String> cols, int index) {
        if (cols == null || index < 0 || index >= cols.size() || cols.get(index) == null) {
            return "";
        }
        return cols.get(index).trim();
    }

    private Integer parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim()).intValue();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String trimToNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    private Integer zeroToNull(Integer value) {
        return value == null || value <= 0 ? null : value;
    }

    private BigDecimal zeroToNull(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0 ? null : value;
    }

    private boolean notBlank(String raw) {
        return raw != null && !raw.isBlank();
    }

    private String compact(String raw) {
        return raw == null ? "" : WHITESPACE.matcher(raw).replaceAll("");
    }

    private String nullToEmpty(String raw) {
        return raw == null ? "" : raw;
    }
}
