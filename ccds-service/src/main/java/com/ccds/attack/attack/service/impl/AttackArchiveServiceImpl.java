package com.ccds.attack.attack.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ccds.attack.attack.dto.AttackArchiveCommand;
import com.ccds.attack.attack.entity.AttackArchiveDO;
import com.ccds.attack.attack.entity.AttackPersonDO;
import com.ccds.attack.attack.enums.AttackStatusEnum;
import com.ccds.attack.attack.mapper.AttackArchiveMapper;
import com.ccds.attack.attack.mapper.AttackPersonMapper;
import com.ccds.attack.attack.service.AttackArchiveService;
import com.ccds.attack.attack.vo.AttackArchivePersonVO;
import com.ccds.attack.attack.vo.AttackArchiveVO;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.enums.AccountRoleEnum;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.iam.identity.service.AuthGuardService;
import com.ccds.infra.excel.SpreadsheetCodec;
import com.ccds.infra.excel.SpreadsheetTable;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.org.org.service.OrgQueryService;
import com.ccds.roster.roster.service.RosterAccessService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 内攻历史归档实现。归档要求全部撤出，归档后清空本站人员行。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttackArchiveServiceImpl implements AttackArchiveService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final DateTimeFormatter TIME_TEXT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String MSG_NOT_WITHDRAWN = "仍有人员未撤出，全部撤出后才能归档";

    private static final String MSG_EMPTY = "暂无内攻人员卡片";

    private static final String MSG_KIND_INVALID = "事件类型须为 drill 或 dispatch";

    private static final String MSG_NOT_FOUND = "归档记录不存在";

    private static final String MSG_FORBIDDEN = "无权操作该站内攻历史";

    private static final String MSG_JSON = "归档快照序列化失败";

    private static final List<String> EXPORT_HEADERS = List.of(
            "记录场次", "归档时间", "单位", "类型", "名称", "地点",
            "内攻组/编组", "姓名/编号", "入场时间", "入场空呼压力(MPa)",
            "出场时间", "出场空呼压力(MPa)", "强度", "场景", "状态", "气瓶(L)");

    private static final List<String> STATS_HEADERS = List.of(
            "单位", "类型", "记录数", "累计人次");

    private final AttackArchiveMapper attackArchiveMapper;

    private final AttackPersonMapper attackPersonMapper;

    private final RosterAccessService rosterAccessService;

    private final StationMapper stationMapper;

    private final AuthGuardService authGuardService;

    private final OrgQueryService orgQueryService;

    private final SpreadsheetCodec spreadsheetCodec;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttackArchiveVO archive(AuthPrincipal principal, Long stationId, AttackArchiveCommand command) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireWritableStation(account, stationId);
        String kind = normalizeKind(command == null ? null : command.getEventKind());
        List<AttackPersonDO> persons = attackPersonMapper.selectByStationId(stationId);
        if (persons.isEmpty()) {
            throw new BizException(ErrorCodeConstant.ATTACK_ARCHIVE_INVALID, MSG_EMPTY);
        }
        for (AttackPersonDO person : persons) {
            if (AttackStatusEnum.fromCode(person.getStatus()) != AttackStatusEnum.OUT) {
                throw new BizException(ErrorCodeConstant.ATTACK_ARCHIVE_NOT_WITHDRAWN, MSG_NOT_WITHDRAWN);
            }
        }
        LocalDateTime finishedAt = LocalDateTime.now();
        LocalDateTime startedAt = persons.stream()
                .map(AttackPersonDO::getGmtCreate)
                .filter(value -> value != null)
                .min(LocalDateTime::compareTo)
                .orElse(finishedAt);
        List<AttackArchivePersonVO> snapshot = persons.stream()
                .map(this::toSnapshotPerson)
                .collect(Collectors.toList());
        AttackArchiveDO archive = AttackArchiveDO.builder()
                .stationId(stationId)
                .eventKind(kind)
                .eventName(trimToNull(command == null ? null : command.getEventName()))
                .location(trimToNull(command == null ? null : command.getLocation()))
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .personCount(snapshot.size())
                .personSnapshotJson(writeJson(snapshot))
                .createdBy(account.getId())
                .build();
        attackArchiveMapper.insert(archive);
        attackPersonMapper.deleteByStationId(stationId);
        log.info("内攻归档：stationId={}, archiveId={}, kind={}, persons={}",
                stationId, archive.getId(), kind, snapshot.size());
        return toVO(archive, station.getName(), snapshot);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AttackArchiveVO> list(AuthPrincipal principal, Long stationId, String eventKind) {
        AccountDO account = requireAccount(principal);
        rosterAccessService.requireVisibleStation(account, stationId);
        String name = stationNameOf(stationId);
        return attackArchiveMapper.selectByStation(stationId).stream()
                .filter(archive -> kindMatches(archive, eventKind))
                .map(archive -> toVO(archive, name, readJson(archive.getPersonSnapshotJson())))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AttackArchiveVO> listAll(AuthPrincipal principal, Long stationId, String eventKind) {
        AccountDO account = requireAccount(principal);
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        if (role != AccountRoleEnum.HQ && role != AccountRoleEnum.DEVELOPER && role != AccountRoleEnum.BRIGADE) {
            throw new BizException(ErrorCodeConstant.ATTACK_ARCHIVE_FORBIDDEN, MSG_FORBIDDEN);
        }
        List<Long> stationIds = stationId != null
                ? List.of(stationId)
                : visibleStationIds(account);
        if (stationIds.isEmpty()) {
            return new ArrayList<>();
        }
        /* 指定站时校验该站可见；全部模式时校验首个可见站，其余站均来自同一可见集 */
        rosterAccessService.requireVisibleStation(account, stationIds.get(0));
        List<AttackArchiveDO> archives = attackArchiveMapper.selectByStations(stationIds);
        Map<Long, String> names = stationNameMap();
        return archives.stream()
                .filter(archive -> kindMatches(archive, eventKind))
                .map(archive -> toVO(archive, names.get(archive.getStationId()),
                        readJson(archive.getPersonSnapshotJson())))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttackArchiveVO updateInfo(AuthPrincipal principal, Long stationId, Long archiveId,
                                       AttackArchiveCommand command) {
        AccountDO account = requireAccount(principal);
        StationDO station = rosterAccessService.requireVisibleStation(account, stationId);
        if (!canManage(account, stationId)) {
            throw new BizException(ErrorCodeConstant.ATTACK_ARCHIVE_FORBIDDEN, MSG_FORBIDDEN);
        }
        AttackArchiveDO archive = requireArchive(stationId, archiveId);
        archive.setEventKind(normalizeKind(command == null ? archive.getEventKind() : command.getEventKind()));
        if (command != null) {
            archive.setEventName(trimToNull(command.getEventName()));
            archive.setLocation(trimToNull(command.getLocation()));
        }
        attackArchiveMapper.updateById(archive);
        return toVO(archive, station.getName(), readJson(archive.getPersonSnapshotJson()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(AuthPrincipal principal, Long stationId, Long archiveId) {
        AccountDO account = requireAccount(principal);
        rosterAccessService.requireVisibleStation(account, stationId);
        if (!canManage(account, stationId)) {
            throw new BizException(ErrorCodeConstant.ATTACK_ARCHIVE_FORBIDDEN, MSG_FORBIDDEN);
        }
        AttackArchiveDO archive = requireArchive(stationId, archiveId);
        attackArchiveMapper.deleteById(archive.getId());
        log.info("内攻历史删除：stationId={}, archiveId={}, kind={}, persons={}, operatorId={}",
                stationId, archiveId, archive.getEventKind(), archive.getPersonCount(), account.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] export(AuthPrincipal principal, Long stationId, String eventKind) {
        List<AttackArchiveVO> archives = stationId != null
                ? list(principal, stationId, eventKind)
                : listAll(principal, stationId, eventKind);
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(new ArrayList<String>(EXPORT_HEADERS));
        for (AttackArchiveVO archive : archives) {
            List<AttackArchivePersonVO> persons = archive.getPersons() == null
                    ? List.of() : archive.getPersons();
            if (persons.isEmpty()) {
                rows.add(detailRow(archive, null));
                continue;
            }
            for (AttackArchivePersonVO person : persons) {
                rows.add(detailRow(archive, person));
            }
        }
        return spreadsheetCodec.writeXlsx("内攻历史记录",
                SpreadsheetTable.builder().rows(rows).build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] exportStats(AuthPrincipal principal, Long stationId, String eventKind) {
        List<AttackArchiveVO> archives = stationId != null
                ? list(principal, stationId, eventKind)
                : listAll(principal, stationId, eventKind);
        Map<String, int[]> stats = new LinkedHashMap<String, int[]>();
        for (AttackArchiveVO archive : archives) {
            String key = (archive.getStationName() == null ? "" : archive.getStationName())
                    + "|" + (archive.getEventKind() == null ? "" : archive.getEventKind());
            int[] cell = stats.computeIfAbsent(key, ignored -> new int[2]);
            cell[0] += 1;
            cell[1] += archive.getPersonCount() == null ? 0 : archive.getPersonCount();
        }
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(new ArrayList<String>(STATS_HEADERS));
        stats.forEach((key, cell) -> {
            String[] parts = key.split("\\|", 2);
            rows.add(List.of(parts[0], kindLabel(parts.length > 1 ? parts[1] : ""),
                    String.valueOf(cell[0]), String.valueOf(cell[1])));
        });
        return spreadsheetCodec.writeXlsx("内攻历史统计",
                SpreadsheetTable.builder().rows(rows).build());
    }

    private List<String> detailRow(AttackArchiveVO archive, AttackArchivePersonVO person) {
        List<String> row = new ArrayList<String>();
        row.add(nz(archive.getFinishedAt()));
        row.add(nz(archive.getStationName()));
        row.add(kindLabel(archive.getEventKind()));
        row.add(nz(archive.getEventName()));
        row.add(nz(archive.getLocation()));
        if (person == null) {
            for (int i = 0; i < 10; i += 1) {
                row.add("");
            }
            return row;
        }
        row.add(nz(person.getGroup()));
        row.add(nz(person.getName()));
        row.add(nz(person.getEnterTime()));
        row.add(person.getInitPressure() == null ? "" : person.getInitPressure().toPlainString());
        row.add(nz(person.getExitTime()));
        row.add(person.getFinalPressure() == null ? "" : person.getFinalPressure().toPlainString());
        row.add(nz(person.getWorkLevel()));
        row.add(nz(person.getScene()));
        row.add(nz(person.getStatus()));
        row.add(nz(person.getCylType()));
        return row;
    }

    private boolean canManage(AccountDO account, Long stationId) {
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        if (role == AccountRoleEnum.HQ || role == AccountRoleEnum.DEVELOPER) {
            return true;
        }
        return role == AccountRoleEnum.STATION
                && account.getStationId() != null
                && account.getStationId().equals(stationId);
    }

    private List<Long> visibleStationIds(AccountDO account) {
        return orgQueryService.listVisibleStationEntities(account).stream()
                .map(StationDO::getId)
                .collect(Collectors.toList());
    }

    private AttackArchiveDO requireArchive(Long stationId, Long archiveId) {
        AttackArchiveDO archive = archiveId == null ? null : attackArchiveMapper.selectById(archiveId);
        if (archive == null || !stationId.equals(archive.getStationId())) {
            throw new BizException(ErrorCodeConstant.ATTACK_ARCHIVE_NOT_FOUND, MSG_NOT_FOUND);
        }
        return archive;
    }

    private AccountDO requireAccount(AuthPrincipal principal) {
        return authGuardService.requireAccount(principal);
    }

    private AttackArchivePersonVO toSnapshotPerson(AttackPersonDO person) {
        return AttackArchivePersonVO.builder()
                .name(person.getDisplayName())
                .group(person.getGroupName())
                .cylType(person.getCylType())
                .initPressure(person.getInitPressure())
                .finalPressure(person.getCurrentPressure())
                .enterTime(timeText(person.getEnteredAt()))
                .exitTime(timeText(person.getWithdrawnAt()))
                .workLevel(person.getWorkLevel())
                .scene(person.getScene())
                .status(person.getStatus())
                .build();
    }

    private AttackArchiveVO toVO(AttackArchiveDO archive, String stationName,
                                 List<AttackArchivePersonVO> persons) {
        return AttackArchiveVO.builder()
                .id(archive.getId())
                .stationId(archive.getStationId())
                .stationName(stationName)
                .eventKind(archive.getEventKind())
                .eventName(archive.getEventName())
                .location(archive.getLocation())
                .startedAt(timeText(archive.getStartedAt()))
                .finishedAt(timeText(archive.getFinishedAt()))
                .personCount(archive.getPersonCount())
                .persons(persons)
                .build();
    }

    private String normalizeKind(String kind) {
        if (kind == null || kind.isEmpty()) {
            return "drill";
        }
        String value = kind.trim().toLowerCase(Locale.ROOT);
        if (!"drill".equals(value) && !"dispatch".equals(value)) {
            throw new BizException(ErrorCodeConstant.ATTACK_ARCHIVE_INVALID, MSG_KIND_INVALID);
        }
        return value;
    }

    private boolean kindMatches(AttackArchiveDO archive, String eventKind) {
        if (eventKind == null || eventKind.isEmpty() || "all".equals(eventKind)) {
            return true;
        }
        return eventKind.equals(archive.getEventKind());
    }

    private String kindLabel(String kind) {
        return "dispatch".equals(kind) ? "出警" : "演练";
    }

    private Map<Long, String> stationNameMap() {
        /* 归档列表仅按可见站查询，站名直接取全部站字典，避免逐站查询 */
        return stationMapper.selectAll().stream()
                .collect(Collectors.toMap(StationDO::getId,
                        station -> station.getName() == null ? "" : station.getName()));
    }

    private String stationNameOf(Long stationId) {
        StationDO station = stationMapper.selectById(stationId);
        return station == null ? "" : station.getName();
    }

    private String timeText(LocalDateTime value) {
        return value == null ? "" : value.format(TIME_TEXT);
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String writeJson(List<AttackArchivePersonVO> snapshot) {
        try {
            return MAPPER.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCodeConstant.ATTACK_ARCHIVE_INVALID, MSG_JSON);
        }
    }

    private List<AttackArchivePersonVO> readJson(String json) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, AttackArchivePersonVO.class));
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
