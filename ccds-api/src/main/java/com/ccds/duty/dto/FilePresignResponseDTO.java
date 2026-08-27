package com.ccds.duty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 文件预签名响应DTO
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"uploadUrl", "confirmTicket"})
public class FilePresignResponseDTO {

    /**
     * 文件对象ID
     */
    private Long fileId;

    /**
     * 预签名上传URL
     */
    private String uploadUrl;

    /**
     * 上传完成后的确认票据。
     */
    private String confirmTicket;

    /**
     * URL过期时间(秒)
     */
    private Integer expiresIn;
}
