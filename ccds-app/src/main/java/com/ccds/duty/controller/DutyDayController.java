package com.ccds.duty.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.duty.dto.DutyDayDTO;
import com.ccds.duty.service.DutyDayService;
import com.ccds.iam.identity.model.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 值班日历接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class DutyDayController {

    private static final String MSG_DATE_INVALID = "日期格式须为 yyyy-MM-dd";

    private final DutyDayService dutyDayService;

    /**
     * 查询指定日期值班。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param date      日期
     * @return 值班记录
     */
    @GetMapping(ApiPathConstant.STATION_DUTY_DATE)
    public ApiResultVO<DutyDayDTO> getByDate(HttpServletRequest request,
                                             @PathVariable Long stationId,
                                             @PathVariable String date) {
        return ApiResultVO.ok(dutyDayService.getByStationAndDate(current(request), stationId, parseDate(date)));
    }

    /**
     * 查询月度值班。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param year      年
     * @param month     月
     * @return 列表
     */
    @GetMapping(ApiPathConstant.STATION_DUTY_MONTH)
    public ApiResultVO<List<DutyDayDTO>> listByMonth(HttpServletRequest request,
                                                     @PathVariable Long stationId,
                                                     @RequestParam @NotNull @Min(2020) @Max(2100) Integer year,
                                                     @RequestParam @NotNull @Min(1) @Max(12) Integer month) {
        return ApiResultVO.ok(dutyDayService.listByMonth(current(request), stationId, year, month));
    }

    /**
     * 保存或更新指定日期值班。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param date      日期
     * @param dto       值班记录
     * @return 保存结果
     */
    @PutMapping(ApiPathConstant.STATION_DUTY_DATE)
    public ApiResultVO<DutyDayDTO> saveOrUpdate(HttpServletRequest request,
                                                @PathVariable Long stationId,
                                                @PathVariable String date,
                                                @Valid @RequestBody DutyDayDTO dto) {
        return ApiResultVO.ok(dutyDayService.saveOrUpdate(current(request), stationId, parseDate(date), dto));
    }

    /**
     * 删除指定日期值班。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param date      日期
     * @return 空成功
     */
    @DeleteMapping(ApiPathConstant.STATION_DUTY_DATE)
    public ApiResultVO<Void> delete(HttpServletRequest request,
                                    @PathVariable Long stationId,
                                    @PathVariable String date) {
        dutyDayService.delete(current(request), stationId, parseDate(date));
        return ApiResultVO.ok(null);
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_DATE_INVALID);
        }
    }

    private AuthPrincipal current(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
    }
}
