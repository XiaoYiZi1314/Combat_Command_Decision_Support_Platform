package com.ccds.common.api.constant;

/**
 * 对外错误码。内部抛业务异常，HTTP 层转成码。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class ErrorCodeConstant {

    /**
     * 成功。
     */
    public static final String SUCCESS = "0";

    /**
     * 账号或口令错误（不区分原因）。
     */
    public static final String AUTH_LOGIN_FAILED = "AUTH_LOGIN_FAILED";

    /**
     * 账号已锁定。
     */
    public static final String AUTH_LOCKED = "AUTH_LOCKED";

    /**
     * 必须先改密。
     */
    public static final String AUTH_MUST_CHANGE_PASSWORD = "AUTH_MUST_CHANGE_PASSWORD";

    /**
     * 未登录或令牌失效。
     */
    public static final String AUTH_UNAUTHORIZED = "AUTH_UNAUTHORIZED";

    /**
     * 刷新令牌失效。
     */
    public static final String AUTH_REFRESH_INVALID = "AUTH_REFRESH_INVALID";

    /**
     * 新口令不符合规则。
     */
    public static final String AUTH_PASSWORD_INVALID = "AUTH_PASSWORD_INVALID";

    /**
     * 入参不合法。
     */
    public static final String PARAM_INVALID = "PARAM_INVALID";

    /**
     * 系统故障。
     */
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    private ErrorCodeConstant() {
    }
}
