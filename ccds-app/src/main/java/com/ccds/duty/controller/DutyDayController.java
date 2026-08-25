package com.ccds.duty.controller;

import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.duty.dto.DutyDayDTO;
import com.ccds.duty.service.DutyDayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 值班日历Controller
 *
 * @author system
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stations/{stationId}/duty")
@RequiredArgsConstructor
@Validated
public class DutyDayController {

    private final DutyDayService dutyDayService;

    /**
     * 查询指定日期的值班记录
     *
     * @param stationId 消防站ID
     * @param date      日期（yyyy-MM-dd）
     * @return 值班记录
     */
    @GetMapping("/{date}")
    public ApiResultVO<DutyDayDTO> getByDate(
            @PathVariable Long stationId,
            @PathVariable String date) {
        LocalDate dutyDate = LocalDate.parse(date);
        DutyDayDTO dutyDay = dutyDayService.getByStationAndDate(stationId, dutyDate);
        return ApiResultVO.ok(dutyDay);
    }

    /**
     * 查询指定月份的值班记录
     *
     * @param stationId 消防站ID
     * @param year      年份
     * @param month     月份
     * @return 值班记录列表
     */
    @GetMapping("/month")
    public ApiResultVO<List<DutyDayDTO>> listByMonth(
            @PathVariable Long stationId,
            @RequestParam @NotNull @Min(2020) @Max(2100) Integer year,
            @RequestParam @NotNull @Min(1) @Max(12) Integer month) {
        List<DutyDayDTO> dutyDays = dutyDayService.listByMonth(stationId, year, month);
        return ApiResultVO.ok(dutyDays);
    }

    /**
     * 保存或更新值班记录
     *
     * @param stationId 消防站ID
     * @param dto       值班记录
     * @return 保存后的值班记录
     */
    @PutMapping
    public ApiResultVO<DutyDayDTO> saveOrUpdate(
            @PathVariable Long stationId,
            @Valid @RequestBody DutyDayDTO dto) {
        // 确保stationId一致
        dto.setStationId(stationId);
        DutyDayDTO saved = dutyDayService.saveOrUpdate(dto);
        return ApiResultVO.ok(saved);
    }

    /**
     * 删除值班记录
     *
     * @param stationId 消防站ID
     * @param date      日期（yyyy-MM-dd）
     * @return 成功标识
     */
    @DeleteMapping("/{date}")
    public ApiResultVO<Void> delete(
            @PathVariable Long stationId,
            @PathVariable String date) {
        LocalDate dutyDate = LocalDate.parse(date);
        dutyDayService.delete(stationId, dutyDate);
        return ApiResultVO.ok(null);
    }
}
