package com.ccds.duty.service;

import java.util.List;

import com.ccds.duty.dto.FileObjectDTO;
import com.ccds.duty.dto.FilePresignRequestDTO;
import com.ccds.duty.dto.FilePresignResponseDTO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 文件对象与 COS 预签名。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface FileObjectService {

    /**
     * 生成预签名上传 URL，并登记文件元数据。
     *
     * @param principal 当前身份
     * @param request   预签名请求
     * @return 预签名响应，不含对象键
     */
    FilePresignResponseDTO generatePresignedUploadUrl(AuthPrincipal principal, FilePresignRequestDTO request);

    /**
     * 业务下文件列表，不含预览 URL。
     *
     * @param bizType 业务类型
     * @param bizId   业务主键
     * @return 列表，不会为 null
     */
    List<FileObjectDTO> listByBiz(String bizType, Long bizId);

    /**
     * 业务下文件列表，带短时预览 URL。
     *
     * @param bizType 业务类型
     * @param bizId   业务主键
     * @return 列表，不会为 null
     */
    List<FileObjectDTO> listByBizWithPreview(String bizType, Long bizId);

    /**
     * 单个文件预览。
     *
     * @param principal 当前身份
     * @param fileId    文件主键
     * @return 含短时 URL 的文件
     */
    FileObjectDTO getWithPreviewUrl(AuthPrincipal principal, Long fileId);

    /**
     * 删除业务下全部文件。调用方须已完成写权限校验。
     *
     * @param bizType 业务类型
     * @param bizId   业务主键
     */
    void deleteByBiz(String bizType, Long bizId);

    /**
     * 删除单个文件。
     *
     * @param principal 当前身份
     * @param fileId    文件主键
     */
    void deleteById(AuthPrincipal principal, Long fileId);
}
