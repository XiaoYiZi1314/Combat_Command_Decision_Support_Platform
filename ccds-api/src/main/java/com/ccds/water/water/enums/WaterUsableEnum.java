package com.ccds.water.water.enums;

/**
 * 水源可使用状态：好用 / 不好用。
 *
 * @author ccds
 * @since 0.1.2
 */
public enum WaterUsableEnum {

    /**
     * 好用。
     */
    GOOD("good", "好用"),

    /**
     * 不好用。
     */
    BAD("bad", "不好用");

    private final String code;

    private final String label;

    WaterUsableEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 库内可使用码。
     *
     * @return 可使用码
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
     * 解析可使用码。未知值为 null。
     *
     * @param code 可使用码
     * @return 可使用枚举
     */
    public static WaterUsableEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String trimmed = code.trim();
        for (WaterUsableEnum item : values()) {
            if (item.code.equalsIgnoreCase(trimmed)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 按中文原文解析可使用。含「不好用」或「坏」为不好用，含「好用」为好用，其余 null。
     *
     * @param raw 原文
     * @return 可使用枚举
     */
    public static WaterUsableEnum fromText(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        if (text.contains("不好用") || text.contains("坏") || text.contains("否")) {
            return BAD;
        }
        if (text.contains("好用") || text.contains("是")) {
            return GOOD;
        }
        return fromCode(text);
    }
}
