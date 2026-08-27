package com.ccds.duty.controller;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ccds.common.api.constant.ApiPathConstant;

/**
 * 微信扫码共享页入口。页面通过匿名 API 拉取脱敏态势。
 *
 * @author ccds
 * @since 0.1.0
 */
@Controller
public class SharePageController {

    private static final String SHARE_PAGE_FORWARD = "forward:/share/index.html";

    private static final String CACHE_CONTROL_NO_STORE = "no-store, max-age=0";

    private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; "
            + "script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    /**
     * 返回微信可直接打开的只读共享页面。
     *
     * @param token    共享令牌，仅由页面 URL 和匿名 API 使用
     * @param response HTTP 响应
     * @return 静态共享页面转发地址
     */
    @GetMapping(ApiPathConstant.SHARE_PUBLIC_PAGE)
    public String page(@PathVariable String token, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE);
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        return SHARE_PAGE_FORWARD;
    }
}
