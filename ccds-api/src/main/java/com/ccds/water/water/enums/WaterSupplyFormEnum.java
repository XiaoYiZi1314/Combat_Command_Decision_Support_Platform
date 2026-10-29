package com.ccds.water.water.enums;

/**
 * 水源给水形式：环状 / 枝状。
 *
 * @author ccds
 * @since 0.1.2
 */
public enum WaterSupplyFormEnum {

    /**
     * 环状。
     */
    RING("ring", "环状"),

    /**
     * 枝状。
     */
    BRANCH("branch", "枝状");

    private final String code;

    private final String label;

    WaterSupplyFormEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 库内给水形式码。
     *
     * @return 给水形式码
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
     * 解析给水形式码。未知值为 null。
     *
     * @param code 给水形式码
     * @return 给水形式枚举
     */
    public static WaterSupplyFormEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String trimmed = code.trim();
        for (WaterSupplyFormEnum item : values()) {
            if (item.code.equalsIgnoreCase(trimmed)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 按中文原文解析给水形式。含「环」为环状，含「枝」为枝状，其余 null。
     *
     * @param raw 原文
     * @return 给水形式枚举
     */
    public static WaterSupplyFormEnum fromText(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        if (text.contains("环")) {
            return RING;
        }
        if (text.contains("枝")) {
            return BRANCH;
        }
        return fromCode(text);
    }
}
