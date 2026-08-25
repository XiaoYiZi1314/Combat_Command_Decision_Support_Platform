package com.ccds.water.water.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.excel.SpreadsheetCodec;
import com.ccds.infra.excel.SpreadsheetTable;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.water.water.constant.WaterRuleConstant;
import com.ccds.water.water.dto.WaterSaveCommand;
import com.ccds.water.water.entity.WaterSourceDO;
import com.ccds.water.water.enums.WaterStatusEnum;
import com.ccds.water.water.enums.WaterTypeEnum;
import com.ccds.water.water.mapper.WaterSourceMapper;
import com.ccds.water.water.service.WaterAccessService;
import com.ccds.water.water.service.WaterService;
import com.ccds.water.water.vo.StationWaterVO;
import com.ccds.water.water.vo.WaterImportResultVO;
import com.ccds.water.water.vo.WaterNearbyResultVO;
import com.ccds.water.water.vo.WaterNearbyVO;
import com.ccds.water.water.vo.WaterVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 水源档案读写、表格导入导出与附近水源检索。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaterServiceImpl implements WaterService {

    private static final String MSG_UNAUTHORIZED = "登录已失效，请重新登录";

    private static final String MSG_WATER_MISSING = "水源档案不存在";

    private static final String MSG_NAME_DUPLICATE = "本站已有同名水源";

    private static final String MSG_IMPORT_INVALID = "表格无法解析或没有可导入的数据";

    private static final String MSG_IMPORT_TOO_LARGE = "导入文件过大";

    private static final String MSG_IMPORT_EMPTY = "请选择要导入的表格";

    private static final String MSG_COORD_INVALID = "经纬度不合法";

    private static final double EARTH_RADIUS_M = 6371008.8;

    private final AccountMapper accountMapper;

    private final WaterSourceMapper waterSourceMapper;

    private final WaterAccessService waterAccessService;

    private final StationMapper stationMapper;

    private final WaterViewTranslator waterViewTranslator;

    private final SpreadsheetCodec spreadsheetCodec;

    /**
     * {@inheritDoc}
     */
    @Override
    public StationWaterVO getWaters(AuthPrincipal principal, Long stationId) {
        AccountDO account = requireAccount(principal);
        StationDO station = waterAccessService.requireVisibleStation(account, stationId);
        boolean writable = waterAccessService.isWritable(account, stationId);
        List<WaterVO> waters = waterViewTranslator.toVos(
                waterSourceMapper.selectAliveByStationId(stationId), station.getName());
        return StationWaterVO.builder()
                .stationId(station.getId())
                .stationName(station.getName())
                .writable(Boolean.valueOf(writable))
                .waters(waters)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WaterVO> listCityWaters(AuthPrincipal principal) {
        requireAccount(principal);
        // 批量查询所有在册水源，避免 N+1
        List<WaterSourceDO> allWaters = waterSourceMapper.selectAliveAll();
        // 构建站名映射
        List<StationDO> stations = stationMapper.selectAll();
        java.util.Map<Long, String> stationNames = new java.util.HashMap<Long, String>();
        for (StationDO station : stations) {
            stationNames.put(station.getId(), station.getName());
        }
        // 按站分组并转换
        java.util.Map<Long, List<WaterSourceDO>> byStation = new java.util.LinkedHashMap<Long, List<WaterSourceDO>>();
        for (WaterSourceDO water : allWaters) {
            byStation.computeIfAbsent(water.getStationId(), k -> new ArrayList<WaterSourceDO>()).add(water);
        }
        List<WaterVO> out = new ArrayList<WaterVO>();
        for (StationDO station : stations) {
            List<WaterSourceDO> stationWaters = byStation.get(station.getId());
            if (stationWaters != null) {
                out.addAll(waterViewTranslator.toVos(stationWaters, station.getName()));
            }
        }
        return out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WaterVO getWater(AuthPrincipal principal, Long stationId, Long waterId) {
        AccountDO account = requireAccount(principal);
        StationDO station = waterAccessService.requireVisibleStation(account, stationId);
        WaterSourceDO water = requireWater(stationId, waterId);
        return waterViewTranslator.toVo(water, station.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaterVO createWater(AuthPrincipal principal, Long stationId, WaterSaveCommand command) {
        AccountDO account = requireAccount(principal);
        StationDO station = waterAccessService.requireWritableStation(account, stationId);
        if (waterSourceMapper.selectAliveByStationAndName(stationId, command.getName().trim()) != null) {
            throw new BizException(ErrorCodeConstant.WATER_NAME_DUPLICATE, MSG_NAME_DUPLICATE);
        }
        LocalDateTime now = LocalDateTime.now();
        WaterSourceDO water = WaterSourceDO.builder()
                .stationId(stationId)
                .name(command.getName().trim())
                .type(command.getType())
                .status(command.getStatus())
                .address(trimToNull(command.getAddress()))
                .lng(command.getLng())
                .lat(command.getLat())
                .extraJson(trimToNull(command.getNotes()))
                .gmtCreate(now)
                .gmtModified(now)
                .build();
        waterSourceMapper.insert(water);
        return waterViewTranslator.toVo(water, station.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaterVO updateWater(AuthPrincipal principal, Long stationId, Long waterId, WaterSaveCommand command) {
        AccountDO account = requireAccount(principal);
        StationDO station = waterAccessService.requireWritableStation(account, stationId);
        WaterSourceDO water = requireWater(stationId, waterId);
        String name = command.getName().trim();
        if (!name.equals(water.getName())) {
            WaterSourceDO dup = waterSourceMapper.selectAliveByStationAndName(stationId, name);
            if (dup != null && !dup.getId().equals(waterId)) {
                throw new BizException(ErrorCodeConstant.WATER_NAME_DUPLICATE, MSG_NAME_DUPLICATE);
            }
        }
        water.setName(name);
        water.setType(command.getType());
        water.setStatus(command.getStatus());
        water.setAddress(trimToNull(command.getAddress()));
        water.setLng(command.getLng());
        water.setLat(command.getLat());
        water.setExtraJson(trimToNull(command.getNotes()));
        water.setGmtModified(LocalDateTime.now());
        waterSourceMapper.update(water);
        return waterViewTranslator.toVo(water, station.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWater(AuthPrincipal principal, Long stationId, Long waterId) {
        AccountDO account = requireAccount(principal);
        waterAccessService.requireWritableStation(account, stationId);
        requireWater(stationId, waterId);
        LocalDateTime now = LocalDateTime.now();
        waterSourceMapper.softDelete(waterId, now, now);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaterImportResultVO importWaters(AuthPrincipal principal, Long stationId, String fileName, byte[] bytes) {
        AccountDO account = requireAccount(principal);
        waterAccessService.requireWritableStation(account, stationId);
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
        return applyImportRows(stationId, rows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] exportWaters(AuthPrincipal principal, Long stationId) {
        StationWaterVO stationWaters = getWaters(principal, stationId);
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(new ArrayList<String>(WaterSpreadsheetSupport.EXPORT_HEADERS));
        for (WaterVO water : stationWaters.getWaters()) {
            rows.add(toExportRow(water, stationWaters.getStationName()));
        }
        if (rows.size() == 1) {
            rows.add(new ArrayList<String>(WaterSpreadsheetSupport.TEMPLATE_EXAMPLE));
        }
        return spreadsheetCodec.writeXlsx(WaterRuleConstant.EXPORT_SHEET_NAME,
                SpreadsheetTable.builder().rows(rows).build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] exportTemplate() {
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
    public WaterNearbyResultVO nearby(AuthPrincipal principal, BigDecimal lng, BigDecimal lat,
                                       Integer radiusM, Integer limit) {
        requireAccount(principal);
        if (lng == null || lat == null) {
            throw new BizException(ErrorCodeConstant.WATER_COORD_INVALID, MSG_COORD_INVALID);
        }
        int radius = clamp(radiusM, WaterRuleConstant.NEARBY_DEFAULT_RADIUS_M,
                WaterRuleConstant.NEARBY_MAX_RADIUS_M);
        int max = clamp(limit, WaterRuleConstant.NEARBY_DEFAULT_LIMIT, WaterRuleConstant.NEARBY_MAX_LIMIT);
        List<WaterSourceDO> actives = waterSourceMapper.selectActiveAll(WaterRuleConstant.NEARBY_SCAN_MAX);
        // 构建站名映射
        List<StationDO> stations = stationMapper.selectAll();
        java.util.Map<Long, String> stationNames = new java.util.HashMap<Long, String>();
        for (StationDO station : stations) {
            stationNames.put(station.getId(), station.getName());
        }
        List<WaterNearbyVO> hits = new ArrayList<WaterNearbyVO>();
        for (WaterSourceDO water : actives) {
            if (water.getLng() == null || water.getLat() == null) {
                continue;
            }
            long distance = distanceMeters(lng.doubleValue(), lat.doubleValue(),
                    water.getLng().doubleValue(), water.getLat().doubleValue());
            if (distance > radius) {
                continue;
            }
            WaterTypeEnum type = WaterTypeEnum.fromCode(water.getType());
            hits.add(WaterNearbyVO.builder()
                    .id(water.getId())
                    .stationName(stationNames.get(water.getStationId()))
                    .name(water.getName())
                    .type(water.getType())
                    .typeLabel(type == null ? water.getType() : type.getLabel())
                    .address(water.getAddress())
                    .lng(water.getLng())
                    .lat(water.getLat())
                    .distanceM(Long.valueOf(distance))
                    .estimateFlow(Double.valueOf(type == null
                            ? WaterRuleConstant.ESTIMATE_FLOW_OTHER : type.getEstimateFlow()))
                    .build());
        }
        hits.sort(Comparator.comparing(WaterNearbyVO::getDistanceM));
        List<WaterNearbyVO> trimmed = hits.size() > max ? new ArrayList<WaterNearbyVO>(hits.subList(0, max)) : hits;
        return WaterNearbyResultVO.builder()
                .radiusM(Integer.valueOf(radius))
                .count(Integer.valueOf(trimmed.size()))
                .waters(trimmed)
                .build();
    }

    private WaterImportResultVO applyImportRows(Long stationId, List<List<String>> rows) {
        List<String> header = rows.get(0);
        int typeIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_TYPE, 1);
        int setupIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_SETUP, 3);
        int addressIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_ADDRESS, 4);
        int usableIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_USABLE, 10);
        int repairIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_REPAIR, 12);
        int notesIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_NOTES, 14);
        int lngIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_LNG, 15);
        int latIdx = WaterSpreadsheetSupport.findColumn(header, WaterSpreadsheetSupport.HEADER_LAT, 16);

        // 预加载本站在册名称，避免 N+1
        List<WaterSourceDO> existing = waterSourceMapper.selectAliveByStationId(stationId);
        java.util.Set<String> existingNames = new java.util.HashSet<String>();
        for (WaterSourceDO water : existing) {
            existingNames.add(water.getName());
        }

        int added = 0;
        int skipExample = 0;
        int skipTypeInvalid = 0;
        int skipCoordMissing = 0;
        int skipDuplicate = 0;
        LocalDateTime now = LocalDateTime.now();
        List<WaterSourceDO> toInsert = new ArrayList<WaterSourceDO>();

        for (int r = 1; r < rows.size(); r++) {
            List<String> cols = rows.get(r);
            if (joinBlank(cols)) {
                continue;
            }
            if (WaterSpreadsheetSupport.isExampleRow(cols, typeIdx, 2, addressIdx)) {
                skipExample++;
                continue;
            }
            String typeRaw = WaterSpreadsheetSupport.cell(cols, typeIdx);
            WaterTypeEnum type = resolveType(typeRaw, WaterSpreadsheetSupport.cell(cols, setupIdx));
            if (type == null) {
                skipTypeInvalid++;
                continue;
            }
            String address = WaterSpreadsheetSupport.cell(cols, addressIdx);
            String name = buildName(type, WaterSpreadsheetSupport.cell(cols, 0), r);
            BigDecimal lng = parseDecimal(WaterSpreadsheetSupport.cell(cols, lngIdx));
            BigDecimal lat = parseDecimal(WaterSpreadsheetSupport.cell(cols, latIdx));
            if (lng == null || lat == null) {
                skipCoordMissing++;
                continue;
            }
            String status = resolveStatus(WaterSpreadsheetSupport.cell(cols, usableIdx),
                    WaterSpreadsheetSupport.cell(cols, repairIdx));
            if (existingNames.contains(name)) {
                skipDuplicate++;
                continue;
            }
            existingNames.add(name);
            WaterSourceDO water = WaterSourceDO.builder()
                    .stationId(stationId)
                    .name(name)
                    .type(type.getCode())
                    .status(status)
                    .address(trimToNull(address))
                    .lng(lng)
                    .lat(lat)
                    .extraJson(trimToNull(WaterSpreadsheetSupport.cell(cols, notesIdx)))
                    .gmtCreate(now)
                    .gmtModified(now)
                    .build();
            toInsert.add(water);
        }

        // 批量插入
        if (!toInsert.isEmpty()) {
            waterSourceMapper.insertBatch(toInsert);
            added = toInsert.size();
        }

        int totalSkipped = skipExample + skipTypeInvalid + skipCoordMissing + skipDuplicate;
        return WaterImportResultVO.builder()
                .addedCount(Integer.valueOf(added))
                .skippedCount(Integer.valueOf(totalSkipped))
                .skipExampleCount(Integer.valueOf(skipExample))
                .skipTypeInvalidCount(Integer.valueOf(skipTypeInvalid))
                .skipCoordMissingCount(Integer.valueOf(skipCoordMissing))
                .skipDuplicateCount(Integer.valueOf(skipDuplicate))
                .build();
    }

    private List<String> toExportRow(WaterVO water, String stationName) {
        List<String> row = new ArrayList<String>();
        row.add(water.getTypeLabel());
        row.add(stationName);
        row.add(WaterTypeEnum.UNDERGROUND.getCode().equals(water.getType()) ? "地下" : "地上");
        row.add(water.getAddress() == null ? "" : water.getAddress());
        row.add(WaterStatusEnum.REPAIR.getCode().equals(water.getStatus()) ? "不好用" : "好用");
        row.add(WaterStatusEnum.REPAIR.getCode().equals(water.getStatus()) ? "是" : "否");
        row.add(water.getNotes() == null ? "" : water.getNotes());
        row.add(water.getLng() == null ? "" : water.getLng().toPlainString());
        row.add(water.getLat() == null ? "" : water.getLat().toPlainString());
        return row;
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

    private WaterSourceDO requireWater(Long stationId, Long waterId) {
        WaterSourceDO water = waterSourceMapper.selectAliveById(waterId);
        if (water == null || !stationId.equals(water.getStationId())) {
            throw new BizException(ErrorCodeConstant.WATER_NOT_FOUND, MSG_WATER_MISSING);
        }
        return water;
    }

    private static WaterTypeEnum resolveType(String typeRaw, String setupRaw) {
        WaterTypeEnum type = WaterTypeEnum.fromLabel(typeRaw);
        if (type == null && (setupRaw.contains("地下"))) {
            return WaterTypeEnum.UNDERGROUND;
        }
        return type;
    }

    private static String resolveStatus(String usable, String repair) {
        if (usable.contains("不好用") || usable.contains("坏") || repair.contains("是")) {
            return WaterStatusEnum.REPAIR.getCode();
        }
        return WaterStatusEnum.ACTIVE.getCode();
    }

    private static String buildName(WaterTypeEnum type, String seqRaw, int rowNo) {
        String seq = seqRaw == null || seqRaw.isBlank() ? String.valueOf(rowNo) : seqRaw.trim();
        String fullName = (type.getLabel() + " " + seq).trim();
        // 限长防止超 VARCHAR(64)
        if (fullName.length() > WaterRuleConstant.NAME_MAX_LENGTH) {
            return fullName.substring(0, WaterRuleConstant.NAME_MAX_LENGTH);
        }
        return fullName;
    }

    private static long distanceMeters(double lng1, double lat1, double lng2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return Math.round(2 * EARTH_RADIUS_M * Math.asin(Math.min(1.0, Math.sqrt(a))));
    }

    private static int clamp(Integer value, int fallback, int max) {
        if (value == null || value.intValue() <= 0) {
            return fallback;
        }
        return Math.min(value.intValue(), max);
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
