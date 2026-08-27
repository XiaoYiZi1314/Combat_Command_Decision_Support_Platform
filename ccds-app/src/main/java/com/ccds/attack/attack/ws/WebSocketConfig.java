package com.ccds.attack.attack.ws;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.ccds.common.api.constant.ApiPathConstant;

/**
 * 指挥端 WebSocket 路由。
 *
 * @author ccds
 * @since 0.1.0
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CommandWebSocketHandler commandWebSocketHandler;

    private final CommandHandshakeInterceptor commandHandshakeInterceptor;

    private final String[] allowedOrigins;

    /**
     * 构造 WebSocket 路由配置。
     *
     * @param allowedOrigins 允许的来源列表，逗号分隔
     */
    public WebSocketConfig(
            CommandWebSocketHandler commandWebSocketHandler,
            CommandHandshakeInterceptor commandHandshakeInterceptor,
            @Value("${ccds.web.cors.allowed-origins:http://127.0.0.1:*,http://localhost:*}") String allowedOrigins) {
        this.commandWebSocketHandler = commandWebSocketHandler;
        this.commandHandshakeInterceptor = commandHandshakeInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(commandWebSocketHandler, ApiPathConstant.WS_COMMAND)
                .addInterceptors(commandHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins);
    }
}
