package com.ccds.common.controller;

import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.vo.HealthVO;
import com.ccds.common.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class HealthController {

    private final HealthService healthService;

    /**
     * 匿名健康检查，供空模块启动验收。
     *
     * @return 健康结果
     */
    @GetMapping(ApiPathConstant.HEALTH)
    public HealthVO health() {
        return healthService.getHealth();
    }
}
