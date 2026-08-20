package com.ccds.attack.attack.ws;

import java.util.List;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.ccds.attack.attack.constant.AttackRuleConstant;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.iam.identity.constant.AuthRuleConstant;
import com.ccds.iam.identity.enums.AccountRoleEnum;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.iam.identity.service.AuthGuardService;
import com.ccds.infra.jwt.JwtPayload;
import com.ccds.infra.jwt.JwtTokenUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 指挥 WS 握手：从查询参数 token 解析访问令牌。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenUtil jwtTokenUtil;

    private final AuthGuardService authGuardService;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            return false;
        }
        JwtPayload payload;
        try {
            payload = jwtTokenUtil.parseAccessToken(token);
        } catch (RuntimeException ex) {
            log.warn("command ws token parse failed", ex);
            return false;
        }
        AccountRoleEnum role = AccountRoleEnum.fromCode(payload.getRole());
        if (payload.getAccountId() == null || payload.getSessionId() == null || role == null) {
            return false;
        }
        if (!role.isCommandRole()) {
            log.warn("command ws rejected non-command role");
            return false;
        }
        AuthPrincipal principal = AuthPrincipal.builder()
                .accountId(payload.getAccountId())
                .sessionId(payload.getSessionId())
                .role(role)
                .build();
        try {
            authGuardService.assertActiveSession(principal);
            authGuardService.assertPasswordChangedIfRequired(principal);
        } catch (RuntimeException ex) {
            log.warn("command ws auth rejected", ex);
            return false;
        }
        attributes.put(RequestAttributeConstant.AUTH_PRINCIPAL, principal);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.warn("command ws handshake error", exception);
        }
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String query = servletRequest.getServletRequest().getParameter(AttackRuleConstant.WS_TOKEN_QUERY);
            if (query != null && !query.isBlank()) {
                return query.trim();
            }
        }
        List<String> auth = request.getHeaders().get("Authorization");
        if (auth == null || auth.isEmpty()) {
            return null;
        }
        String header = auth.get(0);
        if (header == null || !header.startsWith(AuthRuleConstant.BEARER_PREFIX)) {
            return null;
        }
        return header.substring(AuthRuleConstant.BEARER_PREFIX.length()).trim();
    }
}
