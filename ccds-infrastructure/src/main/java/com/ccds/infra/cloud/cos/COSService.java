package com.ccds.infra.cloud.cos;

import java.net.URL;

/**
 * COS对象存储服务
 *
 * @author system
 * @since 2024
 */
public interface COSService {

    /**
     * 生成预签名上传URL
     *
     * @param cosKey      对象键
     * @param contentType MIME类型
     * @param expireSec   过期时间(秒)
     * @return 预签名URL
     */
    String generatePresignedUploadUrl(String cosKey, String contentType, int expireSec);

    /**
     * 生成预签名下载URL
     *
     * @param cosKey    对象键
     * @param expireSec 过期时间(秒)
     * @return 预签名URL
     */
    String generatePresignedDownloadUrl(String cosKey, int expireSec);

    /**
     * 删除对象
     *
     * @param cosKey 对象键
     */
    void deleteObject(String cosKey);

    /**
     * 批量删除对象
     *
     * @param cosKeys 对象键列表
     */
    void deleteObjects(String[] cosKeys);

    /**
     * 检查对象是否存在
     *
     * @param cosKey 对象键
     * @return 是否存在
     */
    boolean doesObjectExist(String cosKey);
}
