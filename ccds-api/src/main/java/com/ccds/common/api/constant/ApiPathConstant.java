package com.ccds.common.api.constant;

/**
 * HTTP 路径常量。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class ApiPathConstant {

    /**
     * 一期接口前缀。
     */
    public static final String API_V1 = "/api/v1";

    /**
     * 健康检查（匿名）。
     */
    public static final String HEALTH = "/health";

    /**
     * 登录。
     */
    public static final String AUTH_LOGIN = "/auth/login";

    /**
     * 改密。
     */
    public static final String AUTH_PASSWORD = "/auth/password";

    /**
     * 登出。
     */
    public static final String AUTH_LOGOUT = "/auth/logout";

    /**
     * 刷新访问令牌。
     */
    public static final String AUTH_REFRESH = "/auth/refresh";

    /**
     * 当前账号与可见站。
     */
    public static final String ME = "/me";

    /**
     * 可见编制树。
     */
    public static final String ORG_STATIONS = "/org/stations";

    private ApiPathConstant() {
    }
}
