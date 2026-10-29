package com.ccds.duty.service.impl;

/**
 * 历史对象导入项。
 *
 * @author ccds
 * @since 0.1.1
 */
class LegacyImportItem {

    /**
     * 业务类型。
     */
    public String bizType;

    /**
     * 站名。
     */
    public String stationName;

    /**
     * 业务对象名称，重点单位名或水源名。
     */
    public String bizName;

    /**
     * 原始文件名。
     */
    public String originalName;

    /**
     * MIME。
     */
    public String contentType;

    /**
     * 字节数。
     */
    public Long sizeBytes;

    /**
     * COS 对象键。
     */
    public String cosKey;

    /**
     * 本地待上传路径，可空。
     */
    public String localPath;

    /**
     * 类别提示，可空。
     */
    public String categoryHint;
}
