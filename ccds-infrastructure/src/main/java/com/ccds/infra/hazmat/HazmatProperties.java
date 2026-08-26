package com.ccds.infra.hazmat;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 危化品检索上游配置。地址只来自环境变量。
 *
 * @author ccds
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ccds.hazmat")
public class HazmatProperties {

    /**
     * 应急管理部库或兼容检索地址。空则拒绝。
     */
    private String searchUrl;
}
