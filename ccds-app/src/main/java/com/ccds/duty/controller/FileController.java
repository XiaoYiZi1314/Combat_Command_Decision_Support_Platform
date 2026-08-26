package com.ccds.duty.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.duty.dto.FileObjectDTO;
import com.ccds.duty.dto.FilePresignRequestDTO;
import com.ccds.duty.dto.FilePresignResponseDTO;
import com.ccds.duty.service.FileObjectService;
import com.ccds.iam.identity.model.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 文件预签名与预览接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class FileController {

    private final FileObjectService fileObjectService;

    /**
     * 生成预签名上传 URL。
     *
     * @param request HTTP 请求
     * @param body    预签名入参
     * @return 上传 URL
     */
    @PostMapping(ApiPathConstant.FILES_PRESIGN)
    public ApiResultVO<FilePresignResponseDTO> presign(HttpServletRequest request,
                                                       @Valid @RequestBody FilePresignRequestDTO body) {
        return ApiResultVO.ok(fileObjectService.generatePresignedUploadUrl(current(request), body));
    }

    /**
     * 获取短时预览 URL。
     *
     * @param request HTTP 请求
     * @param fileId  文件主键
     * @return 文件
     */
    @GetMapping(ApiPathConstant.FILE_OBJECT)
    public ApiResultVO<FileObjectDTO> getFile(HttpServletRequest request, @PathVariable Long fileId) {
        return ApiResultVO.ok(fileObjectService.getWithPreviewUrl(current(request), fileId));
    }

    /**
     * 删除文件。
     *
     * @param request HTTP 请求
     * @param fileId  文件主键
     * @return 空成功
     */
    @DeleteMapping(ApiPathConstant.FILE_OBJECT)
    public ApiResultVO<Void> deleteFile(HttpServletRequest request, @PathVariable Long fileId) {
        fileObjectService.deleteById(current(request), fileId);
        return ApiResultVO.ok(null);
    }

    private AuthPrincipal current(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
    }
}
