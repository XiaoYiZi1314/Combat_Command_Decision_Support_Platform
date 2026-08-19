package com.ccds.bootstrap.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ccds.infra.crypto.FieldCipherProperties;
import com.ccds.infra.jwt.JwtProperties;

/**
 * 启用 JWT 与字段加密配置绑定。
 *
 * @author ccds
 * @since 0.1.0
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, FieldCipherProperties.class})
public class JwtPropertiesConfig {
}
