package com.ccds.attack.attack.enums;

import java.math.BigDecimal;

import com.ccds.attack.attack.constant.AttackRuleConstant;

/**
 * 作业场景。
 *
 * @author ccds
 * @since 0.1.0
 */
public enum AttackSceneEnum {

    /**
     * 平地。
     */
    FLAT("flat", AttackRuleConstant.SCENE_FLAT),

    /**
     * 高层。
     */
    HIGHRISE("highrise", AttackRuleConstant.SCENE_HIGHRISE),

    /**
     * 黑暗。
     */
    DARK("dark", AttackRuleConstant.SCENE_DARK),

    /**
     * 大跨度。
     */
    LARGE("large", AttackRuleConstant.SCENE_LARGE),

    /**
     * 搜救。
     */
    SEARCH("search", AttackRuleConstant.SCENE_SEARCH);

    private final String code;

    private final BigDecimal factor;

    AttackSceneEnum(String code, BigDecimal factor) {
        this.code = code;
        this.factor = factor;
    }

    /**
     * 库内场景码。
     *
     * @return 场景码
     */
    public String getCode() {
        return code;
    }

    /**
     * 场景系数。
     *
     * @return 系数
     */
    public BigDecimal getFactor() {
        return factor;
    }

    /**
     * 解析场景。未知回落到平地。
     *
     * @param code 场景码
     * @return 枚举，不会为 null
     */
    public static AttackSceneEnum fromCodeOrDefault(String code) {
        if (code == null || code.isBlank()) {
            return FLAT;
        }
        for (AttackSceneEnum item : values()) {
            if (item.code.equals(code.trim())) {
                return item;
            }
        }
        return FLAT;
    }
}
