package com.ccds.duty.mapper;

import com.ccds.duty.entity.FileObjectDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件对象Mapper
 *
 * @author system
 * @since 2024
 */
@Mapper
public interface FileObjectMapper {

    /**
     * 插入
     */
    int insert(FileObjectDO fileObject);

    /**
     * 根据ID删除
     */
    int deleteById(Long id);

    /**
     * 将待确认记录切换为正式业务类型。
     *
     * @param id             文件主键
     * @param pendingBizType 待确认业务类型
     * @param bizType        正式业务类型
     * @return 更新数量
     */
    int confirmUpload(@Param("id") Long id,
                      @Param("pendingBizType") String pendingBizType,
                      @Param("bizType") String bizType);

    /**
     * 根据ID查询
     */
    FileObjectDO selectById(Long id);

    /**
     * 根据业务类型和业务ID查询文件列表
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @return 文件列表
     */
    List<FileObjectDO> selectByBiz(@Param("bizType") String bizType,
                                     @Param("bizId") Long bizId);

    /**
     * 删除业务关联的文件记录
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @return 删除数量
     */
    int deleteByBiz(@Param("bizType") String bizType,
                     @Param("bizId") Long bizId);
}
