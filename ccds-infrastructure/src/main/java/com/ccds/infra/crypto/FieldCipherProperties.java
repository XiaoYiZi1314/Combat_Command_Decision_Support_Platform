package com.ccds.infra.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务字段加解密密钥，值来自环境变量，禁止硬编码。
 *
 * @author ccds
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ccds.field")
public class FieldCipherProperties {

    /**
     * AES-256 密钥，十六进制 64 位。
     */
    private String secret;
}
