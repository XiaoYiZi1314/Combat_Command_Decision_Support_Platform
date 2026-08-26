package com.ccds.infra.weather;

import org.springframework.stereotype.Service;

import com.ccds.duty.dto.WeatherDTO;

/**
 * 天气代理占位实现。未配置真实厂商时拒绝调用，禁止返回假数据。
 *
 * @author ccds
 * @since 0.1.0
 */
@Service
public class WeatherClientImpl implements WeatherClient {

    /**
     * {@inheritDoc}
     */
    @Override
    public WeatherDTO getWeather(Double lng, Double lat) {
        throw new IllegalStateException("weather proxy not configured");
    }
}
