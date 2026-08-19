package com.ccds.iam.identity.model;

import com.ccds.iam.identity.enums.AccountRoleEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前请求身份，由访问令牌解析得到。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthPrincipal {

    /**
     * 账号主键。
     */
    private Long accountId;

    /**
     * 角色。
     */
    private AccountRoleEnum role;

    /**
     * 会话主键。
     */
    private Long sessionId;
}
