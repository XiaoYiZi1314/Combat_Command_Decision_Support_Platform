package com.ccds.infra.cloud.cos;

/**
 * COS 对象存储。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface COSService {

    /**
     * 生成预签名上传 URL。
     *
     * @param cosKey      对象键
     * @param contentType MIME 类型
     * @param expireSec   过期秒数
     * @return 预签名 URL
     */
    String generatePresignedUploadUrl(String cosKey, String contentType, int expireSec);

    /**
     * 生成预签名下载 URL。
     *
     * @param cosKey    对象键
     * @param expireSec 过期秒数
     * @return 预签名 URL
     */
    String generatePresignedDownloadUrl(String cosKey, int expireSec);

    /**
     * 删除对象。失败抛出非法状态，由业务转为错误码。
     *
     * @param cosKey 对象键
     */
    void deleteObject(String cosKey);

    /**
     * 批量删除对象。失败抛出非法状态，由业务转为错误码。
     *
     * @param cosKeys 对象键列表
     */
    void deleteObjects(String[] cosKeys);

    /**
     * 检查对象是否存在。
     *
     * @param cosKey 对象键
     * @return 存在为 true；未配置或调用失败为 false
     */
    boolean doesObjectExist(String cosKey);

    /**
     * 获取对象实际大小。
     *
     * @param cosKey 对象键
     * @return 对象字节数；对象不存在、未配置或调用失败返回 null
     */
    Long getObjectSize(String cosKey);

    /**
     * 读取对象内容，限制由实现统一控制。
     *
     * @param cosKey 对象键
     * @return 对象内容；对象不存在、未配置或读取失败返回 null
     */
    byte[] getObjectContent(String cosKey);
}
