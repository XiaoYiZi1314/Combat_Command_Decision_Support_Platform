package com.ccds.iam.identity.service;

import com.ccds.iam.identity.dto.ChangePasswordCommand;
import com.ccds.iam.identity.dto.LoginCommand;
import com.ccds.iam.identity.dto.RefreshTokenCommand;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.iam.identity.vo.LoginVO;
import com.ccds.iam.identity.vo.MeVO;
import com.ccds.iam.identity.vo.OrgTreeVO;

/**
 * 登录、改密、会话与当前账号。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface AuthService {

    /**
     * 账号口令登录。失败不区分账号是否存在。
     *
     * @param command 登录入参
     * @return 令牌与当前账号
     */
    LoginVO login(LoginCommand command);

    /**
     * 改密。成功后作废全部会话并签发新令牌。
     *
     * @param principal 当前身份
     * @param command   改密入参
     * @return 新令牌与当前账号
     */
    LoginVO changePassword(AuthPrincipal principal, ChangePasswordCommand command);

    /**
     * 用刷新令牌换新访问令牌。
     *
     * @param command 刷新入参
     * @return 新令牌与当前账号
     */
    LoginVO refresh(RefreshTokenCommand command);

    /**
     * 登出当前会话。
     *
     * @param principal 当前身份
     */
    void logout(AuthPrincipal principal);

    /**
     * 当前账号与可见站。
     *
     * @param principal 当前身份
     * @return 当前账号
     */
    MeVO me(AuthPrincipal principal);

    /**
     * 可见编制树。
     *
     * @param principal 当前身份
     * @return 编制树
     */
    OrgTreeVO orgStations(AuthPrincipal principal);
}
