package com.ccds.bootstrap.seed;

import java.nio.file.Path;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.ccds.duty.service.LegacyDataImportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 启动时按环境变量一次性导入旧系统花名册数据。未配置数据目录则跳过。
 * 设置 CCDS_LEGACY_DRY_RUN=1 只读不写。
 *
 * @author ccds
 * @since 0.1.1
 */
@Slf4j
@Component
@Order(210)
@RequiredArgsConstructor
public class LegacyDataImportRunner implements ApplicationRunner {

    private static final String DATA_DIR_ENV = "CCDS_LEGACY_DATA_DIR";

    private final LegacyDataImportService legacyDataImportService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(ApplicationArguments args) {
        String dataDir = System.getenv(DATA_DIR_ENV);
        if (dataDir == null || dataDir.isBlank()) {
            return;
        }
        log.info("开始导入旧系统花名册数据");
        String summary = legacyDataImportService.importAll(Path.of(dataDir.trim()));
        log.info("旧系统花名册数据导入结束：{}", summary);
    }
}
