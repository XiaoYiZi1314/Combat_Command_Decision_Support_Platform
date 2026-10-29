package com.ccds.water.water.service.impl;

import com.ccds.water.water.enums.WaterStatusEnum;
import com.ccds.water.water.enums.WaterUsableEnum;

/**
 * 水源「好用 / 不好用」唯一判定口径。
 *
 * <p>全系统只有这一个谓词，界面筛选、卡片配色、汇总数字、导出与附近水源检索都必须调它，
 * 不允许各处另写一套。口径（见客户改法明细 1.2）：</p>
 *
 * <ul>
 *   <li>不好用：状态=报修，或可使用=不好用，或是否报修=是；</li>
 *   <li>好用：状态=在用且可使用不是不好用。</li>
 * </ul>
 *
 * <p>严禁把「原因原文非空」当作不好用：卡片改成在用后，备注里残留的旧原因不得再计入统计。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
public final class WaterStatusResolver {

    private WaterStatusResolver() {
    }

    /**
     * 是否不好用。
     *
     * @param status  状态码 active/repair
     * @param usable  可使用码 good/bad
     * @param isRepair 是否报修 1/0，可空
     * @return 不好用为 true
     */
    public static boolean isBad(String status, String usable, Integer isRepair) {
        if (WaterStatusEnum.REPAIR.getCode().equalsIgnoreCase(nullToEmpty(status))) {
            return true;
        }
        if (WaterUsableEnum.BAD.getCode().equalsIgnoreCase(nullToEmpty(usable))) {
            return true;
        }
        return isRepair != null && isRepair.intValue() == 1;
    }

    /**
     * 是否好用。
     *
     * @param status   状态码
     * @param usable   可使用码
     * @param isRepair 是否报修
     * @return 好用为 true
     */
    public static boolean isGood(String status, String usable, Integer isRepair) {
        if (!WaterStatusEnum.ACTIVE.getCode().equalsIgnoreCase(nullToEmpty(status))) {
            return false;
        }
        return !WaterUsableEnum.BAD.getCode().equalsIgnoreCase(nullToEmpty(usable))
                && (isRepair == null || isRepair.intValue() != 1);
    }

    /**
     * 由可使用 / 是否报修派生状态码。保存时服务端派生，避免前端传入矛盾值。
     *
     * @param usable   可使用码
     * @param isRepair 是否报修
     * @return active / repair
     */
    public static String deriveStatus(String usable, Integer isRepair) {
        if (WaterUsableEnum.BAD.getCode().equalsIgnoreCase(nullToEmpty(usable))) {
            return WaterStatusEnum.REPAIR.getCode();
        }
        if (isRepair != null && isRepair.intValue() == 1) {
            return WaterStatusEnum.REPAIR.getCode();
        }
        return WaterStatusEnum.ACTIVE.getCode();
    }

    /**
     * 归一「是否报修」布尔值为库内整数。
     *
     * @param isRepair 是否报修
     * @return 1/0，空返回 null
     */
    public static Integer normalizeRepairFlag(Boolean isRepair) {
        if (isRepair == null) {
            return null;
        }
        return Integer.valueOf(isRepair.booleanValue() ? 1 : 0);
    }

    /**
     * 由状态码反推可使用码，用于旧数据兜底。
     *
     * @param status 状态码
     * @return 可使用码，未知为 null
     */
    public static String usableOfStatus(String status) {
        if (WaterStatusEnum.REPAIR.getCode().equalsIgnoreCase(nullToEmpty(status))) {
            return WaterUsableEnum.BAD.getCode();
        }
        if (WaterStatusEnum.ACTIVE.getCode().equalsIgnoreCase(nullToEmpty(status))) {
            return WaterUsableEnum.GOOD.getCode();
        }
        return null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
