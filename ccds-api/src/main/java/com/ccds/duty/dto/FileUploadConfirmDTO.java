package com.ccds.duty.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传确认请求。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadConfirmDTO {

    /**
     * 预签名阶段返回的随机确认票据。
     */
    @NotBlank(message = "上传确认票据不能为空")
    private String confirmTicket;
}
