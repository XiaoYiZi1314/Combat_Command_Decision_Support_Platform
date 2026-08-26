package com.ccds.infra.cloud.cos;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.DeleteObjectsRequest;
import com.qcloud.cos.model.DeleteObjectsResult;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * COS 对象存储实现。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class COSServiceImpl implements COSService {

    private static final String MSG_NOT_READY = "COS客户端未初始化";

    private static final String MSG_DELETE_FAILED = "删除COS对象失败";

    private final CosClientHolder cosClientHolder;

    /**
     * {@inheritDoc}
     */
    @Override
    public String generatePresignedUploadUrl(String cosKey, String contentType, int expireSec) {
        requireReady();
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                cosClientHolder.getBucket(), cosKey, HttpMethodName.PUT);
        request.setExpiration(new Date(System.currentTimeMillis() + expireSec * 1000L));
        if (contentType != null) {
            request.setContentType(contentType);
        }
        return cosClientHolder.getClient().generatePresignedUrl(request).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generatePresignedDownloadUrl(String cosKey, int expireSec) {
        requireReady();
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                cosClientHolder.getBucket(), cosKey, HttpMethodName.GET);
        request.setExpiration(new Date(System.currentTimeMillis() + expireSec * 1000L));
        return cosClientHolder.getClient().generatePresignedUrl(request).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteObject(String cosKey) {
        requireReady();
        try {
            cosClientHolder.getClient().deleteObject(cosClientHolder.getBucket(), cosKey);
        } catch (RuntimeException ex) {
            log.error(MSG_DELETE_FAILED, ex);
            throw new IllegalStateException(MSG_DELETE_FAILED, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteObjects(String[] cosKeys) {
        requireReady();
        if (cosKeys == null || cosKeys.length == 0) {
            return;
        }
        try {
            List<DeleteObjectsRequest.KeyVersion> keys = Arrays.stream(cosKeys)
                    .map(DeleteObjectsRequest.KeyVersion::new)
                    .collect(Collectors.toList());
            DeleteObjectsRequest request = new DeleteObjectsRequest(cosClientHolder.getBucket());
            request.setKeys(keys);
            DeleteObjectsResult result = cosClientHolder.getClient().deleteObjects(request);
            log.info("批量删除COS对象：成功={}, 总数={}", result.getDeletedObjects().size(), cosKeys.length);
        } catch (RuntimeException ex) {
            log.error(MSG_DELETE_FAILED, ex);
            throw new IllegalStateException(MSG_DELETE_FAILED, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean doesObjectExist(String cosKey) {
        if (!cosClientHolder.ready()) {
            return false;
        }
        try {
            return cosClientHolder.getClient().doesObjectExist(cosClientHolder.getBucket(), cosKey);
        } catch (RuntimeException ex) {
            log.error("检查COS对象存在性失败", ex);
            return false;
        }
    }

    private void requireReady() {
        if (!cosClientHolder.ready()) {
            throw new IllegalStateException(MSG_NOT_READY);
        }
    }
}
