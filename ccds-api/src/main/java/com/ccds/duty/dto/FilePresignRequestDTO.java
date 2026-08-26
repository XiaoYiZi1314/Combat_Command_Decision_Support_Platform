package com.ccds.duty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.ccds.duty.constant.FileRuleConstant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 文件预签名请求DTO
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilePresignRequestDTO {

    /**
     * 业务类型
     */
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    /**
     * 业务ID
     */
    @NotNull(message = "业务ID不能为空")
    private Long bizId;

    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * MIME类型
     */
    @NotBlank(message = "MIME类型不能为空")
    private String contentType;

    /**
     * 文件大小(字节)
     */
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小须大于0")
    @Max(value = FileRuleConstant.MAX_SIZE_BYTES, message = "文件不能超过20MB")
    private Long sizeBytes;
}
