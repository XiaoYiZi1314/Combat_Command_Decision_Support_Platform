package com.ccds.duty.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.ccds.attack.attack.entity.StationScbaSettingDO;
import com.ccds.attack.attack.entity.StationWorkModeDO;
import com.ccds.attack.attack.mapper.StationScbaSettingMapper;
import com.ccds.attack.attack.mapper.StationWorkModeMapper;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.entity.DutyDayDO;
import com.ccds.duty.mapper.DutyDayMapper;
import com.ccds.duty.service.LegacyDataImportService;
import com.ccds.infra.crypto.FieldCipherUtil;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.roster.roster.constant.RosterRuleConstant;
import com.ccds.roster.roster.entity.BattleGroupDO;
import com.ccds.roster.roster.entity.BattleGroupMemberDO;
import com.ccds.roster.roster.entity.ProfileDO;
import com.ccds.roster.roster.entity.ProfileTombstoneDO;
import com.ccds.roster.roster.entity.ScbaCalibrationDO;
import com.ccds.roster.roster.mapper.BattleGroupMapper;
import com.ccds.roster.roster.mapper.BattleGroupMemberMapper;
import com.ccds.roster.roster.mapper.ProfileMapper;
import com.ccds.roster.roster.mapper.ProfileTombstoneMapper;
import com.ccds.roster.roster.mapper.ScbaCalibrationMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 旧系统花名册一次性导入实现。逐站独立事务：单站异常只回滚该站，不中断整批，也不留半站数据。
 * 记录级唯一键冲突（旧数据脏重名/脏 NFC）按条跳过并计数，不中断整站。
 *
 * @author ccds
 * @since 0.1.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegacyDataImportServiceImpl implements LegacyDataImportService {

    private static final String MSG_DATA_DIR_INVALID = "旧数据目录不存在或缺少 roster.ndjson";

    private static final String DATA_FILE = "roster.ndjson";

    private static final String DRY_RUN_ENV = "CCDS_LEGACY_DRY_RUN";

    private static final String DRY_RUN_ON = "1";

    private static final String TOMBSTONE_SOURCE_USER_DELETE = "user_delete";

    private static final String TOMBSTONE_SOURCE_TRANSFER_OUT = "transfer_out";

    private static final String DELETED_KEY_ID_PREFIX = "id:";

    private static final String DELETED_KEY_NFC_PREFIX = "nfc:";

    private static final String TRANSFER_STATUS_APPROVED = "approved";

    private static final String DEFAULT_CALIBRATION_SOURCE = "旧系统迁移";

    private static final String WORK_MODE_DISPATCH = "dispatch";

    private static final String WORK_MODE_TRAINING = "training";

    private static final String WORK_MODE_TEST = "test";

    private static final String OPERATION_MODE_TRAINING_CN = "训练";

    private static final String OPERATION_MODE_DRILL_CN = "演练";

    private static final String DEFAULT_WORK_LEVEL = "moderate";

    private static final String DEFAULT_SCENE = "flat";

    private static final int VOICE_ENABLED = 1;

    private static final int VOICE_DISABLED = 0;

    private static final int NOTE_MAX_LENGTH = 500;

    private static final int LEGACY_ID_MAX_LENGTH = 64;

    private static final long DRY_RUN_PROFILE_ID_START = -1L;

    private static final long DRY_RUN_GROUP_ID_START = -1_000_000L;

    private static final int DEFAULT_WARN_TIME_MIN = 12;

    private static final int DEFAULT_DANGER_TIME_MIN = 5;

    private static final Pattern NFC_TRIM = Pattern.compile("[\\s:：\\-_]");

    private static final Pattern NFC_PATTERN = Pattern.compile("^[0-9A-F]{8,32}$");

    private static final Set<String> WORK_LEVELS = Set.of("light", "moderate", "heavy");

    private static final Set<String> SCENES = Set.of("flat", "highrise", "dark", "large", "search");

    private static final BigDecimal DEFAULT_WARN_PRESSURE = new BigDecimal("10.0");

    private static final BigDecimal DEFAULT_DANGER_PRESSURE = new BigDecimal("4.0");

    private static final LocalDate DUTY_MIN_DATE = LocalDate.of(2000, 1, 1);

    private static final LocalDate DUTY_MAX_DATE = LocalDate.of(2100, 12, 31);

    private final ObjectMapper objectMapper;

    private final StationMapper stationMapper;

    private final ProfileMapper profileMapper;

    private final BattleGroupMapper battleGroupMapper;

    private final BattleGroupMemberMapper battleGroupMemberMapper;

    private final ScbaCalibrationMapper scbaCalibrationMapper;

    private final DutyDayMapper dutyDayMapper;

    private final ProfileTombstoneMapper profileTombstoneMapper;

    private final StationScbaSettingMapper stationScbaSettingMapper;

    private final StationWorkModeMapper stationWorkModeMapper;

    private final FieldCipherUtil fieldCipherUtil;

    private final PlatformTransactionManager transactionManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public String importAll(Path dataDir) {
        Path file = requireDataFile(dataDir);
        boolean dryRun = isDryRun();
        ImportCounters counters = new ImportCounters();
        Map<String, Long> stations = loadStationIndex();
        TransactionTemplate stationTransaction = buildStationTransaction();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                counters.rows++;
                LegacyStationPayload payload = parseLine(line, lineNo, counters);
                if (payload == null) {
                    continue;
                }
                Long stationId = resolveStationId(payload, stations);
                if (stationId == null) {
                    counters.skippedStations++;
                    counters.skippedStationNames.add(payload.station == null ? "?" : payload.station.trim());
                    log.warn("旧数据站名未匹配，跳过 station={}", payload.station);
                    continue;
                }
                try {
                    if (dryRun) {
                        importStation(stationId, payload, true, counters);
                    } else {
                        stationTransaction.execute(status -> {
                            importStation(stationId, payload, false, counters);
                            return null;
                        });
                    }
                    counters.stations++;
                } catch (RuntimeException ex) {
                    counters.failedStations++;
                    // 异常消息可能含唯一键值（姓名/NFC），按脱敏规约只记类型与业务标识，不打印堆栈。
                    log.error("旧数据单站导入失败，已回滚该站，继续下一站 stationId={} errorType={}",
                            stationId, ex.getClass().getSimpleName());
                }
            }
        } catch (IOException ex) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_DATA_DIR_INVALID, ex);
        }
        String summary = counters.toSummary(dryRun);
        log.info(summary);
        return summary;
    }

    /**
     * 每站一个独立事务（REQUIRES_NEW），失败只回滚该站；用 TransactionTemplate 避免自调用导致注解失效。
     */
    private TransactionTemplate buildStationTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private Path requireDataFile(Path dataDir) {
        if (dataDir == null) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_DATA_DIR_INVALID);
        }
        Path file = Files.isDirectory(dataDir) ? dataDir.resolve(DATA_FILE) : dataDir;
        if (!Files.isRegularFile(file)) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_DATA_DIR_INVALID);
        }
        return file;
    }

    private LegacyStationPayload parseLine(String line, int lineNo, ImportCounters counters) {
        try {
            return objectMapper.readValue(line, LegacyStationPayload.class);
        } catch (JsonProcessingException ex) {
            counters.failedStations++;
            log.warn("旧数据行解析失败，跳过 lineNo={}", lineNo);
            return null;
        }
    }

    private boolean isDryRun() {
        String value = System.getenv(DRY_RUN_ENV);
        return value != null && DRY_RUN_ON.equals(value.trim());
    }

    private Map<String, Long> loadStationIndex() {
        Map<String, Long> index = new HashMap<String, Long>();
        for (StationDO station : stationMapper.selectAll()) {
            if (station.getName() != null) {
                index.put(station.getName().trim(), station.getId());
            }
            if (station.getNameNorm() != null) {
                index.put(station.getNameNorm().trim(), station.getId());
            }
        }
        return index;
    }

    private Long resolveStationId(LegacyStationPayload payload, Map<String, Long> stations) {
        if (payload.station == null || payload.station.isBlank()) {
            return null;
        }
        String name = payload.station.trim();
        Long stationId = stations.get(name);
        if (stationId != null) {
            return stationId;
        }
        return stations.get(name.replace(" ", ""));
    }

    private void importStation(Long stationId, LegacyStationPayload payload, boolean dryRun,
                               ImportCounters counters) {
        StationContext context = new StationContext(stationId, dryRun, counters);
        loadExistingProfiles(context);
        loadExistingGroups(context);
        loadExistingDutyDays(context);
        loadExistingCalibrations(context);
        loadExistingTombstones(context);
        importProfiles(context, payload);
        importGroups(context, payload);
        importDutyDays(context, payload);
        importCalibrations(context, payload);
        importTransfers(context, payload);
        importDeletedKeys(context, payload);
        importSettings(context, payload);
        importWorkMode(context, payload);
        countGroupTasks(context, payload);
    }

    private void loadExistingProfiles(StationContext context) {
        for (ProfileDO profile : profileMapper.selectAliveByStationId(context.stationId)) {
            context.aliveByName.put(profile.getName(), profile);
            if (isValidNfcNorm(profile.getNfcTag())) {
                context.aliveByNfc.put(profile.getNfcTag(), profile);
            }
        }
    }

    private void loadExistingGroups(StationContext context) {
        for (BattleGroupDO group : battleGroupMapper.selectByStationId(context.stationId)) {
            context.groupByName.put(group.getName(), group);
        }
        for (BattleGroupMemberDO member : battleGroupMemberMapper.selectByStationId(context.stationId)) {
            context.groupMemberKeys.add(memberKey(member.getGroupId(), member.getProfileId()));
        }
    }

    private void loadExistingDutyDays(StationContext context) {
        List<DutyDayDO> dutyDays = dutyDayMapper.selectByStationAndDateRange(
                context.stationId, DUTY_MIN_DATE, DUTY_MAX_DATE);
        for (DutyDayDO dutyDay : dutyDays) {
            context.dutyByDate.put(dutyDay.getDutyDate(), dutyDay);
        }
    }

    private void loadExistingCalibrations(StationContext context) {
        for (ScbaCalibrationDO calibration : scbaCalibrationMapper.selectByStationId(context.stationId)) {
            context.calibrationKeys.add(calibrationKey(calibration.getProfileId(), calibration.getMeasuredAt()));
        }
    }

    private void loadExistingTombstones(StationContext context) {
        for (ProfileTombstoneDO tombstone : profileTombstoneMapper.selectByStationId(context.stationId)) {
            if (tombstone.getLegacyProfileId() != null) {
                context.tombstoneKeys.add(DELETED_KEY_ID_PREFIX + tombstone.getLegacyProfileId());
            }
            if (tombstone.getNfcNorm() != null) {
                context.tombstoneKeys.add(DELETED_KEY_NFC_PREFIX + tombstone.getNfcNorm());
            }
        }
    }

    private void importProfiles(StationContext context, LegacyStationPayload payload) {
        List<LegacyProfilePayload> items = payload.profiles == null ? List.of() : payload.profiles;
        Set<String> payloadNames = new HashSet<String>();
        Set<String> payloadNfcs = new HashSet<String>();
        int index = 0;
        for (LegacyProfilePayload item : items) {
            index++;
            if (item == null) {
                context.counters.skippedProfiles++;
                continue;
            }
            String name = limitText(item.name, RosterRuleConstant.NAME_MAX_LENGTH);
            if (name == null) {
                context.counters.skippedProfiles++;
                continue;
            }
            if (!isAllowedCylType(item.cylType)) {
                context.counters.invalidCylTypes++;
                log.warn("旧数据人员气瓶规格非法，跳过 stationId={} index={}", context.stationId, index);
                continue;
            }
            if (!payloadNames.add(name)) {
                context.counters.profileDupInPayload++;
                log.warn("旧数据人员姓名站内重复，跳过 stationId={} index={}", context.stationId, index);
                continue;
            }
            String nfc = normalizeNfc(item.nfcTag);
            if (nfc != null && !isValidNfcNorm(nfc)) {
                nfc = null;
                context.counters.nfcInvalid++;
                log.warn("旧数据人员NFC非法，置空 stationId={} index={}", context.stationId, index);
            }
            if (nfc != null && !payloadNfcs.add(nfc)) {
                context.counters.profileDupInPayload++;
                log.warn("旧数据人员NFC站内重复，跳过 stationId={} index={}", context.stationId, index);
                continue;
            }
            ProfileDO sameNfc = nfc == null ? null : context.aliveByNfc.get(nfc);
            ProfileDO existing = context.aliveByName.get(name);
            if (sameNfc != null && (existing == null || !sameNfc.getId().equals(existing.getId()))) {
                context.counters.nfcConflicts++;
                log.warn("旧数据人员NFC冲突，保留原卡并跳过 stationId={} profileId={}",
                        context.stationId, sameNfc.getId());
                continue;
            }
            LocalDateTime now = LocalDateTime.now();
            try {
                if (existing == null) {
                    createProfile(context, item, name, nfc, now);
                } else {
                    updateProfile(context, item, existing, nfc, now);
                }
            } catch (DataIntegrityViolationException ex) {
                context.counters.profileDuplicates++;
                log.warn("旧数据人员写入唯一键冲突，跳过 stationId={} index={} errorType={}",
                        context.stationId, index, ex.getClass().getSimpleName());
            }
        }
    }

    private void createProfile(StationContext context, LegacyProfilePayload item, String name,
                               String nfc, LocalDateTime now) {
        ProfileDO.ProfileDOBuilder builder = ProfileDO.builder()
                .stationId(context.stationId)
                .name(name)
                .title(limitText(item.role, RosterRuleConstant.TITLE_MAX_LENGTH))
                .rankName(limitText(item.rank, RosterRuleConstant.RANK_MAX_LENGTH))
                .phoneCipher(encryptPhone(context, item.phone))
                .cylType(item.cylType.trim())
                .nfcTag(nfc)
                .heightCm(positiveOrNull(item.height))
                .weightKg(positiveOrNull(item.weight))
                .age(positiveOrNull(item.age))
                .morningPressure(positiveOrNull(item.morningPressure))
                .gmtCreate(now)
                .gmtModified(now);
        if (context.dryRun) {
            builder.id(context.counters.nextDryRunProfileId());
        }
        ProfileDO created = builder.build();
        if (!context.dryRun) {
            profileMapper.insert(created);
        }
        context.counters.createdProfiles++;
        context.aliveByName.put(name, created);
        if (created.getNfcTag() != null) {
            context.aliveByNfc.put(created.getNfcTag(), created);
        }
        mapLegacyProfile(context, item, created);
    }

    private void updateProfile(StationContext context, LegacyProfilePayload item, ProfileDO existing,
                               String nfc, LocalDateTime now) {
        boolean changed = fillProfile(context, item, existing, nfc);
        if (changed) {
            if (!context.dryRun) {
                existing.setGmtModified(now);
                profileMapper.update(existing);
            }
            context.counters.updatedProfiles++;
        }
        if (existing.getNfcTag() != null) {
            context.aliveByNfc.put(existing.getNfcTag(), existing);
        }
        mapLegacyProfile(context, item, existing);
    }

    private boolean fillProfile(StationContext context, LegacyProfilePayload item, ProfileDO existing,
                                String nfc) {
        boolean changed = false;
        if (isBlank(existing.getTitle()) && !isBlank(item.role)) {
            existing.setTitle(limitText(item.role, RosterRuleConstant.TITLE_MAX_LENGTH));
            changed = true;
        }
        if (isBlank(existing.getRankName()) && !isBlank(item.rank)) {
            existing.setRankName(limitText(item.rank, RosterRuleConstant.RANK_MAX_LENGTH));
            changed = true;
        }
        if (existing.getPhoneCipher() == null && !isBlank(item.phone)) {
            if (!context.dryRun) {
                existing.setPhoneCipher(fieldCipherUtil.encrypt(item.phone.trim()));
            }
            changed = true;
        }
        if (existing.getHeightCm() == null && positiveOrNull(item.height) != null) {
            existing.setHeightCm(positiveOrNull(item.height));
            changed = true;
        }
        if (existing.getWeightKg() == null && positiveOrNull(item.weight) != null) {
            existing.setWeightKg(positiveOrNull(item.weight));
            changed = true;
        }
        if (existing.getAge() == null && positiveOrNull(item.age) != null) {
            existing.setAge(positiveOrNull(item.age));
            changed = true;
        }
        if (existing.getMorningPressure() == null && positiveOrNull(item.morningPressure) != null) {
            existing.setMorningPressure(positiveOrNull(item.morningPressure));
            changed = true;
        }
        if (isBlank(existing.getNfcTag())) {
            if (nfc != null) {
                existing.setNfcTag(nfc);
                changed = true;
            }
        } else if (!isValidNfcNorm(existing.getNfcTag())) {
            // 上一轮脏数据可能已把非法 NFC 写进库，这里用合法值替换或清空，避免脏值长期残留。
            existing.setNfcTag(nfc);
            context.counters.nfcCleaned++;
            changed = true;
        }
        return changed;
    }

    private String encryptPhone(StationContext context, String phone) {
        if (context.dryRun || isBlank(phone)) {
            return null;
        }
        return fieldCipherUtil.encrypt(phone.trim());
    }

    private void mapLegacyProfile(StationContext context, LegacyProfilePayload item, ProfileDO profile) {
        String legacyId = limitText(item.legacyId, LEGACY_ID_MAX_LENGTH);
        if (legacyId == null || profile.getId() == null) {
            return;
        }
        context.profileIdByLegacyId.put(legacyId, profile.getId());
        if (profile.getCylType() != null) {
            context.cylTypeByLegacyId.put(legacyId, profile.getCylType());
        }
    }

    private void importGroups(StationContext context, LegacyStationPayload payload) {
        List<LegacyGroupPayload> items = payload.groups == null ? List.of() : payload.groups;
        countGroupPayload(context, items);
        Set<String> payloadMemberIds = new HashSet<String>();
        int duplicateInStation = 0;
        int sortCursor = context.groupByName.size() + 1;
        for (LegacyGroupPayload item : items) {
            if (item == null) {
                context.counters.skippedGroups++;
                continue;
            }
            String groupName = limitText(item.name, RosterRuleConstant.GROUP_NAME_MAX_LENGTH);
            if (groupName == null) {
                context.counters.skippedGroups++;
                continue;
            }
            BattleGroupDO group = context.groupByName.get(groupName);
            if (group == null) {
                group = createGroup(context, item, groupName, sortCursor);
                if (group == null) {
                    context.counters.skippedGroups++;
                    continue;
                }
                sortCursor++;
            }
            if (item.members == null) {
                continue;
            }
            for (LegacyGroupMemberPayload member : item.members) {
                String legacyId = member == null ? null
                        : limitText(member.legacyId, LEGACY_ID_MAX_LENGTH);
                if (legacyId != null && !payloadMemberIds.add(legacyId)) {
                    duplicateInStation++;
                }
                importGroupMember(context, group, member);
            }
        }
        context.counters.duplicateGroupMembers += duplicateInStation;
        if (duplicateInStation > 0) {
            log.warn("旧数据编组成员跨组重复（同一 legacyId 出现在多个组） stationId={} count={}",
                    context.stationId, duplicateInStation);
        }
    }

    /**
     * 统计本站 NDJSON 的编组与成员输入量，用于把库内差值与输入对账（编组只来自 groups[]，groupTasks 不参与）。
     */
    private void countGroupPayload(StationContext context, List<LegacyGroupPayload> items) {
        for (LegacyGroupPayload item : items) {
            if (item == null) {
                continue;
            }
            context.counters.inputGroups++;
            if (item.members != null) {
                context.counters.inputMembers += item.members.size();
            }
        }
    }

    private BattleGroupDO createGroup(StationContext context, LegacyGroupPayload item, String groupName,
                                      int sortCursor) {
        LocalDateTime now = LocalDateTime.now();
        int sortNo = item.sortNo == null || item.sortNo <= 0 ? sortCursor : item.sortNo;
        BattleGroupDO.BattleGroupDOBuilder builder = BattleGroupDO.builder()
                .stationId(context.stationId)
                .name(groupName)
                .sortNo(sortNo)
                .gmtCreate(now)
                .gmtModified(now);
        if (context.dryRun) {
            builder.id(context.counters.nextDryRunGroupId());
        }
        BattleGroupDO group = builder.build();
        if (!context.dryRun) {
            try {
                battleGroupMapper.insert(group);
            } catch (DataIntegrityViolationException ex) {
                context.counters.groupDuplicates++;
                log.warn("旧数据编组写入唯一键冲突，跳过 stationId={} errorType={}",
                        context.stationId, ex.getClass().getSimpleName());
                return null;
            }
        }
        context.counters.createdGroups++;
        context.groupByName.put(groupName, group);
        return group;
    }

    private void importGroupMember(StationContext context, BattleGroupDO group,
                                   LegacyGroupMemberPayload member) {
        if (member == null) {
            context.counters.skippedGroupMembers++;
            return;
        }
        String legacyId = limitText(member.legacyId, LEGACY_ID_MAX_LENGTH);
        if (legacyId == null) {
            context.counters.skippedGroupMembers++;
            return;
        }
        Long profileId = context.profileIdByLegacyId.get(legacyId);
        if (profileId == null) {
            context.counters.unresolvedGroupMembers++;
            return;
        }
        String key = memberKey(group.getId(), profileId);
        if (!context.groupMemberKeys.add(key)) {
            context.counters.existingGroupMembers++;
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        BattleGroupMemberDO created = BattleGroupMemberDO.builder()
                .groupId(group.getId())
                .profileId(profileId)
                .roleInGroup(limitText(member.roleInGroup, RosterRuleConstant.ROLE_IN_GROUP_MAX_LENGTH))
                .gmtCreate(now)
                .gmtModified(now)
                .build();
        if (!context.dryRun) {
            try {
                battleGroupMemberMapper.insert(created);
            } catch (DataIntegrityViolationException ex) {
                context.counters.memberDuplicates++;
                log.warn("旧数据编组成员唯一键冲突，跳过 stationId={} groupId={} errorType={}",
                        context.stationId, group.getId(), ex.getClass().getSimpleName());
                return;
            }
        }
        context.counters.createdGroupMembers++;
    }

    private void importDutyDays(StationContext context, LegacyStationPayload payload) {
        List<LegacyDutyPayload> items = payload.duty == null ? List.of() : payload.duty;
        for (LegacyDutyPayload item : items) {
            if (item == null || isBlank(item.date)) {
                context.counters.skippedDutyDays++;
                continue;
            }
            LocalDate dutyDate = parseDutyDate(context, item.date);
            if (dutyDate == null) {
                context.counters.skippedDutyDays++;
                continue;
            }
            Long mainOfficerId = resolveOfficerId(context, item.mainOfficerName);
            Long deputyOfficerId = resolveOfficerId(context, item.deputyOfficerName);
            DutyDayDO existing = context.dutyByDate.get(dutyDate);
            if (existing == null) {
                createDutyDay(context, item, dutyDate, mainOfficerId, deputyOfficerId);
            } else {
                updateDutyDay(context, item, dutyDate, existing, mainOfficerId, deputyOfficerId);
            }
        }
    }

    private LocalDate parseDutyDate(StationContext context, String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            log.warn("旧数据值班日期非法，跳过 stationId={} raw={}", context.stationId, raw);
            return null;
        }
    }

    private Long resolveOfficerId(StationContext context, String rawName) {
        String name = limitText(rawName, RosterRuleConstant.NAME_MAX_LENGTH);
        if (name == null) {
            return null;
        }
        ProfileDO profile = context.aliveByName.get(name);
        if (profile == null) {
            context.counters.unresolvedOfficers++;
            return null;
        }
        return profile.getId();
    }

    private void createDutyDay(StationContext context, LegacyDutyPayload item, LocalDate dutyDate,
                               Long mainOfficerId, Long deputyOfficerId) {
        DutyDayDO created = DutyDayDO.builder()
                .stationId(context.stationId)
                .dutyDate(dutyDate)
                .mainOfficerId(mainOfficerId)
                .deputyOfficerId(deputyOfficerId)
                .note(limitText(item.note, NOTE_MAX_LENGTH))
                .build();
        if (!context.dryRun) {
            try {
                dutyDayMapper.insert(created);
            } catch (DataIntegrityViolationException ex) {
                context.counters.dutyDuplicates++;
                log.warn("旧数据值班写入唯一键冲突，跳过 stationId={} dutyDate={}",
                        context.stationId, dutyDate);
                return;
            }
        }
        context.dutyByDate.put(dutyDate, created);
        context.counters.createdDutyDays++;
    }

    private void updateDutyDay(StationContext context, LegacyDutyPayload item, LocalDate dutyDate,
                               DutyDayDO existing, Long mainOfficerId, Long deputyOfficerId) {
        boolean changed = false;
        if (!isBlank(item.mainOfficerName) && !Objects.equals(existing.getMainOfficerId(), mainOfficerId)) {
            existing.setMainOfficerId(mainOfficerId);
            changed = true;
        }
        if (!isBlank(item.deputyOfficerName) && !Objects.equals(existing.getDeputyOfficerId(), deputyOfficerId)) {
            existing.setDeputyOfficerId(deputyOfficerId);
            changed = true;
        }
        String note = limitText(item.note, NOTE_MAX_LENGTH);
        if (note != null && !note.equals(existing.getNote())) {
            existing.setNote(note);
            changed = true;
        }
        if (!changed) {
            return;
        }
        if (!context.dryRun) {
            existing.setGmtModified(LocalDateTime.now());
            try {
                dutyDayMapper.updateById(existing);
            } catch (DataIntegrityViolationException ex) {
                context.counters.dutyDuplicates++;
                log.warn("旧数据值班更新唯一键冲突，跳过 stationId={} dutyDate={}",
                        context.stationId, dutyDate);
                return;
            }
        }
        context.counters.updatedDutyDays++;
    }

    private void importCalibrations(StationContext context, LegacyStationPayload payload) {
        List<LegacyCalibrationPayload> items = payload.calibrations == null
                ? List.of() : payload.calibrations;
        for (LegacyCalibrationPayload item : items) {
            if (item == null) {
                context.counters.skippedCalibrations++;
                continue;
            }
            String legacyId = limitText(item.legacyId, LEGACY_ID_MAX_LENGTH);
            Long profileId = legacyId == null ? null : context.profileIdByLegacyId.get(legacyId);
            if (profileId == null) {
                context.counters.unresolvedCalibrations++;
                continue;
            }
            if (positiveOrNull(item.pressure) == null || positiveOrNull(item.fullTimeSec) == null
                    || item.measuredAt == null) {
                context.counters.skippedCalibrations++;
                continue;
            }
            LocalDateTime measuredAt = toLocalDateTime(item.measuredAt);
            String key = calibrationKey(profileId, measuredAt);
            if (!context.calibrationKeys.add(key)) {
                context.counters.existingCalibrations++;
                continue;
            }
            String cylType = resolveCalibrationCylType(context, item, legacyId);
            LocalDateTime now = LocalDateTime.now();
            ScbaCalibrationDO created = ScbaCalibrationDO.builder()
                    .profileId(profileId)
                    .pressure(item.pressure)
                    .fullTimeSec(item.fullTimeSec)
                    .cylType(cylType)
                    .source(resolveCalibrationSource(item.source))
                    .measuredAt(measuredAt)
                    .gmtCreate(now)
                    .gmtModified(now)
                    .build();
            if (!context.dryRun) {
                try {
                    scbaCalibrationMapper.insert(created);
                } catch (DataIntegrityViolationException ex) {
                    context.counters.calibrationDuplicates++;
                    log.warn("旧数据标定写入唯一键冲突，跳过 stationId={} profileId={} errorType={}",
                            context.stationId, profileId, ex.getClass().getSimpleName());
                    continue;
                }
            }
            context.counters.createdCalibrations++;
        }
    }

    private String resolveCalibrationCylType(StationContext context, LegacyCalibrationPayload item,
                                             String legacyId) {
        if (isAllowedCylType(item.cylType)) {
            return item.cylType.trim();
        }
        String profileCylType = context.cylTypeByLegacyId.get(legacyId);
        if (isAllowedCylType(profileCylType)) {
            return profileCylType;
        }
        return RosterRuleConstant.CYL_TYPE_68;
    }

    private String resolveCalibrationSource(String raw) {
        String source = limitText(raw, RosterRuleConstant.SCBA_CALIBRATION_SOURCE_MAX_LENGTH);
        return source == null ? DEFAULT_CALIBRATION_SOURCE : source;
    }

    private void importTransfers(StationContext context, LegacyStationPayload payload) {
        List<LegacyTransferPayload> items = payload.transfers == null ? List.of() : payload.transfers;
        for (LegacyTransferPayload item : items) {
            if (item == null || isBlank(item.status)
                    || !TRANSFER_STATUS_APPROVED.equalsIgnoreCase(item.status.trim())
                    || isBlank(item.sourceProfileId)) {
                context.counters.skippedTransfers++;
                continue;
            }
            String legacyId = limitText(item.sourceProfileId, LEGACY_ID_MAX_LENGTH);
            if (legacyId == null) {
                context.counters.skippedTransfers++;
                continue;
            }
            String nfcNorm = normalizeNfc(item.nfcTag);
            String nfcTag = null;
            if (nfcNorm != null) {
                if (isValidNfcNorm(nfcNorm)) {
                    nfcTag = limitText(item.nfcTag, RosterRuleConstant.NFC_MAX_LENGTH);
                } else {
                    nfcNorm = null;
                    context.counters.nfcInvalid++;
                    log.warn("旧数据调档NFC非法，置空 stationId={} legacyId={}",
                            context.stationId, legacyId);
                }
            }
            String name = limitText(item.name, RosterRuleConstant.NAME_MAX_LENGTH);
            ProfileTombstoneDO tombstone = ProfileTombstoneDO.builder()
                    .stationId(context.stationId)
                    .legacyProfileId(legacyId)
                    .name(name)
                    .nameNorm(name)
                    .nfcTag(nfcTag)
                    .nfcNorm(nfcNorm)
                    .source(TOMBSTONE_SOURCE_TRANSFER_OUT)
                    .gmtCreate(LocalDateTime.now())
                    .gmtModified(LocalDateTime.now())
                    .build();
            writeTombstone(context, tombstone, DELETED_KEY_ID_PREFIX + legacyId);
        }
    }

    private void importDeletedKeys(StationContext context, LegacyStationPayload payload) {
        List<String> items = payload.deletedKeys == null ? List.of() : payload.deletedKeys;
        for (String rawKey : items) {
            if (isBlank(rawKey)) {
                continue;
            }
            String key = rawKey.trim();
            if (key.startsWith(DELETED_KEY_ID_PREFIX)) {
                importDeletedById(context, key.substring(DELETED_KEY_ID_PREFIX.length()));
            } else if (key.startsWith(DELETED_KEY_NFC_PREFIX)) {
                importDeletedByNfc(context, key.substring(DELETED_KEY_NFC_PREFIX.length()));
            } else {
                context.counters.skippedDeletedKeys++;
                log.warn("旧数据删除键格式未知，跳过 stationId={}", context.stationId);
            }
        }
    }

    private void importDeletedById(StationContext context, String rawLegacyId) {
        String legacyId = limitText(rawLegacyId, LEGACY_ID_MAX_LENGTH);
        if (legacyId == null) {
            context.counters.skippedDeletedKeys++;
            return;
        }
        ProfileTombstoneDO tombstone = ProfileTombstoneDO.builder()
                .stationId(context.stationId)
                .legacyProfileId(legacyId)
                .source(TOMBSTONE_SOURCE_USER_DELETE)
                .gmtCreate(LocalDateTime.now())
                .gmtModified(LocalDateTime.now())
                .build();
        writeTombstone(context, tombstone, DELETED_KEY_ID_PREFIX + legacyId);
    }

    private void importDeletedByNfc(StationContext context, String rawNfc) {
        String nfcNorm = normalizeNfc(rawNfc);
        if (nfcNorm == null) {
            context.counters.skippedDeletedKeys++;
            return;
        }
        if (!isValidNfcNorm(nfcNorm)) {
            context.counters.nfcInvalid++;
            context.counters.skippedDeletedKeys++;
            log.warn("旧数据注销键NFC非法，跳过 stationId={}", context.stationId);
            return;
        }
        ProfileTombstoneDO tombstone = ProfileTombstoneDO.builder()
                .stationId(context.stationId)
                .nfcTag(limitText(rawNfc, RosterRuleConstant.NFC_MAX_LENGTH))
                .nfcNorm(nfcNorm)
                .source(TOMBSTONE_SOURCE_USER_DELETE)
                .gmtCreate(LocalDateTime.now())
                .gmtModified(LocalDateTime.now())
                .build();
        writeTombstone(context, tombstone, DELETED_KEY_NFC_PREFIX + nfcNorm);
    }

    private void writeTombstone(StationContext context, ProfileTombstoneDO tombstone, String dedupeKey) {
        if (context.tombstoneKeys.contains(dedupeKey)) {
            context.counters.existingTombstones++;
            return;
        }
        context.tombstoneKeys.add(dedupeKey);
        if (tombstone.getLegacyProfileId() != null) {
            context.tombstoneKeys.add(DELETED_KEY_ID_PREFIX + tombstone.getLegacyProfileId());
        }
        if (tombstone.getNfcNorm() != null) {
            context.tombstoneKeys.add(DELETED_KEY_NFC_PREFIX + tombstone.getNfcNorm());
        }
        if (context.dryRun) {
            context.counters.tombstones++;
            return;
        }
        try {
            int inserted = profileTombstoneMapper.insertIgnore(tombstone);
            if (inserted > 0) {
                context.counters.tombstones++;
            } else {
                context.counters.existingTombstones++;
            }
        } catch (DataIntegrityViolationException ex) {
            context.counters.tombstoneFailures++;
            log.warn("旧数据墓碑写入失败，跳过 stationId={} errorType={}",
                    context.stationId, ex.getClass().getSimpleName());
        }
    }

    private void importSettings(StationContext context, LegacyStationPayload payload) {
        LegacyScbaSettingPayload settings = payload.settings;
        if (settings == null) {
            context.counters.skippedSettings++;
            return;
        }
        BigDecimal warnPressure = settings.warnPressure;
        BigDecimal dangerPressure = settings.dangerPressure;
        Integer warnTimeMin = settings.warnTimeMin;
        Integer dangerTimeMin = settings.dangerTimeMin;
        if (!isValidThreshold(warnPressure, dangerPressure, warnTimeMin, dangerTimeMin)) {
            context.counters.fallbackSettings++;
            log.warn("旧数据阈值非法，回落默认 stationId={}", context.stationId);
            warnPressure = DEFAULT_WARN_PRESSURE;
            dangerPressure = DEFAULT_DANGER_PRESSURE;
            warnTimeMin = DEFAULT_WARN_TIME_MIN;
            dangerTimeMin = DEFAULT_DANGER_TIME_MIN;
        }
        String workLevel = normalizeChoice(settings.workLevel, WORK_LEVELS, DEFAULT_WORK_LEVEL);
        String scene = normalizeChoice(settings.scene, SCENES, DEFAULT_SCENE);
        if (!isAllowedChoice(settings.workLevel, WORK_LEVELS) || !isAllowedChoice(settings.scene, SCENES)) {
            context.counters.fallbackChoices++;
            log.warn("旧数据作业强度或场景非法，回落默认 stationId={}", context.stationId);
        }
        Integer voiceEnabled = settings.voiceEnabled == null ? VOICE_ENABLED
                : (settings.voiceEnabled ? VOICE_ENABLED : VOICE_DISABLED);
        LocalDateTime now = LocalDateTime.now();
        StationScbaSettingDO setting = StationScbaSettingDO.builder()
                .stationId(context.stationId)
                .warnPressure(warnPressure)
                .dangerPressure(dangerPressure)
                .warnTimeMin(warnTimeMin)
                .dangerTimeMin(dangerTimeMin)
                .workLevel(workLevel)
                .scene(scene)
                .voiceEnabled(voiceEnabled)
                .gmtCreate(now)
                .gmtModified(now)
                .build();
        if (!context.dryRun) {
            stationScbaSettingMapper.insertOrUpdate(setting);
        }
        context.counters.settings++;
    }

    private boolean isValidThreshold(BigDecimal warnPressure, BigDecimal dangerPressure,
                                     Integer warnTimeMin, Integer dangerTimeMin) {
        if (warnPressure == null || dangerPressure == null || warnTimeMin == null || dangerTimeMin == null) {
            return false;
        }
        return dangerPressure.compareTo(warnPressure) < 0 && dangerTimeMin < warnTimeMin;
    }

    private void importWorkMode(StationContext context, LegacyStationPayload payload) {
        if (isBlank(payload.operationMode)) {
            context.counters.skippedWorkModes++;
            return;
        }
        String workMode = normalizeWorkMode(payload.operationMode);
        LocalDateTime now = LocalDateTime.now();
        StationWorkModeDO mode = StationWorkModeDO.builder()
                .stationId(context.stationId)
                .workMode(workMode)
                .gmtCreate(now)
                .gmtModified(now)
                .build();
        if (!context.dryRun) {
            stationWorkModeMapper.insertOrUpdate(mode);
        }
        context.counters.workModes++;
    }

    private String normalizeWorkMode(String raw) {
        String value = raw.trim();
        if (WORK_MODE_DISPATCH.equalsIgnoreCase(value)) {
            return WORK_MODE_DISPATCH;
        }
        if (WORK_MODE_TRAINING.equalsIgnoreCase(value)
                || OPERATION_MODE_TRAINING_CN.equals(value)
                || OPERATION_MODE_DRILL_CN.equals(value)) {
            return WORK_MODE_TRAINING;
        }
        return WORK_MODE_TEST;
    }

    private void countGroupTasks(StationContext context, LegacyStationPayload payload) {
        if (payload.groupTasks == null || payload.groupTasks.isEmpty()) {
            return;
        }
        context.counters.groupTasks += payload.groupTasks.size();
        log.debug("旧数据编组任务暂不落库 stationId={} count={}",
                context.stationId, payload.groupTasks.size());
    }

    private String memberKey(Long groupId, Long profileId) {
        return groupId + "#" + profileId;
    }

    private String calibrationKey(Long profileId, LocalDateTime measuredAt) {
        return profileId + "#" + measuredAt;
    }

    private LocalDateTime toLocalDateTime(Long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                .withNano(0);
    }

    private String normalizeChoice(String raw, Set<String> allowed, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return allowed.contains(normalized) ? normalized : fallback;
    }

    private boolean isAllowedChoice(String raw, Set<String> allowed) {
        return raw != null && allowed.contains(raw.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isAllowedCylType(String raw) {
        if (raw == null) {
            return false;
        }
        String value = raw.trim();
        return RosterRuleConstant.CYL_TYPE_68.equals(value) || RosterRuleConstant.CYL_TYPE_9.equals(value);
    }

    private String normalizeNfc(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = NFC_TRIM.matcher(raw.trim()).replaceAll("").toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * 归一后的 NFC 只接受 8~32 位十六进制；带中文/字母混杂/人名的脏值一律视为非法。
     */
    private boolean isValidNfcNorm(String nfcNorm) {
        return nfcNorm != null && NFC_PATTERN.matcher(nfcNorm).matches();
    }

    private String limitText(String raw, int maxLength) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private Integer positiveOrNull(Integer value) {
        return value == null || value <= 0 ? null : value;
    }

    private BigDecimal positiveOrNull(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0 ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 单站导入上下文，缓存本站存量数据，避免循环内查库。
     */
    private static final class StationContext {

        private final Long stationId;

        private final boolean dryRun;

        private final ImportCounters counters;

        private final Map<String, ProfileDO> aliveByName = new LinkedHashMap<String, ProfileDO>();

        private final Map<String, ProfileDO> aliveByNfc = new HashMap<String, ProfileDO>();

        private final Map<String, Long> profileIdByLegacyId = new HashMap<String, Long>();

        private final Map<String, String> cylTypeByLegacyId = new HashMap<String, String>();

        private final Map<String, BattleGroupDO> groupByName = new LinkedHashMap<String, BattleGroupDO>();

        private final Set<String> groupMemberKeys = new HashSet<String>();

        private final Map<LocalDate, DutyDayDO> dutyByDate = new HashMap<LocalDate, DutyDayDO>();

        private final Set<String> calibrationKeys = new HashSet<String>();

        private final Set<String> tombstoneKeys = new HashSet<String>();

        private StationContext(Long stationId, boolean dryRun, ImportCounters counters) {
            this.stationId = stationId;
            this.dryRun = dryRun;
            this.counters = counters;
        }
    }

    /**
     * 导入计数与干跑替身主键。
     */
    private static final class ImportCounters {

        private int rows;

        private int stations;

        private int skippedStations;

        private int failedStations;

        private final List<String> skippedStationNames = new ArrayList<String>();

        private int createdProfiles;

        private int updatedProfiles;

        private int skippedProfiles;

        private int profileDupInPayload;

        private int profileDuplicates;

        private int nfcConflicts;

        private int nfcInvalid;

        private int nfcCleaned;

        private int invalidCylTypes;

        private int createdGroups;

        private int skippedGroups;

        private int groupDuplicates;

        private int inputGroups;

        private int inputMembers;

        private int duplicateGroupMembers;

        private int createdGroupMembers;

        private int existingGroupMembers;

        private int skippedGroupMembers;

        private int unresolvedGroupMembers;

        private int memberDuplicates;

        private int createdDutyDays;

        private int updatedDutyDays;

        private int skippedDutyDays;

        private int unresolvedOfficers;

        private int dutyDuplicates;

        private int createdCalibrations;

        private int existingCalibrations;

        private int skippedCalibrations;

        private int unresolvedCalibrations;

        private int calibrationDuplicates;

        private int tombstones;

        private int existingTombstones;

        private int tombstoneFailures;

        private int skippedDeletedKeys;

        private int skippedTransfers;

        private int settings;

        private int fallbackSettings;

        private int fallbackChoices;

        private int skippedSettings;

        private int workModes;

        private int skippedWorkModes;

        private int groupTasks;

        private long dryRunProfileId = DRY_RUN_PROFILE_ID_START;

        private long dryRunGroupId = DRY_RUN_GROUP_ID_START;

        private long nextDryRunProfileId() {
            return dryRunProfileId--;
        }

        private long nextDryRunGroupId() {
            return dryRunGroupId--;
        }

        private String toSummary(boolean dryRun) {
            String skippedDetail = skippedStationNames.isEmpty()
                    ? "" : ", 跳过站明细=" + String.join(",", skippedStationNames);
            int profileSkipped = skippedProfiles + invalidCylTypes + profileDupInPayload
                    + profileDuplicates + nfcConflicts;
            StringBuilder summary = new StringBuilder(512);
            summary.append(String.format(Locale.ROOT,
                    "旧数据导入完成%s：行=%d, 站=%d, 跳过站=%d%s, 失败站(已回滚)=%d, ",
                    dryRun ? "(干跑，只读不写)" : "",
                    rows, stations, skippedStations, skippedDetail, failedStations));
            summary.append(String.format(Locale.ROOT,
                    "人员新增=%d, 人员更新=%d, 人员跳过=%d"
                            + "(姓名为空=%d,payload重复=%d,库唯一键冲突=%d,NFC冲突=%d,气瓶非法=%d), "
                            + "NFC非法置空=%d, 存量脏NFC清除=%d, ",
                    createdProfiles, updatedProfiles, profileSkipped,
                    skippedProfiles, profileDupInPayload, profileDuplicates, nfcConflicts, invalidCylTypes,
                    nfcInvalid, nfcCleaned));
            summary.append(String.format(Locale.ROOT,
                    "编组新增=%d, 编组跳过=%d(库唯一键冲突=%d), 输入编组=%d, 输入成员=%d, 成员跨组重复=%d, "
                            + "成员新增=%d, 成员已存在=%d, 成员跳过=%d, 成员无档案=%d, 成员库冲突=%d, ",
                    createdGroups, skippedGroups, groupDuplicates,
                    inputGroups, inputMembers, duplicateGroupMembers,
                    createdGroupMembers, existingGroupMembers, skippedGroupMembers,
                    unresolvedGroupMembers, memberDuplicates));
            summary.append(String.format(Locale.ROOT,
                    "值班新增=%d, 值班更新=%d, 值班跳过=%d, 值班无档案=%d, 值班库冲突=%d, ",
                    createdDutyDays, updatedDutyDays, skippedDutyDays,
                    unresolvedOfficers, dutyDuplicates));
            summary.append(String.format(Locale.ROOT,
                    "标定新增=%d, 标定已存在=%d, 标定跳过=%d, 标定无档案=%d, 标定库冲突=%d, ",
                    createdCalibrations, existingCalibrations, skippedCalibrations,
                    unresolvedCalibrations, calibrationDuplicates));
            summary.append(String.format(Locale.ROOT,
                    "墓碑新增=%d, 墓碑已存在=%d, 墓碑写入失败=%d, 注销键跳过=%d, 调档跳过=%d, ",
                    tombstones, existingTombstones, tombstoneFailures,
                    skippedDeletedKeys, skippedTransfers));
            summary.append(String.format(Locale.ROOT,
                    "阈值=%d(回落默认=%d,选项回落=%d,跳过=%d), 模式=%d(跳过=%d), "
                            + "编组任务=%d条(仅统计，未落库)",
                    settings, fallbackSettings, fallbackChoices, skippedSettings,
                    workModes, skippedWorkModes, groupTasks));
            return summary.toString();
        }
    }
}
