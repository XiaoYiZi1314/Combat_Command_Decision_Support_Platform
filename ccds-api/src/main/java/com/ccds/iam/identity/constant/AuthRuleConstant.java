package com.ccds.iam.identity.constant;

/**
 * 登录与改密规则。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class AuthRuleConstant {

    /**
     * 连续失败次数上限。
     */
    public static final int MAX_FAILED_LOGIN = 5;

    /**
     * 锁定时长（分钟）。
     */
    public static final int LOCK_MINUTES = 10;

    /**
     * 新口令最短长度。
     */
    public static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * 访问令牌默认有效秒数（15 分钟）。
     */
    public static final long DEFAULT_ACCESS_TTL_SECONDS = 900L;

    /**
     * 刷新令牌默认有效秒数（14 天）。
     */
    public static final long DEFAULT_REFRESH_TTL_SECONDS = 1_209_600L;

    /**
     * 刷新令牌哈希算法。
     */
    public static final String TOKEN_HASH_ALGORITHM = "SHA-256";

    /**
     * JWT 签发者。
     */
    public static final String JWT_ISSUER = "ccds";

    /**
     * JWT 主题：访问令牌。
     */
    public static final String JWT_SUBJECT_ACCESS = "access";

    /**
     * JWT 主题：刷新令牌。
     */
    public static final String JWT_SUBJECT_REFRESH = "refresh";

    /**
     * JWT 账号声明。
     */
    public static final String JWT_CLAIM_ACCOUNT_ID = "aid";

    /**
     * JWT 角色声明。
     */
    public static final String JWT_CLAIM_ROLE = "role";

    /**
     * JWT 会话声明。
     */
    public static final String JWT_CLAIM_SESSION_ID = "sid";

    /**
     * Authorization 前缀。
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * 设备提示最大长度。
     */
    public static final int DEVICE_HINT_MAX_LENGTH = 64;

    /**
     * 登录名最大长度。
     */
    public static final int USERNAME_MAX_LENGTH = 32;

    /**
     * 新口令最大长度。
     */
    public static final int PASSWORD_MAX_LENGTH = 64;

    private AuthRuleConstant() {
    }
}
