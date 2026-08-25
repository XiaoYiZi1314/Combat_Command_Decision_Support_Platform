package com.ccds.duty.service;

import com.ccds.duty.dto.FileObjectDTO;
import com.ccds.duty.dto.FilePresignRequestDTO;
import com.ccds.duty.dto.FilePresignResponseDTO;

import java.util.List;

/**
 * 文件对象服务
 *
 * @author system
 * @since 2024
 */
public interface FileObjectService {

    /**
     * 生成预签名上传URL
     *
     * @param request   预签名请求
     * @param accountId 账号ID
     * @return 预签名响应
     */
    FilePresignResponseDTO generatePresignedUploadUrl(FilePresignRequestDTO request, Long accountId);

    /**
     * 根据业务类型和业务ID查询文件列表
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @return 文件列表
     */
    List<FileObjectDTO> listByBiz(String bizType, Long bizId);

    /**
     * 生成文件预览URL
     *
     * @param fileId 文件ID
     * @return 文件信息（含预览URL）
     */
    FileObjectDTO getWithPreviewUrl(Long fileId);

    /**
     * 删除业务关联的文件（包括COS对象）
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     */
    void deleteByBiz(String bizType, Long bizId);

    /**
     * 删除单个文件（包括COS对象）
     *
     * @param fileId 文件ID
     */
    void deleteById(Long fileId);
}
