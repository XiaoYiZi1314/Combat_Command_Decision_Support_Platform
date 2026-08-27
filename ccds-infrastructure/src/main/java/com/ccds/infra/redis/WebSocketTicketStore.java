package com.ccds.infra.redis;

import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 指挥 WebSocket 短时一次性票据存储。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface WebSocketTicketStore {

    /**
     * 为当前身份签发票据。实现不得把访问令牌写入票据。
     *
     * @param principal 当前身份
     * @return 随机票据
     */
    String issue(AuthPrincipal principal);

    /**
     * 原子消费票据。成功后票据立即失效。
     *
     * @param ticket 随机票据
     * @return 票据身份，不存在、过期或已消费时返回 null
     */
    AuthPrincipal consume(String ticket);
}
