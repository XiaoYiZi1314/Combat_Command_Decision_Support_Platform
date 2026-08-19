package com.ccds.roster.roster.enums;

import com.ccds.roster.roster.constant.RosterRuleConstant;

/**
 * 气瓶规格。
 *
 * @author ccds
 * @since 0.1.0
 */
public enum CylTypeEnum {

    /**
     * 6.8L。
     */
    L68(RosterRuleConstant.CYL_TYPE_68),

    /**
     * 9L。
     */
    L9(RosterRuleConstant.CYL_TYPE_9);

    private final String code;

    CylTypeEnum(String code) {
        this.code = code;
    }

    /**
     * 库内规格码。
     *
     * @return 规格码
     */
    public String getCode() {
        return code;
    }

    /**
     * 解析规格。未知值回落到 6.8L。
     *
     * @param code 规格码
     * @return 规格枚举，不会为 null
     */
    public static CylTypeEnum fromCodeOrDefault(String code) {
        if (code == null || code.isBlank()) {
            return L68;
        }
        String trimmed = code.trim();
        if (trimmed.endsWith("L") || trimmed.endsWith("l")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        for (CylTypeEnum item : values()) {
            if (item.code.equals(trimmed)) {
                return item;
            }
        }
        return L68;
    }
}
