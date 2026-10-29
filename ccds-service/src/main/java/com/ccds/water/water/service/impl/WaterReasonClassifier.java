package com.ccds.water.water.service.impl;

import com.ccds.water.water.enums.WaterPartEnum;
import com.ccds.water.water.enums.WaterReasonEnum;

/**
 * 水源无法出水原因与损坏部件的纯函数归类器。
 *
 * <p>无 Spring、无数据库依赖，供导入、CRUD 与汇总统计共用。归类顺序由枚举声明顺序决定
 * （见 {@link WaterReasonEnum} / {@link WaterPartEnum}），只命中第一个。</p>
 *
 * <p>仅在卡片「不好用」时调用；好用的卡片必须把原因码与部件码清空，不得因备注里出现关键字就补打勾。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
public final class WaterReasonClassifier {

    private WaterReasonClassifier() {
    }

    /**
     * 归类原因。九项都未命中时兜底「毁坏」。
     *
     * @param reasonText 原因原文，可空
     * @return 原因枚举，不会为 null
     */
    public static WaterReasonEnum classifyReason(String reasonText) {
        String text = reasonText == null ? "" : reasonText.trim();
        if (text.isEmpty()) {
            return WaterReasonEnum.DAMAGED;
        }
        for (WaterReasonEnum item : WaterReasonEnum.values()) {
            if (item == WaterReasonEnum.DAMAGED) {
                continue;
            }
            if (containsAny(text, item.getKeywords())) {
                return item;
            }
        }
        return WaterReasonEnum.DAMAGED;
    }

    /**
     * 归类部件。八项都未命中时兜底「闸阀」。
     *
     * @param reasonText 原因原文，可空
     * @return 部件枚举，不会为 null
     */
    public static WaterPartEnum classifyPart(String reasonText) {
        String text = reasonText == null ? "" : reasonText.trim();
        if (text.isEmpty()) {
            return WaterPartEnum.VALVE;
        }
        for (WaterPartEnum item : WaterPartEnum.values()) {
            if (containsAny(text, item.getKeywords())) {
                return item;
            }
        }
        return WaterPartEnum.VALVE;
    }

    /**
     * 归类原因码，便于直接落库。
     *
     * @param reasonText 原因原文
     * @return 原因码
     */
    public static String classifyReasonCode(String reasonText) {
        return classifyReason(reasonText).getCode();
    }

    /**
     * 归类部件码，便于直接落库。
     *
     * @param reasonText 原因原文
     * @return 部件码
     */
    public static String classifyPartCode(String reasonText) {
        return classifyPart(reasonText).getCode();
    }

    private static boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
