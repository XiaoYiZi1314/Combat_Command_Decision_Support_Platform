package com.ccds.assist.assist.constant;

/**
 * 危化品检索与 AI 研判规则。Key 只在服务端，APP 不持有。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class AssistRuleConstant {

    /**
     * 检索词最短长度。
     */
    public static final int QUERY_MIN_LENGTH = 1;

    /**
     * 检索词最长长度。
     */
    public static final int QUERY_MAX_LENGTH = 64;

    /**
     * 检索默认条数。
     */
    public static final int SEARCH_DEFAULT_LIMIT = 20;

    /**
     * 检索条数上限。
     */
    public static final int SEARCH_MAX_LIMIT = 50;

    /**
     * 文字补充描述最长。
     */
    public static final int TEXT_MAX_LENGTH = 500;

    /**
     * 火点/现场描述最长。
     */
    public static final int SCENE_DESC_MAX_LENGTH = 200;

    /**
     * 天气摘要最长。
     */
    public static final int WEATHER_SUMMARY_MAX_LENGTH = 200;

    /**
     * 内攻研判展示姓名最长。
     */
    public static final int DISPLAY_NAME_MAX_LENGTH = 10;

    /**
     * 内攻研判编组名最长。
     */
    public static final int GROUP_NAME_MAX_LENGTH = 32;

    /**
     * 状态码最长。
     */
    public static final int STATUS_CODE_MAX_LENGTH = 16;

    /**
     * 内攻研判最多带入的未撤出人员。
     */
    public static final int ATTACK_PERSON_MAX = 40;

    /**
     * 供水附近水源最多带入条数。
     */
    public static final int SUPPLY_SOURCE_MAX = 8;

    /**
     * 供水参战车辆最多带入条数。
     */
    public static final int SUPPLY_VEHICLE_MAX = 20;

    /**
     * 图片 Base64 最长字符（约 1.5MB JPEG）。
     */
    public static final int IMAGE_BASE64_MAX_LENGTH = 2_000_000;

    /**
     * 模型连接超时毫秒。
     */
    public static final int MODEL_CONNECT_TIMEOUT_MS = 5000;

    /**
     * 模型读取超时毫秒。
     */
    public static final int MODEL_READ_TIMEOUT_MS = 30000;

    /**
     * 模型最大重试。
     */
    public static final int MODEL_RETRY_MAX = 3;

    /**
     * 检索连接超时毫秒。
     */
    public static final int SEARCH_CONNECT_TIMEOUT_MS = 5000;

    /**
     * 检索读取超时毫秒。
     */
    public static final int SEARCH_READ_TIMEOUT_MS = 15000;

    /**
     * 检索最大重试。
     */
    public static final int SEARCH_RETRY_MAX = 3;

    /**
     * 检索内存缓存秒数。
     */
    public static final int SEARCH_CACHE_TTL_SECONDS = 300;

    /**
     * 检索缓存最多条目。
     */
    public static final int SEARCH_CACHE_MAX_ENTRIES = 64;

    /**
     * 默认模型名。
     */
    public static final String DEFAULT_MODEL_NAME = "qwen-plus";

    /**
     * 聊天补全路径。
     */
    public static final String MODEL_CHAT_PATH = "/chat/completions";

    /**
     * 界面必须保留的辅助声明。
     */
    public static final String AUXILIARY_DISCLAIMER = "本结果仅作辅助参考，不替代现场指挥。";

    /**
     * 模型未配置时的提示。
     */
    public static final String MSG_MODEL_UNAVAILABLE = "辅助研判暂不可用";

    /**
     * 检索服务未配置时的提示。
     */
    public static final String MSG_SEARCH_UNAVAILABLE = "危化品检索暂不可用";

    /**
     * 检索词不合法。
     */
    public static final String MSG_QUERY_INVALID = "请输入名称、俗称、UN 或 CAS";

    /**
     * 危化品研判入参不合法。
     */
    public static final String MSG_VISION_INVALID = "请提供图片或文字描述";

    /**
     * 内攻研判入参不合法。
     */
    public static final String MSG_ATTACK_INVALID = "内攻研判参数不合法";

    /**
     * 供水研判入参不合法。
     */
    public static final String MSG_SUPPLY_INVALID = "供水研判参数不合法";

    /**
     * 无权使用该站辅助。
     */
    public static final String MSG_STATION_FORBIDDEN = "无权使用该单位辅助研判";

    /**
     * 登录失效。
     */
    public static final String MSG_UNAUTHORIZED = "登录已失效，请重新登录";

    private AssistRuleConstant() {
    }
}
