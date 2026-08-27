package com.ccds.duty.service.impl;

import org.springframework.stereotype.Service;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.dto.WeatherDTO;
import com.ccds.duty.service.DutyAccessService;
import com.ccds.duty.service.WeatherService;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.weather.WeatherClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 天气查询。禁止把精确坐标写入日志。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private static final String MSG_COORD_INVALID = "经纬度不合法";

    private static final String MSG_UNAVAILABLE = "天气服务暂不可用";

    private final WeatherClient weatherClient;

    private final DutyAccessService dutyAccessService;

    /**
     * {@inheritDoc}
     */
    @Override
    public WeatherDTO getWeather(AuthPrincipal principal, Double lng, Double lat) {
        dutyAccessService.requireAccount(principal);
        if (lng == null || lat == null || lng < -180 || lng > 180 || lat < -90 || lat > 90) {
            throw new BizException(ErrorCodeConstant.WEATHER_COORD_INVALID, MSG_COORD_INVALID);
        }
        try {
            return weatherClient.getWeather(lng, lat);
        } catch (IllegalStateException ex) {
            log.error("天气代理调用失败", ex);
            throw new BizException(ErrorCodeConstant.WEATHER_UNAVAILABLE, MSG_UNAVAILABLE);
        }
    }
}
