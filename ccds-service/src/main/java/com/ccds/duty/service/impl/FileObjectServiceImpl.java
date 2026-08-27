package com.ccds.duty.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.constant.FileBizTypeConstant;
import com.ccds.duty.constant.FileRuleConstant;
import com.ccds.duty.dto.FileObjectDTO;
import com.ccds.duty.dto.FilePresignRequestDTO;
import com.ccds.duty.dto.FilePresignResponseDTO;
import com.ccds.duty.dto.FileUploadConfirmDTO;
import com.ccds.duty.entity.FileObjectDO;
import com.ccds.duty.entity.KeyUnitDO;
import com.ccds.duty.mapper.FileObjectMapper;
import com.ccds.duty.mapper.KeyUnitMapper;
import com.ccds.duty.service.DutyAccessService;
import com.ccds.duty.service.FileObjectService;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.cloud.cos.COSService;
import com.ccds.infra.cloud.cos.FileContentValidator;
import com.ccds.infra.cloud.cos.FileTextExtractor;
import com.ccds.infra.redis.FileUploadConfirmStore;
import com.ccds.water.water.entity.WaterSourceDO;
import com.ccds.water.water.mapper.WaterSourceMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件元数据与 COS 预签名。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileObjectServiceImpl implements FileObjectService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String MSG_FILE_MISSING = "文件不存在";

    private static final String MSG_BIZ_MISSING = "文件关联业务不存在";

    private static final String MSG_BIZ_TYPE = "文件业务类型不合法";

    private static final String MSG_MIME = "文件类型不在允许范围";

    private static final String MSG_SIZE = "文件超过大小限制";

    private static final String MSG_NAME = "文件名不合法";

    private static final String MSG_COS = "对象存储暂不可用";

    private static final String MSG_CONFIRM = "文件上传确认失败，请重新上传";

    private final FileObjectMapper fileObjectMapper;

    private final KeyUnitMapper keyUnitMapper;

    private final WaterSourceMapper waterSourceMapper;

    private final COSService cosService;

    private final FileContentValidator fileContentValidator;

    private final FileTextExtractor fileTextExtractor;

    private final DutyAccessService dutyAccessService;

    private final FileUploadConfirmStore fileUploadConfirmStore;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FilePresignResponseDTO generatePresignedUploadUrl(AuthPrincipal principal, FilePresignRequestDTO request) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        validateUpload(request);
        Long stationId = requireBizStationId(request.getBizType(), request.getBizId());
        if (stationId != null) {
            dutyAccessService.requireWritableStation(account, stationId);
        }
        String actualBizType = request.getBizType();
        String cosKey = generateCOSKey(actualBizType, request.getFileName());
        FileObjectDO fileObject = FileObjectDO.builder()
                .bizType(FileRuleConstant.pendingBizType(actualBizType))
                .bizId(request.getBizId())
                .cosKey(cosKey)
                .contentType(request.getContentType().trim().toLowerCase())
                .sizeBytes(request.getSizeBytes())
                .createdBy(account.getId())
                .build();
        fileObjectMapper.insert(fileObject);
        String uploadUrl;
        try {
            uploadUrl = cosService.generatePresignedUploadUrl(
                    cosKey, fileObject.getContentType(), FileRuleConstant.PRESIGN_EXPIRE_SECONDS);
        } catch (IllegalStateException ex) {
            fileObjectMapper.deleteById(fileObject.getId());
            log.error("生成文件上传URL失败 fileId={}", fileObject.getId(), ex);
            throw new BizException(ErrorCodeConstant.FILE_COS_UNAVAILABLE, MSG_COS);
        }
        log.info("生成文件上传URL：fileId={}, bizType={}, bizId={}",
                fileObject.getId(), request.getBizType(), request.getBizId());
        return FilePresignResponseDTO.builder()
                .fileId(fileObject.getId())
                .uploadUrl(uploadUrl)
                .confirmTicket(issueConfirmTicket(fileObject.getId(), account.getId()))
                .expiresIn(FileRuleConstant.PRESIGN_EXPIRE_SECONDS)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileObjectDTO confirmUpload(AuthPrincipal principal, Long fileId, FileUploadConfirmDTO request) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        FileObjectDO fileObject = requireFile(fileId);
        String actualBizType = FileRuleConstant.actualBizType(fileObject.getBizType());
        Long stationId = requireBizStationId(actualBizType, fileObject.getBizId());
        if (stationId != null) {
            dutyAccessService.requireWritableStation(account, stationId);
        }
        if (!account.getId().equals(fileObject.getCreatedBy())
                || !FileRuleConstant.isPendingBizType(fileObject.getBizType())) {
            throw new BizException(ErrorCodeConstant.FILE_UPLOAD_CONFIRM_INVALID, MSG_CONFIRM);
        }
        Long actualSize = cosService.getObjectSize(fileObject.getCosKey());
        if (actualSize == null || !actualSize.equals(fileObject.getSizeBytes())) {
            throw new BizException(ErrorCodeConstant.FILE_UPLOAD_CONFIRM_INVALID, MSG_CONFIRM);
        }
        byte[] content = cosService.getObjectContent(fileObject.getCosKey());
        validateUploadedContent(fileObject, actualBizType, content);
        boolean confirmed = fileUploadConfirmStore.consume(
                request.getConfirmTicket(), fileObject.getId(), account.getId());
        if (!confirmed) {
            throw new BizException(ErrorCodeConstant.FILE_UPLOAD_CONFIRM_INVALID, MSG_CONFIRM);
        }
        int updated = fileObjectMapper.confirmUpload(
                fileObject.getId(), fileObject.getBizType(), actualBizType);
        if (updated != 1) {
            throw new BizException(ErrorCodeConstant.FILE_UPLOAD_CONFIRM_INVALID, MSG_CONFIRM);
        }
        fileObject.setBizType(actualBizType);
        extractPlanTextIfSupported(fileObject, content);
        log.info("确认文件上传：fileId={}, bizType={}, bizId={}",
                fileObject.getId(), fileObject.getBizType(), fileObject.getBizId());
        return convertToDTO(fileObject, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FileObjectDTO> listByBiz(String bizType, Long bizId) {
        return fileObjectMapper.selectByBiz(bizType, bizId).stream()
                .filter(this::isUploadedObject)
                .map(item -> convertToDTO(item, false))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FileObjectDTO> listByBizWithPreview(String bizType, Long bizId) {
        return fileObjectMapper.selectByBiz(bizType, bizId).stream()
                .filter(this::isUploadedObject)
                .map(item -> convertToDTO(item, true))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileObjectDTO getWithPreviewUrl(AuthPrincipal principal, Long fileId) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        FileObjectDO fileObject = requireFile(fileId);
        String actualBizType = FileRuleConstant.actualBizType(fileObject.getBizType());
        Long stationId = requireBizStationId(actualBizType, fileObject.getBizId());
        if (stationId != null) {
            dutyAccessService.requireVisibleStation(account, stationId);
        }
        if (FileRuleConstant.isPendingBizType(fileObject.getBizType()) || !isUploadedObject(fileObject)) {
            throw new BizException(ErrorCodeConstant.FILE_UPLOAD_CONFIRM_INVALID, MSG_CONFIRM);
        }
        return convertToDTO(fileObject, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByBiz(String bizType, Long bizId) {
        List<FileObjectDO> files = fileObjectMapper.selectByBiz(bizType, bizId);
        if (files.isEmpty()) {
            return;
        }
        String[] cosKeys = files.stream().map(FileObjectDO::getCosKey).toArray(String[]::new);
        try {
            cosService.deleteObjects(cosKeys);
        } catch (IllegalStateException ex) {
            throw new BizException(ErrorCodeConstant.FILE_COS_UNAVAILABLE, MSG_COS);
        }
        fileObjectMapper.deleteByBiz(bizType, bizId);
        log.info("删除业务文件：bizType={}, bizId={}, count={}", bizType, bizId, files.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(AuthPrincipal principal, Long fileId) {
        AccountDO account = dutyAccessService.requireAccount(principal);
        FileObjectDO fileObject = requireFile(fileId);
        String actualBizType = FileRuleConstant.actualBizType(fileObject.getBizType());
        Long stationId = requireBizStationId(actualBizType, fileObject.getBizId());
        if (stationId != null) {
            dutyAccessService.requireWritableStation(account, stationId);
        }
        try {
            cosService.deleteObject(fileObject.getCosKey());
        } catch (IllegalStateException ex) {
            throw new BizException(ErrorCodeConstant.FILE_COS_UNAVAILABLE, MSG_COS);
        }
        fileObjectMapper.deleteById(fileId);
        log.info("删除文件：fileId={}", fileId);
    }

    private void validateUploadedContent(FileObjectDO fileObject, String actualBizType, byte[] content) {
        try {
            if (fileContentValidator.matches(fileObject.getContentType(), content)) {
                return;
            }
            log.warn("上传文件内容校验失败 fileId={}, bizType={}",
                    fileObject.getId(), actualBizType);
            throw new BizException(ErrorCodeConstant.FILE_CONTENT_TYPE_INVALID, MSG_MIME);
        } catch (BizException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("上传文件内容校验异常 fileId={}, bizType={}",
                    fileObject.getId(), actualBizType, ex);
            throw new BizException(ErrorCodeConstant.FILE_UPLOAD_CONFIRM_INVALID, MSG_CONFIRM);
        }
    }

    private void extractPlanTextIfSupported(FileObjectDO fileObject, byte[] content) {
        if (!FileBizTypeConstant.KEYUNIT_PLAN.equals(fileObject.getBizType())) {
            return;
        }
        String planText = fileTextExtractor.extract(fileObject.getContentType(), content);
        if (planText == null) {
            log.info("文件正文未自动抽取，保留手工补充入口 fileId={}", fileObject.getId());
            return;
        }
        int updated = keyUnitMapper.updatePlanText(fileObject.getBizId(), planText);
        if (updated != 1) {
            log.warn("文件正文回写未更新重点单位 fileId={}, bizId={}",
                    fileObject.getId(), fileObject.getBizId());
        }
    }

    private boolean isUploadedObject(FileObjectDO fileObject) {
        if (FileRuleConstant.isPendingBizType(fileObject.getBizType())) {
            return false;
        }
        Long actualSize = cosService.getObjectSize(fileObject.getCosKey());
        return actualSize != null && actualSize.equals(fileObject.getSizeBytes());
    }

    private String issueConfirmTicket(Long fileId, Long accountId) {
        try {
            return fileUploadConfirmStore.issue(
                    fileId, accountId, FileRuleConstant.PRESIGN_EXPIRE_SECONDS);
        } catch (RuntimeException ex) {
            fileObjectMapper.deleteById(fileId);
            log.error("签发文件上传确认票据失败 fileId={}", fileId, ex);
            throw new BizException(ErrorCodeConstant.FILE_COS_UNAVAILABLE, MSG_COS);
        }
    }

    private void validateUpload(FilePresignRequestDTO request) {
        if (!FileRuleConstant.isAllowedBizType(request.getBizType())) {
            throw new BizException(ErrorCodeConstant.FILE_BIZ_TYPE_INVALID, MSG_BIZ_TYPE);
        }
        if (!FileRuleConstant.isAllowedMime(request.getBizType(), request.getContentType())) {
            throw new BizException(ErrorCodeConstant.FILE_CONTENT_TYPE_INVALID, MSG_MIME);
        }
        if (request.getSizeBytes() == null || request.getSizeBytes() > FileRuleConstant.MAX_SIZE_BYTES) {
            throw new BizException(ErrorCodeConstant.FILE_SIZE_INVALID, MSG_SIZE);
        }
        String fileName = request.getFileName();
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_NAME);
        }
    }

    private FileObjectDO requireFile(Long fileId) {
        FileObjectDO fileObject = fileObjectMapper.selectById(fileId);
        if (fileObject == null) {
            throw new BizException(ErrorCodeConstant.FILE_NOT_FOUND, MSG_FILE_MISSING);
        }
        return fileObject;
    }

    private Long requireBizStationId(String bizType, Long bizId) {
        if (FileBizTypeConstant.KEYUNIT_PLAN.equals(bizType)
                || FileBizTypeConstant.KEYUNIT_FLOOR.equals(bizType)) {
            KeyUnitDO keyUnit = keyUnitMapper.selectById(bizId);
            if (keyUnit == null || keyUnit.getDeletedAt() != null) {
                throw new BizException(ErrorCodeConstant.FILE_BIZ_NOT_FOUND, MSG_BIZ_MISSING);
            }
            return keyUnit.getStationId();
        }
        if (FileBizTypeConstant.WATER_PHOTO.equals(bizType)
                || FileBizTypeConstant.WATER_DOC.equals(bizType)) {
            WaterSourceDO water = waterSourceMapper.selectAliveById(bizId);
            if (water == null) {
                throw new BizException(ErrorCodeConstant.FILE_BIZ_NOT_FOUND, MSG_BIZ_MISSING);
            }
            return water.getStationId();
        }
        if (FileBizTypeConstant.HAZMAT_IMAGE.equals(bizType)) {
            return null;
        }
        throw new BizException(ErrorCodeConstant.FILE_BIZ_TYPE_INVALID, MSG_BIZ_TYPE);
    }

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

    private FileObjectDTO convertToDTO(FileObjectDO fileObject, boolean withPreview) {
        FileObjectDTO dto = FileObjectDTO.builder()
                .id(fileObject.getId())
                .bizType(fileObject.getBizType())
                .bizId(fileObject.getBizId())
                .contentType(fileObject.getContentType())
                .sizeBytes(fileObject.getSizeBytes())
                .build();
        if (!withPreview) {
            return dto;
        }
        try {
            String url = cosService.generatePresignedDownloadUrl(
                    fileObject.getCosKey(), FileRuleConstant.PRESIGN_EXPIRE_SECONDS);
            dto.setPreviewUrl(url);
            dto.setDownloadUrl(url);
        } catch (IllegalStateException ex) {
            throw new BizException(ErrorCodeConstant.FILE_COS_UNAVAILABLE, MSG_COS);
        }
        return dto;
    }
}
