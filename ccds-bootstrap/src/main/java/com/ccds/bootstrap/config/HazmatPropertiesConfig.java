package com.ccds.bootstrap.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ccds.infra.hazmat.HazmatProperties;

/**
 * 危化品检索配置注册。
 *
 * @author ccds
 * @since 0.1.0
 */
@Configuration
@EnableConfigurationProperties(HazmatProperties.class)
public class HazmatPropertiesConfig {
}
