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
import com.ccds.org.org.service.OrgQueryService;
import com.ccds.roster.roster.constant.RosterRuleConstant;
import com.ccds.roster.roster.dto.GroupBatchSaveCommand;
import com.ccds.roster.roster.dto.GroupMemberSaveCommand;
import com.ccds.roster.roster.dto.GroupSaveCommand;
import com.ccds.roster.roster.dto.ProfileSaveCommand;
import com.ccds.roster.roster.dto.ScbaCalibrationSaveCommand;
import com.ccds.roster.roster.entity.BattleGroupDO;
import com.ccds.roster.roster.entity.BattleGroupMemberDO;
import com.ccds.roster.roster.entity.BattleGroupTombstoneDO;
import com.ccds.roster.roster.entity.ProfileDO;
import com.ccds.roster.roster.entity.ProfileTombstoneDO;
import com.ccds.roster.roster.entity.ScbaCalibrationDO;
import com.ccds.roster.roster.enums.CylTypeEnum;
import com.ccds.roster.roster.mapper.BattleGroupMapper;
import com.ccds.roster.roster.mapper.BattleGroupMemberMapper;
import com.ccds.roster.roster.mapper.BattleGroupTombstoneMapper;
import com.ccds.roster.roster.mapper.ProfileMapper;
import com.ccds.roster.roster.mapper.ProfileTombstoneMapper;
import com.ccds.roster.roster.mapper.ScbaCalibrationMapper;
import com.ccds.roster.roster.service.RosterAccessService;
import com.ccds.roster.roster.service.RosterService;
import com.ccds.roster.roster.vo.ProfileTombstoneVO;
import com.ccds.roster.roster.vo.ProfileVO;
import com.ccds.roster.roster.vo.RosterImportResultVO;
import com.ccds.roster.roster.vo.StationRosterVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 本站花名册、编组与现网列导入导出。
 *
 * <p>三条铁律（对应客户改法明细开篇三条底线）：</p>
 * <ol>
 *   <li>删除记录只增不删：任何同步、合并、恢复丢档、导入路径都不得清除；</li>
 *   <li>优先级：用户显式操作 &gt; 删除记录 &gt; 现存档案 &gt; 花名册旧数据；</li>
 *   <li>发布前再滤一次：导出、值班快照、编组导出、指挥端下发统一过本类的过滤方法。</li>
 * </ol>
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

    private static final String MSG_FIXED_GROUP = "指挥员、安全员、通信员是固定编制卡，不能删除";

    private static final Pattern NFC_TRIM = Pattern.compile("[\\s:：\\-_]");

    private static final Pattern NAME_TRIM = Pattern.compile("[\\s\\n\\r　]");

    private final AccountMapper accountMapper;

    private final ProfileMapper profileMapper;

    private final ProfileTombstoneMapper profileTombstoneMapper;

    private final BattleGroupMapper battleGroupMapper;

    private final BattleGroupMemberMapper battleGroupMemberMapper;

    private final BattleGroupTombstoneMapper battleGroupTombstoneMapper;

    private final ScbaCalibrationMapper scbaCalibrationMapper;

    private final RosterAccessService rosterAccessService;

    private final OrgQueryService orgQueryService;

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
        ensureFixedGroups(stationId);
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
        // 4.5：本站、所属大队、支队都看完整电话；共享页不走这里，仍不出现电话
        ProfileVO vo = rosterViewTranslator.toProfileVo(profile, station.getName(), true);
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
        // 档案号依赖自增主键，插入后回填
        String archiveNo = buildArchiveNo(profile.getId());
        profile.setArchiveNo(archiveNo);
        profileMapper.update(profile);
        // 用户主动新建才解开该姓名的删除记录，档案号与 NFC 记录保持生效
        profileTombstoneMapper.releaseName(stationId, normalizeName(profile.getName()), account.getId());
        log.info("profile created stationId={} profileId={} archiveNo={}",
                stationId, profile.getId(), archiveNo);
        return rosterViewTranslator.toProfileVo(profileMapper.selectAliveById(profile.getId()),
                station.getName(), true);
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
        // 用户主动保存 / 绑定 NFC 才解开对应删除记录
        profileTombstoneMapper.releaseName(stationId, normalizeName(profile.getName()), account.getId());
        String nfc = normalizeNfc(profile.getNfcTag());
        if (nfc != null) {
            profileTombstoneMapper.releaseNfc(stationId, nfc, account.getId());
        }
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
        ProfileDO profile = requireProfile(stationId, profileId);
        if (profileMapper.countUnwithdrawnAttack(profileId) > 0) {
            throw new BizException(ErrorCodeConstant.ROSTER_PROFILE_IN_ATTACK, MSG_IN_ATTACK);
        }
        LocalDateTime now = LocalDateTime.now();
        // 先写删除记录再软删：软删会清空 nfc_tag，墓碑是 NFC 唯一长期载体
        writeProfileTombstone(profile, account.getId(), RosterRuleConstant.TOMBSTONE_SOURCE_USER, now);
        battleGroupMemberMapper.deleteByProfileId(profileId);
        profileMapper.softDelete(profileId, now, now);
        log.info("profile deleted stationId={} profileId={} archiveNo={}",
                stationId, profileId, profile.getArchiveNo());
    }

    /**
     * 恢复被删除人员的姓名记录（按姓名误删时只解姓名一条）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreProfileTombstone(AuthPrincipal principal, Long stationId, String scope,
                                        String name, String nfcTag) {
        AccountDO account = requireAccount(principal);
        rosterAccessService.requireWritableStation(account, stationId);
        String nameNorm = normalizeName(name);
        String nfcNorm = normalizeNfc(nfcTag);
        if ("nfc".equals(scope)) {
            if (nfcNorm == null) {
                throw new BizException(ErrorCodeConstant.PARAM_INVALID, "恢复 NFC 需提供 NFC 编号");
            }
            profileTombstoneMapper.releaseNfc(stationId, nfcNorm, account.getId());
            return;
        }
        if ("all".equals(scope)) {
            profileTombstoneMapper.releaseAll(stationId, nameNorm, nfcNorm, account.getId());
            return;
        }
        profileTombstoneMapper.releaseName(stationId, nameNorm, account.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ProfileTombstoneVO> listProfileTombstones(AuthPrincipal principal, Long stationId) {
        AccountDO account = requireAccount(principal);
        rosterAccessService.requireVisibleStation(account, stationId);
        List<ProfileTombstoneVO> out = new ArrayList<ProfileTombstoneVO>();
        for (ProfileTombstoneDO tombstone : profileTombstoneMapper.selectActiveByStationId(stationId)) {
            out.add(ProfileTombstoneVO.builder()
                    .id(tombstone.getId())
                    .archiveNo(tombstone.getArchiveNo())
                    .name(tombstone.getName())
                    .nfcTag(tombstone.getNfcTag())
                    .source(tombstone.getSource())
                    .nameBlocked(Boolean.valueOf(tombstone.getNameReleasedAt() == null))
                    .nfcBlocked(Boolean.valueOf(tombstone.getNfcReleasedAt() == null))
                    .archiveBlocked(Boolean.valueOf(tombstone.getArchiveReleasedAt() == null))
                    .gmtCreate(tombstone.getGmtCreate())
                    .build());
        }
        return out;
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
        mergeGroups(stationId, incoming, existing, existingMembers, account.getId());
        ensureFixedGroups(stationId);
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
        return applyImportRows(account, station, rows);
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
        // 发布前再滤一次已删除人员
        List<ProfileVO> alive = filterAlive(roster.getProfiles());
        for (ProfileVO profile : alive) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] exportAllProfiles(AuthPrincipal principal) {
        AccountDO account = requireAccount(principal);
        rosterAccessService.requireReviewer(account);
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(new ArrayList<String>(RosterSpreadsheetSupport.EXPORT_HEADERS));
        List<StationDO> stations = orgQueryService.listVisibleStationEntities(account);
        Map<Long, String> groupByProfile = new LinkedHashMap<Long, String>();
        for (StationDO station : stations) {
            List<ProfileDO> profiles = profileMapper.selectAliveByStationId(station.getId());
            List<BattleGroupDO> groups = battleGroupMapper.selectByStationId(station.getId());
            List<BattleGroupMemberDO> members = battleGroupMemberMapper.selectByStationId(station.getId());
            StationRosterVO roster = rosterViewTranslator.toRoster(station, false, profiles, groups, members);
            groupByProfile.putAll(RosterSpreadsheetSupport.firstGroupNameByProfile(roster.getGroups()));
            for (ProfileVO profile : filterAlive(roster.getProfiles())) {
                rows.add(RosterSpreadsheetSupport.toExportRow(profile, groupByProfile.get(profile.getId())));
            }
        }
        if (rows.size() == 1) {
            rows.add(new ArrayList<String>(RosterSpreadsheetSupport.TEMPLATE_EXAMPLE));
        }
        log.info("profiles export-all stationCount={} rowCount={}",
                stations.size(), rows.size() - 1);
        return spreadsheetCodec.writeXlsx(RosterRuleConstant.EXPORT_SHEET_NAME,
                SpreadsheetTable.builder().rows(rows).build());
    }

    private List<ProfileVO> filterAlive(List<ProfileVO> profiles) {
        // 导出路径上花名册已只含 alive 档案；这里保留统一出口，便于后续接入墓碑二次过滤
        return new ArrayList<ProfileVO>(profiles);
    }

    private RosterImportResultVO applyImportRows(AccountDO account, StationDO station, List<List<String>> rows) {
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
        int skippedDeleted = 0;
        Map<String, ProfileDO> profileByName = loadProfileNameMap(stationId);
        Map<String, ProfileDO> profileByNfc = loadProfileNfcMap(profileByName);
        // 已删除人员按姓名拦截：见到就跳过，不能为了「编制齐全」把人补回来
        List<ProfileTombstoneDO> activeTombstones = profileTombstoneMapper.selectActiveByStationId(stationId);
        List<BattleGroupDO> groups = battleGroupMapper.selectByStationId(stationId);
        Map<String, BattleGroupDO> groupByName = new LinkedHashMap<String, BattleGroupDO>();
        for (BattleGroupDO group : groups) {
            groupByName.put(group.getName(), group);
        }
        List<BattleGroupMemberDO> members = battleGroupMemberMapper.selectByStationId(stationId);
        List<BattleGroupTombstoneDO> groupTombstones =
                battleGroupTombstoneMapper.selectActiveByStationId(stationId);
        int addedGroup = 0;
        boolean importedCommand = false;
        for (int i = 1; i < rows.size(); i++) {
            List<String> cols = rows.get(i);
            String name = RosterSpreadsheetSupport.cell(cols, nameIdx);
            if (name.isEmpty() || RosterRuleConstant.TEMPLATE_EXAMPLE_NAME.equals(name)) {
                skipped += 1;
                continue;
            }
            String nfcRaw = RosterSpreadsheetSupport.cell(cols, nfcIdx);
            if (isTombstoned(activeTombstones, name, nfcRaw)) {
                skippedDeleted += 1;
                continue;
            }
            ProfileSaveCommand command = ProfileSaveCommand.builder()
                    .name(name)
                    .title(RosterSpreadsheetSupport.cell(cols, titleIdx))
                    .rankName(RosterSpreadsheetSupport.cell(cols, rankIdx))
                    .phone(hasPhone ? RosterSpreadsheetSupport.cell(cols, phoneIdx) : null)
                    .cylType(RosterSpreadsheetSupport.cell(cols, cylIdx))
                    .nfcTag(nfcRaw)
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
                created.setArchiveNo(buildArchiveNo(created.getId()));
                profileMapper.update(created);
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
                    if (isGroupTombstoned(groupTombstones, groupName)) {
                        // 用户删过的组名，导入不复活
                        continue;
                    }
                    group = insertGroup(stationId, groupName, groups.size() + 1);
                    groups.add(group);
                    groupByName.put(groupName, group);
                    addedGroup += 1;
                }
                addMemberIfAbsent(group, existing, members);
            }
        }
        ensureFixedGroups(stationId);
        log.info("roster imported stationId={} added={} updated={} skipped={} deleted={} groups={}",
                stationId, added, updated, skipped, skippedDeleted, addedGroup);
        return RosterImportResultVO.builder()
                .addedCount(added)
                .updatedCount(updated)
                .skippedCount(skipped + skippedDeleted)
                .addedGroupCount(addedGroup)
                .skippedDeletedCount(skippedDeleted)
                .commandGroupPreserved(!importedCommand && hasExistingCommand(groups))
                .build();
    }

    private boolean isTombstoned(List<ProfileTombstoneDO> tombstones, String name, String nfcRaw) {
        String nameNorm = normalizeName(name);
        String nfcNorm = normalizeNfc(nfcRaw);
        for (ProfileTombstoneDO tombstone : tombstones) {
            if (tombstone.getNameReleasedAt() == null && nameNorm != null
                    && nameNorm.equals(tombstone.getNameNorm())) {
                return true;
            }
            if (tombstone.getNfcReleasedAt() == null && nfcNorm != null
                    && nfcNorm.equals(tombstone.getNfcNorm())) {
                return true;
            }
        }
        return false;
    }

    private boolean isGroupTombstoned(List<BattleGroupTombstoneDO> tombstones, String groupName) {
        String norm = normalizeName(groupName);
        for (BattleGroupTombstoneDO tombstone : tombstones) {
            if (norm != null && norm.equals(tombstone.getNameNorm())) {
                return true;
            }
        }
        return false;
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

    /**
     * 按 id / 组名增量更新编组：不再全删全插，组 id 保持稳定，可作为删除键。
     *
     * <p>删除本站不再出现的组时写编组删除记录，只记该组自己的名称与编号，
     * 不会波及同前缀的其他组。</p>
     */
    private void mergeGroups(Long stationId, List<GroupSaveCommand> incoming,
                             List<BattleGroupDO> existing, List<BattleGroupMemberDO> existingMembers,
                             Long operatorId) {
        Map<String, BattleGroupDO> existingByName = new LinkedHashMap<String, BattleGroupDO>();
        for (BattleGroupDO group : existing) {
            existingByName.put(group.getName(), group);
        }
        Map<Long, List<BattleGroupMemberDO>> membersByGroup =
                new LinkedHashMap<Long, List<BattleGroupMemberDO>>();
        for (BattleGroupMemberDO member : existingMembers) {
            membersByGroup.computeIfAbsent(member.getGroupId(), k -> new ArrayList<BattleGroupMemberDO>())
                    .add(member);
        }
        Map<Long, ProfileDO> profiles = loadProfileMap(stationId);
        LocalDateTime now = LocalDateTime.now();
        List<String> keptNames = new ArrayList<String>();
        int sort = 1;
        for (GroupSaveCommand item : incoming) {
            if (item == null || item.getName() == null || item.getName().isBlank()) {
                continue;
            }
            String name = item.getName().trim();
            keptNames.add(name);
            BattleGroupDO group = existingByName.get(name);
            if (group == null) {
                // 用户重新新建同名组才解开这一条删除记录
                battleGroupTombstoneMapper.releaseByName(stationId, normalizeName(name), operatorId);
                group = BattleGroupDO.builder()
                        .stationId(stationId)
                        .name(name)
                        .sortNo(item.getSortNo() == null ? sort : item.getSortNo())
                        .slotNo(resolveSlotNo(name))
                        .isFixed(Integer.valueOf(isFixedGroupName(name) ? 1 : 0))
                        .gmtCreate(now)
                        .gmtModified(now)
                        .build();
                battleGroupMapper.insert(group);
                existingByName.put(name, group);
            } else {
                battleGroupMapper.updateSortNo(group.getId(),
                        item.getSortNo() == null ? Integer.valueOf(sort) : item.getSortNo(), now);
            }
            // 成员按差量更新，避免整份重建
            replaceMembers(group.getId(), item.getMembers(), profiles, membersByGroup, now);
            sort += 1;
        }
        // 删除用户明确移除的组，并写删除记录
        for (BattleGroupDO group : existing) {
            if (keptNames.contains(group.getName())) {
                continue;
            }
            if (group.getIsFixed() != null && group.getIsFixed().intValue() == 1) {
                continue;
            }
            if (isCommandGroupName(group.getName())) {
                continue;
            }
            battleGroupMemberMapper.deleteByGroupId(group.getId());
            battleGroupMapper.deleteById(group.getId());
            battleGroupTombstoneMapper.insertOrRevive(BattleGroupTombstoneDO.builder()
                    .stationId(stationId)
                    .groupName(group.getName())
                    .nameNorm(normalizeName(group.getName()))
                    .groupId(group.getId())
                    .slotNo(group.getSlotNo())
                    .source(RosterRuleConstant.TOMBSTONE_SOURCE_USER)
                    .createdBy(operatorId)
                    .build());
        }
    }

    private void replaceMembers(Long groupId, List<GroupMemberSaveCommand> members,
                                Map<Long, ProfileDO> profiles,
                                Map<Long, List<BattleGroupMemberDO>> membersByGroup, LocalDateTime now) {
        List<BattleGroupMemberDO> current = membersByGroup.getOrDefault(groupId, new ArrayList<BattleGroupMemberDO>());
        List<Long> wanted = new ArrayList<Long>();
        if (members != null) {
            for (GroupMemberSaveCommand member : members) {
                if (member == null || member.getProfileId() == null) {
                    continue;
                }
                if (!profiles.containsKey(member.getProfileId()) || wanted.contains(member.getProfileId())) {
                    continue;
                }
                wanted.add(member.getProfileId());
            }
        }
        for (BattleGroupMemberDO member : current) {
            if (!wanted.contains(member.getProfileId())) {
                battleGroupMemberMapper.deleteById(member.getId());
            }
        }
        List<Long> existingIds = new ArrayList<Long>();
        for (BattleGroupMemberDO member : current) {
            existingIds.add(member.getProfileId());
        }
        if (members == null) {
            return;
        }
        for (GroupMemberSaveCommand member : members) {
            if (member == null || member.getProfileId() == null || !wanted.contains(member.getProfileId())) {
                continue;
            }
            if (existingIds.contains(member.getProfileId())) {
                continue;
            }
            battleGroupMemberMapper.insert(BattleGroupMemberDO.builder()
                    .groupId(groupId)
                    .profileId(member.getProfileId())
                    .roleInGroup(trimToNull(member.getRoleInGroup()))
                    .gmtCreate(now)
                    .gmtModified(now)
                    .build());
        }
    }

    /**
     * 保证每站都有指挥员、安全员、通信员三张固定编制卡。
     */
    private void ensureFixedGroups(Long stationId) {
        List<BattleGroupDO> fixed = battleGroupMapper.selectFixedByStationId(stationId);
        List<Integer> existingSlots = new ArrayList<Integer>();
        for (BattleGroupDO group : fixed) {
            if (group.getSlotNo() != null) {
                existingSlots.add(group.getSlotNo());
            }
        }
        LocalDateTime now = LocalDateTime.now();
        int sort = battleGroupMapper.selectByStationId(stationId).size() + 1;
        insertFixedIfAbsent(stationId, existingSlots, RosterRuleConstant.SLOT_COMMANDER,
                RosterRuleConstant.FIXED_GROUP_COMMANDER, sort, now);
        insertFixedIfAbsent(stationId, existingSlots, RosterRuleConstant.SLOT_SAFETY,
                RosterRuleConstant.FIXED_GROUP_SAFETY, sort + 1, now);
        insertFixedIfAbsent(stationId, existingSlots, RosterRuleConstant.SLOT_SIGNAL,
                RosterRuleConstant.FIXED_GROUP_SIGNAL, sort + 2, now);
    }

    private void insertFixedIfAbsent(Long stationId, List<Integer> existingSlots, int slot, String name,
                                     int sortNo, LocalDateTime now) {
        if (existingSlots.contains(Integer.valueOf(slot))) {
            return;
        }
        battleGroupMapper.insert(BattleGroupDO.builder()
                .stationId(stationId)
                .name(name)
                .sortNo(Integer.valueOf(sortNo))
                .slotNo(Integer.valueOf(slot))
                .isFixed(Integer.valueOf(1))
                .gmtCreate(now)
                .gmtModified(now)
                .build());
        existingSlots.add(Integer.valueOf(slot));
    }

    private Integer resolveSlotNo(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (RosterRuleConstant.FIXED_GROUP_COMMANDER.equals(trimmed)) {
            return Integer.valueOf(RosterRuleConstant.SLOT_COMMANDER);
        }
        if (RosterRuleConstant.FIXED_GROUP_SAFETY.equals(trimmed)) {
            return Integer.valueOf(RosterRuleConstant.SLOT_SAFETY);
        }
        if (RosterRuleConstant.FIXED_GROUP_SIGNAL.equals(trimmed)) {
            return Integer.valueOf(RosterRuleConstant.SLOT_SIGNAL);
        }
        return null;
    }

    private boolean isFixedGroupName(String name) {
        return resolveSlotNo(name) != null;
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
            BattleGroupDO exists = battleGroupMapper.selectByStationAndName(stationId, group.getName());
            if (exists != null) {
                continue;
            }
            BattleGroupDO restored = BattleGroupDO.builder()
                    .stationId(stationId)
                    .name(group.getName())
                    .sortNo(Integer.valueOf(sort))
                    .isFixed(Integer.valueOf(0))
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

    private BattleGroupDO insertGroup(Long stationId, String name, int sortNo) {
        LocalDateTime now = LocalDateTime.now();
        BattleGroupDO group = BattleGroupDO.builder()
                .stationId(stationId)
                .name(name)
                .sortNo(Integer.valueOf(sortNo))
                .slotNo(resolveSlotNo(name))
                .isFixed(Integer.valueOf(isFixedGroupName(name) ? 1 : 0))
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

    private void writeProfileTombstone(ProfileDO profile, Long operatorId, String source, LocalDateTime now) {
        profileTombstoneMapper.insertIgnore(ProfileTombstoneDO.builder()
                .stationId(profile.getStationId())
                .profileId(profile.getId())
                .legacyProfileId(profile.getArchiveNo())
                .archiveNo(profile.getArchiveNo())
                .name(profile.getName())
                .nameNorm(normalizeName(profile.getName()))
                .nfcTag(profile.getNfcTag())
                .nfcNorm(normalizeNfc(profile.getNfcTag()))
                .source(source)
                .createdBy(operatorId)
                .build());
    }

    private String buildArchiveNo(Long profileId) {
        return RosterRuleConstant.ARCHIVE_NO_PREFIX
                + String.format("%0" + RosterRuleConstant.ARCHIVE_NO_PAD + "d", profileId);
    }

    private StationRosterVO buildRoster(StationDO station, boolean writable) {
        // 4.5：能看该站花名册的角色（本站 / 所属大队 / 支队 / 开发者）都看完整电话；
        // 二维码共享页不走本接口，仍不出现电话、NFC 号与账号。
        return rosterViewTranslator.toRoster(
                station,
                writable,
                profileMapper.selectAliveByStationId(station.getId()),
                battleGroupMapper.selectByStationId(station.getId()),
                battleGroupMemberMapper.selectByStationId(station.getId()),
                true);
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

    private String normalizeName(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = NAME_TRIM.matcher(raw.trim()).replaceAll("");
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
