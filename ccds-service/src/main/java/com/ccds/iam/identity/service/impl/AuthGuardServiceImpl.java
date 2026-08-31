package com.ccds.iam.identity.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.constant.AuthRuleConstant;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.entity.AccountSessionDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.mapper.AccountSessionMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.iam.identity.service.AuthGuardService;

import lombok.RequiredArgsConstructor;

/**
 * 校验会话仍有效，并在强制改密未完成时拦住写业务。
 *
 * @author ccds
 * @since 0.1.0
 */
@Service
@RequiredArgsConstructor
public class AuthGuardServiceImpl implements AuthGuardService {

    private static final String MSG_MUST_CHANGE = "首次登录必须修改密码";

    private final AccountMapper accountMapper;

    private final AccountSessionMapper accountSessionMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public AccountDO requireAccount(AuthPrincipal principal) {
        if (principal == null || principal.getAccountId() == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, AuthRuleConstant.MSG_UNAUTHORIZED);
        }
        AccountDO account = accountMapper.selectById(principal.getAccountId());
        if (account == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, AuthRuleConstant.MSG_UNAUTHORIZED);
        }
        return account;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void assertActiveSession(AuthPrincipal principal) {
        if (principal == null || principal.getAccountId() == null || principal.getSessionId() == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, AuthRuleConstant.MSG_UNAUTHORIZED);
        }
        AccountSessionDO session = accountSessionMapper.selectById(principal.getSessionId());
        if (session == null || !principal.getAccountId().equals(session.getAccountId())) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, AuthRuleConstant.MSG_UNAUTHORIZED);
        }
        if (session.getExpireAt() != null && session.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, AuthRuleConstant.MSG_UNAUTHORIZED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void assertPasswordChangedIfRequired(AuthPrincipal principal) {
        if (principal == null) {
            return;
        }
        AccountDO account = accountMapper.selectById(principal.getAccountId());
        if (account != null && Boolean.TRUE.equals(account.getMustChangePassword())) {
            throw new BizException(ErrorCodeConstant.AUTH_MUST_CHANGE_PASSWORD, MSG_MUST_CHANGE);
        }
    }
}
