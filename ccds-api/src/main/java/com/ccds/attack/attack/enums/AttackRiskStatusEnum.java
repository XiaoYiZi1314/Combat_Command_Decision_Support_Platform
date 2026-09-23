package com.ccds.attack.attack.enums;

/**
 * 内攻风险展示态。该状态由压力和剩余时间实时派生，不持久化。
 *
 * @author ccds
 * @since 0.1.0
 */
public enum AttackRiskStatusEnum {

    /**
     * 安全。
     */
    IN("in"),

    /**
     * 预警。
     */
    WARN("warn"),

    /**
     * 危险。
     */
    DANGER("danger"),

    /**
     * 已撤出。
     */
    OUT("out");

    private final String code;

    AttackRiskStatusEnum(String code) {
        this.code = code;
    }

    /**
     * 风险展示码。
     *
     * @return 展示码
     */
    public String getCode() {
        return code;
    }
}
