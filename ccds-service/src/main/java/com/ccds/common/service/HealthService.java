package com.ccds.common.service;

import com.ccds.common.api.vo.HealthVO;

/**
 * 进程健康查询。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface HealthService {

    /**
     * 返回当前进程健康状态。
     *
     * @return 健康结果，不会为 null
     */
    HealthVO getHealth();
}
