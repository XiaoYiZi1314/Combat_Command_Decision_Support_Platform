package com.ccds.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.constant.AuthRuleConstant;
import com.ccds.iam.identity.enums.AccountRoleEnum;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.jwt.JwtPayload;
import com.ccds.infra.jwt.JwtTokenUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 解析访问令牌并写入请求身份。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String MSG_UNAUTHORIZED = "登录已失效，请重新登录";

    private final JwtTokenUtil jwtTokenUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(AuthRuleConstant.BEARER_PREFIX)) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        String token = header.substring(AuthRuleConstant.BEARER_PREFIX.length()).trim();
        JwtPayload payload;
        try {
            payload = jwtTokenUtil.parseAccessToken(token);
        } catch (RuntimeException ex) {
            log.warn("access token parse failed");
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        AccountRoleEnum role = AccountRoleEnum.fromCode(payload.getRole());
        if (payload.getAccountId() == null || payload.getSessionId() == null || role == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        AuthPrincipal principal = AuthPrincipal.builder()
                .accountId(payload.getAccountId())
                .sessionId(payload.getSessionId())
                .role(role)
                .build();
        request.setAttribute(RequestAttributeConstant.AUTH_PRINCIPAL, principal);
        return true;
    }
}
