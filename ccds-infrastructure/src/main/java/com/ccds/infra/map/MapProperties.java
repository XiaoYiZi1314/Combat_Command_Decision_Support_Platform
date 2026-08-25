package com.ccds.infra.map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 地图配置，值来自环境变量，禁止硬编码 AK。
 *
 * @author ccds
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ccds.map")
public class MapProperties {

    /**
     * 百度地图浏览器端 AK。
     */
    private String baiduAk;
}
