package com.ccds.infra.crypto;

/**
 * 敏感字段脱敏。业务里禁止手写截串。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class SensitiveMaskUtil {

    private static final int PHONE_MASK_PREFIX = 3;

    private static final int PHONE_MASK_SUFFIX = 4;

    private static final String PHONE_MASK = "****";

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
        if (compact.length() <= PHONE_MASK_PREFIX + PHONE_MASK_SUFFIX) {
            return PHONE_MASK;
        }
        return compact.substring(0, PHONE_MASK_PREFIX)
                + PHONE_MASK
                + compact.substring(compact.length() - PHONE_MASK_SUFFIX);
    }
}
