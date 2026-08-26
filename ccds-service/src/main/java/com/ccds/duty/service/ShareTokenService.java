package com.ccds.duty.service;

import com.ccds.duty.dto.CreateShareTokenRequestDTO;
import com.ccds.duty.dto.CreateShareTokenResponseDTO;
import com.ccds.duty.vo.ShareAttackVO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 共享令牌。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface ShareTokenService {

    /**
     * 站级为本站当前内攻开令牌。
     *
     * @param principal 当前身份
     * @param request   创建请求
     * @return 明文令牌与绝对 URL，仅此时返回明文
     */
    CreateShareTokenResponseDTO createShareToken(AuthPrincipal principal, CreateShareTokenRequestDTO request);

    /**
     * 提前作废。
     *
     * @param principal 当前身份
     * @param tokenId   令牌主键
     */
    void revokeToken(AuthPrincipal principal, Long tokenId);

    /**
     * 匿名只读投影。
     *
     * @param token 令牌明文
     * @return 脱敏态势
     */
    ShareAttackVO getSharedAttack(String token);
}
