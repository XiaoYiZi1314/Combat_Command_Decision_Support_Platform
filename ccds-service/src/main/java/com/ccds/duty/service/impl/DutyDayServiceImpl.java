package com.ccds.duty.service.impl;

import com.ccds.duty.dto.DutyDayDTO;
import com.ccds.duty.entity.DutyDayDO;
import com.ccds.duty.mapper.DutyDayMapper;
import com.ccds.duty.service.DutyDayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 值班日历服务实现
 *
 * @author system
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DutyDayServiceImpl implements DutyDayService {

    private final DutyDayMapper dutyDayMapper;

    @Override
    public DutyDayDTO getByStationAndDate(Long stationId, LocalDate dutyDate) {
        DutyDayDO dutyDay = dutyDayMapper.selectByStationAndDate(stationId, dutyDate);
        if (dutyDay == null) {
            return null;
        }
        return convertToDTO(dutyDay);
    }

    @Override
    public List<DutyDayDTO> listByMonth(Long stationId, Integer year, Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<DutyDayDO> dutyDays = dutyDayMapper.selectByStationAndDateRange(stationId, startDate, endDate);

        return dutyDays.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DutyDayDTO saveOrUpdate(DutyDayDTO dto) {
        DutyDayDO existing = dutyDayMapper.selectByStationAndDate(dto.getStationId(), dto.getDutyDate());

        DutyDayDO dutyDay = DutyDayDO.builder()
                .stationId(dto.getStationId())
                .dutyDate(dto.getDutyDate())
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

        return convertToDTO(dutyDay);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long stationId, LocalDate dutyDate) {
        DutyDayDO existing = dutyDayMapper.selectByStationAndDate(stationId, dutyDate);
        if (existing != null) {
            dutyDayMapper.deleteById(existing.getId());
        }
    }

    /**
     * 转换为DTO
     */
    private DutyDayDTO convertToDTO(DutyDayDO dutyDay) {
        return DutyDayDTO.builder()
                .id(dutyDay.getId())
                .stationId(dutyDay.getStationId())
                .dutyDate(dutyDay.getDutyDate())
                .mainOfficerId(dutyDay.getMainOfficerId())
                .deputyOfficerId(dutyDay.getDeputyOfficerId())
                .note(dutyDay.getNote())
                .groupSnapshotJson(dutyDay.getGroupSnapshotJson())
                .build();
    }
}
