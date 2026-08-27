package com.ccds.infra.cloud.cos;

/**
 * 上传文件内容校验器。根据声明的 MIME 校验文件签名，阻止伪装文件进入业务数据。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface FileContentValidator {

    /**
     * 校验文件内容是否与声明的 MIME 匹配。
     *
     * @param contentType 声明的 MIME
     * @param content     文件内容
     * @return 匹配为 true
     */
    boolean matches(String contentType, byte[] content);
}
