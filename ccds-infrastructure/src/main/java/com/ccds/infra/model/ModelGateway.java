package com.ccds.infra.model;

/**
 * 模型网关。Key 只在服务端，业务只依赖本接口。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface ModelGateway {

    /**
     * 是否已配置可用。
     *
     * @return 已配置为 true
     */
    boolean available();

    /**
     * 纯文本补全。未配置或失败抛非法状态。
     *
     * @param systemPrompt 系统提示，已脱敏
     * @param userPrompt   用户提示，已脱敏
     * @return 模型文本，不会为 null
     */
    String completeText(String systemPrompt, String userPrompt);

    /**
     * 图文补全。imageBase64 可空。未配置或失败抛非法状态。
     *
     * @param systemPrompt 系统提示
     * @param userPrompt   用户提示
     * @param imageBase64  JPEG/PNG Base64，可空
     * @param contentType  图片 MIME，可空
     * @return 模型文本，不会为 null
     */
    String completeVision(String systemPrompt, String userPrompt, String imageBase64, String contentType);
}
