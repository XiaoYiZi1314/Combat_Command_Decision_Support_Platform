package com.ccds.infra.cloud.cos;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.DeleteObjectsRequest;
import com.qcloud.cos.model.DeleteObjectsResult;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;

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

    private static final int MAX_READ_BYTES = 20 * 1024 * 1024;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getObjectSize(String cosKey) {
        if (!cosClientHolder.ready()) {
            return null;
        }
        try {
            ObjectMetadata metadata = cosClientHolder.getClient()
                    .getObjectMetadata(cosClientHolder.getBucket(), cosKey);
            return metadata == null ? null : metadata.getContentLength();
        } catch (RuntimeException ex) {
            log.error("读取COS对象元数据失败", ex);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getObjectContent(String cosKey) {
        if (!cosClientHolder.ready()) {
            return null;
        }
        try (InputStream input = cosClientHolder.getClient().getObject(
                new GetObjectRequest(cosClientHolder.getBucket(), cosKey)).getObjectContent();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > MAX_READ_BYTES) {
                    log.warn("COS对象超过内容读取上限 cosKey={}", cosKey);
                    return null;
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (Exception ex) {
            log.error("读取COS对象内容失败 cosKey={}", cosKey, ex);
            return null;
        }
    }

    private void requireReady() {
        if (!cosClientHolder.ready()) {
            throw new IllegalStateException(MSG_NOT_READY);
        }
    }
}
