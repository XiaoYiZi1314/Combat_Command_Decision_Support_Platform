package com.ccds.iam.identity.enums;

/**
 * 四级登录身份。
 *
 * @author ccds
 * @since 0.1.0
 */
public enum AccountRoleEnum {

    /**
     * 站级账号。
     */
    STATION("station"),

    /**
     * 大队账号。
     */
    BRIGADE("brigade"),

    /**
     * 支队账号。
     */
    HQ("hq"),

    /**
     * 开发者账号。
     */
    DEVELOPER("developer");

    private final String code;

    AccountRoleEnum(String code) {
        this.code = code;
    }

    /**
     * 库内角色码。
     *
     * @return 角色码
     */
    public String getCode() {
        return code;
    }

    /**
     * 按库内角色码解析。未知码返回 null。
     *
     * @param code 角色码
     * @return 角色枚举，未知时为 null
     */
    public static AccountRoleEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (AccountRoleEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 是否指挥端角色（大队 / 支队 / 开发者）。
     *
     * @return true 表示登录后进指挥汇总
     */
    public boolean isCommandRole() {
        return this == BRIGADE || this == HQ || this == DEVELOPER;
    }
}
