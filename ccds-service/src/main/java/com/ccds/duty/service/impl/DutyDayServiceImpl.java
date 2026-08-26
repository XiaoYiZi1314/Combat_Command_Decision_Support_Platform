package com.ccds.duty.service.impl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.dto.DutyDayDTO;
import com.ccds.duty.entity.DutyDayDO;
import com.ccds.duty.mapper.DutyDayMapper;
import com.ccds.duty.service.DutyAccessService;
import com.ccds.duty.service.DutyDayService;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.roster.roster.entity.ProfileDO;
import com.ccds.roster.roster.mapper.ProfileMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 值班日历读写。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DutyDayServiceImpl implements DutyDayService {

    private static final String MSG_OFFICER_INVALID = "主官或副职须为本站在册人员";

    private final DutyDayMapper dutyDayMapper;

    private final DutyAccessService dutyAccessService;

    private final ProfileMapper profileMapper;

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
        validateOfficer(stationId, dto.getMainOfficerId());
        validateOfficer(stationId, dto.getDeputyOfficerId());
        DutyDayDO existing = dutyDayMapper.selectByStationAndDate(stationId, dutyDate);
        DutyDayDO dutyDay = DutyDayDO.builder()
                .stationId(stationId)
                .dutyDate(dutyDate)
                .mainOfficerId(dto.getMainOfficerId())
                .deputyOfficerId(dto.getDeputyOfficerId())
                .note(dto.getNote())
                .groupSnapshotJson(dto.getGroupSnapshotJson())
                .build();
        if (existing != null) {
            dutyDay.setId(existing.getId());
            dutyDayMapper.updateById(dutyDay);
        } else {
            dutyDayMapper.insert(dutyDay);
        }
        log.info("保存值班：stationId={}, dutyDate={}", stationId, dutyDate);
        return convertToDTO(dutyDay, officerNames(stationId));
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
                .build();
    }
}
