package com.ccds.duty.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 共享令牌实体
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareTokenDO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 消防站ID
     */
    private Long stationId;

    /**
     * 令牌哈希(SHA256)
     */
    private String tokenHash;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 作废时间
     */
    private LocalDateTime revokedAt;

    /**
     * 创建人账号ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;
}
