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
import com.ccds.infra.excel.SpreadsheetCodec;
import com.ccds.infra.excel.SpreadsheetTable;
import com.ccds.org.org.entity.StationDO;
import com.ccds.roster.roster.constant.RosterRuleConstant;
import com.ccds.roster.roster.dto.GroupBatchSaveCommand;
import com.ccds.roster.roster.dto.GroupMemberSaveCommand;
import com.ccds.roster.roster.dto.GroupSaveCommand;
import com.ccds.roster.roster.dto.ProfileSaveCommand;
import com.ccds.roster.roster.dto.ScbaCalibrationSaveCommand;
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
import com.ccds.roster.roster.vo.ProfileVO;
import com.ccds.roster.roster.vo.RosterImportResultVO;
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

    private static final String MSG_IN_ATTACK = "该人员有未撤出内攻卡片，请先撤出后再删";

    private static final Pattern NFC_TRIM = Pattern.compile("[\\s:：\\-_]");

    private final AccountMapper accountMapper;

    private final ProfileMapper profileMapper;

    private final BattleGroupMapper battleGroupMapper;

    private final BattleGroupMemberMapper battleGroupMemberMapper;

    private final ScbaCalibrationMapper scbaCalibrationMapper;

    private final RosterAccessService rosterAccessService;

    private final RosterViewTranslator rosterViewTranslator;

    private final FieldCipherUtil fieldCipherUtil;

    private final SpreadsheetCodec spreadsheetCodec;

    /**
     * {@inheritDoc}
     */
    @Override
    public StationRosterVO getRoster(AuthPrincipal principal, Long stationId) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireVisibleStation(account, stationId);
        boolean writable = rosterAccessService.isWritable(account, stationId);
        return buildRoster(station, writable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProfileVO getProfile(AuthPrincipal principal, Long stationId, Long profileId) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireVisibleStation(account, stationId);
        boolean writable = rosterAccessService.isWritable(account, stationId);
        ProfileDO profile = requireProfile(stationId, profileId);
        ProfileVO vo = rosterViewTranslator.toProfileVo(profile, station.getName(), writable);
        vo.setCalibrations(rosterViewTranslator.toCalibrationVos(scbaCalibrationMapper.selectByProfileId(profileId)));
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
        return rosterViewTranslator.toProfileVo(profileMapper.selectAliveById(profile.getId()), station.getName(), true);
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
        return rosterViewTranslator.toProfileVo(profileMapper.selectAliveById(profileId), station.getName(), true);
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
        if (profileMapper.countUnwithdrawnAttack(profileId) > 0) {
            throw new BizException(ErrorCodeConstant.ROSTER_PROFILE_IN_ATTACK, MSG_IN_ATTACK);
        }
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
    public ProfileVO saveScbaCalibration(AuthPrincipal principal, Long stationId, Long profileId,
                                         ScbaCalibrationSaveCommand command) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireWritableStation(account, stationId);
        ProfileDO profile = requireProfile(stationId, profileId);
        LocalDateTime now = LocalDateTime.now();
        ScbaCalibrationDO calibration = ScbaCalibrationDO.builder()
                .profileId(profileId)
                .pressure(command.getPressure())
                .fullTimeSec(command.getFullTimeSec())
                .cylType(CylTypeEnum.fromCodeOrDefault(command.getCylType()).getCode())
                .source(command.getSource() == null || command.getSource().isBlank()
                        ? RosterRuleConstant.SCBA_CALIBRATION_SOURCE_CALCULATOR
                        : command.getSource().trim())
                .measuredAt(now)
                .gmtCreate(now)
                .gmtModified(now)
                .build();
        scbaCalibrationMapper.insert(calibration);
        log.info("scba calibration saved stationId={} profileId={} calibrationId={}",
                stationId, profileId, calibration.getId());
        ProfileVO vo = rosterViewTranslator.toProfileVo(
                profileMapper.selectAliveById(profileId), station.getName(), true);
        vo.setCalibrations(rosterViewTranslator.toCalibrationVos(
                scbaCalibrationMapper.selectByProfileId(profileId)));
        return vo;
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
            log.warn("roster import parse failed stationId={}", stationId, ex);
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
        Map<Long, String> groupByProfile = RosterSpreadsheetSupport.firstGroupNameByProfile(roster.getGroups());
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(new ArrayList<String>(RosterSpreadsheetSupport.EXPORT_HEADERS));
        for (ProfileVO profile : roster.getProfiles()) {
            rows.add(RosterSpreadsheetSupport.toExportRow(profile, groupByProfile.get(profile.getId())));
        }
        if (rows.size() == 1) {
            rows.add(new ArrayList<String>(RosterSpreadsheetSupport.TEMPLATE_EXAMPLE));
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
        rows.add(new ArrayList<String>(RosterSpreadsheetSupport.EXPORT_HEADERS));
        rows.add(new ArrayList<String>(RosterSpreadsheetSupport.TEMPLATE_EXAMPLE));
        return spreadsheetCodec.writeXlsx(RosterRuleConstant.EXPORT_SHEET_NAME,
                SpreadsheetTable.builder().rows(rows).build());
    }

    private RosterImportResultVO applyImportRows(StationDO station, List<List<String>> rows) {
        Long stationId = station.getId();
        List<String> header = rows.get(0);
        int nameIdx = RosterSpreadsheetSupport.findColumn(header, RosterSpreadsheetSupport.HEADER_NAME, 0);
        int titleIdx = RosterSpreadsheetSupport.findColumn(header, RosterSpreadsheetSupport.HEADER_TITLE, 1);
        int rankIdx = RosterSpreadsheetSupport.findColumn(header, RosterSpreadsheetSupport.HEADER_RANK, 2);
        int phoneIdx = RosterSpreadsheetSupport.findColumn(header, RosterSpreadsheetSupport.HEADER_PHONE, -1);
        boolean hasPhone = phoneIdx >= 0;
        int cylFallback = hasPhone ? 4 : 3;
        int cylIdx = RosterSpreadsheetSupport.findColumn(header, RosterSpreadsheetSupport.HEADER_CYL, cylFallback);
        int nfcIdx = RosterSpreadsheetSupport.findColumn(header, RosterSpreadsheetSupport.HEADER_NFC, cylFallback + 1);
        int groupIdx = RosterSpreadsheetSupport.findColumn(
                header, RosterSpreadsheetSupport.HEADER_GROUP, cylFallback + 2);
        int heightIdx = RosterSpreadsheetSupport.findColumn(
                header, RosterSpreadsheetSupport.HEADER_HEIGHT, cylFallback + 3);
        int weightIdx = RosterSpreadsheetSupport.findColumn(
                header, RosterSpreadsheetSupport.HEADER_WEIGHT, cylFallback + 4);
        int ageIdx = RosterSpreadsheetSupport.findColumn(header, RosterSpreadsheetSupport.HEADER_AGE, cylFallback + 5);
        int morningIdx = RosterSpreadsheetSupport.findColumn(
                header, RosterSpreadsheetSupport.HEADER_MORNING, cylFallback + 6);
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
            String name = RosterSpreadsheetSupport.cell(cols, nameIdx);
            if (name.isEmpty() || RosterRuleConstant.TEMPLATE_EXAMPLE_NAME.equals(name)) {
                skipped += 1;
                continue;
            }
            ProfileSaveCommand command = ProfileSaveCommand.builder()
                    .name(name)
                    .title(RosterSpreadsheetSupport.cell(cols, titleIdx))
                    .rankName(RosterSpreadsheetSupport.cell(cols, rankIdx))
                    .phone(hasPhone ? RosterSpreadsheetSupport.cell(cols, phoneIdx) : null)
                    .cylType(RosterSpreadsheetSupport.cell(cols, cylIdx))
                    .nfcTag(RosterSpreadsheetSupport.cell(cols, nfcIdx))
                    .heightCm(RosterSpreadsheetSupport.parseInt(RosterSpreadsheetSupport.cell(cols, heightIdx)))
                    .weightKg(RosterSpreadsheetSupport.parseInt(RosterSpreadsheetSupport.cell(cols, weightIdx)))
                    .age(RosterSpreadsheetSupport.parseInt(RosterSpreadsheetSupport.cell(cols, ageIdx)))
                    .morningPressure(RosterSpreadsheetSupport.parseDecimal(
                            RosterSpreadsheetSupport.cell(cols, morningIdx)))
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
            String groupName = RosterSpreadsheetSupport.cell(cols, groupIdx);
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
        return rosterViewTranslator.toRoster(
                station,
                writable,
                profileMapper.selectAliveByStationId(station.getId()),
                battleGroupMapper.selectByStationId(station.getId()),
                battleGroupMemberMapper.selectByStationId(station.getId()));
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
}
