package com.ccds.common.web;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * H5 开发期同源代理之外的跨域放行。
 *
 * @author ccds
 * @since 0.1.0
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final long CORS_MAX_AGE_SECONDS = 3600L;

    private final String[] allowedOrigins;

    /**
     * 构造跨域配置。
     *
     * @param allowedOrigins 允许的来源列表，逗号分隔
     */
    public CorsConfig(@Value("${ccds.web.cors.allowed-origins:http://127.0.0.1:*,http://localhost:*}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Content-Disposition")
                .allowCredentials(false)
                .maxAge(CORS_MAX_AGE_SECONDS);
    }
}
