package com.ccds.attack.attack.constant;

import java.math.BigDecimal;

/**
 * 内攻字段、阈值与空呼估算常量。前后端必须同一套数值。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class AttackRuleConstant {

    /**
     * 展示姓名最大长度。
     */
    public static final int NAME_MAX_LENGTH = 10;

    /**
     * 编组名最大长度。
     */
    public static final int GROUP_NAME_MAX_LENGTH = 32;

    /**
     * 客户端事件幂等键最大长度。
     */
    public static final int EVENT_ID_MAX_LENGTH = 64;

    /**
     * 默认压力 MPa。
     */
    public static final String DEFAULT_PRESSURE = "25.0";

    /**
     * 压力下限 MPa。
     */
    public static final String PRESSURE_MIN = "0.0";

    /**
     * 压力上限 MPa。
     */
    public static final String PRESSURE_MAX = "30.0";

    /**
     * 默认压力预警阈值 MPa。
     */
    public static final BigDecimal WARN_PRESSURE = new BigDecimal("10.0");

    /**
     * 默认压力危险阈值 MPa。
     */
    public static final BigDecimal DANGER_PRESSURE = new BigDecimal("4.0");

    /**
     * 默认时间预警阈值秒。
     */
    public static final int WARN_TIME_SEC = 12 * 60;

    /**
     * 默认时间危险阈值秒。
     */
    public static final int DANGER_TIME_SEC = 5 * 60;

    /**
     * 6.8L 气瓶容积。
     */
    public static final BigDecimal CYL_VOLUME_68 = new BigDecimal("6.8");

    /**
     * 9L 气瓶容积。
     */
    public static final BigDecimal CYL_VOLUME_9 = new BigDecimal("9");

    /**
     * 低强度默认 RMV L/min。
     */
    public static final int RMV_LIGHT = 41;

    /**
     * 中强度默认 RMV L/min。
     */
    public static final int RMV_MODERATE = 55;

    /**
     * 高强度默认 RMV L/min。
     */
    public static final int RMV_HEAVY = 68;

    /**
     * 平地场景系数。
     */
    public static final BigDecimal SCENE_FLAT = BigDecimal.ONE;

    /**
     * 高层场景系数。
     */
    public static final BigDecimal SCENE_HIGHRISE = new BigDecimal("1.19");

    /**
     * 黑暗场景系数。
     */
    public static final BigDecimal SCENE_DARK = new BigDecimal("1.09");

    /**
     * 大跨度场景系数。
     */
    public static final BigDecimal SCENE_LARGE = new BigDecimal("1.10");

    /**
     * 搜救场景系数。
     */
    public static final BigDecimal SCENE_SEARCH = new BigDecimal("1.25");

    /**
     * 强制撤离参考系数。
     */
    public static final BigDecimal EVAC_RATIO = new BigDecimal("0.8");

    /**
     * 气压换算：剩余分钟 = 压力 * 10 * 容积 / RMV。
     */
    public static final int PRESSURE_TO_VOLUME = 10;

    /**
     * 秒/分钟。
     */
    public static final int SECONDS_PER_MINUTE = 60;

    /**
     * 未分组展示名。
     */
    public static final String UNGROUPED_NAME = "未分组";

    /**
     * 编组 Tab「全部」。
     */
    public static final String GROUP_TAB_ALL = "全部";

    private AttackRuleConstant() {
    }
}
