package com.ccds.attack.attack.service.impl;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ccds.attack.attack.constant.AttackRuleConstant;
import com.ccds.attack.attack.dto.AttackEventCommand;
import com.ccds.attack.attack.entity.AttackEventDO;
import com.ccds.attack.attack.entity.AttackPersonDO;
import com.ccds.attack.attack.entity.StationSnapshotDO;
import com.ccds.attack.attack.enums.AttackEventTypeEnum;
import com.ccds.attack.attack.enums.AttackSceneEnum;
import com.ccds.attack.attack.enums.AttackStatusEnum;
import com.ccds.attack.attack.enums.WorkLevelEnum;
import com.ccds.attack.attack.mapper.AttackEventMapper;
import com.ccds.attack.attack.mapper.AttackPersonMapper;
import com.ccds.attack.attack.mapper.StationSnapshotMapper;
import com.ccds.attack.attack.service.AttackService;
import com.ccds.attack.attack.service.CommandPushService;
import com.ccds.attack.attack.service.ScbaEstimateService;
import com.ccds.attack.attack.vo.AttackEventResultVO;
import com.ccds.attack.attack.vo.AttackPersonVO;
import com.ccds.attack.attack.vo.AttackSnapshotVO;
import com.ccds.attack.attack.vo.StationAttackVO;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.infra.redis.AttackEventDedupStore;
import com.ccds.infra.redis.AttackRealtimePublisher;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.iam.identity.vo.StationVO;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.org.org.service.OrgQueryService;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 人员卡片状态机与事件幂等。重复 eventId 返回已有结果。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttackServiceImpl implements AttackService {

    private static final String MSG_UNAUTHORIZED = "登录已失效，请重新登录";

    private static final String MSG_EVENT_INVALID = "事件不合法";

    private static final String MSG_PERSON_MISSING = "人员卡片不存在";

    private static final String MSG_DUPLICATE = "该人员已有未撤出卡片";

    private static final String MSG_STATUS = "当前状态不允许该操作";

    private static final String MSG_NAME_REQUIRED = "临时人员姓名不能为空";

    private static final String MSG_PRESSURE_REQUIRED = "请填写气瓶压力";

    private static final String MSG_PROFILE_MISSING = "人员档案不存在";

    private static final String MSG_NFC_NOT_FOUND = "未找到对应花名册";

    private static final String MSG_SINCE_INVALID = "增量时间不合法";

    private static final Pattern NFC_TRIM = Pattern.compile("[\\s:：\\-_]");

    private final AccountMapper accountMapper;

    private final OrgQueryService orgQueryService;

    private final StationMapper stationMapper;

    private final RosterAccessService rosterAccessService;

    private final ProfileMapper profileMapper;

    private final BattleGroupMapper battleGroupMapper;

    private final BattleGroupMemberMapper battleGroupMemberMapper;

    private final ScbaCalibrationMapper scbaCalibrationMapper;

    private final AttackPersonMapper attackPersonMapper;

    private final AttackEventMapper attackEventMapper;

    private final StationSnapshotMapper stationSnapshotMapper;

    private final ScbaEstimateService scbaEstimateService;

    private final AttackViewTranslator attackViewTranslator;

    private final AttackEventDedupStore attackEventDedupStore;

    private final AttackRealtimePublisher attackRealtimePublisher;

    private final CommandPushService commandPushService;

    /**
     * {@inheritDoc}
     */
    @Override
    public StationAttackVO getStationAttack(AuthPrincipal principal, Long stationId) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireVisibleStation(account, stationId);
        boolean writable = rosterAccessService.isWritable(account, stationId);
        return buildStationAttack(station, writable, LocalDateTime.now());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttackEventResultVO submitEvent(AuthPrincipal principal, Long stationId, AttackEventCommand command) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireWritableStation(account, stationId);
        AttackEventTypeEnum type = AttackEventTypeEnum.fromCode(command.getType());
        if (type == null) {
            throw new BizException(ErrorCodeConstant.ATTACK_EVENT_INVALID, MSG_EVENT_INVALID);
        }
        String eventId = command.getEventId().trim();
        LocalDateTime now = LocalDateTime.now();
        AttackEventDO existed = attackEventMapper.selectByClientEventId(eventId);
        if (existed != null) {
            return duplicateResult(station, existed, now, true, command);
        }
        boolean firstSeen = attackEventDedupStore.tryMark(eventId);
        if (!firstSeen) {
            AttackEventDO racedEarly = attackEventMapper.selectByClientEventId(eventId);
            if (racedEarly != null) {
                return duplicateResult(station, racedEarly, now, true, command);
            }
            throw new BizException(ErrorCodeConstant.ATTACK_EVENT_INVALID, MSG_EVENT_INVALID);
        }
        AttackPersonDO person;
        try {
            person = dispatchEvent(stationId, command, type, eventId, now);
            insertEvent(stationId, person, type, eventId, now);
        } catch (DuplicateKeyException ex) {
            return recoverDuplicateKey(station, stationId, command, eventId, now, ex);
        } catch (RuntimeException ex) {
            attackEventDedupStore.release(eventId);
            throw ex;
        }
        releaseDedupAfterRollback(eventId);
        AttackSnapshotVO snapshot = refreshSnapshot(stationId, now);
        publishAfterCommit(snapshot);
        log.info("attack event type={} stationId={} personId={} eventIdHash={}",
                type.getCode(), stationId, person == null ? null : person.getId(),
                Integer.valueOf(eventId.hashCode()));
        AttackPersonVO personVo = null;
        if (person != null && type != AttackEventTypeEnum.DELETE) {
            personVo = toLivePerson(person, now);
        }
        StationAttackVO attack = buildStationAttack(station, true, now);
        return AttackEventResultVO.builder()
                .duplicate(Boolean.FALSE)
                .cloudOverride(Boolean.valueOf(isCloudOverride(command, personVo, attack)))
                .person(personVo)
                .attack(attack)
                .build();
    }

    private AttackPersonDO dispatchEvent(Long stationId, AttackEventCommand command, AttackEventTypeEnum type,
                                         String eventId, LocalDateTime now) {
        switch (type) {
            case PRE_ADD:
                return handlePreAdd(stationId, command, eventId, now);
            case ENTER:
                return handleEnter(stationId, command, eventId, now);
            case REMEASURE:
                return handleRemeasure(stationId, command, eventId, now);
            case WITHDRAW:
                return handleWithdraw(stationId, command, eventId, now);
            case DELETE:
                return handleDelete(stationId, command, eventId, now);
            default:
                throw new BizException(ErrorCodeConstant.ATTACK_EVENT_INVALID, MSG_EVENT_INVALID);
        }
    }

    private AttackEventResultVO recoverDuplicateKey(StationDO station, Long stationId, AttackEventCommand command,
                                                    String eventId, LocalDateTime now, DuplicateKeyException ex) {
        AttackEventDO raced = attackEventMapper.selectByClientEventId(eventId);
        if (raced != null) {
            return duplicateResult(station, raced, now, true, command);
        }
        ProfileDO profile = resolveProfileQuiet(stationId, command);
        String displayName = profile != null ? profile.getName() : trimToNull(command.getDisplayName());
        AttackPersonDO existing = findActive(stationId, profile, displayName);
        if (existing != null) {
            throw new BizException(ErrorCodeConstant.ATTACK_PERSON_DUPLICATE, MSG_DUPLICATE);
        }
        log.warn("attack event duplicate key stationId={} eventIdHash={}", stationId, eventId.hashCode(), ex);
        throw new BizException(ErrorCodeConstant.ATTACK_EVENT_INVALID, MSG_EVENT_INVALID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AttackSnapshotVO> listSnapshots(AuthPrincipal principal, String sinceRaw) {
        AccountDO account = requireAccount(principal);
        Map<Long, StationDO> visible = visibleStationMap(account);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = parseSince(sinceRaw);
        List<StationSnapshotDO> rows = since == null
                ? stationSnapshotMapper.selectActive()
                : stationSnapshotMapper.selectModifiedSince(since);
        List<AttackSnapshotVO> result = new ArrayList<AttackSnapshotVO>();
        for (StationSnapshotDO row : rows) {
            StationDO station = visible.get(row.getStationId());
            if (station == null) {
                continue;
            }
            AttackSnapshotVO snapshot = attackViewTranslator.toSnapshotVo(row, station, now);
            if (since == null && !attackViewTranslator.hasUnwithdrawn(snapshot)) {
                continue;
            }
            result.add(snapshot);
        }
        return result;
    }

    private Map<Long, StationDO> visibleStationMap(AccountDO account) {
        Map<Long, StationDO> map = new HashMap<Long, StationDO>();
        for (StationVO station : orgQueryService.listVisibleStations(account)) {
            StationDO row = new StationDO();
            row.setId(station.getId());
            row.setName(station.getName());
            row.setBrigadeId(station.getBrigadeId());
            row.setBrigadeName(station.getBrigadeName());
            row.setCategory(station.getCategory());
            row.setSortNo(station.getSortNo());
            map.put(station.getId(), row);
        }
        return map;
    }

    private AttackPersonDO handlePreAdd(Long stationId, AttackEventCommand command, String eventId,
                                        LocalDateTime now) {
        ProfileDO profile = resolveProfile(stationId, command);
        String displayName = resolveDisplayName(command, profile);
        AttackPersonDO existing = findActive(stationId, profile, displayName);
        if (existing != null) {
            throw new BizException(ErrorCodeConstant.ATTACK_PERSON_DUPLICATE, MSG_DUPLICATE);
        }
        BigDecimal pressure = defaultPressure(command.getPressure(), profile);
        AttackPersonDO person = new AttackPersonDO();
        person.setStationId(stationId);
        person.setProfileId(profile == null ? null : profile.getId());
        person.setDisplayName(displayName);
        person.setGroupName(resolveGroupName(stationId, command, profile));
        person.setCylType(resolveCylType(command, profile));
        person.setInitPressure(pressure);
        person.setCurrentPressure(pressure);
        person.setWorkLevel(WorkLevelEnum.fromCodeOrDefault(command.getWorkLevel()).getCode());
        person.setScene(AttackSceneEnum.fromCodeOrDefault(command.getScene()).getCode());
        person.setStatus(AttackStatusEnum.PENDING.getCode());
        person.setRemainSec(null);
        person.setClientEventId(eventId);
        person.setGmtCreate(now);
        person.setGmtModified(now);
        attackPersonMapper.insert(person);
        return person;
    }

    private AttackPersonDO handleEnter(Long stationId, AttackEventCommand command, String eventId,
                                       LocalDateTime now) {
        AttackPersonDO person = requirePerson(stationId, command.getPersonId());
        AttackStatusEnum status = AttackStatusEnum.fromCode(person.getStatus());
        if (status != AttackStatusEnum.PENDING) {
            throw new BizException(ErrorCodeConstant.ATTACK_STATUS_INVALID, MSG_STATUS);
        }
        BigDecimal pressure = requirePressure(command.getPressure(), person.getCurrentPressure());
        if (command.getCylType() != null && !command.getCylType().isBlank()) {
            person.setCylType(CylTypeEnum.fromCodeOrDefault(command.getCylType()).getCode());
        }
        person.setInitPressure(pressure);
        person.setCurrentPressure(pressure);
        person.setEnteredAt(now);
        person.setWithdrawnAt(null);
        applyWorkContext(person, command);
        refreshRuntime(person, now);
        person.setClientEventId(eventId);
        person.setGmtModified(now);
        attackPersonMapper.update(person);
        return person;
    }

    private AttackPersonDO handleRemeasure(Long stationId, AttackEventCommand command, String eventId,
                                           LocalDateTime now) {
        AttackPersonDO person = requirePerson(stationId, command.getPersonId());
        AttackStatusEnum status = AttackStatusEnum.fromCode(person.getStatus());
        if (status == null || !status.isInside()) {
            throw new BizException(ErrorCodeConstant.ATTACK_STATUS_INVALID, MSG_STATUS);
        }
        BigDecimal pressure = requirePressure(command.getPressure(), null);
        person.setCurrentPressure(pressure);
        applyWorkContext(person, command);
        refreshRuntime(person, now);
        person.setClientEventId(eventId);
        person.setGmtModified(now);
        attackPersonMapper.update(person);
        return person;
    }

    private AttackPersonDO handleWithdraw(Long stationId, AttackEventCommand command, String eventId,
                                          LocalDateTime now) {
        AttackPersonDO person = requirePerson(stationId, command.getPersonId());
        AttackStatusEnum status = AttackStatusEnum.fromCode(person.getStatus());
        if (status == AttackStatusEnum.OUT) {
            throw new BizException(ErrorCodeConstant.ATTACK_STATUS_INVALID, MSG_STATUS);
        }
        if (status == AttackStatusEnum.PENDING) {
            throw new BizException(ErrorCodeConstant.ATTACK_STATUS_INVALID, MSG_STATUS);
        }
        person.setWithdrawnAt(now);
        person.setStatus(AttackStatusEnum.OUT.getCode());
        person.setRemainSec(null);
        person.setClientEventId(eventId);
        person.setGmtModified(now);
        attackPersonMapper.update(person);
        return person;
    }

    private AttackPersonDO handleDelete(Long stationId, AttackEventCommand command, String eventId,
                                        LocalDateTime now) {
        AttackPersonDO person = requirePerson(stationId, command.getPersonId());
        AttackStatusEnum status = AttackStatusEnum.fromCode(person.getStatus());
        if (status != AttackStatusEnum.PENDING) {
            throw new BizException(ErrorCodeConstant.ATTACK_STATUS_INVALID, MSG_STATUS);
        }
        attackPersonMapper.deleteById(person.getId());
        person.setClientEventId(eventId);
        person.setGmtModified(now);
        return person;
    }

    private void refreshRuntime(AttackPersonDO person, LocalDateTime now) {
        ScbaCalibrationDO calibration = latestCalibration(person.getProfileId());
        Integer remain = liveRemainSec(person, now, calibration);
        AttackStatusEnum current = AttackStatusEnum.fromCode(person.getStatus());
        AttackStatusEnum next = scbaEstimateService.resolveStatus(current, person.getCurrentPressure(), remain, now,
                person.getEnteredAt(), person.getWithdrawnAt());
        person.setRemainSec(remain);
        person.setStatus(next.getCode());
    }

    private Integer liveRemainSec(AttackPersonDO person, LocalDateTime now, ScbaCalibrationDO calibration) {
        Integer remain = scbaEstimateService.remainSec(person.getCurrentPressure(), person.getCylType(),
                person.getWorkLevel(), person.getScene(), calibration);
        if (remain == null) {
            return null;
        }
        int elapsed = 0;
        if (person.getEnteredAt() != null && person.getGmtModified() != null
                && person.getEnteredAt().isBefore(person.getGmtModified())) {
            elapsed = (int) Math.max(0L, Duration.between(person.getGmtModified(), now).getSeconds());
        } else if (person.getEnteredAt() != null) {
            elapsed = (int) Math.max(0L, Duration.between(person.getEnteredAt(), now).getSeconds());
        }
        return Math.max(0, remain - elapsed);
    }

    private AttackEventResultVO duplicateResult(StationDO station, AttackEventDO existed, LocalDateTime now,
                                                boolean writable, AttackEventCommand command) {
        AttackPersonDO person = existed.getPersonId() == null
                ? null
                : attackPersonMapper.selectById(existed.getPersonId());
        AttackEventTypeEnum existedType = AttackEventTypeEnum.fromCode(existed.getEventType());
        AttackPersonVO personVo = null;
        if (person != null && existedType != AttackEventTypeEnum.DELETE) {
            personVo = toLivePerson(person, now);
        }
        StationAttackVO attack = buildStationAttack(station, writable, now);
        return AttackEventResultVO.builder()
                .duplicate(Boolean.TRUE)
                .cloudOverride(Boolean.valueOf(isCloudOverride(command, personVo, attack)))
                .person(personVo)
                .attack(attack)
                .build();
    }

    private boolean isCloudOverride(AttackEventCommand command, AttackPersonVO person, StationAttackVO attack) {
        if (command == null || command.getClientUpdatedAt() == null) {
            return false;
        }
        LocalDateTime localAt = command.getClientUpdatedAt();
        if (person != null && person.getGmtModified() != null && person.getGmtModified().isAfter(localAt)) {
            return true;
        }
        if (attack == null || attack.getPersons() == null || command.getPersonId() == null) {
            return false;
        }
        for (AttackPersonVO item : attack.getPersons()) {
            if (item.getId() != null && item.getId().equals(command.getPersonId())
                    && item.getGmtModified() != null && item.getGmtModified().isAfter(localAt)) {
                return true;
            }
        }
        return false;
    }

    private void releaseDedupAfterRollback(String eventId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                attackEventDedupStore.release(eventId);
            }
        });
    }

    private void insertEvent(Long stationId, AttackPersonDO person, AttackEventTypeEnum type, String eventId,
                             LocalDateTime now) {
        AttackEventDO event = AttackEventDO.builder()
                .stationId(stationId)
                .personId(person == null ? null : person.getId())
                .eventType(type.getCode())
                .payloadJson(null)
                .clientEventId(eventId)
                .gmtCreate(now)
                .gmtModified(now)
                .build();
        attackEventMapper.insert(event);
    }

    private AttackSnapshotVO refreshSnapshot(Long stationId, LocalDateTime now) {
        List<AttackPersonDO> persons = attackPersonMapper.selectByStationId(stationId);
        Map<Long, ScbaCalibrationDO> latest = latestCalibrationMap(stationId);
        for (AttackPersonDO person : persons) {
            refreshDisplayedRemain(person, now, latest.get(person.getProfileId()));
        }
        StatusCounts counts = countStatuses(persons);
        persistLiveSnapshot(stationId, counts, now);
        StationDO station = stationMapper.selectById(stationId);
        if (station == null) {
            StationDO fallback = new StationDO();
            fallback.setId(stationId);
            return countsToSnapshot(fallback, counts, now);
        }
        return countsToSnapshot(station, counts, now);
    }

    private void publishAfterCommit(AttackSnapshotVO snapshot) {
        if (snapshot == null) {
            return;
        }
        Runnable publish = () -> {
            commandPushService.broadcast(snapshot);
            attackRealtimePublisher.publishSnapshot(snapshot);
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }

    private StatusCounts countStatuses(List<AttackPersonDO> persons) {
        StatusCounts counts = new StatusCounts();
        Set<String> groups = new LinkedHashSet<String>();
        for (AttackPersonDO person : persons) {
            AttackStatusEnum status = AttackStatusEnum.fromCode(person.getStatus());
            if (status == null) {
                continue;
            }
            switch (status) {
                case IN:
                    counts.inCount++;
                    groups.add(attackViewTranslator.blankToUngrouped(person.getGroupName()));
                    break;
                case WARN:
                    counts.warnCount++;
                    groups.add(attackViewTranslator.blankToUngrouped(person.getGroupName()));
                    break;
                case DANGER:
                    counts.dangerCount++;
                    groups.add(attackViewTranslator.blankToUngrouped(person.getGroupName()));
                    break;
                case OUT:
                    counts.outCount++;
                    break;
                case PENDING:
                    counts.pendingCount++;
                    groups.add(attackViewTranslator.blankToUngrouped(person.getGroupName()));
                    break;
                default:
                    break;
            }
        }
        counts.groupCount = groups.size();
        return counts;
    }

    private AttackSnapshotVO countsToSnapshot(StationDO station, StatusCounts counts, LocalDateTime now) {
        StationSnapshotDO existed = stationSnapshotMapper.selectByStationId(station.getId());
        LocalDateTime lastEvent = existed == null ? now : existed.getLastEventAt();
        return AttackSnapshotVO.builder()
                .stationId(station.getId())
                .stationName(station.getName())
                .brigadeId(station.getBrigadeId())
                .brigadeName(station.getBrigadeName())
                .inCount(counts.inCount)
                .warnCount(counts.warnCount)
                .dangerCount(counts.dangerCount)
                .outCount(counts.outCount)
                .pendingCount(counts.pendingCount)
                .groupCount(counts.groupCount)
                .lastEventAt(lastEvent)
                .gmtModified(now)
                .staleSec(staleSec(lastEvent, now))
                .build();
    }

    private Integer staleSec(LocalDateTime lastEvent, LocalDateTime now) {
        if (lastEvent == null || now == null) {
            return null;
        }
        long seconds = Duration.between(lastEvent, now).getSeconds();
        if (seconds < 0L) {
            return 0;
        }
        if (seconds > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) seconds;
    }

    private void persistLiveSnapshot(Long stationId, StatusCounts counts, LocalDateTime now) {
        StationSnapshotDO existed = stationSnapshotMapper.selectByStationId(stationId);
        StationSnapshotDO snapshot = existed == null ? new StationSnapshotDO() : existed;
        snapshot.setStationId(stationId);
        snapshot.setInCount(counts.inCount);
        snapshot.setWarnCount(counts.warnCount);
        snapshot.setDangerCount(counts.dangerCount);
        snapshot.setOutCount(counts.outCount);
        snapshot.setPendingCount(counts.pendingCount);
        snapshot.setGroupCount(counts.groupCount);
        snapshot.setLastEventAt(now);
        snapshot.setGmtModified(now);
        if (existed == null) {
            snapshot.setGmtCreate(now);
            stationSnapshotMapper.insert(snapshot);
            return;
        }
        stationSnapshotMapper.update(snapshot);
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private static final class StatusCounts {
        private int inCount;
        private int warnCount;
        private int dangerCount;
        private int outCount;
        private int pendingCount;
        private int groupCount;
    }

    private StationAttackVO buildStationAttack(StationDO station, boolean writable, LocalDateTime now) {
        List<AttackPersonDO> rows = attackPersonMapper.selectByStationId(station.getId());
        Map<Long, ScbaCalibrationDO> latest = latestCalibrationMap(station.getId());
        List<Boolean> calibrated = new ArrayList<Boolean>();
        List<BigDecimal> personalKs = new ArrayList<BigDecimal>();
        List<AttackPersonDO> live = new ArrayList<AttackPersonDO>();
        for (AttackPersonDO row : rows) {
            ScbaCalibrationDO calibration = latest.get(row.getProfileId());
            refreshDisplayedRemain(row, now, calibration);
            live.add(row);
            calibrated.add(Boolean.valueOf(calibration != null));
            personalKs.add(scbaEstimateService.personalK(calibration));
        }
        List<AttackPersonVO> persons = attackViewTranslator.toPersonVos(live, now, calibrated, personalKs);
        AttackSnapshotVO snapshotVo = countsToSnapshot(station, countStatuses(live), now);
        return attackViewTranslator.toStationAttack(station, writable, persons, snapshotVo);
    }

    private void refreshDisplayedRemain(AttackPersonDO person, LocalDateTime now, ScbaCalibrationDO calibration) {
        AttackStatusEnum status = AttackStatusEnum.fromCode(person.getStatus());
        if (status == null || !status.isInside()) {
            return;
        }
        Integer remain = liveRemainSec(person, now, calibration);
        AttackStatusEnum next = scbaEstimateService.resolveStatus(status, person.getCurrentPressure(), remain, now,
                person.getEnteredAt(), person.getWithdrawnAt());
        person.setRemainSec(remain);
        person.setStatus(next.getCode());
    }

    private AttackPersonVO toLivePerson(AttackPersonDO person, LocalDateTime now) {
        ScbaCalibrationDO calibration = latestCalibration(person.getProfileId());
        refreshDisplayedRemain(person, now, calibration);
        return attackViewTranslator.toPersonVo(person, now, calibration != null,
                scbaEstimateService.personalK(calibration));
    }

    private ProfileDO resolveProfileQuiet(Long stationId, AttackEventCommand command) {
        try {
            return resolveProfile(stationId, command);
        } catch (BizException ex) {
            return null;
        }
    }

    private ProfileDO resolveProfile(Long stationId, AttackEventCommand command) {
        if (command.getProfileId() != null) {
            ProfileDO profile = profileMapper.selectAliveById(command.getProfileId());
            if (profile == null || !stationId.equals(profile.getStationId())) {
                throw new BizException(ErrorCodeConstant.ROSTER_PROFILE_NOT_FOUND, MSG_PROFILE_MISSING);
            }
            return profile;
        }
        String nfc = normalizeNfc(command.getNfcTag());
        if (nfc != null) {
            ProfileDO byNfc = profileMapper.selectAliveByStationAndNfc(stationId, nfc);
            if (byNfc != null) {
                return byNfc;
            }
            throw new BizException(ErrorCodeConstant.ATTACK_NFC_NOT_FOUND, MSG_NFC_NOT_FOUND);
        }
        String name = trimToNull(command.getDisplayName());
        if (name != null) {
            return profileMapper.selectAliveByStationAndName(stationId, name);
        }
        return null;
    }

    private String resolveDisplayName(AttackEventCommand command, ProfileDO profile) {
        if (profile != null) {
            return profile.getName();
        }
        String name = trimToNull(command.getDisplayName());
        if (name == null) {
            throw new BizException(ErrorCodeConstant.ATTACK_EVENT_INVALID, MSG_NAME_REQUIRED);
        }
        return name;
    }

    private String resolveGroupName(Long stationId, AttackEventCommand command, ProfileDO profile) {
        String given = trimToNull(command.getGroupName());
        if (given != null) {
            return given;
        }
        if (profile == null) {
            return AttackRuleConstant.UNGROUPED_NAME;
        }
        List<BattleGroupDO> groups = battleGroupMapper.selectByStationId(stationId);
        List<BattleGroupMemberDO> members = battleGroupMemberMapper.selectByStationId(stationId);
        for (BattleGroupMemberDO member : members) {
            if (!profile.getId().equals(member.getProfileId())) {
                continue;
            }
            for (BattleGroupDO group : groups) {
                if (group.getId().equals(member.getGroupId())) {
                    return group.getName();
                }
            }
        }
        return AttackRuleConstant.UNGROUPED_NAME;
    }

    private String resolveCylType(AttackEventCommand command, ProfileDO profile) {
        if (command.getCylType() != null && !command.getCylType().isBlank()) {
            return CylTypeEnum.fromCodeOrDefault(command.getCylType()).getCode();
        }
        if (profile != null) {
            return CylTypeEnum.fromCodeOrDefault(profile.getCylType()).getCode();
        }
        return CylTypeEnum.L68.getCode();
    }

    private AttackPersonDO findActive(Long stationId, ProfileDO profile, String displayName) {
        if (profile != null) {
            return attackPersonMapper.selectActiveByStationAndProfile(stationId, profile.getId());
        }
        return attackPersonMapper.selectActiveTempByStationAndName(stationId, displayName);
    }

    private AttackPersonDO requirePerson(Long stationId, Long personId) {
        if (personId == null) {
            throw new BizException(ErrorCodeConstant.ATTACK_PERSON_NOT_FOUND, MSG_PERSON_MISSING);
        }
        AttackPersonDO person = attackPersonMapper.selectById(personId);
        if (person == null || !stationId.equals(person.getStationId())) {
            throw new BizException(ErrorCodeConstant.ATTACK_PERSON_NOT_FOUND, MSG_PERSON_MISSING);
        }
        return person;
    }

    private void applyWorkContext(AttackPersonDO person, AttackEventCommand command) {
        if (command.getWorkLevel() != null && !command.getWorkLevel().isBlank()) {
            person.setWorkLevel(WorkLevelEnum.fromCodeOrDefault(command.getWorkLevel()).getCode());
        } else if (person.getWorkLevel() == null) {
            person.setWorkLevel(WorkLevelEnum.MODERATE.getCode());
        }
        if (command.getScene() != null && !command.getScene().isBlank()) {
            person.setScene(AttackSceneEnum.fromCodeOrDefault(command.getScene()).getCode());
        } else if (person.getScene() == null) {
            person.setScene(AttackSceneEnum.FLAT.getCode());
        }
    }

    private BigDecimal requirePressure(BigDecimal incoming, BigDecimal fallback) {
        if (incoming != null) {
            return incoming;
        }
        if (fallback != null) {
            return fallback;
        }
        throw new BizException(ErrorCodeConstant.ATTACK_EVENT_INVALID, MSG_PRESSURE_REQUIRED);
    }

    private BigDecimal defaultPressure(BigDecimal incoming, ProfileDO profile) {
        if (incoming != null) {
            return incoming;
        }
        if (profile != null && profile.getMorningPressure() != null) {
            return profile.getMorningPressure();
        }
        return new BigDecimal(AttackRuleConstant.DEFAULT_PRESSURE);
    }

    private ScbaCalibrationDO latestCalibration(Long profileId) {
        if (profileId == null) {
            return null;
        }
        List<ScbaCalibrationDO> list = scbaCalibrationMapper.selectByProfileId(profileId);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private Map<Long, ScbaCalibrationDO> latestCalibrationMap(Long stationId) {
        Map<Long, ScbaCalibrationDO> map = new HashMap<Long, ScbaCalibrationDO>();
        List<ScbaCalibrationDO> list = scbaCalibrationMapper.selectByStationId(stationId);
        for (ScbaCalibrationDO item : list) {
            if (item.getProfileId() == null || map.containsKey(item.getProfileId())) {
                continue;
            }
            map.put(item.getProfileId(), item);
        }
        return map;
    }

    private AccountDO requireAccount(AuthPrincipal principal) {
        if (principal == null || principal.getAccountId() == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        AccountDO account = accountMapper.selectById(principal.getAccountId());
        if (account == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        return account;
    }

    private String normalizeNfc(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String compact = NFC_TRIM.matcher(raw.trim()).replaceAll("");
        if (compact.isEmpty()) {
            return null;
        }
        return compact.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDateTime parseSince(String since) {
        if (since == null || since.isBlank()) {
            return null;
        }
        String trimmed = since.trim();
        try {
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException nested) {
                throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_SINCE_INVALID);
            }
        }
    }
}
