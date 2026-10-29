package com.ccds.water.water.enums;

import java.util.Locale;

/**
 * 水源「部件损坏」八部件码。
 *
 * <p>顺序即优先级，全部未命中时兜底 {@link #VALVE}（闸阀）。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
public enum WaterPartEnum {

    /**
     * 井盖。
     */
    COVER("cover", "井盖", new String[] {"井盖"}),

    /**
     * 出水口闷盖。
     */
    BLIND_COVER("blind_cover", "出水口闷盖", new String[] {"闷盖"}),

    /**
     * 放水阀。
     */
    DRAIN_VALVE("drain_valve", "放水阀", new String[] {"放水阀"}),

    /**
     * 上水阀。
     */
    INLET_VALVE("inlet_valve", "上水阀", new String[] {"上水阀", "开水阀"}),

    /**
     * 标志牌。
     */
    SIGN("sign", "标志牌", new String[] {"标志牌"}),

    /**
     * 出水口。
     */
    OUTLET("outlet", "出水口", new String[] {"出水口"}),

    /**
     * 闸阀。
     */
    VALVE("valve", "闸阀", new String[] {"闸阀", "阀门", "锁芯", "扳手", "转换器", "开关"}),

    /**
     * 管网。
     */
    PIPE("pipe", "管网", new String[] {"管网", "管道", "水管"});

    private final String code;

    private final String label;

    private final String[] keywords;

    WaterPartEnum(String code, String label, String[] keywords) {
        this.code = code;
        this.label = label;
        this.keywords = keywords;
    }

    /**
     * 库内部件码。
     *
     * @return 部件码
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
     * 命中关键字。
     *
     * @return 关键字数组，不会为 null
     */
    public String[] getKeywords() {
        return keywords.clone();
    }

    /**
     * 解析部件码。未知值为 null。
     *
     * @param code 部件码
     * @return 部件枚举
     */
    public static WaterPartEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String trimmed = code.trim().toLowerCase(Locale.ROOT);
        for (WaterPartEnum item : values()) {
            if (item.code.equals(trimmed)) {
                return item;
            }
        }
        return null;
    }
}
