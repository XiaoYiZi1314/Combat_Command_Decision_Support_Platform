package com.ccds.water.water.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.water.water.service.MapTokenService;
import com.ccds.water.water.vo.BaiduMapTokenVO;

import lombok.RequiredArgsConstructor;

/**
 * 地图凭证接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class MapTokenController {

    private final MapTokenService mapTokenService;

    /**
     * 浏览器端百度地图 AK。
     *
     * @param request HTTP 请求
     * @return 凭证
     */
    @GetMapping(ApiPathConstant.MAPS_BAIDU_TOKEN)
    public ApiResultVO<BaiduMapTokenVO> baiduToken(HttpServletRequest request) {
        return ApiResultVO.ok(mapTokenService.baiduToken());
    }
}
