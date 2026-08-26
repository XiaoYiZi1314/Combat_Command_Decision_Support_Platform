package com.ccds.infra.model;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 模型网关占位实现。未配置真实厂商时拒绝调用，禁止返回假研判。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelGatewayImpl implements ModelGateway {

    private static final String MSG_NOT_CONFIGURED = "model gateway not configured";

    private final ModelProperties modelProperties;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean available() {
        return hasText(modelProperties.getApiKey()) && hasText(modelProperties.getBaseUrl());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String completeText(String systemPrompt, String userPrompt) {
        requireConfigured();
        throw new IllegalStateException(MSG_NOT_CONFIGURED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String completeVision(String systemPrompt, String userPrompt, String imageBase64, String contentType) {
        requireConfigured();
        throw new IllegalStateException(MSG_NOT_CONFIGURED);
    }

    private void requireConfigured() {
        if (!available()) {
            log.warn("模型网关未配置");
        }
        throw new IllegalStateException(MSG_NOT_CONFIGURED);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
