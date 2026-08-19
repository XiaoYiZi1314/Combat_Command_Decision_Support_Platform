package com.ccds.iam.identity.service;

import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 登录后业务守卫。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface AuthGuardService {

    /**
     * 确认访问令牌对应的会话仍有效。登出或改密后应失败。
     *
     * @param principal 当前身份
     */
    void assertActiveSession(AuthPrincipal principal);

    /**
     * 若账号仍须改密，抛业务异常。放行范围由 app 拦截配置决定。
     *
     * @param principal 当前身份
     */
    void assertPasswordChangedIfRequired(AuthPrincipal principal);
}
