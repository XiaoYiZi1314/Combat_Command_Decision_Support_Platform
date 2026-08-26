package com.ccds.infra.weather;

import com.ccds.duty.dto.WeatherDTO;

/**
 * 天气代理客户端。真实厂商接入前保持未配置。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface WeatherClient {

    /**
     * 按经纬度取天气。未配置时抛非法状态，由业务转为错误码。
     *
     * @param lng 经度
     * @param lat 纬度
     * @return 天气
     */
    WeatherDTO getWeather(Double lng, Double lat);
}
