package com.ccds.attack.attack.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.ccds.common.api.constant.ApiPathConstant;

import lombok.RequiredArgsConstructor;

/**
 * 指挥端 WebSocket 路由。
 *
 * @author ccds
 * @since 0.1.0
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final CommandWebSocketHandler commandWebSocketHandler;

    private final CommandHandshakeInterceptor commandHandshakeInterceptor;

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(commandWebSocketHandler, ApiPathConstant.WS_COMMAND)
                .addInterceptors(commandHandshakeInterceptor)
                .setAllowedOriginPatterns(
                        "http://127.0.0.1:*",
                        "http://localhost:*",
                        "file://*",
                        "null");
    }
}
