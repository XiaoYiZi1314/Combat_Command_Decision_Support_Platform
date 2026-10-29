package com.ccds.water.water.enums;

/**
 * 水源设置方式：地上 / 地下。
 *
 * @author ccds
 * @since 0.1.2
 */
public enum WaterSetupEnum {

    /**
     * 地上。
     */
    ABOVE("above", "地上"),

    /**
     * 地下。
     */
    UNDER("under", "地下");

    private final String code;

    private final String label;

    WaterSetupEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 库内设置方式码。
     *
     * @return 设置方式码
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
     * 解析设置方式码。未知值为 null。
     *
     * @param code 设置方式码
     * @return 设置方式枚举
     */
    public static WaterSetupEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String trimmed = code.trim();
        for (WaterSetupEnum item : values()) {
            if (item.code.equalsIgnoreCase(trimmed)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 按中文原文解析设置方式。含「地下」为地下，含「地上」为地上，其余 null。
     *
     * @param raw 原文
     * @return 设置方式枚举
     */
    public static WaterSetupEnum fromText(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        if (text.contains("地下")) {
            return UNDER;
        }
        if (text.contains("地上")) {
            return ABOVE;
        }
        return fromCode(text);
    }
}
