package com.ccds.duty.constant;

import java.util.Set;

/**
 * 重点单位类别常量。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class KeyUnitCategoryConstant {

    private static final Set<String> ALL;

    /**
     * 人员密集场所
     */
    public static final String CROWDED_PLACE = "人员密集场所";

    /**
     * 高层建筑
     */
    public static final String HIGH_RISE = "高层建筑";

    /**
     * 地下建筑
     */
    public static final String UNDERGROUND = "地下建筑";

    /**
     * 危险化学品
     */
    public static final String HAZMAT = "危险化学品";

    /**
     * 文物古建
     */
    public static final String CULTURAL_RELIC = "文物古建";

    /**
     * 大跨度厂房
     */
    public static final String LARGE_SPAN = "大跨度厂房";

    /**
     * 其他。
     */
    public static final String OTHER = "其他";

    static {
        ALL = Set.of(CROWDED_PLACE, HIGH_RISE, UNDERGROUND, HAZMAT, CULTURAL_RELIC, LARGE_SPAN, OTHER);
    }

    private KeyUnitCategoryConstant() {
    }

    /**
     * 是否为已登记类别。
     *
     * @param category 类别
     * @return 已登记为 true
     */
    public static boolean isAllowed(String category) {
        return category != null && ALL.contains(category);
    }
}
