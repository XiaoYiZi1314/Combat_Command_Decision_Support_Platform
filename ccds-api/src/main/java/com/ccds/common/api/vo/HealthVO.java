package com.ccds.common.api.vo;

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
     * 状态码，骨架阶段固定 UP。
     */
    private String status;

    /**
     * 应用名。
     */
    private String application;
}
