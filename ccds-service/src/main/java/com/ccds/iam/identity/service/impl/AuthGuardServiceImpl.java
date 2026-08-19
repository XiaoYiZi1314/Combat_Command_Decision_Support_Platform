package com.ccds.iam.identity.service.impl;

import org.springframework.stereotype.Service;

import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.iam.identity.service.AuthGuardService;

import lombok.RequiredArgsConstructor;

/**
 * 强制改密未完成时拦住写业务。
 *
 * @author ccds
 * @since 0.1.0
 */
@Service
@RequiredArgsConstructor
public class AuthGuardServiceImpl implements AuthGuardService {

    private static final String MSG_MUST_CHANGE = "首次登录必须修改密码";

    private final AccountMapper accountMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public void assertPasswordChangedIfRequired(AuthPrincipal principal, String requestUri) {
        if (principal == null || isAllowWhileMustChange(requestUri)) {
            return;
        }
        AccountDO account = accountMapper.selectById(principal.getAccountId());
        if (account != null && Boolean.TRUE.equals(account.getMustChangePassword())) {
            throw new BizException(ErrorCodeConstant.AUTH_MUST_CHANGE_PASSWORD, MSG_MUST_CHANGE);
        }
    }

    private boolean isAllowWhileMustChange(String requestUri) {
        if (requestUri == null) {
            return false;
        }
        return requestUri.endsWith(ApiPathConstant.AUTH_PASSWORD)
                || requestUri.endsWith(ApiPathConstant.AUTH_LOGOUT)
                || requestUri.endsWith(ApiPathConstant.ME);
    }
}
