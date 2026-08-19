package com.ccds.ops.health.service.impl;

import org.springframework.stereotype.Service;

import com.ccds.ops.health.enums.HealthStatusEnum;
import com.ccds.ops.health.service.HealthService;
import com.ccds.ops.health.vo.HealthVO;

/**
 * 骨架阶段仅报告进程已启动。
 *
 * @author ccds
 * @since 0.1.0
 */
@Service
public class HealthServiceImpl implements HealthService {

    private static final String APPLICATION_NAME = "ccds-bootstrap";

    /**
     * {@inheritDoc}
     */
    @Override
    public HealthVO getHealth() {
        return HealthVO.builder()
                .status(HealthStatusEnum.UP)
                .application(APPLICATION_NAME)
                .build();
    }
}
