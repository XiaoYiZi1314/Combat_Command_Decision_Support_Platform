package com.ccds.duty.service;

import com.ccds.duty.dto.WeatherDTO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 天气代理。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface WeatherService {

    /**
     * 登录后按经纬度查询。不落精确坐标日志。
     *
     * @param principal 当前身份
     * @param lng       经度
     * @param lat       纬度
     * @return 天气
     */
    WeatherDTO getWeather(AuthPrincipal principal, Double lng, Double lat);
}
