package com.ccds.ops.health.vo;

import com.ccds.ops.health.enums.HealthStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 进程健康检查结果。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthVO {

    /**
     * 健康状态。
     */
    private HealthStatusEnum status;

    /**
     * 应用名。
     */
    private String application;
}
