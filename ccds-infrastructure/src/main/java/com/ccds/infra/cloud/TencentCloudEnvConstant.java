package com.ccds.infra.cloud;

/**
 * 腾讯云一期环境变量名。只登记键名，禁止写入密钥或口令。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class TencentCloudEnvConstant {

    /**
     * CVM 上 Java 进程监听端口。
     */
    public static final String SERVER_PORT = "CCDS_SERVER_PORT";

    /**
     * 云 MySQL JDBC 地址。
     */
    public static final String MYSQL_URL = "CCDS_MYSQL_URL";

    /**
     * 云 MySQL 用户名。
     */
    public static final String MYSQL_USERNAME = "CCDS_MYSQL_USERNAME";

    /**
     * 云 MySQL 口令。
     */
    public static final String MYSQL_PASSWORD = "CCDS_MYSQL_PASSWORD";

    /**
     * Redis 主机。
     */
    public static final String REDIS_HOST = "CCDS_REDIS_HOST";

    /**
     * Redis 端口。
     */
    public static final String REDIS_PORT = "CCDS_REDIS_PORT";

    /**
     * Redis 口令。
     */
    public static final String REDIS_PASSWORD = "CCDS_REDIS_PASSWORD";

    /**
     * COS 地域。
     */
    public static final String COS_REGION = "CCDS_COS_REGION";

    /**
     * COS 桶名。
     */
    public static final String COS_BUCKET = "CCDS_COS_BUCKET";

    /**
     * COS SecretId。
     */
    public static final String COS_SECRET_ID = "CCDS_COS_SECRET_ID";

    /**
     * COS SecretKey。
     */
    public static final String COS_SECRET_KEY = "CCDS_COS_SECRET_KEY";

    /**
     * JWT 访问令牌密钥。
     */
    public static final String JWT_ACCESS_SECRET = "CCDS_JWT_ACCESS_SECRET";

    /**
     * JWT 刷新令牌密钥。
     */
    public static final String JWT_REFRESH_SECRET = "CCDS_JWT_REFRESH_SECRET";

    /**
     * 百度地图服务端 AK。
     */
    public static final String BAIDU_MAP_AK = "CCDS_BAIDU_MAP_AK";

    /**
     * 模型网关 Key。
     */
    public static final String MODEL_API_KEY = "CCDS_MODEL_API_KEY";

    /**
     * 种子账号一次性口令，仅初始化时使用。
     */
    public static final String SEED_PASSWORD = "CCDS_SEED_PASSWORD";

    /**
     * 种子口令的 BCrypt 结果，写入账号表。
     */
    public static final String SEED_PASSWORD_HASH = "CCDS_SEED_PASSWORD_HASH";

    private TencentCloudEnvConstant() {
    }
}
