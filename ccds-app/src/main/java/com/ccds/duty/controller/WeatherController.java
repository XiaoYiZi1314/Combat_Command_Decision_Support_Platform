package com.ccds.duty.controller;

import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.duty.dto.WeatherDTO;
import com.ccds.duty.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotNull;

/**
 * 天气Controller
 *
 * @author system
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * 根据经纬度获取天气信息
     *
     * @param lng 经度
     * @param lat 纬度
     * @return 天气信息
     */
    @GetMapping
    public ApiResultVO<WeatherDTO> getWeather(
            @RequestParam @NotNull Double lng,
            @RequestParam @NotNull Double lat) {
        WeatherDTO weather = weatherService.getWeather(lng, lat);
        return ApiResultVO.ok(weather);
    }
}
