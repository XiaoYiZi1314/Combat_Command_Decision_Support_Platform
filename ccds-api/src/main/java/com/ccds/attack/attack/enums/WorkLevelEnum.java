package com.ccds.attack.attack.enums;

import java.math.BigDecimal;

import com.ccds.attack.attack.constant.AttackRuleConstant;

/**
 * 作业强度。
 *
 * @author ccds
 * @since 0.1.0
 */
public enum WorkLevelEnum {

    /**
     * 低。
     */
    LIGHT("light", AttackRuleConstant.RMV_LIGHT, AttackRuleConstant.MET_LIGHT),

    /**
     * 中。
     */
    MODERATE("moderate", AttackRuleConstant.RMV_MODERATE, AttackRuleConstant.MET_MODERATE),

    /**
     * 高。
     */
    HEAVY("heavy", AttackRuleConstant.RMV_HEAVY, AttackRuleConstant.MET_HEAVY);

    private final String code;

    private final int rmv;

    private final BigDecimal met;

    WorkLevelEnum(String code, int rmv, BigDecimal met) {
        this.code = code;
        this.rmv = rmv;
        this.met = met;
    }

    /**
     * 库内强度码。
     *
     * @return 强度码
     */
    public String getCode() {
        return code;
    }

    /**
     * 默认 RMV L/min。
     *
     * @return RMV
     */
    public int getRmv() {
        return rmv;
    }

    /**
     * 默认 MET。
     *
     * @return MET
     */
    public BigDecimal getMet() {
        return met;
    }

    /**
     * 解析强度。未知回落到中。
     *
     * @param code 强度码
     * @return 枚举，不会为 null
     */
    public static WorkLevelEnum fromCodeOrDefault(String code) {
        if (code == null || code.isBlank()) {
            return MODERATE;
        }
        for (WorkLevelEnum item : values()) {
            if (item.code.equals(code.trim())) {
                return item;
            }
        }
        return MODERATE;
    }
}
