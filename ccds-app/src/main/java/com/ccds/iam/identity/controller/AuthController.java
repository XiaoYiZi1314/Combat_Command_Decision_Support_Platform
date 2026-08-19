package com.ccds.iam.identity.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.iam.identity.dto.ChangePasswordCommand;
import com.ccds.iam.identity.dto.LoginCommand;
import com.ccds.iam.identity.dto.RefreshTokenCommand;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.iam.identity.service.AuthService;
import com.ccds.iam.identity.vo.LoginVO;
import com.ccds.iam.identity.vo.MeVO;
import com.ccds.iam.identity.vo.OrgTreeVO;

import lombok.RequiredArgsConstructor;

/**
 * 登录、改密、会话与当前账号接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class AuthController {

    private final AuthService authService;

    /**
     * 账号口令登录。
     *
     * @param command 登录入参
     * @return 令牌与当前账号
     */
    @PostMapping(ApiPathConstant.AUTH_LOGIN)
    public ApiResultVO<LoginVO> login(@Valid @RequestBody LoginCommand command) {
        return ApiResultVO.ok(authService.login(command));
    }

    /**
     * 改密。
     *
     * @param request HTTP 请求
     * @param command 改密入参
     * @return 新令牌
     */
    @PostMapping(ApiPathConstant.AUTH_PASSWORD)
    public ApiResultVO<LoginVO> changePassword(HttpServletRequest request,
                                               @Valid @RequestBody ChangePasswordCommand command) {
        return ApiResultVO.ok(authService.changePassword(current(request), command));
    }

    /**
     * 刷新访问令牌。
     *
     * @param command 刷新入参
     * @return 新令牌
     */
    @PostMapping(ApiPathConstant.AUTH_REFRESH)
    public ApiResultVO<LoginVO> refresh(@Valid @RequestBody RefreshTokenCommand command) {
        return ApiResultVO.ok(authService.refresh(command));
    }

    /**
     * 登出当前会话。
     *
     * @param request HTTP 请求
     * @return 空成功
     */
    @PostMapping(ApiPathConstant.AUTH_LOGOUT)
    public ApiResultVO<Void> logout(HttpServletRequest request) {
        authService.logout(current(request));
        return ApiResultVO.ok(null);
    }

    /**
     * 当前账号与可见站。
     *
     * @param request HTTP 请求
     * @return 当前账号
     */
    @GetMapping(ApiPathConstant.ME)
    public ApiResultVO<MeVO> me(HttpServletRequest request) {
        return ApiResultVO.ok(authService.me(current(request)));
    }

    /**
     * 可见编制树。
     *
     * @param request HTTP 请求
     * @return 编制树
     */
    @GetMapping(ApiPathConstant.ORG_STATIONS)
    public ApiResultVO<OrgTreeVO> orgStations(HttpServletRequest request) {
        return ApiResultVO.ok(authService.orgStations(current(request)));
    }

    private AuthPrincipal current(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
    }
}
