package com.ccds.duty.service;

import com.ccds.duty.dto.WeatherDTO;

/**
 * 天气服务
 *
 * @author system
 * @since 2024
 */
public interface WeatherService {

    /**
     * 根据经纬度获取天气信息
     *
     * @param lng 经度
     * @param lat 纬度
     * @return 天气信息
     */
    WeatherDTO getWeather(Double lng, Double lat);
}
