package com.ccds.duty.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.duty.dto.WeatherDTO;
import com.ccds.duty.service.WeatherService;
import com.ccds.iam.identity.model.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 天气代理接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * 按经纬度查询天气。
     *
     * @param request HTTP 请求
     * @param lng     经度
     * @param lat     纬度
     * @return 天气
     */
    @GetMapping(ApiPathConstant.WEATHER)
    public ApiResultVO<WeatherDTO> getWeather(HttpServletRequest request,
                                              @RequestParam @NotNull
                                              @DecimalMin("-180.0") @DecimalMax("180.0") Double lng,
                                              @RequestParam @NotNull
                                              @DecimalMin("-90.0") @DecimalMax("90.0") Double lat) {
        return ApiResultVO.ok(weatherService.getWeather(current(request), lng, lat));
    }

    private AuthPrincipal current(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
    }
}
