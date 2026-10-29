package com.ccds.duty.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.constant.FileBizTypeConstant;
import com.ccds.duty.constant.FileRuleConstant;
import com.ccds.duty.constant.KeyUnitCategoryConstant;
import com.ccds.duty.entity.KeyUnitDO;
import com.ccds.duty.mapper.KeyUnitMapper;
import com.ccds.duty.service.FileObjectService;
import com.ccds.duty.service.LegacyObjectImportService;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.infra.cloud.cos.COSService;
import com.ccds.infra.cloud.cos.FileContentValidator;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.water.water.entity.WaterSourceDO;
import com.ccds.water.water.mapper.WaterSourceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 历史 COS 对象登记实现。
 *
 * @author ccds
 * @since 0.1.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegacyObjectImportServiceImpl implements LegacyObjectImportService {

    private static final String MSG_MANIFEST = "历史导入清单无法读取";

    private static final String MSG_OPERATOR = "历史导入操作账号不存在";

    private static final String DEFAULT_OPERATOR = "kaifazhe";

    private static final String IMPORT_NOTES = "由历史预案导入，类别按名称推断，待复核";

    private static final String OPERATOR_ENV = "CCDS_LEGACY_IMPORT_OPERATOR";

    private static final Map<String, String> STATION_ALIASES = Map.of(
            "华溪消防救援站", "华溪消防站",
            "战勤保障站", "战勤保障分队",
            "正阳路消防救援站", "新明路消防站",
            "正阳路消防站", "新明路消防站",
            "新明路消防救援站", "新明路消防站");

    private final ObjectMapper objectMapper;

    private final AccountMapper accountMapper;

    private final StationMapper stationMapper;

    private final KeyUnitMapper keyUnitMapper;

    private final WaterSourceMapper waterSourceMapper;

    private final FileObjectService fileObjectService;

    private final COSService cosService;

    private final FileContentValidator fileContentValidator;

    /**
     * {@inheritDoc}
     */
    @Override
    public String importManifest(Path manifestPath) {
        LegacyImportManifest manifest = readManifest(manifestPath);
        AccountDO operator = requireOperator();
        Map<String, Long> stations = loadStationMap();
        int createdUnits = 0;
        int registeredFiles = 0;
        int skipped = 0;
        List<LegacyImportItem> items = manifest.items;
        for (LegacyImportItem item : items) {
            try {
                ImportItemResult result = importOne(operator.getId(), stations, item);
                createdUnits += result.createdUnit ? 1 : 0;
                registeredFiles += result.registeredFile ? 1 : 0;
                skipped += result.skipped ? 1 : 0;
            } catch (RuntimeException ex) {
                skipped++;
                log.warn("历史对象导入跳过 station={} name={}",
                        item == null ? null : item.stationName,
                        item == null ? null : item.bizName, ex);
            }
        }
        String summary = String.format(Locale.ROOT,
                "历史导入完成：单位=%d, 文件=%d, 跳过=%d, 清单=%d",
                createdUnits, registeredFiles, skipped, items.size());
        log.info(summary);
        return summary;
    }

    private ImportItemResult importOne(Long operatorId, Map<String, Long> stations, LegacyImportItem item) {
        if (item == null || item.bizType == null || item.bizName == null || item.stationName == null) {
            return ImportItemResult.skipped();
        }
        Long stationId = resolveStationId(item.stationName, stations);
        if (stationId == null) {
            log.warn("历史导入找不到消防站");
            return ImportItemResult.skipped();
        }
        ensureObjectOnCos(item);
        if (FileBizTypeConstant.KEYUNIT_PLAN.equals(item.bizType)
                || FileBizTypeConstant.KEYUNIT_FLOOR.equals(item.bizType)) {
            return importKeyUnitFile(operatorId, stationId, item);
        }
        if (FileBizTypeConstant.WATER_PHOTO.equals(item.bizType)
                || FileBizTypeConstant.WATER_DOC.equals(item.bizType)) {
            return importWaterFile(operatorId, stationId, item);
        }
        return ImportItemResult.skipped();
    }

    private ImportItemResult importKeyUnitFile(Long operatorId, Long stationId, LegacyImportItem item) {
        String unitName = item.bizName.trim();
        KeyUnitDO existing = keyUnitMapper.selectAliveByStationAndName(stationId, unitName);
        boolean created = false;
        if (existing == null) {
            existing = KeyUnitDO.builder()
                    .stationId(stationId)
                    .name(unitName)
                    .category(resolveCategory(item))
                    .notes(IMPORT_NOTES)
                    .build();
            keyUnitMapper.insert(existing);
            created = true;
        }
        fileObjectService.registerImportedObject(
                operatorId, item.bizType, existing.getId(), item.originalName,
                item.contentType, item.cosKey, item.sizeBytes);
        return new ImportItemResult(created, true, false);
    }

    private ImportItemResult importWaterFile(Long operatorId, Long stationId, LegacyImportItem item) {
        WaterSourceDO water = waterSourceMapper.selectAliveByStationAndName(stationId, item.bizName.trim());
        if (water == null) {
            log.warn("历史导入找不到对应水源");
            return ImportItemResult.skipped();
        }
        fileObjectService.registerImportedObject(
                operatorId, item.bizType, water.getId(), item.originalName,
                item.contentType, item.cosKey, item.sizeBytes);
        return new ImportItemResult(false, true, false);
    }

    private void ensureObjectOnCos(LegacyImportItem item) {
        if (item.cosKey == null || item.cosKey.isBlank()) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_MANIFEST);
        }
        if (cosService.doesObjectExist(item.cosKey)) {
            return;
        }
        if (item.localPath == null || item.localPath.isBlank()) {
            throw new BizException(ErrorCodeConstant.FILE_COS_UNAVAILABLE, MSG_MANIFEST);
        }
        byte[] content = readLocalFile(item.localPath);
        if (item.sizeBytes != null && item.sizeBytes.longValue() != content.length) {
            throw new BizException(ErrorCodeConstant.FILE_SIZE_INVALID, MSG_MANIFEST);
        }
        if (content.length > FileRuleConstant.MAX_SIZE_BYTES) {
            throw new BizException(ErrorCodeConstant.FILE_SIZE_INVALID, MSG_MANIFEST);
        }
        if (!fileContentValidator.matches(item.contentType, content)) {
            throw new BizException(ErrorCodeConstant.FILE_CONTENT_TYPE_INVALID, MSG_MANIFEST);
        }
        try {
            cosService.putObject(item.cosKey, item.contentType, content);
        } catch (IllegalStateException ex) {
            throw new BizException(ErrorCodeConstant.FILE_COS_UNAVAILABLE, MSG_MANIFEST, ex);
        }
        item.sizeBytes = Long.valueOf(content.length);
    }

    private byte[] readLocalFile(String localPath) {
        try {
            Path path = Path.of(localPath);
            if (!Files.isRegularFile(path)) {
                throw new BizException(ErrorCodeConstant.FILE_NOT_FOUND, MSG_MANIFEST);
            }
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new BizException(ErrorCodeConstant.FILE_NOT_FOUND, MSG_MANIFEST, ex);
        }
    }

    private LegacyImportManifest readManifest(Path manifestPath) {
        if (manifestPath == null || !Files.isRegularFile(manifestPath)) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_MANIFEST);
        }
        try {
            LegacyImportManifest manifest = objectMapper.readValue(
                    manifestPath.toFile(), LegacyImportManifest.class);
            if (manifest == null || manifest.items == null) {
                throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_MANIFEST);
            }
            return manifest;
        } catch (IOException ex) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_MANIFEST, ex);
        }
    }

    private AccountDO requireOperator() {
        String username = System.getenv(OPERATOR_ENV);
        if (username == null || username.isBlank()) {
            username = DEFAULT_OPERATOR;
        }
        AccountDO account = accountMapper.selectByUsername(username.trim());
        if (account == null) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_OPERATOR);
        }
        return account;
    }

    private Map<String, Long> loadStationMap() {
        Map<String, Long> map = new HashMap<String, Long>();
        for (StationDO station : stationMapper.selectAll()) {
            if (station.getName() != null) {
                map.put(station.getName(), station.getId());
            }
            if (station.getNameNorm() != null) {
                map.put(station.getNameNorm(), station.getId());
            }
        }
        return map;
    }

    private Long resolveStationId(String stationName, Map<String, Long> stations) {
        String trimmed = stationName.trim();
        Long id = stations.get(trimmed);
        if (id != null) {
            return id;
        }
        String alias = STATION_ALIASES.get(trimmed);
        if (alias == null) {
            return null;
        }
        return stations.get(alias);
    }

    private String resolveCategory(LegacyImportItem item) {
        if (KeyUnitCategoryConstant.isAllowed(item.categoryHint)) {
            return item.categoryHint;
        }
        return guessCategory(item.bizName);
    }

    private String guessCategory(String name) {
        if (containsAny(name, "加油站", "加气", "液化气", "危化", "油库", "天然气", "爆竹")) {
            return KeyUnitCategoryConstant.HAZMAT;
        }
        if (containsAny(name, "庙", "寺", "文物")) {
            return KeyUnitCategoryConstant.CULTURAL_RELIC;
        }
        if (containsAny(name, "地下", "人防")) {
            return KeyUnitCategoryConstant.UNDERGROUND;
        }
        if (containsAny(name, "高层", "大厦")) {
            return KeyUnitCategoryConstant.HIGH_RISE;
        }
        if (containsAny(name, "厂房", "仓库", "物流")) {
            return KeyUnitCategoryConstant.LARGE_SPAN;
        }
        if (containsAny(name, "酒店", "宾馆", "商场", "超市", "医院", "学校",
                "幼儿园", "养老", "市场", "银行", "广场", "影院", "体育馆",
                "公寓", "水汇", "汤汇", "沐足", "摄影")) {
            return KeyUnitCategoryConstant.CROWDED_PLACE;
        }
        return KeyUnitCategoryConstant.OTHER;
    }

    private boolean containsAny(String name, String... keywords) {
        if (name == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (name.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static final class ImportItemResult {

        private final boolean createdUnit;

        private final boolean registeredFile;

        private final boolean skipped;

        private ImportItemResult(boolean createdUnit, boolean registeredFile, boolean skipped) {
            this.createdUnit = createdUnit;
            this.registeredFile = registeredFile;
            this.skipped = skipped;
        }

        private static ImportItemResult skipped() {
            return new ImportItemResult(false, false, true);
        }
    }
}
