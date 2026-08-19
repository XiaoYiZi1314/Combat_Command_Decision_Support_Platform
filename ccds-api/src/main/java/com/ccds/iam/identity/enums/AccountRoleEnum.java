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
}
