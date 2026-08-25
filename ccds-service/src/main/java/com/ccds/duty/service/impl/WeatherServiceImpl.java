package com.ccds.duty.service.impl;

import com.ccds.duty.dto.WeatherDTO;
import com.ccds.duty.service.WeatherService;
import com.ccds.infra.weather.WeatherClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 天气服务实现
 *
 * @author system
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final WeatherClient weatherClient;

    @Override
    public WeatherDTO getWeather(Double lng, Double lat) {
        if (lng == null || lat == null) {
            throw new IllegalArgumentException("经纬度不能为空");
        }

        // 简单校验经纬度范围
        if (lng < -180 || lng > 180 || lat < -90 || lat > 90) {
            throw new IllegalArgumentException("经纬度超出有效范围");
        }

        return weatherClient.getWeather(lng, lat);
    }
}
