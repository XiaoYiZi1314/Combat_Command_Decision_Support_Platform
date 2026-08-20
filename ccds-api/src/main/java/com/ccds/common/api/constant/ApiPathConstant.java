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

    /**
     * 站级花名册与编组。
     */
    public static final String STATION_ROSTER = "/stations/{stationId}/roster";

    /**
     * 站级人员档案列表与新建。
     */
    public static final String STATION_PROFILES = "/stations/{stationId}/profiles";

    /**
     * 单条人员档案。
     */
    public static final String STATION_PROFILE = "/stations/{stationId}/profiles/{profileId}";

    /**
     * 站级战斗编组整存。
     */
    public static final String STATION_GROUPS = "/stations/{stationId}/groups";

    /**
     * 花名册表格导入。
     */
    public static final String STATION_PROFILE_IMPORT = "/stations/{stationId}/profiles/import";

    /**
     * 花名册表格导出。
     */
    public static final String STATION_PROFILE_EXPORT = "/stations/{stationId}/profiles/export";

    /**
     * 花名册导入模板。
     */
    public static final String STATION_PROFILE_TEMPLATE = "/stations/{stationId}/profiles/template";

    /**
     * 本站内攻卡片与快照。
     */
    public static final String STATION_ATTACK = "/stations/{stationId}/attack";

    /**
     * 本站内攻事件（预录入/入场/复测/撤出/删除）。
     */
    public static final String STATION_ATTACK_EVENTS = "/stations/{stationId}/attack/events";

    /**
     * 指挥端站快照列表。
     */
    public static final String ATTACK_SNAPSHOTS = "/attack/snapshots";

    /**
     * 指挥端 WebSocket。查询参数 token=访问令牌。
     */
    public static final String WS_COMMAND = "/ws/command";

    private ApiPathConstant() {
    }
}
