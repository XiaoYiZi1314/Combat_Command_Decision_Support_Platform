package com.ccds.common.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;

/**
 * CORS 预检请求识别。预检请求不携带业务令牌，各拦截器统一放行，由 CORS 配置响应。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class CorsPreflight {

    private CorsPreflight() {
    }

    /**
     * 判断是否为 CORS 预检（OPTIONS）请求。
     *
     * @param request HTTP 请求
     * @return 是预检请求时为 true
     */
    public static boolean isPreflight(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }
}
