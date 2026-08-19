package com.ccds.iam.identity.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.iam.identity.entity.AccountDO;

/**
 * 账号表访问。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface AccountMapper {

    /**
     * 按登录名查询。
     *
     * @param username 登录名
     * @return 账号，不存在为 null
     */
    AccountDO selectByUsername(@Param("username") String username);

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 账号，不存在为 null
     */
    AccountDO selectById(@Param("id") Long id);

    /**
     * 记录登录失败并可选锁定。
     *
     * @param id               主键
     * @param failedLoginCount 失败次数
     * @param lockedUntil      锁定截止，未锁定为 null
     * @param gmtModified      修改时间
     * @return 影响行数
     */
    int updateLoginFailure(@Param("id") Long id,
                           @Param("failedLoginCount") Integer failedLoginCount,
                           @Param("lockedUntil") LocalDateTime lockedUntil,
                           @Param("gmtModified") LocalDateTime gmtModified);

    /**
     * 登录成功后清失败次数。
     *
     * @param id          主键
     * @param gmtModified 修改时间
     * @return 影响行数
     */
    int clearLoginFailure(@Param("id") Long id, @Param("gmtModified") LocalDateTime gmtModified);

    /**
     * 改密并取消强制改密标记。
     *
     * @param id           主键
     * @param passwordHash 新哈希
     * @param gmtModified  修改时间
     * @return 影响行数
     */
    int updatePassword(@Param("id") Long id,
                       @Param("passwordHash") String passwordHash,
                       @Param("gmtModified") LocalDateTime gmtModified);
}
