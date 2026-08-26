package com.ccds.infra.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 模型网关配置。Key 只来自环境变量，禁止硬编码。
 *
 * @author ccds
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ccds.model")
public class ModelProperties {

    /**
     * 模型 API Key。
     */
    private String apiKey;

    /**
     * 模型接口地址，空则拒绝调用。
     */
    private String baseUrl;

    /**
     * 模型名，空则用规则常量默认值。
     */
    private String modelName;
}
