package com.ccds.infra.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.ccds.iam.identity.constant.AuthRuleConstant;

import lombok.Getter;
import lombok.Setter;

/**
 * JWT 密钥与有效期，值来自环境变量，禁止硬编码。
 *
 * @author ccds
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ccds.jwt")
public class JwtProperties {

    /**
     * 访问令牌密钥。
     */
    private String accessSecret;

    /**
     * 刷新令牌密钥。
     */
    private String refreshSecret;

    /**
     * 访问令牌有效秒数。
     */
    private long accessTtlSeconds = AuthRuleConstant.DEFAULT_ACCESS_TTL_SECONDS;

    /**
     * 刷新令牌有效秒数。
     */
    private long refreshTtlSeconds = AuthRuleConstant.DEFAULT_REFRESH_TTL_SECONDS;
}
