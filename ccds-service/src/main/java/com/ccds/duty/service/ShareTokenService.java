package com.ccds.duty.service;

import com.ccds.duty.dto.CreateShareTokenRequestDTO;
import com.ccds.duty.dto.CreateShareTokenResponseDTO;
import com.ccds.duty.vo.ShareAttackVO;

/**
 * 共享令牌服务
 *
 * @author system
 * @since 2024
 */
public interface ShareTokenService {

    /**
     * 创建共享令牌
     *
     * @param request   创建请求
     * @param accountId 账号ID
     * @return 共享令牌响应
     */
    CreateShareTokenResponseDTO createShareToken(CreateShareTokenRequestDTO request, Long accountId);

    /**
     * 作废共享令牌
     *
     * @param tokenId 令牌ID
     */
    void revokeToken(Long tokenId);

    /**
     * 根据令牌获取共享态势（匿名访问）
     *
     * @param token 令牌明文
     * @return 共享态势VO
     */
    ShareAttackVO getSharedAttack(String token);

    /**
     * 验证令牌是否有效
     *
     * @param token 令牌明文
     * @return 是否有效
     */
    boolean validateToken(String token);
}
