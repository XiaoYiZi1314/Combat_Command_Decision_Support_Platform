package com.ccds.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ccds.common.api.constant.ApiPathConstant;

import lombok.RequiredArgsConstructor;

/**
 * 鉴权拦截配置。
 *
 * @author ccds
 * @since 0.1.0
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    private final MustChangePasswordInterceptor mustChangePasswordInterceptor;

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(ApiPathConstant.API_V1 + "/**")
                .excludePathPatterns(
                        ApiPathConstant.API_V1 + ApiPathConstant.HEALTH,
                        ApiPathConstant.API_V1 + ApiPathConstant.AUTH_LOGIN,
                        ApiPathConstant.API_V1 + ApiPathConstant.AUTH_REFRESH,
                        ApiPathConstant.API_V1 + ApiPathConstant.SHARE_ANONYMOUS_PATTERN);
        registry.addInterceptor(mustChangePasswordInterceptor)
                .addPathPatterns(ApiPathConstant.API_V1 + "/**")
                .excludePathPatterns(
                        ApiPathConstant.API_V1 + ApiPathConstant.HEALTH,
                        ApiPathConstant.API_V1 + ApiPathConstant.AUTH_LOGIN,
                        ApiPathConstant.API_V1 + ApiPathConstant.AUTH_REFRESH,
                        ApiPathConstant.API_V1 + ApiPathConstant.AUTH_PASSWORD,
                        ApiPathConstant.API_V1 + ApiPathConstant.AUTH_LOGOUT,
                        ApiPathConstant.API_V1 + ApiPathConstant.ME,
                        ApiPathConstant.API_V1 + ApiPathConstant.ORG_STATIONS,
                        ApiPathConstant.API_V1 + ApiPathConstant.SHARE_ANONYMOUS_PATTERN);
    }
}
