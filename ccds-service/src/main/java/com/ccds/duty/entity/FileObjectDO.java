package com.ccds.duty.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件对象实体
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileObjectDO {

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
     * COS对象键
     */
    private String cosKey;

    /**
     * MIME类型
     */
    private String contentType;

    /**
     * 文件大小(字节)
     */
    private Long sizeBytes;

    /**
     * 创建人账号ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;
}
