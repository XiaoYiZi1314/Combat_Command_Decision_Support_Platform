package com.ccds.common.service.impl;

import com.ccds.common.api.vo.HealthVO;
import com.ccds.common.service.HealthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 骨架阶段仅报告进程已启动。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
public class HealthServiceImpl implements HealthService {

    private static final String STATUS_UP = "UP";

    private static final String APPLICATION_NAME = "ccds-bootstrap";

    /**
     * {@inheritDoc}
     */
    @Override
    public HealthVO getHealth() {
        log.info("health check application={}", APPLICATION_NAME);
        return HealthVO.builder()
                .status(STATUS_UP)
                .application(APPLICATION_NAME)
                .build();
    }
}
