package com.ccds.duty.service;

import java.nio.file.Path;

/**
 * 将 ETL 产出的旧系统花名册数据一次性导入现有 CCDS 表。
 *
 * @author ccds
 * @since 0.1.1
 */
public interface LegacyDataImportService {

    /**
     * 逐站读取数据目录下的 roster.ndjson 并导入人员、编组、值班、标定、删除记录、阈值与作业模式。
     * 每站独立事务：单站异常只回滚该站，不中断整批；同一站内旧数据脏重名、脏 NFC 等唯一键冲突按条跳过并计数。
     * 重复执行按业务唯一键幂等。
     *
     * @param dataDir 迁移数据目录（含 roster.ndjson），也允许直接传 roster.ndjson 文件
     * @return 导入摘要文本，不会为 null
     */
    String importAll(Path dataDir);
}
