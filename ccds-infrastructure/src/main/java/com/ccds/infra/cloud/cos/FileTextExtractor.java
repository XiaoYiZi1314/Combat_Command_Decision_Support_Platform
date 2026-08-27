package com.ccds.infra.cloud.cos;

/**
 * 文件正文抽取器。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface FileTextExtractor {

    /**
     * 按 MIME 类型抽取正文。
     *
     * @param contentType MIME 类型
     * @param content     文件内容
     * @return 抽取正文；不支持或抽取失败返回 null
     */
    String extract(String contentType, byte[] content);
}
