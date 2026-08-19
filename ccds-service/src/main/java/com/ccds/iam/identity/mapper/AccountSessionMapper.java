package com.ccds.iam.identity.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ccds.iam.identity.entity.AccountSessionDO;

/**
 * 账号会话表访问。
 *
 * @author ccds
 * @since 0.1.0
 */
@Mapper
public interface AccountSessionMapper {

    /**
     * 插入会话。
     *
     * @param session 会话
     * @return 影响行数
     */
    int insert(AccountSessionDO session);

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 会话，不存在为 null
     */
    AccountSessionDO selectById(@Param("id") Long id);

    /**
     * 轮换刷新令牌哈希。
     *
     * @param id          主键
     * @param tokenHash   新哈希
     * @param expireAt    新过期
     * @param gmtModified 修改时间
     * @return 影响行数
     */
    int updateTokenHash(@Param("id") Long id,
                        @Param("tokenHash") String tokenHash,
                        @Param("expireAt") LocalDateTime expireAt,
                        @Param("gmtModified") LocalDateTime gmtModified);

    /**
     * 按主键删除。
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 删除某账号全部会话。
     *
     * @param accountId 账号主键
     * @return 影响行数
     */
    int deleteByAccountId(@Param("accountId") Long accountId);
}
