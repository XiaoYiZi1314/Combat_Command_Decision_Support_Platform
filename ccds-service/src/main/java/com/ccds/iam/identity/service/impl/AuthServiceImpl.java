package com.ccds.iam.identity.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.constant.AuthRuleConstant;
import com.ccds.iam.identity.dto.ChangePasswordCommand;
import com.ccds.iam.identity.dto.LoginCommand;
import com.ccds.iam.identity.dto.RefreshTokenCommand;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.entity.AccountSessionDO;
import com.ccds.iam.identity.enums.AccountRoleEnum;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.mapper.AccountSessionMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.iam.identity.service.AuthService;
import com.ccds.iam.identity.vo.LoginVO;
import com.ccds.iam.identity.vo.MeVO;
import com.ccds.iam.identity.vo.OrgTreeVO;
import com.ccds.infra.crypto.PasswordHashUtil;
import com.ccds.infra.crypto.TokenHashUtil;
import com.ccds.infra.jwt.JwtPayload;
import com.ccds.infra.jwt.JwtTokenUtil;
import com.ccds.org.org.service.OrgQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录锁定、强制改密与 JWT 会话。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String MSG_LOGIN_FAILED = "账号或密码错误";

    private static final String MSG_LOCKED = "账号已锁定，请稍后再试";

    private static final String MSG_UNAUTHORIZED = "登录已失效，请重新登录";

    private static final String MSG_REFRESH_INVALID = "会话已失效，请重新登录";

    private static final String MSG_PASSWORD_INVALID = "新密码不少于8位";

    private static final String MSG_OLD_PASSWORD_WRONG = "账号或密码错误";

    private final AccountMapper accountMapper;

    private final AccountSessionMapper accountSessionMapper;

    private final JwtTokenUtil jwtTokenUtil;

    private final OrgQueryService orgQueryService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginCommand command) {
        String username = normalizeUsername(command.getUsername());
        AccountDO account = accountMapper.selectByUsername(username);
        if (account == null) {
            log.warn("login failed reason=unknown_or_bad_password");
            throw new BizException(ErrorCodeConstant.AUTH_LOGIN_FAILED, MSG_LOGIN_FAILED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (isLocked(account, now)) {
            log.warn("login locked accountId={}", account.getId());
            throw new BizException(ErrorCodeConstant.AUTH_LOCKED, MSG_LOCKED);
        }
        if (!passwordMatches(command.getPassword(), account.getPasswordHash())) {
            handleLoginFailure(account, now);
            throw new BizException(ErrorCodeConstant.AUTH_LOGIN_FAILED, MSG_LOGIN_FAILED);
        }
        accountMapper.clearLoginFailure(account.getId(), now);
        account.setFailedLoginCount(0);
        account.setLockedUntil(null);
        return issueTokens(account, command.getDeviceHint());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO changePassword(AuthPrincipal principal, ChangePasswordCommand command) {
        AccountDO account = requireAccount(principal.getAccountId());
        if (!passwordMatches(command.getOldPassword(), account.getPasswordHash())) {
            log.warn("change password failed accountId={}", account.getId());
            throw new BizException(ErrorCodeConstant.AUTH_LOGIN_FAILED, MSG_OLD_PASSWORD_WRONG);
        }
        String newPassword = command.getNewPassword() == null ? "" : command.getNewPassword();
        if (newPassword.length() < AuthRuleConstant.MIN_PASSWORD_LENGTH) {
            throw new BizException(ErrorCodeConstant.AUTH_PASSWORD_INVALID, MSG_PASSWORD_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        accountMapper.updatePassword(account.getId(), PasswordHashUtil.hash(newPassword), now);
        accountSessionMapper.deleteByAccountId(account.getId());
        account.setMustChangePassword(Boolean.FALSE);
        log.info("password changed accountId={}", account.getId());
        return issueTokens(account, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO refresh(RefreshTokenCommand command) {
        JwtPayload payload;
        try {
            payload = jwtTokenUtil.parseRefreshToken(command.getRefreshToken());
        } catch (RuntimeException ex) {
            log.warn("refresh token parse failed");
            throw new BizException(ErrorCodeConstant.AUTH_REFRESH_INVALID, MSG_REFRESH_INVALID);
        }
        Long sessionId = payload.getSessionId();
        Long accountId = payload.getAccountId();
        AccountSessionDO session = accountSessionMapper.selectById(sessionId);
        if (session == null || !session.getAccountId().equals(accountId)) {
            throw new BizException(ErrorCodeConstant.AUTH_REFRESH_INVALID, MSG_REFRESH_INVALID);
        }
        if (session.getExpireAt() != null && session.getExpireAt().isBefore(LocalDateTime.now())) {
            accountSessionMapper.deleteById(session.getId());
            throw new BizException(ErrorCodeConstant.AUTH_REFRESH_INVALID, MSG_REFRESH_INVALID);
        }
        String expectedHash = TokenHashUtil.sha256(command.getRefreshToken());
        if (!expectedHash.equals(session.getTokenHash())) {
            accountSessionMapper.deleteById(session.getId());
            throw new BizException(ErrorCodeConstant.AUTH_REFRESH_INVALID, MSG_REFRESH_INVALID);
        }
        AccountDO account = requireAccount(accountId);
        return rotateTokens(account, session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(AuthPrincipal principal) {
        if (principal.getSessionId() != null) {
            accountSessionMapper.deleteById(principal.getSessionId());
        }
        log.info("logout accountId={}", principal.getAccountId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MeVO me(AuthPrincipal principal) {
        return orgQueryService.buildMe(requireAccount(principal.getAccountId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrgTreeVO orgStations(AuthPrincipal principal) {
        return orgQueryService.buildOrgTree(requireAccount(principal.getAccountId()));
    }

    private LoginVO issueTokens(AccountDO account, String deviceHint) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = LocalDateTime.ofInstant(
                jwtTokenUtil.refreshExpireAt(), ZoneId.systemDefault());
        AccountSessionDO session = AccountSessionDO.builder()
                .accountId(account.getId())
                .tokenHash(TokenHashUtil.sha256(UUID.randomUUID().toString()))
                .expireAt(expireAt)
                .deviceHint(trimDeviceHint(deviceHint))
                .gmtCreate(now)
                .gmtModified(now)
                .build();
        accountSessionMapper.insert(session);
        return fillSessionTokens(account, session);
    }

    private LoginVO rotateTokens(AccountDO account, AccountSessionDO session) {
        return fillSessionTokens(account, session);
    }

    private LoginVO fillSessionTokens(AccountDO account, AccountSessionDO session) {
        String accessToken = jwtTokenUtil.issueAccessToken(account.getId(), account.getRole(), session.getId());
        String refreshToken = jwtTokenUtil.issueRefreshToken(account.getId(), account.getRole(), session.getId());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = LocalDateTime.ofInstant(
                jwtTokenUtil.refreshExpireAt(), ZoneId.systemDefault());
        accountSessionMapper.updateTokenHash(
                session.getId(), TokenHashUtil.sha256(refreshToken), expireAt, now);
        MeVO me = orgQueryService.buildMe(account);
        return LoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresIn(jwtTokenUtil.getAccessTtlSeconds())
                .mustChangePassword(Boolean.TRUE.equals(account.getMustChangePassword()))
                .me(me)
                .build();
    }

    private void handleLoginFailure(AccountDO account, LocalDateTime now) {
        int failed = account.getFailedLoginCount() == null ? 0 : account.getFailedLoginCount();
        int next = failed + 1;
        LocalDateTime lockedUntil = null;
        if (next >= AuthRuleConstant.MAX_FAILED_LOGIN) {
            lockedUntil = now.plusMinutes(AuthRuleConstant.LOCK_MINUTES);
            next = 0;
        }
        accountMapper.updateLoginFailure(account.getId(), next, lockedUntil, now);
        log.warn("login failed accountId={} failedCount={}", account.getId(), next);
    }

    private boolean isLocked(AccountDO account, LocalDateTime now) {
        return account.getLockedUntil() != null && account.getLockedUntil().isAfter(now);
    }

    private boolean passwordMatches(String rawPassword, String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        return PasswordHashUtil.matches(rawPassword, passwordHash);
    }

    private AccountDO requireAccount(Long accountId) {
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null || AccountRoleEnum.fromCode(account.getRole()) == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        return account;
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String trimDeviceHint(String deviceHint) {
        if (deviceHint == null || deviceHint.isBlank()) {
            return null;
        }
        String trimmed = deviceHint.trim();
        if (trimmed.length() <= AuthRuleConstant.DEVICE_HINT_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, AuthRuleConstant.DEVICE_HINT_MAX_LENGTH);
    }
}
