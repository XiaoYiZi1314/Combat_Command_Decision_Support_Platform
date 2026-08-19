package com.ccds.ops.health.enums;

/**
 * 进程健康状态。
 *
 * @author ccds
 * @since 0.1.0
 */
public enum HealthStatusEnum {

    /**
     * 进程已启动。
     */
    UP("UP");

    private final String code;

    HealthStatusEnum(String code) {
        this.code = code;
    }

    /**
     * 对外状态码。
     *
     * @return 状态码
     */
    public String getCode() {
        return code;
    }
}
