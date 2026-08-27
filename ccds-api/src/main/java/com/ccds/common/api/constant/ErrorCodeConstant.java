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

    /**
     * 无权查看该站。
     */
    public static final String ROSTER_STATION_FORBIDDEN = "ROSTER_STATION_FORBIDDEN";

    /**
     * 无权改该站档案。
     */
    public static final String ROSTER_WRITE_FORBIDDEN = "ROSTER_WRITE_FORBIDDEN";

    /**
     * 档案不存在。
     */
    public static final String ROSTER_PROFILE_NOT_FOUND = "ROSTER_PROFILE_NOT_FOUND";

    /**
     * 本站姓名重复。
     */
    public static final String ROSTER_NAME_DUPLICATE = "ROSTER_NAME_DUPLICATE";

    /**
     * 本站 NFC 编号重复。
     */
    public static final String ROSTER_NFC_DUPLICATE = "ROSTER_NFC_DUPLICATE";

    /**
     * 导入表格无法解析。
     */
    public static final String ROSTER_IMPORT_INVALID = "ROSTER_IMPORT_INVALID";

    /**
     * 档案有未撤出内攻卡片，禁止删除。
     */
    public static final String ROSTER_PROFILE_IN_ATTACK = "ROSTER_PROFILE_IN_ATTACK";

    /**
     * 无权查看该站内攻。
     */
    public static final String ATTACK_STATION_FORBIDDEN = "ATTACK_STATION_FORBIDDEN";

    /**
     * 无权改该站内攻。
     */
    public static final String ATTACK_WRITE_FORBIDDEN = "ATTACK_WRITE_FORBIDDEN";

    /**
     * 人员卡片不存在。
     */
    public static final String ATTACK_PERSON_NOT_FOUND = "ATTACK_PERSON_NOT_FOUND";

    /**
     * 同一人已有未撤出卡片。
     */
    public static final String ATTACK_PERSON_DUPLICATE = "ATTACK_PERSON_DUPLICATE";

    /**
     * 状态不允许该操作。
     */
    public static final String ATTACK_STATUS_INVALID = "ATTACK_STATUS_INVALID";

    /**
     * 事件类型或载荷不合法。
     */
    public static final String ATTACK_EVENT_INVALID = "ATTACK_EVENT_INVALID";

    /**
     * NFC 未匹配本站花名册。
     */
    public static final String ATTACK_NFC_NOT_FOUND = "ATTACK_NFC_NOT_FOUND";

    /**
     * 无权查看该站水源。
     */
    public static final String WATER_STATION_FORBIDDEN = "WATER_STATION_FORBIDDEN";

    /**
     * 仅本站账号可改水源。
     */
    public static final String WATER_WRITE_FORBIDDEN = "WATER_WRITE_FORBIDDEN";

    /**
     * 水源档案不存在。
     */
    public static final String WATER_NOT_FOUND = "WATER_NOT_FOUND";

    /**
     * 水源名称重复。
     */
    public static final String WATER_NAME_DUPLICATE = "WATER_NAME_DUPLICATE";

    /**
     * 水源表格无法解析。
     */
    public static final String WATER_IMPORT_INVALID = "WATER_IMPORT_INVALID";

    /**
     * 附近水源坐标不合法。
     */
    public static final String WATER_COORD_INVALID = "WATER_COORD_INVALID";

    /**
     * 百度地图 AK 未配置。
     */
    public static final String MAP_AK_MISSING = "MAP_AK_MISSING";

    /**
     * 无权查看该站值班/预案/共享。
     */
    public static final String DUTY_STATION_FORBIDDEN = "DUTY_STATION_FORBIDDEN";

    /**
     * 仅本站账号可改值班/预案/共享。
     */
    public static final String DUTY_WRITE_FORBIDDEN = "DUTY_WRITE_FORBIDDEN";

    /**
     * 主官或副职不在本站花名册。
     */
    public static final String DUTY_OFFICER_INVALID = "DUTY_OFFICER_INVALID";

    /**
     * 重点单位不存在。
     */
    public static final String KEY_UNIT_NOT_FOUND = "KEY_UNIT_NOT_FOUND";

    /**
     * 重点单位类别不合法。
     */
    public static final String KEY_UNIT_CATEGORY_INVALID = "KEY_UNIT_CATEGORY_INVALID";

    /**
     * 文件不存在。
     */
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";

    /**
     * 文件关联业务不存在。
     */
    public static final String FILE_BIZ_NOT_FOUND = "FILE_BIZ_NOT_FOUND";

    /**
     * 文件业务类型不合法。
     */
    public static final String FILE_BIZ_TYPE_INVALID = "FILE_BIZ_TYPE_INVALID";

    /**
     * 文件 MIME 不在白名单。
     */
    public static final String FILE_CONTENT_TYPE_INVALID = "FILE_CONTENT_TYPE_INVALID";

    /**
     * 文件超过大小限制。
     */
    public static final String FILE_SIZE_INVALID = "FILE_SIZE_INVALID";

    /**
     * COS 未配置或调用失败。
     */
    public static final String FILE_COS_UNAVAILABLE = "FILE_COS_UNAVAILABLE";

    /**
     * 文件上传尚未确认、确认票据无效或 COS 对象不匹配。
     */
    public static final String FILE_UPLOAD_CONFIRM_INVALID = "FILE_UPLOAD_CONFIRM_INVALID";

    /**
     * 共享令牌不存在。
     */
    public static final String SHARE_TOKEN_NOT_FOUND = "SHARE_TOKEN_NOT_FOUND";

    /**
     * 共享链接无效。
     */
    public static final String SHARE_TOKEN_INVALID = "SHARE_TOKEN_INVALID";

    /**
     * 共享链接已作废。
     */
    public static final String SHARE_TOKEN_REVOKED = "SHARE_TOKEN_REVOKED";

    /**
     * 共享链接已过期。
     */
    public static final String SHARE_TOKEN_EXPIRED = "SHARE_TOKEN_EXPIRED";

    /**
     * 天气代理未配置。
     */
    public static final String WEATHER_UNAVAILABLE = "WEATHER_UNAVAILABLE";

    /**
     * 天气查询经纬度不合法。
     */
    public static final String WEATHER_COORD_INVALID = "WEATHER_COORD_INVALID";

    /**
     * 模型网关未配置或调用失败。
     */
    public static final String ASSIST_MODEL_UNAVAILABLE = "ASSIST_MODEL_UNAVAILABLE";

    /**
     * 危化品检索词不合法。
     */
    public static final String HAZMAT_QUERY_INVALID = "HAZMAT_QUERY_INVALID";

    /**
     * 危化品检索外部服务不可用。
     */
    public static final String HAZMAT_SEARCH_UNAVAILABLE = "HAZMAT_SEARCH_UNAVAILABLE";

    /**
     * 危化品研判入参不合法。
     */
    public static final String HAZMAT_VISION_INVALID = "HAZMAT_VISION_INVALID";

    /**
     * 内攻研判入参不合法。
     */
    public static final String ASSIST_ATTACK_INVALID = "ASSIST_ATTACK_INVALID";

    /**
     * 供水研判入参不合法。
     */
    public static final String ASSIST_SUPPLY_INVALID = "ASSIST_SUPPLY_INVALID";

    /**
     * 无权使用该站辅助研判。
     */
    public static final String ASSIST_STATION_FORBIDDEN = "ASSIST_STATION_FORBIDDEN";

    private ErrorCodeConstant() {
    }
}
