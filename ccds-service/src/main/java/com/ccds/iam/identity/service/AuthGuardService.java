package com.ccds.iam.identity.service;

import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 登录后业务守卫。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface AuthGuardService {

    /**
     * 加载并校验当前身份对应的账号。未登录或账号已不存在时抛鉴权异常。
     *
     * @param principal 当前身份
     * @return 账号实体
     */
    AccountDO requireAccount(AuthPrincipal principal);

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
