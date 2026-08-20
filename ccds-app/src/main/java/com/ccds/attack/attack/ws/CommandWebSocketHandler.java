package com.ccds.attack.attack.ws;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.ccds.attack.attack.service.CommandPushService;
import com.ccds.attack.attack.service.CommandSessionSink;
import com.ccds.attack.attack.vo.CommandPushVO;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 指挥端 WebSocket。连接后按可见站接收快照推送。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandWebSocketHandler extends TextWebSocketHandler {

    private final CommandPushService commandPushService;

    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<String, WebSocketSession>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        AuthPrincipal principal = principalOf(session);
        if (principal == null) {
            closeQuiet(session, CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        sessions.put(session.getId(), session);
        commandPushService.register(session.getId(), principal, new WsSink(session));
        log.info("command ws opened sessionHash={}", Integer.valueOf(session.getId().hashCode()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        commandPushService.unregister(session.getId());
        log.info("command ws closed sessionHash={} code={}", Integer.valueOf(session.getId().hashCode()),
                Integer.valueOf(status.getCode()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("command ws transport error sessionHash={}", Integer.valueOf(session.getId().hashCode()), exception);
        sessions.remove(session.getId());
        commandPushService.unregister(session.getId());
        closeQuiet(session, CloseStatus.SERVER_ERROR);
    }

    private AuthPrincipal principalOf(WebSocketSession session) {
        Object value = session.getAttributes().get(RequestAttributeConstant.AUTH_PRINCIPAL);
        if (value instanceof AuthPrincipal principal) {
            return principal;
        }
        return null;
    }

    private void closeQuiet(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ex) {
            log.warn("command ws close failed", ex);
        }
    }

    private final class WsSink implements CommandSessionSink {

        private final WebSocketSession session;

        private WsSink(WebSocketSession session) {
            this.session = session;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void send(CommandPushVO push) {
            if (!session.isOpen()) {
                return;
            }
            try {
                String json = objectMapper.writeValueAsString(push);
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (JsonProcessingException ex) {
                log.error("command ws serialize failed sessionHash={}", Integer.valueOf(session.getId().hashCode()), ex);
            } catch (IOException ex) {
                log.warn("command ws send failed sessionHash={}", Integer.valueOf(session.getId().hashCode()), ex);
                closeQuiet(session, CloseStatus.SERVER_ERROR);
            }
        }
    }
}
