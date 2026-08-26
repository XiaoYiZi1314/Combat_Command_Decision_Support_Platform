package com.ccds.infra.model;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.ccds.assist.assist.constant.AssistRuleConstant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * 模型网关。Key 只在服务端；未配置则拒绝，禁止返回假研判。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
public class ModelGatewayImpl implements ModelGateway {

    private static final String MSG_NOT_CONFIGURED = "model gateway not configured";

    private static final String MSG_CALL_FAILED = "model call failed";

    private final ModelProperties modelProperties;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    /**
     * 构造模型网关。
     *
     * @param modelProperties 环境配置
     * @param objectMapper    JSON
     */
    public ModelGatewayImpl(ModelProperties modelProperties, ObjectMapper objectMapper) {
        this.modelProperties = modelProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(AssistRuleConstant.MODEL_CONNECT_TIMEOUT_MS))
                .build();
    }

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
        return complete(systemPrompt, userPrompt, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String completeVision(String systemPrompt, String userPrompt, String imageBase64, String contentType) {
        return complete(systemPrompt, userPrompt, imageBase64, contentType);
    }

    private String complete(String systemPrompt, String userPrompt, String imageBase64, String contentType) {
        requireConfigured();
        IllegalStateException last = null;
        for (int attempt = 1; attempt <= AssistRuleConstant.MODEL_RETRY_MAX; attempt++) {
            long started = System.currentTimeMillis();
            try {
                String text = postOnce(systemPrompt, userPrompt, imageBase64, contentType);
                log.info("模型调用成功 attempt={} elapsedMs={}", attempt, System.currentTimeMillis() - started);
                return text;
            } catch (RetryableCallException ex) {
                last = ex;
                log.warn("模型调用可重试失败 attempt={}", attempt, ex);
            }
        }
        if (last == null) {
            throw new IllegalStateException(MSG_CALL_FAILED);
        }
        throw last;
    }

    private String postOnce(String systemPrompt, String userPrompt, String imageBase64, String contentType) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(chatUrl()))
                    .timeout(Duration.ofMillis(AssistRuleConstant.MODEL_READ_TIMEOUT_MS))
                    .header("Authorization", "Bearer " + modelProperties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            buildBody(systemPrompt, userPrompt, imageBase64, contentType)))
                    .build();
        } catch (IllegalArgumentException | IOException ex) {
            throw new IllegalStateException(MSG_CALL_FAILED, ex);
        }
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return readContent(response.body());
            }
            if (status == 429 || status >= 500) {
                throw new RetryableCallException("model http " + status);
            }
            log.error("模型调用失败 status={}", status);
            throw new IllegalStateException(MSG_CALL_FAILED);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(MSG_CALL_FAILED, ex);
        } catch (IOException ex) {
            throw new RetryableCallException(MSG_CALL_FAILED, ex);
        }
    }

    private String buildBody(String systemPrompt, String userPrompt, String imageBase64, String contentType)
            throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", resolveModelName());
        ArrayNode messages = root.putArray("messages");
        ObjectNode systemNode = messages.addObject();
        systemNode.put("role", "system");
        systemNode.put("content", nullToEmpty(systemPrompt));
        ObjectNode userNode = messages.addObject();
        userNode.put("role", "user");
        if (!hasText(imageBase64)) {
            userNode.put("content", nullToEmpty(userPrompt));
        } else {
            ArrayNode content = userNode.putArray("content");
            content.addObject().put("type", "text").put("text", nullToEmpty(userPrompt));
            String mime = hasText(contentType) ? contentType.trim() : "image/jpeg";
            content.addObject().put("type", "image_url")
                    .putObject("image_url")
                    .put("url", "data:" + mime + ";base64," + imageBase64);
        }
        return objectMapper.writeValueAsString(root);
    }

    private String readContent(String body) {
        if (!hasText(body)) {
            throw new IllegalStateException(MSG_CALL_FAILED);
        }
        try {
            JsonNode content = objectMapper.readTree(body).path("choices").path(0).path("message").path("content");
            if (content.isTextual()) {
                String text = content.asText();
                if (hasText(text)) {
                    return text.trim();
                }
            }
            if (content.isArray()) {
                StringBuilder builder = new StringBuilder();
                for (JsonNode part : content) {
                    if (part.has("text") && part.get("text").isTextual()) {
                        builder.append(part.get("text").asText());
                    }
                }
                String text = builder.toString().trim();
                if (hasText(text)) {
                    return text;
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(MSG_CALL_FAILED, ex);
        }
        throw new IllegalStateException(MSG_CALL_FAILED);
    }

    private String chatUrl() {
        String base = modelProperties.getBaseUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith(AssistRuleConstant.MODEL_CHAT_PATH)) {
            return base;
        }
        return base + AssistRuleConstant.MODEL_CHAT_PATH;
    }

    private String resolveModelName() {
        if (hasText(modelProperties.getModelName())) {
            return modelProperties.getModelName().trim();
        }
        return AssistRuleConstant.DEFAULT_MODEL_NAME;
    }

    private void requireConfigured() {
        if (!available()) {
            log.warn("模型网关未配置");
            throw new IllegalStateException(MSG_NOT_CONFIGURED);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class RetryableCallException extends IllegalStateException {

        private RetryableCallException(String message) {
            super(message);
        }

        private RetryableCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
