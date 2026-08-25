package com.ccds.bootstrap.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ccds.infra.map.MapProperties;

/**
 * 地图配置注册。
 *
 * @author ccds
 * @since 0.1.0
 */
@Configuration
@EnableConfigurationProperties(MapProperties.class)
public class MapPropertiesConfig {
}
