package com.ccds.bootstrap.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ccds.infra.weather.WeatherProperties;

/**
 * 天气上游配置注册。
 *
 * @author ccds
 * @since 0.1.0
 */
@Configuration
@EnableConfigurationProperties(WeatherProperties.class)
public class WeatherPropertiesConfig {
}
