package com.ccds.duty.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.duty.dto.CreateShareTokenRequestDTO;
import com.ccds.duty.dto.CreateShareTokenResponseDTO;
import com.ccds.duty.service.ShareTokenService;
import com.ccds.duty.vo.ShareAttackVO;
import com.ccds.iam.identity.model.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 共享令牌接入。匿名只读走 {@code /s/{token}}。
 *
 * @author ccds
 * @since 0.1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class ShareTokenController {

    private final ShareTokenService shareTokenService;

    /**
     * 站级开共享令牌。
     *
     * @param request HTTP 请求
     * @param body    创建入参
     * @return 明文令牌与绝对 URL
     */
    @PostMapping(ApiPathConstant.SHARE)
    public ApiResultVO<CreateShareTokenResponseDTO> createToken(HttpServletRequest request,
                                                                @Valid @RequestBody CreateShareTokenRequestDTO body) {
        return ApiResultVO.ok(shareTokenService.createShareToken(current(request), body));
    }

    /**
     * 提前作废。
     *
     * @param request HTTP 请求
     * @param tokenId 令牌主键
     * @return 空成功
     */
    @DeleteMapping(ApiPathConstant.SHARE_ID)
    public ApiResultVO<Void> revokeToken(HttpServletRequest request, @PathVariable Long tokenId) {
        shareTokenService.revokeToken(current(request), tokenId);
        return ApiResultVO.ok(null);
    }

    /**
     * 匿名访问共享态势。
     *
     * @param token 令牌明文
     * @return 脱敏态势
     */
    @GetMapping(ApiPathConstant.SHARE_ANONYMOUS)
    public ApiResultVO<ShareAttackVO> getSharedAttack(@PathVariable String token) {
        return ApiResultVO.ok(shareTokenService.getSharedAttack(token));
    }

    private AuthPrincipal current(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
    }
}
