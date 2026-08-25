package com.ccds.duty.service.impl;

import com.ccds.common.api.exception.BizException;
import com.ccds.duty.dto.FileObjectDTO;
import com.ccds.duty.dto.FilePresignRequestDTO;
import com.ccds.duty.dto.FilePresignResponseDTO;
import com.ccds.duty.entity.FileObjectDO;
import com.ccds.duty.mapper.FileObjectMapper;
import com.ccds.duty.service.FileObjectService;
import com.ccds.infra.cloud.cos.COSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件对象服务实现
 *
 * @author system
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileObjectServiceImpl implements FileObjectService {

    private final FileObjectMapper fileObjectMapper;
    private final COSService cosService;

    private static final int PRESIGN_EXPIRE_SECONDS = 3600;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FilePresignResponseDTO generatePresignedUploadUrl(FilePresignRequestDTO request, Long accountId) {
        // 生成COS对象键
        String cosKey = generateCOSKey(request.getBizType(), request.getFileName());

        // 创建文件记录
        FileObjectDO fileObject = FileObjectDO.builder()
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .cosKey(cosKey)
                .contentType(request.getContentType())
                .sizeBytes(request.getSizeBytes())
                .createdBy(accountId)
                .build();

        fileObjectMapper.insert(fileObject);

        // 生成预签名URL
        String uploadUrl = cosService.generatePresignedUploadUrl(
                cosKey, 
                request.getContentType(), 
                PRESIGN_EXPIRE_SECONDS
        );

        log.info("生成文件上传URL：fileId={}, bizType={}, bizId={}", 
                fileObject.getId(), request.getBizType(), request.getBizId());

        return FilePresignResponseDTO.builder()
                .fileId(fileObject.getId())
                .cosKey(cosKey)
                .uploadUrl(uploadUrl)
                .expiresIn(PRESIGN_EXPIRE_SECONDS)
                .build();
    }

    @Override
    public List<FileObjectDTO> listByBiz(String bizType, Long bizId) {
        List<FileObjectDO> files = fileObjectMapper.selectByBiz(bizType, bizId);
        return files.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FileObjectDTO getWithPreviewUrl(Long fileId) {
        FileObjectDO fileObject = fileObjectMapper.selectById(fileId);
        if (fileObject == null) {
            throw new BizException("FILE_NOT_FOUND", "文件不存在");
        }

        FileObjectDTO dto = convertToDTO(fileObject);
        
        // 生成预览和下载URL
        String downloadUrl = cosService.generatePresignedDownloadUrl(
                fileObject.getCosKey(), 
                PRESIGN_EXPIRE_SECONDS
        );
        dto.setPreviewUrl(downloadUrl);
        dto.setDownloadUrl(downloadUrl);

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByBiz(String bizType, Long bizId) {
        List<FileObjectDO> files = fileObjectMapper.selectByBiz(bizType, bizId);
        if (files.isEmpty()) {
            return;
        }

        // 删除COS对象
        String[] cosKeys = files.stream()
                .map(FileObjectDO::getCosKey)
                .toArray(String[]::new);
        cosService.deleteObjects(cosKeys);

        // 删除数据库记录
        fileObjectMapper.deleteByBiz(bizType, bizId);

        log.info("删除业务文件：bizType={}, bizId={}, count={}", bizType, bizId, files.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long fileId) {
        FileObjectDO fileObject = fileObjectMapper.selectById(fileId);
        if (fileObject == null) {
            return;
        }

        // 删除COS对象
        cosService.deleteObject(fileObject.getCosKey());

        // 删除数据库记录
        fileObjectMapper.deleteById(fileId);

        log.info("删除文件：fileId={}", fileId);
    }

    /**
     * 生成COS对象键
     * 格式：{bizType}/{yyyyMMdd}/{uuid}.{ext}
     */
    private String generateCOSKey(String bizType, String fileName) {
        String date = LocalDateTime.now().format(DATE_FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            extension = fileName.substring(dotIndex);
        }
        
        return String.format("%s/%s/%s%s", bizType, date, uuid, extension);
    }

    /**
     * 转换为DTO
     */
    private FileObjectDTO convertToDTO(FileObjectDO fileObject) {
        return FileObjectDTO.builder()
                .id(fileObject.getId())
                .bizType(fileObject.getBizType())
                .bizId(fileObject.getBizId())
                .cosKey(fileObject.getCosKey())
                .contentType(fileObject.getContentType())
                .sizeBytes(fileObject.getSizeBytes())
                .build();
    }
}
