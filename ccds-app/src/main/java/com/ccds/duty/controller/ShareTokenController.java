package com.ccds.duty.controller;

import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.duty.dto.CreateShareTokenRequestDTO;
import com.ccds.duty.dto.CreateShareTokenResponseDTO;
import com.ccds.duty.service.ShareTokenService;
import com.ccds.duty.vo.ShareAttackVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 共享令牌Controller
 *
 * @author system
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ShareTokenController {

    private final ShareTokenService shareTokenService;

    /**
     * 创建共享令牌
     *
     * @param request 创建请求
     * @return 共享令牌响应（含完整URL）
     */
    @PostMapping("/share")
    public ApiResultVO<CreateShareTokenResponseDTO> createToken(@Valid @RequestBody CreateShareTokenRequestDTO request) {
        // TODO: 从当前会话获取账号ID
        Long accountId = 1L; // 临时硬编码，实际应从认证上下文获取
        
        CreateShareTokenResponseDTO response = shareTokenService.createShareToken(request, accountId);
        return ApiResultVO.ok(response);
    }

    /**
     * 作废共享令牌
     *
     * @param tokenId 令牌ID
     * @return 成功标识
     */
    @DeleteMapping("/share/{tokenId}")
    public ApiResultVO<Void> revokeToken(@PathVariable Long tokenId) {
        shareTokenService.revokeToken(tokenId);
        return ApiResultVO.ok(null);
    }

    /**
     * 匿名访问：根据令牌获取共享态势
     *
     * @param token 令牌明文
     * @return 共享态势VO（脱敏）
     */
    @GetMapping("/s/{token}")
    public ApiResultVO<ShareAttackVO> getSharedAttack(@PathVariable String token) {
        ShareAttackVO vo = shareTokenService.getSharedAttack(token);
        return ApiResultVO.ok(vo);
    }
}
