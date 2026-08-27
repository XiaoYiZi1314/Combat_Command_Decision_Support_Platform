package com.ccds.infra.redis;

/**
 * 文件上传确认票据存储。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface FileUploadConfirmStore {

    /**
     * 为待上传文件签发随机确认票据。
     *
     * @param fileId    文件主键
     * @param accountId 创建账号主键
     * @param expireSec 有效秒数
     * @return 确认票据
     */
    String issue(Long fileId, Long accountId, int expireSec);

    /**
     * 校验并原子消费确认票据。
     *
     * @param ticket    确认票据
     * @param fileId    文件主键
     * @param accountId 当前账号主键
     * @return 校验成功为 true
     */
    boolean consume(String ticket, Long fileId, Long accountId);
}
