package com.ccds.iam.identity.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 登录账号持久化对象。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "passwordHash")
public class AccountDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 登录名。
     */
    private String username;

    /**
     * 口令哈希。
     */
    private String passwordHash;

    /**
     * 角色码。
     */
    private String role;

    /**
     * 站级所属站。
     */
    private Long stationId;

    /**
     * 大队所属大队。
     */
    private Long brigadeId;

    /**
     * 是否必须改密。
     */
    private Boolean mustChangePassword;

    /**
     * 连续失败次数。
     */
    private Integer failedLoginCount;

    /**
     * 锁定截止。
     */
    private LocalDateTime lockedUntil;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
