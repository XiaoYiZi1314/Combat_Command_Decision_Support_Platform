package com.ccds.duty.service.impl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.dto.DutyDayDTO;
import com.ccds.duty.entity.DutyDayDO;
import com.ccds.duty.mapper.DutyDayMapper;
import com.ccds.duty.service.DutyAccessService;
import com.ccds.duty.service.DutyDayService;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.excel.SpreadsheetCodec;
import com.ccds.infra.excel.SpreadsheetTable;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.service.OrgQueryService;
import com.ccds.roster.roster.entity.BattleGroupDO;
import com.ccds.roster.roster.entity.BattleGroupMemberDO;
import com.ccds.roster.roster.entity.ProfileDO;
import com.ccds.roster.roster.mapper.BattleGroupMapper;
import com.ccds.roster.roster.mapper.BattleGroupMemberMapper;
import com.ccds.roster.roster.mapper.ProfileMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 值班日历读写。
 *
 * <p>只有今天与明天允许改战斗编组与车辆；历史日期保存时强制保持原快照，
 * 绝不用今天的花名册覆盖那一天的人。</p>
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DutyDayServiceImpl implements DutyDayService {

    private static final String MSG_OFFICER_INVALID = "主官或副职须为本站在册人员";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DutyDayMapper dutyDayMapper;

    private final DutyAccessService dutyAccessService;

    private final ProfileMapper profileMapper;

    private final BattleGroupMapper battleGroupMapper;

    private final BattleGroupMemberMapper battleGroupMemberMapper;

    private final OrgQueryService orgQueryService;

    private final SpreadsheetCodec spreadsheetCodec;

    /**
     * {@inheritDoc}
     */
    @Override
    public DutyDayDTO getByStationAndDate(AuthPrincipal principal, Long stationId, LocalDate dutyDate) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        dutyAccessService.requireVisibleStation(account, stationId);
        DutyDayDO dutyDay = dutyDayMapper.selectByStationAndDate(stationId, dutyDate);
        if (dutyDay == null) {
            return null;
        }
        return convertToDTO(dutyDay, officerNames(stationId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DutyDayDTO> listByMonth(AuthPrincipal principal, Long stationId, Integer year, Integer month) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        dutyAccessService.requireVisibleStation(account, stationId);
        YearMonth yearMonth = YearMonth.of(year, month);
        List<DutyDayDO> dutyDays = dutyDayMapper.selectByStationAndDateRange(
                stationId, yearMonth.atDay(1), yearMonth.atEndOfMonth());
        Map<Long, String> names = officerNames(stationId);
        return dutyDays.stream().map(item -> convertToDTO(item, names)).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DutyDayDTO saveOrUpdate(AuthPrincipal principal, Long stationId, LocalDate dutyDate, DutyDayDTO dto) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        dutyAccessService.requireWritableStation(account, stationId);
        DutyDayDO existing = dutyDayMapper.selectByStationAndDate(stationId, dutyDate);
        boolean editable = isEditableDate(dutyDate);
        Long mainOfficerId = dto.getMainOfficerId();
        Long deputyOfficerId = dto.getDeputyOfficerId();
        boolean keepOfficers = Boolean.TRUE.equals(dto.getKeepOfficers())
                || mainOfficerId == null || deputyOfficerId == null;
        if (keepOfficers && existing != null) {
            // 页面上没有对应选框时保留原值，不要存成空
            mainOfficerId = mainOfficerId == null ? existing.getMainOfficerId() : mainOfficerId;
            deputyOfficerId = deputyOfficerId == null ? existing.getDeputyOfficerId() : deputyOfficerId;
        }
        validateOfficer(stationId, mainOfficerId);
        validateOfficer(stationId, deputyOfficerId);
        // 只有今天与明天允许写编组与车辆；历史日期强制保持原快照
        String groupSnapshot = existing == null ? null : existing.getGroupSnapshotJson();
        String vehicleSnapshot = existing == null ? null : existing.getVehicleSnapshotJson();
        boolean replace = editable && !"preserve".equalsIgnoreCase(dto.getGroupSnapshotMode());
        if (replace) {
            if (dto.getGroupSnapshotJson() != null) {
                groupSnapshot = dto.getGroupSnapshotJson();
            }
            if (dto.getVehicleSnapshotJson() != null) {
                vehicleSnapshot = dto.getVehicleSnapshotJson();
            }
        }
        DutyDayDO dutyDay = DutyDayDO.builder()
                .stationId(stationId)
                .dutyDate(dutyDate)
                .mainOfficerId(mainOfficerId)
                .deputyOfficerId(deputyOfficerId)
                .note(dto.getNote())
                .groupSnapshotJson(groupSnapshot)
                .vehicleSnapshotJson(vehicleSnapshot)
                .build();
        if (existing != null) {
            dutyDay.setId(existing.getId());
            dutyDayMapper.updateById(dutyDay);
        } else {
            dutyDayMapper.insert(dutyDay);
        }
        log.info("保存值班：stationId={}, dutyDate={}, editable={}, replaceGroups={}",
                stationId, dutyDate, Boolean.valueOf(editable), Boolean.valueOf(replace));
        return convertToDTO(dutyDay, officerNames(stationId));
    }

    /**
     * 是否可编辑该日的编组与车辆：只有今天与明天可改，历史日期只能看当天快照。
     *
     * @param dutyDate 值班日期
     * @return 可编辑为 true
     */
    private boolean isEditableDate(LocalDate dutyDate) {
        LocalDate today = LocalDate.now();
        return !dutyDate.isBefore(today) && !dutyDate.isAfter(today.plusDays(1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(AuthPrincipal principal, Long stationId, LocalDate dutyDate) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        dutyAccessService.requireWritableStation(account, stationId);
        DutyDayDO existing = dutyDayMapper.selectByStationAndDate(stationId, dutyDate);
        if (existing != null) {
            dutyDayMapper.deleteById(existing.getId());
            log.info("删除值班：stationId={}, dutyDate={}", stationId, dutyDate);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] exportBattleGroups(AuthPrincipal principal) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        dutyAccessService.requireReviewer(account);
        List<StationDO> stations = orgQueryService.listVisibleStationEntities(account);
        List<Long> stationIds = new ArrayList<Long>();
        Map<Long, String> stationNames = new HashMap<Long, String>();
        for (StationDO station : stations) {
            stationIds.add(station.getId());
            stationNames.put(station.getId(), station.getName());
        }
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(List.of("单位", "值班日期", "编组名称", "成员", "主官", "副职"));
        if (!stationIds.isEmpty()) {
            for (DutyDayDO dutyDay : dutyDayMapper.selectLatestByStationIds(stationIds)) {
                Map<Long, String> names = officerNames(dutyDay.getStationId());
                List<List<String>> groupRows = parseGroupRows(dutyDay.getGroupSnapshotJson());
                if (groupRows.isEmpty()) {
                    groupRows = currentGroupRows(dutyDay.getStationId(), names);
                }
                for (List<String> line : groupRows) {
                    rows.add(List.of(stationNames.getOrDefault(dutyDay.getStationId(), ""),
                            String.valueOf(dutyDay.getDutyDate()), line.get(0), line.get(1),
                            names.getOrDefault(dutyDay.getMainOfficerId(), ""),
                            names.getOrDefault(dutyDay.getDeputyOfficerId(), "")));
                }
            }
        }
        log.info("battle groups exported stationCount={}", stationIds.size());
        return spreadsheetCodec.writeXlsx("战斗编组导出", SpreadsheetTable.builder().rows(rows).build());
    }

    private List<List<String>> parseGroupRows(String snapshotJson) {
        List<List<String>> out = new ArrayList<List<String>>();
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return out;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(snapshotJson);
            JsonNode groups = root.path("groups");
            for (JsonNode group : groups) {
                List<String> memberNames = new ArrayList<String>();
                for (JsonNode member : group.path("members")) {
                    String name = member.path("name").asText("").trim();
                    if (!name.isEmpty()) {
                        memberNames.add(name);
                    }
                }
                out.add(List.of(group.path("name").asText("未命名编组"),
                        String.join("、", memberNames)));
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.warn("duty group snapshot parse failed", ex);
        }
        return out;
    }

    private List<List<String>> currentGroupRows(Long stationId, Map<Long, String> names) {
        Map<Long, List<String>> namesByGroup = new HashMap<Long, List<String>>();
        for (BattleGroupMemberDO member : battleGroupMemberMapper.selectByStationId(stationId)) {
            String name = names.get(member.getProfileId());
            if (name != null) {
                namesByGroup.computeIfAbsent(member.getGroupId(), k -> new ArrayList<String>()).add(name);
            }
        }
        List<List<String>> out = new ArrayList<List<String>>();
        for (BattleGroupDO group : battleGroupMapper.selectByStationId(stationId)) {
            out.add(List.of(group.getName(),
                    String.join("、", namesByGroup.getOrDefault(group.getId(), List.of()))));
        }
        return out;
    }

    private void validateOfficer(Long stationId, Long profileId) {
        if (profileId == null) {
            return;
        }
        ProfileDO profile = profileMapper.selectAliveById(profileId);
        if (profile == null || !stationId.equals(profile.getStationId())) {
            throw new BizException(ErrorCodeConstant.DUTY_OFFICER_INVALID, MSG_OFFICER_INVALID);
        }
    }

    private Map<Long, String> officerNames(Long stationId) {
        Map<Long, String> names = new HashMap<Long, String>();
        List<ProfileDO> profiles = profileMapper.selectAliveByStationId(stationId);
        for (ProfileDO profile : profiles) {
            names.put(profile.getId(), profile.getName());
        }
        return names;
    }

    private DutyDayDTO convertToDTO(DutyDayDO dutyDay, Map<Long, String> names) {
        return DutyDayDTO.builder()
                .id(dutyDay.getId())
                .stationId(dutyDay.getStationId())
                .dutyDate(dutyDay.getDutyDate())
                .mainOfficerId(dutyDay.getMainOfficerId())
                .mainOfficerName(names.get(dutyDay.getMainOfficerId()))
                .deputyOfficerId(dutyDay.getDeputyOfficerId())
                .deputyOfficerName(names.get(dutyDay.getDeputyOfficerId()))
                .note(dutyDay.getNote())
                .groupSnapshotJson(dutyDay.getGroupSnapshotJson())
                .vehicleSnapshotJson(dutyDay.getVehicleSnapshotJson())
                .build();
    }
}
