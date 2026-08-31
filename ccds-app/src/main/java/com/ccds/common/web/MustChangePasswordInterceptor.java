package com.ccds.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.iam.identity.service.AuthGuardService;

import lombok.RequiredArgsConstructor;

/**
 * 强制改密未完成时，只允许改密、登出与查询自己。
 *
 * @author ccds
 * @since 0.1.0
 */
@Component
@RequiredArgsConstructor
public class MustChangePasswordInterceptor implements HandlerInterceptor {

    private final AuthGuardService authGuardService;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        AuthPrincipal principal = (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
        authGuardService.assertPasswordChangedIfRequired(principal);
        return true;
    }
}
