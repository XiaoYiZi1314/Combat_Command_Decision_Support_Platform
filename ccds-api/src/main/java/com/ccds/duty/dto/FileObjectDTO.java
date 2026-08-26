package com.ccds.duty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件对象DTO
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileObjectDTO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * MIME类型
     */
    private String contentType;

    /**
     * 文件大小(字节)
     */
    private Long sizeBytes;

    /**
     * 预览URL(短时有效)
     */
    private String previewUrl;

    /**
     * 下载URL(短时有效)
     */
    private String downloadUrl;
}
