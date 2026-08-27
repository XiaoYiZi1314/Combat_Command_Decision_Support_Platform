package com.ccds.duty.constant;

/**
 * 天气代理规则。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class WeatherRuleConstant {

    /** 连接超时毫秒数。 */
    public static final int CONNECT_TIMEOUT_MS = 5000;

    /** 单次读取超时毫秒数。 */
    public static final int READ_TIMEOUT_MS = 10000;

    /** 返回小时预报数量。 */
    public static final int HOURLY_LIMIT = 12;

    /** 天气缓存秒数。 */
    public static final long CACHE_TTL_SECONDS = 600L;

    /** 天气缓存最大坐标格数量。 */
    public static final int CACHE_MAX_ENTRIES = 200;

    /** 坐标缓存粒度的小数位数，约为公里级，避免保存精确位置。 */
    public static final int CACHE_COORD_SCALE = 2;

    /** 千米每小时转米每秒除数。 */
    public static final double KILOMETRES_PER_HOUR_TO_METRES_PER_SECOND = 3.6D;

    private WeatherRuleConstant() {
    }
}
