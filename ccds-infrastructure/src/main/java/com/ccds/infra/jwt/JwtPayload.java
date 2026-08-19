package com.ccds.infra.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已解析的令牌声明。不含令牌原文。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtPayload {

    /**
     * 账号主键。
     */
    private Long accountId;

    /**
     * 角色码。
     */
    private String role;

    /**
     * 会话主键。
     */
    private Long sessionId;
}
