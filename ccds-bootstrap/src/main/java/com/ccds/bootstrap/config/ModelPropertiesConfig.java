package com.ccds.bootstrap.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ccds.infra.model.ModelProperties;

/**
 * 模型网关配置注册。
 *
 * @author ccds
 * @since 0.1.0
 */
@Configuration
@EnableConfigurationProperties(ModelProperties.class)
public class ModelPropertiesConfig {
}
