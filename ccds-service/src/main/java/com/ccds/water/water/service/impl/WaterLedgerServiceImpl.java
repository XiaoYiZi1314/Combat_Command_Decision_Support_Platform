package com.ccds.water.water.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.excel.NamedSheet;
import com.ccds.infra.excel.SpreadsheetCodec;
import com.ccds.infra.excel.SpreadsheetTable;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.org.org.service.OrgQueryService;
import com.ccds.water.water.constant.WaterRuleConstant;
import com.ccds.water.water.entity.WaterRequestDO;
import com.ccds.water.water.entity.WaterSourceDO;
import com.ccds.water.water.enums.WaterSetupEnum;
import com.ccds.water.water.enums.WaterStatusEnum;
import com.ccds.water.water.enums.WaterSupplyFormEnum;
import com.ccds.water.water.enums.WaterTypeEnum;
import com.ccds.water.water.enums.WaterUsableEnum;
import com.ccds.water.water.mapper.WaterDeletedMapper;
import com.ccds.water.water.mapper.WaterRequestMapper;
import com.ccds.water.water.mapper.WaterSourceMapper;
import com.ccds.water.water.service.WaterAccessService;
import com.ccds.water.water.service.WaterLedgerService;
import com.ccds.water.water.vo.WaterImportResultVO;
import com.ccds.water.water.vo.WaterReasonStatsVO;
import com.ccds.water.water.vo.WaterStatsRowVO;
import com.ccds.water.water.vo.WaterStatsVO;
import com.ccds.water.water.vo.WaterTypeStatsRowVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 水源检查表导入导出与汇总统计。
 *
 * <p>统计口径：一个谓词（{@link WaterStatusResolver}）、一条聚合路径、一份 VO，
 * 屏幕与导出共用，禁止各处另算。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaterLedgerServiceImpl implements WaterLedgerService {

    private static final String MSG_UNAUTHORIZED = "登录已失效，请重新登录";

    private static final String MSG_IMPORT_INVALID = "表格无法解析或没有可导入的数据";

    private static final String MSG_IMPORT_TOO_LARGE = "导入文件过大";

    private static final int PERCENT_BASE = 100;

    private static final int RATE_SCALE = 10;

    private final AccountMapper accountMapper;

    private final WaterSourceMapper waterSourceMapper;

    private final WaterDeletedMapper waterDeletedMapper;

    private final WaterRequestMapper waterRequestMapper;

    private final com.ccds.water.water.mapper.WaterVideoMapper waterVideoMapper;

    private final WaterAccessService waterAccessService;

    private final StationMapper stationMapper;

    private final OrgQueryService orgQueryService;

    private final SpreadsheetCodec spreadsheetCodec;

    private final WaterServiceImpl waterService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaterImportResultVO importWaters(AuthPrincipal principal, Long stationId, String fileName,
                                            byte[] bytes) {
        AccountDO account = requireAccount(principal);
        waterAccessService.requireVisibleStation(account, stationId);
        // Excel 直接写正式台账，不允许站级、大队账号借导入接口绕过审批。
        waterAccessService.requireReviewer(account);
        if (bytes == null || bytes.length == 0 || bytes.length > WaterRuleConstant.IMPORT_MAX_BYTES) {
            throw new BizException(ErrorCodeConstant.WATER_IMPORT_INVALID,
                    bytes != null && bytes.length > WaterRuleConstant.IMPORT_MAX_BYTES
                            ? MSG_IMPORT_TOO_LARGE : MSG_IMPORT_INVALID);
        }
        SpreadsheetTable table;
        try {
            table = spreadsheetCodec.parse(fileName, bytes);
        } catch (RuntimeException ex) {
            log.warn("water import parse failed stationId={}", stationId, ex);
            throw new BizException(ErrorCodeConstant.WATER_IMPORT_INVALID, MSG_IMPORT_INVALID);
        }
        List<List<String>> rows = table.getRows();
        if (rows == null || rows.size() < 2 || rows.size() > WaterRuleConstant.IMPORT_MAX_ROWS) {
            throw new BizException(ErrorCodeConstant.WATER_IMPORT_INVALID, MSG_IMPORT_INVALID);
        }
        return applyImportRows(account, stationId, rows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] exportWaters(AuthPrincipal principal, Long stationId) {
        AccountDO account = requireAccount(principal);
        StationDO station = waterAccessService.requireVisibleStation(account, stationId);
        List<WaterSourceDO> rows = waterSourceMapper.selectAliveByStationId(stationId);
        List<List<String>> out = new ArrayList<List<String>>();
        out.add(new ArrayList<String>(WaterSpreadsheetSupport.EXPORT_HEADERS));
        int seq = 1;
        for (WaterSourceDO water : rows) {
            out.add(toExportRow(water, station.getName(), seq));
            seq++;
        }
        if (out.size() == 1) {
            out.add(new ArrayList<String>(WaterSpreadsheetSupport.TEMPLATE_EXAMPLE));
        }
        return spreadsheetCodec.writeXlsx(WaterRuleConstant.EXPORT_SHEET_NAME,
                SpreadsheetTable.builder().rows(out).build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] exportTemplate(AuthPrincipal principal, Long stationId) {
        AccountDO account = requireAccount(principal);
        waterAccessService.requireVisibleStation(account, stationId);
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(new ArrayList<String>(WaterSpreadsheetSupport.EXPORT_HEADERS));
        rows.add(new ArrayList<String>(WaterSpreadsheetSupport.TEMPLATE_EXAMPLE));
        return spreadsheetCodec.writeXlsx(WaterRuleConstant.EXPORT_SHEET_NAME,
                SpreadsheetTable.builder().rows(rows).build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WaterStatsVO summary(AuthPrincipal principal, String month) {
        AccountDO account = requireAccount(principal);
        List<StationDO> stations = orgQueryService.listVisibleStationEntities(account);
        YearMonth target = resolveMonth(month);
        List<Long> stationIds = new ArrayList<Long>();
        Map<Long, StationDO> stationMap = new LinkedHashMap<Long, StationDO>();
        for (StationDO station : stations) {
            stationIds.add(station.getId());
            stationMap.put(station.getId(), station);
        }
        Map<Long, List<WaterSourceDO>> byStation = new LinkedHashMap<Long, List<WaterSourceDO>>();
        Map<Long, Integer> monthRepair = new HashMap<Long, Integer>();
        Map<Long, Integer> videoCount = new HashMap<Long, Integer>();
        if (!stationIds.isEmpty()) {
            for (WaterSourceDO water : waterSourceMapper.selectAliveByStationIds(stationIds)) {
                byStation.computeIfAbsent(water.getStationId(), k -> new ArrayList<WaterSourceDO>()).add(water);
            }
            monthRepair = loadMonthRepair(stationIds, target);
            videoCount = loadVideoCount(stationIds);
        }
        List<WaterStatsRowVO> craneRows = new ArrayList<WaterStatsRowVO>();
        List<WaterStatsRowVO> hydrantRows = new ArrayList<WaterStatsRowVO>();
        WaterReasonStatsVO reasonTotals = WaterReasonStatsVO.zero();
        for (StationDO station : stations) {
            List<WaterSourceDO> waters = byStation.getOrDefault(station.getId(), new ArrayList<WaterSourceDO>());
            WaterStatsRowVO crane = buildRow(station, waters, true);
            WaterStatsRowVO hydrant = buildRow(station, waters, false);
            if (crane.getTotal().intValue() > 0 || hydrant.getTotal().intValue() > 0) {
                craneRows.add(crane);
                hydrantRows.add(hydrant);
            }
            reasonTotals.add(crane.getReasons());
            reasonTotals.add(hydrant.getReasons());
        }
        WaterStatsVO.WaterStatsVOBuilder builder = WaterStatsVO.builder()
                .title(WaterRuleConstant.STATS_TITLE_PREFIX + " " + target.atEndOfMonth())
                .month(target.toString())
                .craneRows(craneRows)
                .hydrantRows(hydrantRows)
                .craneTypeRows(buildTypeRows(stations, byStation, monthRepair, videoCount,
                        WaterTypeEnum.CRANE))
                .groundTypeRows(buildTypeRows(stations, byStation, monthRepair, videoCount,
                        WaterTypeEnum.GROUND))
                .undergroundTypeRows(buildTypeRows(stations, byStation, monthRepair, videoCount,
                        WaterTypeEnum.UNDERGROUND))
                .reasonTotals(reasonTotals)
                // 应急加水点 / 消防水塔 / 其他水源本期先保持 0，不用消火栓去填
                .emergencyPoints(Integer.valueOf(0))
                .waterTowers(Integer.valueOf(0))
                .otherSources(Integer.valueOf(0));
        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] exportSummary(AuthPrincipal principal, String month) {
        WaterStatsVO stats = summary(principal, month);
        List<NamedSheet> sheets = new ArrayList<NamedSheet>();
        sheets.add(new NamedSheet("水源汇总-水鹤", SpreadsheetTable.builder()
                .rows(craneSheetRows(stats)).build()));
        sheets.add(new NamedSheet("水源汇总-消火栓", SpreadsheetTable.builder()
                .rows(hydrantSheetRows(stats)).build()));
        sheets.add(new NamedSheet("类型统计-消防水鹤", SpreadsheetTable.builder()
                .rows(typeSheetRows(stats.getCraneTypeRows(), "消防水鹤")).build()));
        sheets.add(new NamedSheet("类型统计-地上消火栓", SpreadsheetTable.builder()
                .rows(typeSheetRows(stats.getGroundTypeRows(), "地上消火栓")).build()));
        sheets.add(new NamedSheet("类型统计-地下消火栓", SpreadsheetTable.builder()
                .rows(typeSheetRows(stats.getUndergroundTypeRows(), "地下消火栓")).build()));
        return spreadsheetCodec.writeXlsxMultiSheet(sheets);
    }

    private WaterImportResultVO applyImportRows(AccountDO account, Long stationId, List<List<String>> rows) {
        List<String> header = rows.get(0);
        int typeIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_TYPE, 1);
        int stationIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_STATION, 2);
        int setupIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_SETUP, 3);
        int addressIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_ADDRESS, 4);
        int diameterIdx = WaterSpreadsheetSupport.findColumn(header,
                WaterSpreadsheetSupport.HEADER_PIPE_DIAMETER, 5);
        int pressureIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_PRESSURE, 6);
        int flowIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_FLOW, 7);
        int supplyIdx = WaterSpreadsheetSupport.findColumn(header,
                WaterSpreadsheetSupport.HEADER_SUPPLY_FORM, 8);
        int heightIdx = WaterSpreadsheetSupport.findColumn(header,
                WaterSpreadsheetSupport.HEADER_ADD_HEIGHT, 9);
        int usableIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_USABLE, 10);
        int reasonIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_REASON, 11);
        int repairIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_REPAIR, 12);
        int notesIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_NOTES, 14);
        int lngIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_LNG, 15);
        int latIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_LAT, 16);
        int drainViewIdx = WaterSpreadsheetSupport.findColumn(header,
                WaterSpreadsheetSupport.HEADER_DRAIN_VIEWABLE, 17);
        int drainUsableIdx = WaterSpreadsheetSupport.findColumn(header,
                WaterSpreadsheetSupport.HEADER_DRAIN_USABLE, 18);
        int drainReasonIdx = WaterSpreadsheetSupport.findColumn(header,
                WaterSpreadsheetSupport.HEADER_DRAIN_REASON, 19);
        int drainNotesIdx = WaterSpreadsheetSupport.findColumn(header,
                WaterSpreadsheetSupport.HEADER_DRAIN_NOTES, 20);
        int warmthIdx = WaterSpreadsheetSupport.findColumn(header,
                WaterSpreadsheetSupport.HEADER_WARMTH, 21);
        int warmthReasonIdx = WaterSpreadsheetSupport.findColumn(header,
                WaterSpreadsheetSupport.HEADER_WARMTH_REASON, 22);

        List<WaterSourceDO> existing = waterSourceMapper.selectAllByStationIncludingDeleted(stationId);
        Map<String, WaterSourceDO> byAddress = new LinkedHashMap<String, WaterSourceDO>();
        Set<String> deletedKeys = new LinkedHashSet<String>();
        for (WaterSourceDO water : existing) {
            String key = importKey(water.getType(), water.getAddress(), water.getName());
            if (water.getDeletedAt() != null) {
                deletedKeys.add(key);
            } else {
                byAddress.put(key, water);
            }
        }
        Set<String> deletedNos = new LinkedHashSet<String>(waterDeletedMapper.selectActiveNos(stationId));
        for (com.ccds.water.water.entity.WaterDeletedDO deleted
                : waterDeletedMapper.selectActiveByStationIds(List.of(stationId))) {
            deletedKeys.add(importKey(deleted.getType(), null, deleted.getName()));
        }
        StationDO station = stationMapper.selectById(stationId);

        int added = 0;
        int updated = 0;
        int skipExample = 0;
        int skipTypeInvalid = 0;
        int skipCoordMissing = 0;
        int skipDeleted = 0;
        int skipUnchanged = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int r = 1; r < rows.size(); r++) {
            List<String> cols = rows.get(r);
            if (joinBlank(cols)) {
                continue;
            }
            if (WaterSpreadsheetSupport.isExampleRow(cols, typeIdx, stationIdx, addressIdx)) {
                skipExample++;
                continue;
            }
            String setupRaw = WaterSpreadsheetSupport.cell(cols, setupIdx);
            String address = WaterSpreadsheetSupport.cell(cols, addressIdx);
            String typeRaw = WaterSpreadsheetSupport.cell(cols, typeIdx);
            // 空位置不能用 Excel 行号生成名称，否则重排文件会创建重复卡片。
            if (address.isBlank()) {
                skipTypeInvalid++;
                continue;
            }
            String rowStation = WaterSpreadsheetSupport.cell(cols, stationIdx);
            if (station == null || (!rowStation.isBlank() && !rowStation.equals(station.getName()))) {
                skipTypeInvalid++;
                continue;
            }
            WaterTypeEnum type = WaterTypeEnum.resolve(typeRaw, setupRaw, null, address);
            if (type == null) {
                skipTypeInvalid++;
                continue;
            }
            BigDecimal lng = parseDecimal(WaterSpreadsheetSupport.cell(cols, lngIdx));
            BigDecimal lat = parseDecimal(WaterSpreadsheetSupport.cell(cols, latIdx));
            String usableRaw = WaterSpreadsheetSupport.cell(cols, usableIdx);
            String repairRaw = WaterSpreadsheetSupport.cell(cols, repairIdx);
            String key = importKey(type.getCode(), address, buildName(type, address, r));
            WaterSourceDO hit = byAddress.get(key);
            if (deletedKeys.contains(key)
                    || deletedKeys.contains(importKey(type.getCode(), null, buildName(type, address, r)))) {
                skipDeleted++;
                continue;
            }
            if (hit != null && hit.getWaterNo() != null && deletedNos.contains(hit.getWaterNo())) {
                // 已删除编号即使出现在表里也不恢复
                skipDeleted++;
                continue;
            }
            if (hit != null) {
                if (applyRow(hit, cols, type, setupRaw, address, lng, lat, usableRaw, repairRaw,
                        diameterIdx, pressureIdx, flowIdx, supplyIdx, heightIdx, reasonIdx, notesIdx,
                        drainViewIdx, drainUsableIdx, drainReasonIdx, drainNotesIdx,
                        warmthIdx, warmthReasonIdx, now)) {
                    waterSourceMapper.update(hit);
                    updated++;
                } else {
                    skipUnchanged++;
                }
                continue;
            }
            if (lng == null || lat == null) {
                skipCoordMissing++;
                continue;
            }
            if (usableRaw.isBlank() && repairRaw.isBlank()) {
                skipTypeInvalid++;
                continue;
            }
            WaterSourceDO created = new WaterSourceDO();
            applyRow(created, cols, type, setupRaw, address, lng, lat, usableRaw, repairRaw,
                    diameterIdx, pressureIdx, flowIdx, supplyIdx, heightIdx, reasonIdx, notesIdx,
                    drainViewIdx, drainUsableIdx, drainReasonIdx, drainNotesIdx,
                    warmthIdx, warmthReasonIdx, now);
            created.setStationId(stationId);
            created.setName(buildName(type, address, r));
            created.setWaterNo(waterService.nextWaterNo(stationId));
            created.setGmtCreate(now);
            created.setGmtModified(now);
            waterSourceMapper.insert(created);
            byAddress.put(importKey(type.getCode(), address, created.getName()), created);
            added++;
        }
        int totalSkipped = skipExample + skipTypeInvalid + skipCoordMissing + skipDeleted;
        log.info("water import stationId={} added={} updated={} skipped={} unchanged={}",
                stationId, added, updated, totalSkipped, skipUnchanged);
        return WaterImportResultVO.builder()
                .addedCount(Integer.valueOf(added))
                .updatedCount(Integer.valueOf(updated))
                .skippedCount(Integer.valueOf(totalSkipped))
                .skipExampleCount(Integer.valueOf(skipExample))
                .skipTypeInvalidCount(Integer.valueOf(skipTypeInvalid))
                .skipCoordMissingCount(Integer.valueOf(skipCoordMissing))
                .skipDuplicateCount(Integer.valueOf(skipUnchanged))
                .skipDeletedCount(Integer.valueOf(skipDeleted))
                .build();
    }

    private boolean applyRow(WaterSourceDO water, List<String> cols, WaterTypeEnum type, String setupRaw,
                             String address, BigDecimal lng, BigDecimal lat, String usableRaw,
                             String repairRaw, int diameterIdx, int pressureIdx, int flowIdx,
                             int supplyIdx, int heightIdx, int reasonIdx, int notesIdx,
                             int drainViewIdx, int drainUsableIdx, int drainReasonIdx,
                             int drainNotesIdx, int warmthIdx, int warmthReasonIdx,
                             LocalDateTime now) {
        String usable = resolveUsable(usableRaw, repairRaw);
        Integer repairFlag = resolveRepairFlag(usable, repairRaw);
        String status = WaterStatusResolver.deriveStatus(usable, repairFlag);
        boolean bad = WaterStatusResolver.isBad(status, usable, repairFlag);

        // 「只填泄水阀/保暖、没改使用状态」的行：只更新这 6 列，不动在用/报修
        boolean onlyAuxiliary = usableRaw.isEmpty() && repairRaw.isEmpty();
        if (onlyAuxiliary) {
            applyAuxiliaryColumns(water, cols, drainViewIdx, drainUsableIdx, drainReasonIdx,
                    drainNotesIdx, warmthIdx, warmthReasonIdx);
            water.setGmtModified(now);
            return true;
        }
        if (!onlyAuxiliary) {
            water.setStatus(status);
            water.setUsable(usable);
            water.setIsRepair(repairFlag);
        }
        water.setType(type.getCode());
        water.setAddress(trimToNull(address));
        if (lng != null && lat != null) {
            water.setLng(lng);
            water.setLat(lat);
        }
        water.setSetup(resolveSetupCode(setupRaw));
        water.setPipeDiameter(clampInt(WaterSpreadsheetSupport.cell(cols, diameterIdx),
                WaterRuleConstant.PIPE_DIAMETER_MAX));
        water.setPressureMpa(clampDecimal(WaterSpreadsheetSupport.cell(cols, pressureIdx),
                WaterRuleConstant.PRESSURE_MAX_MPA));
        water.setFlowLps(clampDecimal(WaterSpreadsheetSupport.cell(cols, flowIdx),
                WaterRuleConstant.FLOW_MAX_LPS));
        water.setSupplyForm(resolveSupplyCode(WaterSpreadsheetSupport.cell(cols, supplyIdx)));
        water.setAddHeightM(clampDecimal(WaterSpreadsheetSupport.cell(cols, heightIdx),
                WaterRuleConstant.ADD_HEIGHT_MAX_M));
        if (!onlyAuxiliary) {
            String reasonText = trimToNull(WaterSpreadsheetSupport.cell(cols, reasonIdx));
            if (bad) {
                water.setReasonText(reasonText);
                water.setReasonCode(WaterReasonClassifier.classifyReasonCode(reasonText));
                water.setPartCode(WaterReasonClassifier.classifyPartCode(reasonText));
            } else {
                // 好用卡清空原因勾与部件勾，旧原因不得计入统计
                water.setReasonText(null);
                water.setReasonCode(null);
                water.setPartCode(null);
            }
            water.setExtraJson(trimToNull(WaterSpreadsheetSupport.cell(cols, notesIdx)));
        }
        applyAuxiliaryColumns(water, cols, drainViewIdx, drainUsableIdx, drainReasonIdx,
                drainNotesIdx, warmthIdx, warmthReasonIdx);
        if (water.getRetained() == null) {
            water.setRetained(Integer.valueOf(0));
        }
        water.setGmtModified(now);
        return true;
    }

    private void applyAuxiliaryColumns(WaterSourceDO water, List<String> cols,
                                       int drainViewIdx, int drainUsableIdx, int drainReasonIdx,
                                       int drainNotesIdx, int warmthIdx, int warmthReasonIdx) {
        water.setDrainViewable(resolveYesNo(WaterSpreadsheetSupport.cell(cols, drainViewIdx)));
        water.setDrainUsable(resolveUsableCode(WaterSpreadsheetSupport.cell(cols, drainUsableIdx)));
        water.setDrainReason(trimToNull(WaterSpreadsheetSupport.cell(cols, drainReasonIdx)));
        water.setDrainNotes(trimToNull(WaterSpreadsheetSupport.cell(cols, drainNotesIdx)));
        water.setWarmthMeasure(resolveFlag(WaterSpreadsheetSupport.cell(cols, warmthIdx)));
        water.setWarmthReason(trimToNull(WaterSpreadsheetSupport.cell(cols, warmthReasonIdx)));
    }

    private String importKey(String type, String address, String name) {
        return (type == null ? "" : type) + "|" + matchKey(address, name);
    }

    private String resolveUsable(String usableRaw, String repairRaw) {
        WaterUsableEnum usable = WaterUsableEnum.fromText(usableRaw);
        if (usable == WaterUsableEnum.BAD) {
            return WaterUsableEnum.BAD.getCode();
        }
        if (usable == WaterUsableEnum.GOOD) {
            return WaterUsableEnum.GOOD.getCode();
        }
        if (containsYes(repairRaw)) {
            return WaterUsableEnum.BAD.getCode();
        }
        return WaterUsableEnum.GOOD.getCode();
    }

    private Integer resolveRepairFlag(String usable, String repairRaw) {
        if (containsYes(repairRaw)) {
            return Integer.valueOf(1);
        }
        if (repairRaw.contains("否")) {
            return Integer.valueOf(0);
        }
        return WaterUsableEnum.BAD.getCode().equals(usable) ? Integer.valueOf(1) : Integer.valueOf(0);
    }

    private boolean containsYes(String raw) {
        return raw != null && raw.contains("是");
    }

    private String resolveSetupCode(String raw) {
        WaterSetupEnum setup = WaterSetupEnum.fromText(raw);
        return setup == null ? null : setup.getCode();
    }

    private String resolveSupplyCode(String raw) {
        WaterSupplyFormEnum form = WaterSupplyFormEnum.fromText(raw);
        return form == null ? null : form.getCode();
    }

    private String resolveUsableCode(String raw) {
        WaterUsableEnum usable = WaterUsableEnum.fromText(raw);
        return usable == null ? null : usable.getCode();
    }

    private String resolveYesNo(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (raw.contains("否") || raw.contains("无") || raw.contains("不可")) {
            return "no";
        }
        return "yes";
    }

    private Integer resolveFlag(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (raw.contains("否") || raw.contains("无")) {
            return Integer.valueOf(0);
        }
        return Integer.valueOf(1);
    }

    private String buildName(WaterTypeEnum type, String address, int rowNo) {
        String tail = address == null || address.isBlank() ? String.valueOf(rowNo) : address;
        String full = type.getLabel() + " " + tail;
        return full.length() > WaterRuleConstant.NAME_MAX_LENGTH
                ? full.substring(0, WaterRuleConstant.NAME_MAX_LENGTH) : full;
    }

    private String matchKey(String address, String name) {
        String base = address == null || address.isBlank() ? name : address;
        return base == null ? "" : base.replaceAll("[\\s\\n\\r　]", "");
    }

    private List<String> toExportRow(WaterSourceDO water, String stationName, int seq) {
        WaterTypeEnum type = WaterTypeEnum.fromCode(water.getType());
        WaterSetupEnum setup = WaterSetupEnum.fromCode(water.getSetup());
        WaterSupplyFormEnum supply = WaterSupplyFormEnum.fromCode(water.getSupplyForm());
        boolean bad = WaterStatusResolver.isBad(water.getStatus(), water.getUsable(), water.getIsRepair());
        List<String> row = new ArrayList<String>();
        row.add(String.valueOf(seq));
        row.add(type == null ? "" : type.getLabel());
        row.add(stationName == null ? "" : stationName);
        row.add(setup == null ? "" : setup.getLabel());
        row.add(nullToEmpty(water.getAddress()));
        row.add(water.getPipeDiameter() == null ? "" : String.valueOf(water.getPipeDiameter()));
        row.add(water.getPressureMpa() == null ? "" : water.getPressureMpa().toPlainString());
        row.add(water.getFlowLps() == null ? "" : water.getFlowLps().toPlainString());
        row.add(supply == null ? "" : supply.getLabel());
        row.add(water.getAddHeightM() == null ? "" : water.getAddHeightM().toPlainString());
        row.add(bad ? WaterUsableEnum.BAD.getLabel() : WaterUsableEnum.GOOD.getLabel());
        row.add(bad ? nullToEmpty(water.getReasonText()) : "");
        row.add(bad ? "是" : "否");
        row.add("");
        row.add(nullToEmpty(water.getExtraJson()));
        row.add(water.getLng() == null ? "" : water.getLng().toPlainString());
        row.add(water.getLat() == null ? "" : water.getLat().toPlainString());
        row.add("yes".equalsIgnoreCase(nullToEmpty(water.getDrainViewable())) ? "是"
                : ("no".equalsIgnoreCase(nullToEmpty(water.getDrainViewable())) ? "否" : ""));
        row.add(nullToEmpty(water.getDrainUsable()));
        row.add(nullToEmpty(water.getDrainReason()));
        row.add(nullToEmpty(water.getDrainNotes()));
        row.add(water.getWarmthMeasure() == null ? ""
                : (water.getWarmthMeasure().intValue() == 1 ? "是" : "否"));
        row.add(nullToEmpty(water.getWarmthReason()));
        return row;
    }

    private WaterStatsRowVO buildRow(StationDO station, List<WaterSourceDO> waters, boolean crane) {
        WaterReasonStatsVO reasons = WaterReasonStatsVO.zero();
        int total = 0;
        int good = 0;
        int bad = 0;
        int groundTotal = 0;
        int groundGood = 0;
        int groundBad = 0;
        int undergroundTotal = 0;
        int undergroundGood = 0;
        int undergroundBad = 0;
        for (WaterSourceDO water : waters) {
            WaterTypeEnum type = WaterTypeEnum.fromCode(water.getType());
            if (type == null) {
                continue;
            }
            boolean isCrane = type == WaterTypeEnum.CRANE;
            if (isCrane != crane) {
                continue;
            }
            boolean isBad = WaterStatusResolver.isBad(water.getStatus(), water.getUsable(),
                    water.getIsRepair());
            total++;
            if (isBad) {
                bad++;
                reasons.increment(water.getReasonCode() == null
                        ? WaterReasonClassifier.classifyReasonCode(water.getReasonText())
                        : water.getReasonCode());
            } else {
                good++;
            }
            if (!crane) {
                if (type == WaterTypeEnum.GROUND) {
                    groundTotal++;
                    if (isBad) {
                        groundBad++;
                    } else {
                        groundGood++;
                    }
                } else {
                    undergroundTotal++;
                    if (isBad) {
                        undergroundBad++;
                    } else {
                        undergroundGood++;
                    }
                }
            }
        }
        return WaterStatsRowVO.builder()
                .stationId(station.getId())
                .stationName(station.getName())
                .brigadeName(station.getBrigadeName())
                .total(Integer.valueOf(total))
                .good(Integer.valueOf(good))
                .bad(Integer.valueOf(bad))
                .goodRate(Double.valueOf(rate(good, total)))
                .groundTotal(Integer.valueOf(groundTotal))
                .groundGood(Integer.valueOf(groundGood))
                .groundBad(Integer.valueOf(groundBad))
                .undergroundTotal(Integer.valueOf(undergroundTotal))
                .undergroundGood(Integer.valueOf(undergroundGood))
                .undergroundBad(Integer.valueOf(undergroundBad))
                .reasons(reasons)
                .build();
    }

    private List<WaterTypeStatsRowVO> buildTypeRows(List<StationDO> stations,
                                                    Map<Long, List<WaterSourceDO>> byStation,
                                                    Map<Long, Integer> monthRepair,
                                                    Map<Long, Integer> videoCount,
                                                    WaterTypeEnum type) {
        List<WaterTypeStatsRowVO> rows = new ArrayList<WaterTypeStatsRowVO>();
        for (StationDO station : stations) {
            List<WaterSourceDO> waters = byStation.getOrDefault(station.getId(), new ArrayList<WaterSourceDO>());
            int total = 0;
            int bad = 0;
            int videos = 0;
            for (WaterSourceDO water : waters) {
                if (!type.getCode().equals(water.getType())) {
                    continue;
                }
                total++;
                if (WaterStatusResolver.isBad(water.getStatus(), water.getUsable(), water.getIsRepair())) {
                    bad++;
                } else if (hasVideo(station.getId(), water.getId(), videoCount)) {
                    // 只有「在用而且真有视频」才计入上传出水视频数，报修卡即使有视频也不计
                    videos++;
                }
            }
            Integer repair = monthRepair.getOrDefault(station.getId(), Integer.valueOf(0));
            if (total == 0 && repair.intValue() == 0) {
                // 总数与本月维修数都为 0 的单位不列出
                continue;
            }
            rows.add(WaterTypeStatsRowVO.builder()
                    .stationId(station.getId())
                    .stationName(station.getName())
                    .brigadeName(station.getBrigadeName())
                    .total(Integer.valueOf(total))
                    .bad(Integer.valueOf(bad))
                    .videoCount(Integer.valueOf(videos))
                    .monthRepairCount(repair)
                    .build());
        }
        return rows;
    }

    private boolean hasVideo(Long stationId, Long waterId, Map<Long, Integer> videoCount) {
        return waterId != null && videoCount.containsKey(waterId);
    }

    /**
     * 统计各水源的出水视频条数。只数真有视频的卡，是否在用由调用方按唯一谓词判断。
     *
     * @param stationIds 站主键列表，不得为空
     * @return 水源主键到视频条数的映射
     */
    private Map<Long, Integer> loadVideoCount(List<Long> stationIds) {
        Map<Long, Integer> out = new HashMap<Long, Integer>();
        for (Map<String, Object> row : waterVideoMapper.countVideosByWaterIds(stationIds)) {
            Object waterId = row.get("waterId");
            Object cnt = row.get("cnt");
            if (waterId instanceof Number && cnt instanceof Number) {
                out.put(Long.valueOf(((Number) waterId).longValue()),
                        Integer.valueOf(((Number) cnt).intValue()));
            }
        }
        return out;
    }

    private Map<Long, Integer> loadMonthRepair(List<Long> stationIds, YearMonth month) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        Map<Long, Integer> out = new HashMap<Long, Integer>();
        for (Map<String, Object> row : waterRequestMapper.countRepairFixedByStations(stationIds, from, to)) {
            Object stationId = row.get("stationId");
            Object cnt = row.get("cnt");
            if (stationId instanceof Number && cnt instanceof Number) {
                out.put(Long.valueOf(((Number) stationId).longValue()),
                        Integer.valueOf(((Number) cnt).intValue()));
            }
        }
        return out;
    }

    private YearMonth resolveMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (RuntimeException ex) {
            return YearMonth.now();
        }
    }

    private double rate(int good, int total) {
        if (total <= 0) {
            return 0.0;
        }
        double raw = good * (double) PERCENT_BASE / total;
        return Math.round(raw * RATE_SCALE) / (double) RATE_SCALE;
    }

    private List<List<String>> craneSheetRows(WaterStatsVO stats) {
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(List.of(stats.getTitle()));
        rows.add(List.of("单位", "数量", "好用", "好用率%", "不好用",
                "无水", "漏水", "压力不足", "锈蚀", "埋压", "圈占", "挪用", "塌井", "毁坏"));
        for (WaterStatsRowVO row : stats.getCraneRows()) {
            rows.add(reasonRow(row.getStationName(), row.getTotal(), row.getGood(), row.getGoodRate(),
                    row.getBad(), row.getReasons()));
        }
        rows.add(List.of("应急加水点", "0"));
        rows.add(List.of("消防水塔", "0"));
        rows.add(List.of("其他水源", "0"));
        return rows;
    }

    private List<List<String>> hydrantSheetRows(WaterStatsVO stats) {
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(List.of(stats.getTitle()));
        rows.add(List.of("单位", "数量小计", "地上", "地下", "好用小计", "地上", "地下",
                "好用率%", "不好用小计", "地上", "地下",
                "无水", "漏水", "压力不足", "锈蚀", "埋压", "圈占", "挪用", "塌井", "毁坏"));
        for (WaterStatsRowVO row : stats.getHydrantRows()) {
            List<String> line = new ArrayList<String>();
            line.add(row.getStationName());
            line.add(String.valueOf(row.getTotal()));
            line.add(String.valueOf(row.getGroundTotal()));
            line.add(String.valueOf(row.getUndergroundTotal()));
            line.add(String.valueOf(row.getGood()));
            line.add(String.valueOf(row.getGroundGood()));
            line.add(String.valueOf(row.getUndergroundGood()));
            line.add(String.valueOf(row.getGoodRate()));
            line.add(String.valueOf(row.getBad()));
            line.add(String.valueOf(row.getGroundBad()));
            line.add(String.valueOf(row.getUndergroundBad()));
            appendReasons(line, row.getReasons());
            rows.add(line);
        }
        return rows;
    }

    private List<String> reasonRow(String stationName, Integer total, Integer good, Double goodRate,
                                   Integer bad, WaterReasonStatsVO reasons) {
        List<String> line = new ArrayList<String>();
        line.add(stationName);
        line.add(String.valueOf(total));
        line.add(String.valueOf(good));
        line.add(String.valueOf(goodRate));
        line.add(String.valueOf(bad));
        appendReasons(line, reasons);
        return line;
    }

    private void appendReasons(List<String> line, WaterReasonStatsVO reasons) {
        WaterReasonStatsVO safe = reasons == null ? WaterReasonStatsVO.zero() : reasons;
        line.add(String.valueOf(safe.getNoWater()));
        line.add(String.valueOf(safe.getLeak()));
        line.add(String.valueOf(safe.getLowPressure()));
        line.add(String.valueOf(safe.getRust()));
        line.add(String.valueOf(safe.getBuried()));
        line.add(String.valueOf(safe.getOccupied()));
        line.add(String.valueOf(safe.getDiverted()));
        line.add(String.valueOf(safe.getCollapsedWell()));
        line.add(String.valueOf(safe.getDamaged()));
    }

    private List<List<String>> typeSheetRows(List<WaterTypeStatsRowVO> rows, String typeName) {
        List<List<String>> out = new ArrayList<List<String>>();
        out.add(List.of(typeName));
        out.add(List.of("单位", "总数", "不好用数", "上传出水视频数", "本月维修数"));
        for (WaterTypeStatsRowVO row : rows) {
            List<String> line = new ArrayList<String>();
            line.add(row.getStationName());
            line.add(String.valueOf(row.getTotal()));
            line.add(String.valueOf(row.getBad()));
            line.add(String.valueOf(row.getVideoCount()));
            line.add(String.valueOf(row.getMonthRepairCount()));
            out.add(line);
        }
        return out;
    }

    private AccountDO requireAccount(AuthPrincipal principal) {
        if (principal == null || principal.getAccountId() == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        AccountDO account = accountMapper.selectById(principal.getAccountId());
        if (account == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        return account;
    }

    private static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer clampInt(String raw, int max) {
        BigDecimal value = parseDecimal(raw);
        if (value == null || value.signum() < 0) {
            return null;
        }
        int intValue = value.intValue();
        return Integer.valueOf(Math.min(intValue, max));
    }

    private static BigDecimal clampDecimal(String raw, double max) {
        BigDecimal value = parseDecimal(raw);
        if (value == null || value.signum() < 0) {
            return null;
        }
        return value.doubleValue() > max ? BigDecimal.valueOf(max) : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean joinBlank(List<String> cols) {
        if (cols == null) {
            return true;
        }
        for (String col : cols) {
            if (col != null && !col.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
