package com.ccds.bootstrap.seed;

import java.nio.file.Path;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.ccds.duty.service.LegacyObjectImportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 启动时按环境变量导入历史对象清单。未配置则跳过。
 *
 * @author ccds
 * @since 0.1.1
 */
@Slf4j
@Component
@Order(200)
@RequiredArgsConstructor
public class LegacyObjectImportRunner implements ApplicationRunner {

    private static final String MANIFEST_ENV = "CCDS_LEGACY_IMPORT_MANIFEST";

    private final LegacyObjectImportService legacyObjectImportService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(ApplicationArguments args) {
        String manifest = System.getenv(MANIFEST_ENV);
        if (manifest == null || manifest.isBlank()) {
            return;
        }
        log.info("开始导入历史对象清单");
        String summary = legacyObjectImportService.importManifest(Path.of(manifest.trim()));
        log.info("历史对象清单导入结束：{}", summary);
    }
}
