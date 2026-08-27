package com.ccds.attack.attack.ws;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.ccds.attack.attack.constant.AttackRuleConstant;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.iam.identity.service.AuthGuardService;
import com.ccds.infra.redis.WebSocketTicketStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 指挥 WS 握手：只消费短时一次性 ticket，禁止在 URL 中传访问令牌。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandHandshakeInterceptor implements HandshakeInterceptor {

    private final WebSocketTicketStore webSocketTicketStore;
    private final AuthGuardService authGuardService;

    /** {@inheritDoc} */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String ticket = extractTicket(request);
        if (ticket == null) {
            return false;
        }
        AuthPrincipal principal = webSocketTicketStore.consume(ticket);
        if (principal == null || principal.getRole() == null || !principal.getRole().isCommandRole()) {
            log.warn("command ws ticket rejected");
            return false;
        }
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

    /** {@inheritDoc} */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.warn("command ws handshake error", exception);
        }
    }

    private String extractTicket(ServerHttpRequest request) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return null;
        }
        String ticket = servletRequest.getServletRequest()
                .getParameter(AttackRuleConstant.WS_TICKET_QUERY);
        return ticket == null || ticket.isBlank() ? null : ticket.trim();
    }
}
