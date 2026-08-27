package com.ccds.infra.weather;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 天气上游配置。默认使用客户源码已有的 Open-Meteo 公共接口，可由环境变量覆盖。
 *
 * @author ccds
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ccds.weather")
public class WeatherProperties {

    /**
     * Open-Meteo 兼容预报接口地址。
     */
    private String baseUrl = "https://api.open-meteo.com/v1/forecast";
}
