package com.ccds.water.water.enums;

import java.util.Locale;

/**
 * 水源「不好用」九原因码。
 *
 * <p>顺序即优先级：{@link WaterReasonClassifier} 按本枚举声明顺序逐个匹配，先命中先得，
 * 全部未命中时兜底 {@link #DAMAGED}。调整顺序会让历史统计整体漂移，改动前必须评估。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
public enum WaterReasonEnum {

    /**
     * 无水。
     */
    NO_WATER("no_water", "无水", new String[] {"无水", "没水", "管网内没", "管道无水", "管网无"}),

    /**
     * 漏水。
     */
    LEAK("leak", "漏水", new String[] {"漏", "锁不住水"}),

    /**
     * 压力不足。
     */
    LOW_PRESSURE("low_pressure", "压力不足", new String[] {"压力"}),

    /**
     * 锈蚀。
     */
    RUST("rust", "锈蚀", new String[] {"锈", "绣"}),

    /**
     * 埋压。
     */
    BURIED("buried", "埋压", new String[] {"埋压", "被埋"}),

    /**
     * 圈占。
     */
    OCCUPIED("occupied", "圈占", new String[] {"圈占", "围挡"}),

    /**
     * 挪用。
     */
    DIVERTED("diverted", "挪用", new String[] {"挪用"}),

    /**
     * 塌井。
     */
    COLLAPSED_WELL("collapsed_well", "塌井", new String[] {"塌井", "井塌"}),

    /**
     * 毁坏。九项都未命中时的兜底。
     */
    DAMAGED("damaged", "毁坏", new String[] {"毁坏", "损坏", "坏"});

    private final String code;

    private final String label;

    private final String[] keywords;

    WaterReasonEnum(String code, String label, String[] keywords) {
        this.code = code;
        this.label = label;
        this.keywords = keywords;
    }

    /**
     * 库内原因码。
     *
     * @return 原因码
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
     * 解析原因码。未知值为 null。
     *
     * @param code 原因码
     * @return 原因枚举
     */
    public static WaterReasonEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String trimmed = code.trim().toLowerCase(Locale.ROOT);
        for (WaterReasonEnum item : values()) {
            if (item.code.equals(trimmed)) {
                return item;
            }
        }
        return null;
    }
}
