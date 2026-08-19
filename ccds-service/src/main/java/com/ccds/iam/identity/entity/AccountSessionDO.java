package com.ccds.iam.identity.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 账号 refresh 会话。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "tokenHash")
public class AccountSessionDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 账号主键。
     */
    private Long accountId;

    /**
     * 刷新令牌哈希。
     */
    private String tokenHash;

    /**
     * 过期时间。
     */
    private LocalDateTime expireAt;

    /**
     * 设备提示。
     */
    private String deviceHint;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
