package com.ccds.assist.assist.service.impl;

import java.io.IOException;

import com.ccds.assist.assist.constant.AssistRuleConstant;
import com.ccds.assist.assist.dto.AttackAdviceResultDTO;
import com.ccds.assist.assist.dto.HazmatVisionResultDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 把模型文本收成约定字段。解析失败时把原文落到主字段，不编造结论。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class AssistModelReplyParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AssistModelReplyParser() {
    }

    /**
     * 内攻：轮换、撤离、风险。
     *
     * @param raw 模型原文
     * @return 结果，不会为 null
     */
    public static AttackAdviceResultDTO parseAttack(String raw) {
        String text = normalize(raw);
        JsonNode node = extractObject(text);
        String rotation = textOf(node, "rotationAdvice", "rotation");
        String withdraw = textOf(node, "withdrawAdvice", "withdraw");
        String risk = textOf(node, "riskHint", "risk");
        if (!hasText(rotation)) {
            rotation = labeled(text, "轮换");
        }
        if (!hasText(withdraw)) {
            withdraw = labeled(text, "撤离");
        }
        if (!hasText(risk)) {
            risk = labeled(text, "风险");
        }
        if (!hasText(rotation) && !hasText(withdraw) && !hasText(risk)) {
            rotation = text;
        }
        return AttackAdviceResultDTO.builder()
                .rotationAdvice(rotation)
                .withdrawAdvice(withdraw)
                .riskHint(risk)
                .disclaimer(AssistRuleConstant.AUXILIARY_DISCLAIMER)
                .build();
    }

    /**
     * 危化品：可能名称、危险性、处置。
     *
     * @param raw 模型原文
     * @return 结果，不会为 null
     */
    public static HazmatVisionResultDTO parseVision(String raw) {
        String text = normalize(raw);
        JsonNode node = extractObject(text);
        String name = textOf(node, "possibleName", "name");
        String hazard = textOf(node, "hazardSummary", "hazard");
        String advice = textOf(node, "advice", "responseHint");
        if (!hasText(name)) {
            name = labeled(text, "可能物质", "名称");
        }
        if (!hasText(hazard)) {
            hazard = labeled(text, "危险");
        }
        if (!hasText(advice)) {
            advice = labeled(text, "处置", "建议");
        }
        if (!hasText(name) && !hasText(hazard) && !hasText(advice)) {
            hazard = text;
        }
        return HazmatVisionResultDTO.builder()
                .possibleName(name)
                .hazardSummary(hazard)
                .advice(advice)
                .disclaimer(AssistRuleConstant.AUXILIARY_DISCLAIMER)
                .build();
    }

    private static JsonNode extractObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(text.substring(start, end + 1));
            if (node != null && node.isObject()) {
                return node;
            }
        } catch (IOException ex) {
            return null;
        }
        return null;
    }

    private static String textOf(JsonNode node, String... keys) {
        if (node == null) {
            return "";
        }
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && value.isValueNode() && hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return "";
    }

    private static String labeled(String text, String... labels) {
        for (String label : labels) {
            int index = text.indexOf(label);
            if (index < 0) {
                continue;
            }
            int colon = text.indexOf('：', index);
            if (colon < 0) {
                colon = text.indexOf(':', index);
            }
            if (colon < 0) {
                continue;
            }
            int nextLine = text.indexOf('\n', colon + 1);
            String value = nextLine < 0 ? text.substring(colon + 1) : text.substring(colon + 1, nextLine);
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
