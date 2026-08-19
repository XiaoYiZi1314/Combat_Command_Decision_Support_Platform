package com.ccds.infra.crypto;

import com.ccds.roster.roster.constant.RosterRuleConstant;

/**
 * 敏感字段脱敏。业务里禁止手写截串。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class SensitiveMaskUtil {

    private SensitiveMaskUtil() {
    }

    /**
     * 电话脱敏：保留前 3 后 4。
     *
     * @param phone 明文电话，允许为 null
     * @return 脱敏值，空输入为 null
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String compact = phone.trim();
        int prefix = RosterRuleConstant.PHONE_MASK_PREFIX;
        int suffix = RosterRuleConstant.PHONE_MASK_SUFFIX;
        if (compact.length() <= prefix + suffix) {
            return RosterRuleConstant.PHONE_MASK;
        }
        return compact.substring(0, prefix)
                + RosterRuleConstant.PHONE_MASK
                + compact.substring(compact.length() - suffix);
    }
}
