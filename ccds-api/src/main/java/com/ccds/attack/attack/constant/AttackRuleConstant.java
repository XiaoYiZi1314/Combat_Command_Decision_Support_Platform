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
     * 平地代谢倍率。
     */
    public static final BigDecimal SCENE_FLAT_MET = BigDecimal.ONE;

    /**
     * 平地心理负荷。
     */
    public static final BigDecimal SCENE_FLAT_STRESS = BigDecimal.ONE;

    /**
     * 平地环境系数。
     */
    public static final BigDecimal SCENE_FLAT_ENV = BigDecimal.ONE;

    /**
     * 高层代谢倍率。
     */
    public static final BigDecimal SCENE_HIGHRISE_MET = new BigDecimal("1.12");

    /**
     * 高层心理负荷。
     */
    public static final BigDecimal SCENE_HIGHRISE_STRESS = new BigDecimal("1.04");

    /**
     * 高层环境系数。
     */
    public static final BigDecimal SCENE_HIGHRISE_ENV = new BigDecimal("1.02");

    /**
     * 黑暗代谢倍率。
     */
    public static final BigDecimal SCENE_DARK_MET = new BigDecimal("0.95");

    /**
     * 黑暗心理负荷。
     */
    public static final BigDecimal SCENE_DARK_STRESS = new BigDecimal("1.12");

    /**
     * 黑暗环境系数。
     */
    public static final BigDecimal SCENE_DARK_ENV = new BigDecimal("1.02");

    /**
     * 大跨度代谢倍率。
     */
    public static final BigDecimal SCENE_LARGE_MET = new BigDecimal("1.04");

    /**
     * 大跨度心理负荷。
     */
    public static final BigDecimal SCENE_LARGE_STRESS = new BigDecimal("1.04");

    /**
     * 大跨度环境系数。
     */
    public static final BigDecimal SCENE_LARGE_ENV = new BigDecimal("1.02");

    /**
     * 搜救代谢倍率。
     */
    public static final BigDecimal SCENE_SEARCH_MET = new BigDecimal("1.12");

    /**
     * 搜救心理负荷。
     */
    public static final BigDecimal SCENE_SEARCH_STRESS = new BigDecimal("1.08");

    /**
     * 搜救环境系数。
     */
    public static final BigDecimal SCENE_SEARCH_ENV = new BigDecimal("1.03");

    /**
     * 默认代谢 K。
     */
    public static final BigDecimal DEFAULT_K_PER_MET = new BigDecimal("6.35");

    /**
     * 高强度参考 MET 备用。
     */
    public static final BigDecimal HIGH_REFERENCE_MET = new BigDecimal("12");

    /**
     * 低强度 MET。
     */
    public static final BigDecimal MET_LIGHT = new BigDecimal("6.7");

    /**
     * 中强度 MET。
     */
    public static final BigDecimal MET_MODERATE = new BigDecimal("8.9");

    /**
     * 高强度 MET，平地现场用此值。
     */
    public static final BigDecimal MET_HEAVY = new BigDecimal("11.5");

    /**
     * 个人 K 相对默认 K 的下限倍率。
     */
    public static final BigDecimal PERSONAL_K_MIN_FACTOR = new BigDecimal("0.58");

    /**
     * 个人 K 相对默认 K 的上限倍率。
     */
    public static final BigDecimal PERSONAL_K_MAX_FACTOR = new BigDecimal("1.75");

    /**
     * 加权标定条数。
     */
    public static final int CALIBRATION_WINDOW = 6;

    /**
     * RMV 下限 L/min。
     */
    public static final BigDecimal RMV_MIN = new BigDecimal("22");

    /**
     * RMV 上限 L/min。
     */
    public static final BigDecimal RMV_MAX = new BigDecimal("105");

    /**
     * 压降速率下限 MPa/分。
     */
    public static final BigDecimal RATE_MIN = new BigDecimal("0.05");

    /**
     * 压降速率上限 MPa/分。
     */
    public static final BigDecimal RATE_MAX = new BigDecimal("1.9");

    /**
     * 体型起算身高 cm。
     */
    public static final int DEMO_HEIGHT_MIN_CM = 120;

    /**
     * 体型起算体重 kg。
     */
    public static final int DEMO_WEIGHT_MIN_KG = 35;

    /**
     * BSA 参考值。
     */
    public static final BigDecimal BSA_REF = new BigDecimal("1.84");

    /**
     * BSA 系数。
     */
    public static final BigDecimal BSA_COEF = new BigDecimal("0.26");

    /**
     * BSA 公式分母。
     */
    public static final int BSA_DIVISOR = 3600;

    /**
     * 高 BMI 门槛。
     */
    public static final BigDecimal BMI_HIGH = new BigDecimal("28");

    /**
     * 高 BMI 系数。
     */
    public static final BigDecimal BMI_HIGH_COEF = new BigDecimal("0.018");

    /**
     * 高 BMI 附加上限。
     */
    public static final BigDecimal BMI_HIGH_ADD_MAX = new BigDecimal("0.16");

    /**
     * 低 BMI 门槛。
     */
    public static final BigDecimal BMI_LOW = new BigDecimal("20");

    /**
     * 低 BMI 系数。
     */
    public static final BigDecimal BMI_LOW_COEF = new BigDecimal("0.01");

    /**
     * 低 BMI 附加上限。
     */
    public static final BigDecimal BMI_LOW_ADD_MAX = new BigDecimal("0.06");

    /**
     * 高年龄门槛。
     */
    public static final int AGE_HIGH = 35;

    /**
     * 高年龄系数。
     */
    public static final BigDecimal AGE_HIGH_COEF = new BigDecimal("0.005");

    /**
     * 高年龄附加上限。
     */
    public static final BigDecimal AGE_HIGH_ADD_MAX = new BigDecimal("0.18");

    /**
     * 低年龄门槛。
     */
    public static final int AGE_YOUNG = 25;

    /**
     * 低年龄倍率。
     */
    public static final BigDecimal AGE_YOUNG_FACTOR = new BigDecimal("0.98");

    /**
     * 体型系数下限。
     */
    public static final BigDecimal DEMO_FACTOR_MIN = new BigDecimal("0.78");

    /**
     * 体型系数上限。
     */
    public static final BigDecimal DEMO_FACTOR_MAX = new BigDecimal("1.38");

    /**
     * 默认模型体型缩小系数。
     */
    public static final BigDecimal DEFAULT_BODY_BLEND = new BigDecimal("0.35");

    /**
     * 默认模型体型缩放下限。
     */
    public static final BigDecimal DEFAULT_BODY_MIN = new BigDecimal("0.90");

    /**
     * 默认模型体型缩放上限。
     */
    public static final BigDecimal DEFAULT_BODY_MAX = new BigDecimal("1.12");

    /**
     * 校准系数在综合耗气中的权重。
     */
    public static final BigDecimal COMBINED_CAL_BLEND = new BigDecimal("0.75");

    /**
     * 体型在综合耗气中的权重。
     */
    public static final BigDecimal COMBINED_BODY_BLEND = new BigDecimal("0.25");

    /**
     * 综合耗气系数下限。
     */
    public static final BigDecimal COMBINED_FACTOR_MIN = new BigDecimal("0.62");

    /**
     * 综合耗气系数上限。
     */
    public static final BigDecimal COMBINED_FACTOR_MAX = new BigDecimal("1.85");

    /**
     * 只有校准时公式速率权重。
     */
    public static final BigDecimal BLEND_FORMULA_WITH_CAL = new BigDecimal("0.25");

    /**
     * 只有校准时校准速率权重。
     */
    public static final BigDecimal BLEND_CAL_ONLY = new BigDecimal("0.75");

    /**
     * 校准混合后相对公式速率的下限倍率。
     */
    public static final BigDecimal BLEND_CAL_CLAMP_MIN = new BigDecimal("0.92");

    /**
     * 校准混合后相对公式速率的上限倍率。
     */
    public static final BigDecimal BLEND_CAL_CLAMP_MAX = new BigDecimal("1.40");

    /**
     * 剩余夹取低压门槛 MPa。
     */
    public static final BigDecimal REMAIN_BAND_LOW_PRESSURE = new BigDecimal("8");

    /**
     * 剩余夹取中压门槛 MPa。
     */
    public static final BigDecimal REMAIN_BAND_MID_PRESSURE = new BigDecimal("15");

    /**
     * 低压剩余下限倍率。
     */
    public static final BigDecimal REMAIN_LOW_MIN_FACTOR = new BigDecimal("0.70");

    /**
     * 低压剩余上限倍率。
     */
    public static final BigDecimal REMAIN_LOW_MAX_FACTOR = new BigDecimal("1.10");

    /**
     * 低压剩余上限附加分钟。
     */
    public static final BigDecimal REMAIN_LOW_MAX_ADD_MIN = new BigDecimal("0.7");

    /**
     * 中高压剩余下限倍率。
     */
    public static final BigDecimal REMAIN_MID_MIN_FACTOR = new BigDecimal("0.72");

    /**
     * 中压剩余上限倍率。
     */
    public static final BigDecimal REMAIN_MID_MAX_FACTOR = new BigDecimal("1.14");

    /**
     * 中压剩余上限附加分钟。
     */
    public static final BigDecimal REMAIN_MID_MAX_ADD_MIN = new BigDecimal("1.2");

    /**
     * 高压剩余上限倍率。
     */
    public static final BigDecimal REMAIN_HIGH_MAX_FACTOR = new BigDecimal("1.18");

    /**
     * 高压剩余上限附加分钟。
     */
    public static final BigDecimal REMAIN_HIGH_MAX_ADD_MIN = new BigDecimal("2.0");

    /**
     * 计算器报警哨压力 MPa，仅训练标定使用。
     */
    public static final BigDecimal CAL_ALARM_PRESSURE = new BigDecimal("5");

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

    /**
     * 指挥端 WS 断线后改轮询的等待毫秒。
     */
    public static final int COMMAND_POLL_AFTER_DISCONNECT_MS = 3000;

    /**
     * 指挥端降级轮询间隔毫秒。
     */
    public static final int COMMAND_POLL_INTERVAL_MS = 5000;

    /**
     * 离线补传最大重试次数。
     */
    public static final int OFFLINE_RETRY_MAX = 5;

    /**
     * 离线补传首次退避毫秒。
     */
    public static final int OFFLINE_RETRY_BASE_MS = 1000;

    /**
     * 离线补传最大退避毫秒。
     */
    public static final int OFFLINE_RETRY_MAX_MS = 30000;

    /**
     * 指挥 WS 握手查询参数名，只允许短时一次性票据。
     */
    public static final String WS_TICKET_QUERY = "ticket";

    /**
     * 指挥端未更新变琥珀的秒数。
     */
    public static final int STALE_AMBER_SEC = 120;

    /**
     * 指挥端未更新变红的秒数。
     */
    public static final int STALE_RED_SEC = 300;

    /**
     * 指挥端未更新文案。
     */
    public static final String STALE_HINT = "请向现场核实";

    /**
     * 覆盖冲突提示。
     */
    public static final String MSG_CLOUD_OVERRIDE = "已用云端更新覆盖";

    private AttackRuleConstant() {
    }
}
