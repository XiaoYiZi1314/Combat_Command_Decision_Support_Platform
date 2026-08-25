package com.ccds.infra.cloud.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.DeleteObjectsRequest;
import com.qcloud.cos.model.DeleteObjectsResult;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * COS对象存储服务实现
 *
 * @author system
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class COSServiceImpl implements COSService {

    private final COSClient cosClient;
    private final String cosBucket;

    @Override
    public String generatePresignedUploadUrl(String cosKey, String contentType, int expireSec) {
        if (cosClient == null) {
            throw new IllegalStateException("COS客户端未初始化");
        }

        Date expiration = new Date(System.currentTimeMillis() + expireSec * 1000L);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(cosBucket, cosKey, HttpMethodName.PUT);
        request.setExpiration(expiration);
        if (contentType != null) {
            request.setContentType(contentType);
        }

        URL url = cosClient.generatePresignedUrl(request);
        return url.toString();
    }

    @Override
    public String generatePresignedDownloadUrl(String cosKey, int expireSec) {
        if (cosClient == null) {
            throw new IllegalStateException("COS客户端未初始化");
        }

        Date expiration = new Date(System.currentTimeMillis() + expireSec * 1000L);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(cosBucket, cosKey, HttpMethodName.GET);
        request.setExpiration(expiration);

        URL url = cosClient.generatePresignedUrl(request);
        return url.toString();
    }

    @Override
    public void deleteObject(String cosKey) {
        if (cosClient == null) {
            log.warn("COS客户端未初始化，跳过删除：{}", cosKey);
            return;
        }

        try {
            cosClient.deleteObject(cosBucket, cosKey);
            log.info("删除COS对象：{}", cosKey);
        } catch (Exception e) {
            log.error("删除COS对象失败：{}", cosKey, e);
        }
    }

    @Override
    public void deleteObjects(String[] cosKeys) {
        if (cosClient == null) {
            log.warn("COS客户端未初始化，跳过批量删除");
            return;
        }

        if (cosKeys == null || cosKeys.length == 0) {
            return;
        }

        try {
            List<DeleteObjectsRequest.KeyVersion> keys = Arrays.stream(cosKeys)
                    .map(DeleteObjectsRequest.KeyVersion::new)
                    .collect(Collectors.toList());

            DeleteObjectsRequest request = new DeleteObjectsRequest(cosBucket);
            request.setKeys(keys);

            DeleteObjectsResult result = cosClient.deleteObjects(request);
            log.info("批量删除COS对象：成功={}, 总数={}", 
                    result.getDeletedObjects().size(), cosKeys.length);
        } catch (Exception e) {
            log.error("批量删除COS对象失败", e);
        }
    }

    @Override
    public boolean doesObjectExist(String cosKey) {
        if (cosClient == null) {
            return false;
        }

        try {
            return cosClient.doesObjectExist(cosBucket, cosKey);
        } catch (Exception e) {
            log.error("检查COS对象存在性失败：{}", cosKey, e);
            return false;
        }
    }
}
