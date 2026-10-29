package com.ccds.duty.service;

import java.nio.file.Path;

/**
 * 将已上传到 COS 的历史对象登记为现有 APP 业务文件。
 *
 * @author ccds
 * @since 0.1.1
 */
public interface LegacyObjectImportService {

    /**
     * 按清单导入重点单位与水源附件。同一对象键重复执行会跳过。
     *
     * @param manifestPath 清单 JSON 路径
     * @return 导入摘要，不会为 null
     */
    String importManifest(Path manifestPath);
}
