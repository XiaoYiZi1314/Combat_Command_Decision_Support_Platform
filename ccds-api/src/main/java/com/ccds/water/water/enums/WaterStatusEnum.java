package com.ccds.water.water.enums;

/**
 * 水源状态：在用 / 报修。
 *
 * @author ccds
 * @since 0.1.0
 */
public enum WaterStatusEnum {

    /**
     * 在用。
     */
    ACTIVE("active", "在用"),

    /**
     * 报修。
     */
    REPAIR("repair", "报修");

    private final String code;

    private final String label;

    WaterStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 库内状态码。
     *
     * @return 状态码
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
     * 解析状态码。未知值为 null。
     *
     * @param code 状态码
     * @return 状态枚举
     */
    public static WaterStatusEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String trimmed = code.trim();
        for (WaterStatusEnum item : values()) {
            if (item.code.equals(trimmed)) {
                return item;
            }
        }
        return null;
    }
}
