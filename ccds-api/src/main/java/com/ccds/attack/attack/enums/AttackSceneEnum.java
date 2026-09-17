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
    FLAT("flat", AttackRuleConstant.SCENE_FLAT, AttackRuleConstant.SCENE_FLAT_MET,
            AttackRuleConstant.SCENE_FLAT_STRESS, AttackRuleConstant.SCENE_FLAT_ENV),

    /**
     * 高层。
     */
    HIGHRISE("highrise", AttackRuleConstant.SCENE_HIGHRISE, AttackRuleConstant.SCENE_HIGHRISE_MET,
            AttackRuleConstant.SCENE_HIGHRISE_STRESS, AttackRuleConstant.SCENE_HIGHRISE_ENV),

    /**
     * 黑暗。
     */
    DARK("dark", AttackRuleConstant.SCENE_DARK, AttackRuleConstant.SCENE_DARK_MET,
            AttackRuleConstant.SCENE_DARK_STRESS, AttackRuleConstant.SCENE_DARK_ENV),

    /**
     * 大跨度。
     */
    LARGE("large", AttackRuleConstant.SCENE_LARGE, AttackRuleConstant.SCENE_LARGE_MET,
            AttackRuleConstant.SCENE_LARGE_STRESS, AttackRuleConstant.SCENE_LARGE_ENV),

    /**
     * 搜救。
     */
    SEARCH("search", AttackRuleConstant.SCENE_SEARCH, AttackRuleConstant.SCENE_SEARCH_MET,
            AttackRuleConstant.SCENE_SEARCH_STRESS, AttackRuleConstant.SCENE_SEARCH_ENV);

    private final String code;

    private final BigDecimal factor;

    private final BigDecimal metScale;

    private final BigDecimal stress;

    private final BigDecimal env;

    AttackSceneEnum(String code, BigDecimal factor, BigDecimal metScale, BigDecimal stress, BigDecimal env) {
        this.code = code;
        this.factor = factor;
        this.metScale = metScale;
        this.stress = stress;
        this.env = env;
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
     * 代谢倍率。
     *
     * @return 倍率
     */
    public BigDecimal getMetScale() {
        return metScale;
    }

    /**
     * 心理负荷。
     *
     * @return 系数
     */
    public BigDecimal getStress() {
        return stress;
    }

    /**
     * 环境系数。
     *
     * @return 系数
     */
    public BigDecimal getEnv() {
        return env;
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
