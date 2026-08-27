package com.ccds.iam.identity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 指挥 WebSocket 一次性握手票据。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "ticket")
public class WebSocketTicketVO {

    /**
     * 短时一次性票据。
     */
    private String ticket;

    /**
     * 有效秒数。
     */
    private Long expiresInSeconds;
}
