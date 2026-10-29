package com.ccds.water.water.enums;

import com.ccds.water.water.constant.WaterRuleConstant;

/**
 * 水源类型：消防水鹤 / 地上消火栓 / 地下消火栓。
 *
 * @author ccds
 * @since 0.1.0
 */
public enum WaterTypeEnum {

    /**
     * 消防水鹤。
     */
    CRANE("crane", "消防水鹤", WaterRuleConstant.ESTIMATE_FLOW_CRANE),

    /**
     * 地上消火栓。
     */
    GROUND("ground", "地上消火栓", WaterRuleConstant.ESTIMATE_FLOW_HYDRANT),

    /**
     * 地下消火栓。
     */
    UNDERGROUND("underground", "地下消火栓", WaterRuleConstant.ESTIMATE_FLOW_HYDRANT);

    private final String code;

    private final String label;

    private final double estimateFlow;

    WaterTypeEnum(String code, String label, double estimateFlow) {
        this.code = code;
        this.label = label;
        this.estimateFlow = estimateFlow;
    }

    /**
     * 库内类型码。
     *
     * @return 类型码
     */
    public String getCode() {
        return code;
    }

    /**
     * 展示名。
     *
     * @return 展示名
     */
    public String getLabel() {
        return label;
    }

    /**
     * 无档案流量时的估算流量 L/s。
     *
     * @return 估算流量
     */
    public double getEstimateFlow() {
        return estimateFlow;
    }

    /**
     * 解析类型码。未知值为 null。
     *
     * @param code 类型码
     * @return 类型枚举
     */
    public static WaterTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String trimmed = code.trim();
        for (WaterTypeEnum item : values()) {
            if (item.code.equals(trimmed)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 按展示名解析。兼容「消火栓」等现网叫法。
     *
     * @param label 展示名
     * @return 类型枚举，未知为 null
     */
    public static WaterTypeEnum fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String text = label.trim();
        if (text.contains("水鹤")) {
            return CRANE;
        }
        if (text.contains("消火栓") || text.contains("消防栓")) {
            return text.contains("地下") ? UNDERGROUND : GROUND;
        }
        return fromCode(text);
    }

    /**
     * 类型归一（导入与 CRUD 共用的唯一口径）。
     *
     * <p>规则：类别文本含「水鹤」即水鹤；否则设置方式、名称、地址任一含「地下」即地下消火栓；
     * 其余为地上消火栓。设置方式优先于类别文本，修正「类别=消火栓 + 设置方式=地下」被误判为地上的缺陷。</p>
     *
     * @param typeText  类别原文，可空
     * @param setupText 设置方式原文，可空
     * @param name      名称，可空
     * @param address   地址，可空
     * @return 类型枚举，无法判定时为 null
     */
    public static WaterTypeEnum resolve(String typeText, String setupText, String name, String address) {
        String type = typeText == null ? "" : typeText.trim();
        String setup = setupText == null ? "" : setupText.trim();
        if (type.contains("水鹤")) {
            return CRANE;
        }
        if (setup.contains("地下") || containsText(name, "地下") || containsText(address, "地下")) {
            return UNDERGROUND;
        }
        WaterTypeEnum byLabel = fromLabel(type);
        if (byLabel == CRANE) {
            return GROUND;
        }
        if (byLabel != null) {
            return byLabel;
        }
        if (type.isEmpty()) {
            return null;
        }
        return GROUND;
    }

    private static boolean containsText(String text, String keyword) {
        return text != null && text.contains(keyword);
    }
}
