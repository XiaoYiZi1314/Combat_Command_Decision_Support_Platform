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
     * 若账号仍须改密且当前路径不在白名单，抛业务异常。
     *
     * @param principal 当前身份
     * @param requestUri 请求路径
     */
    void assertPasswordChangedIfRequired(AuthPrincipal principal, String requestUri);
}
