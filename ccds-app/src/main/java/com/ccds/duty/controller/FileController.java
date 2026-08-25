package com.ccds.duty.controller;

import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.duty.dto.FileObjectDTO;
import com.ccds.duty.dto.FilePresignRequestDTO;
import com.ccds.duty.dto.FilePresignResponseDTO;
import com.ccds.duty.service.FileObjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 文件上传Controller
 *
 * @author system
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileObjectService fileObjectService;

    /**
     * 生成预签名上传URL
     *
     * @param request 预签名请求
     * @return 预签名响应（含上传URL）
     */
    @PostMapping("/presign")
    public ApiResultVO<FilePresignResponseDTO> presign(@Valid @RequestBody FilePresignRequestDTO request) {
        // TODO: 从当前会话获取账号ID
        Long accountId = 1L; // 临时硬编码，实际应从认证上下文获取
        
        FilePresignResponseDTO response = fileObjectService.generatePresignedUploadUrl(request, accountId);
        return ApiResultVO.ok(response);
    }

    /**
     * 获取文件预览URL
     *
     * @param fileId 文件ID
     * @return 文件信息（含预览URL）
     */
    @GetMapping("/{fileId}")
    public ApiResultVO<FileObjectDTO> getFile(@PathVariable Long fileId) {
        FileObjectDTO file = fileObjectService.getWithPreviewUrl(fileId);
        return ApiResultVO.ok(file);
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @return 成功标识
     */
    @DeleteMapping("/{fileId}")
    public ApiResultVO<Void> deleteFile(@PathVariable Long fileId) {
        fileObjectService.deleteById(fileId);
        return ApiResultVO.ok(null);
    }
}
