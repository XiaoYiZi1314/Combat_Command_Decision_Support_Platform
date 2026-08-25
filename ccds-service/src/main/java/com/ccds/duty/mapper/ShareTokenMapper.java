package com.ccds.duty.mapper;

import com.ccds.duty.entity.ShareTokenDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 共享令牌Mapper
 *
 * @author system
 * @since 2024
 */
@Mapper
public interface ShareTokenMapper {

    /**
     * 插入
     */
    int insert(ShareTokenDO shareToken);

    /**
     * 根据ID更新
     */
    int updateById(ShareTokenDO shareToken);

    /**
     * 根据ID查询
     */
    ShareTokenDO selectById(Long id);

    /**
     * 根据令牌哈希查询
     *
     * @param tokenHash 令牌哈希
     * @return 共享令牌
     */
    ShareTokenDO selectByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * 查询消防站的有效令牌列表
     *
     * @param stationId 消防站ID
     * @param now       当前时间
     * @return 有效令牌列表
     */
    List<ShareTokenDO> selectValidByStation(@Param("stationId") Long stationId,
                                              @Param("now") LocalDateTime now);

    /**
     * 作废消防站的所有有效令牌
     *
     * @param stationId 消防站ID
     * @param now       当前时间
     * @return 更新数量
     */
    int revokeAllByStation(@Param("stationId") Long stationId,
                            @Param("now") LocalDateTime now);
}
