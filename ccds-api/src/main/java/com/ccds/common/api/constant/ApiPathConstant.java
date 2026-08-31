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
     * 保存个人空呼标定。
     */
    public static final String STATION_PROFILE_SCBA_CALIBRATIONS =
            "/stations/{stationId}/profiles/{profileId}/scba-calibrations";

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
     * 指挥端 WebSocket 一次性票据。
     */
    public static final String AUTH_WS_TICKET = "/auth/ws-ticket";

    /**
     * 指挥端 WebSocket。查询参数只接受短时一次性 ticket。
     */
    public static final String WS_COMMAND = "/ws/command";

    /**
     * 站级水源档案列表与新建。
     */
    public static final String STATION_WATERS = "/stations/{stationId}/waters";

    /**
     * 单条水源档案。
     */
    public static final String STATION_WATER = "/stations/{stationId}/waters/{waterId}";

    /**
     * 水源表格导入。
     */
    public static final String STATION_WATER_IMPORT = "/stations/{stationId}/waters/import";

    /**
     * 水源表格导出。
     */
    public static final String STATION_WATER_EXPORT = "/stations/{stationId}/waters/export";

    /**
     * 水源导入模板。
     */
    public static final String STATION_WATER_TEMPLATE = "/stations/{stationId}/waters/template";

    /**
     * 附近水源（全市在用，半径与条数有默认）。
     */
    public static final String WATER_NEARBY = "/waters/nearby";

    /**
     * 全市水源（登录可读，站级「全市」浏览用）。
     */
    public static final String WATERS_CITY = "/waters/city";

    /**
     * 百度地图浏览器端凭证。
     */
    public static final String MAPS_BAIDU_TOKEN = "/maps/baidu-token";

    /**
     * 指定日期值班。
     */
    public static final String STATION_DUTY_DATE = "/stations/{stationId}/duty/{date}";

    /**
     * 月度值班列表。
     */
    public static final String STATION_DUTY_MONTH = "/stations/{stationId}/duty/month";

    /**
     * 站级重点单位列表与新建。
     */
    public static final String STATION_KEY_UNITS = "/stations/{stationId}/key-units";

    /**
     * 单条重点单位。
     */
    public static final String STATION_KEY_UNIT = "/stations/{stationId}/key-units/{id}";

    /**
     * COS 预签名上传。
     */
    public static final String FILES_PRESIGN = "/files/presign";

    /**
     * 确认预签名文件已上传。
     */
    public static final String FILE_UPLOAD_CONFIRM = "/files/{fileId}/confirm";

    /**
     * 单个文件预览或删除。
     */
    public static final String FILE_OBJECT = "/files/{fileId}";

    /**
     * 创建共享令牌。
     */
    public static final String SHARE = "/share";

    /**
     * 作废共享令牌。
     */
    public static final String SHARE_ID = "/share/{tokenId}";

    /**
     * 匿名共享态势。
     */
    public static final String SHARE_ANONYMOUS = "/s/{token}";

    /**
     * 匿名共享拦截排除模式。
     */
    public static final String SHARE_ANONYMOUS_PATTERN = "/s/**";

    /**
     * 微信共享只读页面。
     */
    public static final String SHARE_PUBLIC_PAGE = "/s/{token}";

    /**
     * 天气代理。
     */
    public static final String WEATHER = "/weather";

    /**
     * 危化品检索。
     */
    public static final String HAZMAT_SEARCH = "/hazmat/search";

    /**
     * 危化品视觉/文字研判。
     */
    public static final String ASSIST_HAZMAT_VISION = "/assist/hazmat-vision";

    /**
     * 内攻 AI 研判。
     */
    public static final String ASSIST_ATTACK_ADVICE = "/assist/attack-advice";

    /**
     * 供水 AI 研判。
     */
    public static final String ASSIST_SUPPLY_CALC = "/assist/supply-calc";

    /**
     * AI 通用问答。
     */
    public static final String ASSIST_CHAT = "/assist/chat";

    /**
     * 站点车辆档案列表。
     */
    public static final String STATION_VEHICLES = "/stations/{stationId}/vehicles";

    /**
     * 单车档案。
     */
    public static final String STATION_VEHICLE = "/stations/{stationId}/vehicles/{vehicleId}";

    /**
     * 全部可见车辆（指挥端）。
     */
    public static final String VEHICLES_ALL = "/vehicles";

    /**
     * 车辆变更申请列表。
     */
    public static final String STATION_VEHICLE_REQUESTS = "/stations/{stationId}/vehicle-requests";

    /**
     * 单条车辆变更申请。
     */
    public static final String STATION_VEHICLE_REQUEST = "/stations/{stationId}/vehicle-requests/{requestId}";

    /**
     * 全部车辆变更申请（指挥端）。
     */
    public static final String VEHICLE_REQUESTS_ALL = "/vehicle-requests";

    /**
     * 站点内攻历史列表。
     */
    public static final String STATION_ATTACK_ARCHIVES = "/stations/{stationId}/attack-archives";

    /**
     * 单条内攻历史。
     */
    public static final String STATION_ATTACK_ARCHIVE = "/stations/{stationId}/attack-archives/{archiveId}";

    /**
     * 全部内攻历史（指挥端）。
     */
    public static final String ATTACK_ARCHIVES_ALL = "/attack-archives";

    /**
     * 内攻历史导出。
     */
    public static final String ATTACK_ARCHIVES_EXPORT = "/attack-archives/export";

    /**
     * 内攻历史统计导出。
     */
    public static final String ATTACK_ARCHIVES_EXPORT_STATS = "/attack-archives/export-stats";

    /**
     * AI 文书生成。
     */
    public static final String ASSIST_DOCUMENT = "/assist/document";

    private ApiPathConstant() {
    }
}
