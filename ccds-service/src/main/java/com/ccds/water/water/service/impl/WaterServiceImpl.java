package com.ccds.water.water.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.constant.FileBizTypeConstant;
import com.ccds.duty.service.FileObjectService;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.org.org.service.OrgQueryService;
import com.ccds.water.water.constant.WaterRuleConstant;
import com.ccds.water.water.dto.WaterSaveCommand;
import com.ccds.water.water.entity.WaterDeletedDO;
import com.ccds.water.water.entity.WaterRequestDO;
import com.ccds.water.water.entity.WaterSourceDO;
import com.ccds.water.water.enums.WaterTypeEnum;
import com.ccds.water.water.enums.WaterUsableEnum;
import com.ccds.water.water.mapper.WaterDeletedMapper;
import com.ccds.water.water.mapper.WaterRequestMapper;
import com.ccds.water.water.mapper.WaterSourceMapper;
import com.ccds.water.water.service.WaterAccessService;
import com.ccds.water.water.service.WaterService;
import com.ccds.water.water.vo.StationWaterVO;
import com.ccds.water.water.vo.WaterNearbyResultVO;
import com.ccds.water.water.vo.WaterNearbyVO;
import com.ccds.water.water.vo.WaterVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 水源档案读写与附近水源检索。
 *
 * <p>不好用判定、原因归类与删除回流拦截全部收口在本实现与 {@link WaterStatusResolver}、
 * {@link WaterReasonClassifier}，界面、导出与统计不得另写一套。</p>
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

    private static final String MSG_COORD_INVALID = "经纬度不合法";

    private static final String MSG_REQUEST_REQUIRED = "水源改动需提交审批申请，通过后生效";

    private static final String MSG_STATION_MISSING = "单位不存在";

    private static final double EARTH_RADIUS_M = 6371008.8;

    /**
     * 删除记录来源：用户删除。
     */
    static final String SOURCE_USER_DELETE = "user_delete";

    /**
     * 删除记录来源：审批通过删除。
     */
    static final String SOURCE_APPROVAL = "approval";

    private final AccountMapper accountMapper;

    private final WaterSourceMapper waterSourceMapper;

    private final WaterDeletedMapper waterDeletedMapper;

    private final WaterRequestMapper waterRequestMapper;

    private final WaterAccessService waterAccessService;

    private final StationMapper stationMapper;

    private final OrgQueryService orgQueryService;

    private final WaterViewTranslator waterViewTranslator;

    private final FileObjectService fileObjectService;

    /**
     * {@inheritDoc}
     */
    @Override
    public StationWaterVO getWaters(AuthPrincipal principal, Long stationId) {
        AccountDO account = requireAccount(principal);
        StationDO station = waterAccessService.requireVisibleStation(account, stationId);
        boolean writable = waterAccessService.isWritable(account, stationId);
        // 1.8：明确要求保留的卡即使被删，打开列表也按原编号补回
        restoreRetained(stationId);
        List<WaterSourceDO> rows = waterSourceMapper.selectAliveByStationId(stationId);
        List<WaterVO> waters = waterViewTranslator.toVos(rows, station.getName());
        markPending(waters, stationId);
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
        AccountDO account = requireAccount(principal);
        List<StationDO> stations = orgQueryService.listVisibleStationEntities(account);
        if (stations.isEmpty()) {
            return new ArrayList<WaterVO>();
        }
        List<Long> stationIds = new ArrayList<Long>();
        for (StationDO station : stations) {
            stationIds.add(station.getId());
        }
        List<WaterSourceDO> allWaters = waterSourceMapper.selectAliveByStationIds(stationIds);
        Map<Long, List<WaterSourceDO>> byStation = new LinkedHashMap<Long, List<WaterSourceDO>>();
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
        return attachFiles(waterViewTranslator.toVo(water, station.getName()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaterVO createWater(AuthPrincipal principal, Long stationId, WaterSaveCommand command) {
        AccountDO account = requireAccount(principal);
        waterAccessService.requireVisibleStation(account, stationId);
        requireDirectWrite(account);
        StationDO station = requireStation(stationId);
        if (waterSourceMapper.selectAliveByStationAndName(stationId, command.getName().trim()) != null) {
            throw new BizException(ErrorCodeConstant.WATER_NAME_DUPLICATE, MSG_NAME_DUPLICATE);
        }
        LocalDateTime now = LocalDateTime.now();
        WaterSourceDO water = applyCommand(new WaterSourceDO(), stationId, command, now);
        water.setWaterNo(nextWaterNo(stationId));
        water.setGmtCreate(now);
        water.setGmtModified(now);
        waterSourceMapper.insert(water);
        // 用户主动新建视为解除该编号的删除记录
        waterDeletedMapper.release(stationId, water.getWaterNo(), account.getId());
        log.info("water created stationId={} waterId={} waterNo={}",
                stationId, water.getId(), water.getWaterNo());
        return waterViewTranslator.toVo(water, station.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaterVO updateWater(AuthPrincipal principal, Long stationId, Long waterId, WaterSaveCommand command) {
        AccountDO account = requireAccount(principal);
        waterAccessService.requireVisibleStation(account, stationId);
        requireDirectWrite(account);
        StationDO station = requireStation(stationId);
        WaterSourceDO water = requireWater(stationId, waterId);
        String name = command.getName().trim();
        if (!name.equals(water.getName())) {
            WaterSourceDO dup = waterSourceMapper.selectAliveByStationAndName(stationId, name);
            if (dup != null && !dup.getId().equals(waterId)) {
                throw new BizException(ErrorCodeConstant.WATER_NAME_DUPLICATE, MSG_NAME_DUPLICATE);
            }
        }
        applyCommand(water, stationId, command, LocalDateTime.now());
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
        waterAccessService.requireVisibleStation(account, stationId);
        requireDirectWrite(account);
        WaterSourceDO water = requireWater(stationId, waterId);
        softDeleteWithTombstone(water, account.getId(), SOURCE_USER_DELETE);
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
        Map<Long, String> stationNames = stationNameMap();
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
            // 优先用卡片上的管网流量，没有才按类型估算
            Double flow = water.getFlowLps() != null && water.getFlowLps().doubleValue() > 0
                    ? Double.valueOf(water.getFlowLps().doubleValue())
                    : Double.valueOf(type == null
                            ? WaterRuleConstant.ESTIMATE_FLOW_OTHER : type.getEstimateFlow());
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
                    .estimateFlow(flow)
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

    /**
     * 按站补回「明确要求保留」的卡：原编号、原 id 补回，不换新号。
     *
     * @param stationId 站主键
     * @return 补回条数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int restoreRetained(Long stationId) {
        List<WaterSourceDO> all = waterSourceMapper.selectAllByStationIncludingDeleted(stationId);
        int restored = 0;
        LocalDateTime now = LocalDateTime.now();
        for (WaterSourceDO water : all) {
            if (water.getDeletedAt() == null) {
                continue;
            }
            if (water.getRetained() == null || water.getRetained().intValue() != 1) {
                continue;
            }
            String name = stripDeletedSuffix(water.getName());
            waterSourceMapper.undelete(water.getId(), now);
            waterSourceMapper.restoreName(water.getId(), name, now);
            waterDeletedMapper.release(stationId, water.getWaterNo(), null);
            restored++;
        }
        if (restored > 0) {
            log.info("water retained restored stationId={} count={}", stationId, restored);
        }
        return restored;
    }

    /**
     * 删除水源并写删除记录。删除记录先写、软删后执行，任何同步/导入路径都按编号拦截回流。
     *
     * @param water      水源
     * @param operatorId 操作人账号主键
     * @param source     删除来源
     */
    void softDeleteWithTombstone(WaterSourceDO water, Long operatorId, String source) {
        LocalDateTime now = LocalDateTime.now();
        waterDeletedMapper.insertOrRevive(WaterDeletedDO.builder()
                .stationId(water.getStationId())
                .waterNo(water.getWaterNo() == null ? legacyNo(water) : water.getWaterNo())
                .name(water.getName())
                .type(water.getType())
                .source(source)
                .createdBy(operatorId)
                .build());
        fileObjectService.deleteByBiz(FileBizTypeConstant.WATER_PHOTO, water.getId());
        fileObjectService.deleteByBiz(FileBizTypeConstant.WATER_DOC, water.getId());
        fileObjectService.deleteByBiz(FileBizTypeConstant.WATER_VIDEO, water.getId());
        waterSourceMapper.softDelete(water.getId(), now, now);
        log.info("water deleted stationId={} waterId={} source={}",
                water.getStationId(), water.getId(), source);
    }

    /**
     * 生成站内下一个水源编号。编号不复用：取已分配最大值 +1，避免删卡后编号回收。
     *
     * @param stationId 站主键
     * @return 水源编号
     */
    String nextWaterNo(Long stationId) {
        String max = waterSourceMapper.selectMaxWaterNo(stationId);
        int seq = 1;
        if (max != null) {
            int dash = max.lastIndexOf('-');
            if (dash > 0) {
                String tail = max.substring(dash + 1);
                try {
                    seq = Integer.parseInt(tail) + 1;
                } catch (NumberFormatException ex) {
                    Integer count = waterSourceMapper.countAllByStation(stationId);
                    seq = (count == null ? 0 : count.intValue()) + 1;
                }
            }
        }
        return buildNo(stationId, seq);
    }

    private String buildNo(Long stationId, int seq) {
        return String.format("S%d-%04d", stationId, Integer.valueOf(seq));
    }

    private String legacyNo(WaterSourceDO water) {
        return buildNo(water.getStationId(), water.getId() == null ? 1 : water.getId().intValue());
    }

    private static String stripDeletedSuffix(String name) {
        if (name == null) {
            return null;
        }
        int idx = name.indexOf("#DEL#");
        return idx > 0 ? name.substring(0, idx) : name;
    }

    /**
     * 把命令写到实体上，并派生状态、原因码与部件码。
     *
     * <p>状态由「可使用 / 是否报修」派生，前端不再传矛盾状态；原因与部件只在不好用时归类，
     * 好用卡强制清空，避免备注里的旧原因被算进统计。</p>
     *
     * @param water     目标实体
     * @param stationId 站主键
     * @param command   命令
     * @param now       当前时间
     * @return 实体
     */
    WaterSourceDO applyCommand(WaterSourceDO water, Long stationId, WaterSaveCommand command,
                               LocalDateTime now) {
        String usable = command.getUsable();
        if (usable == null || usable.isBlank()) {
            usable = WaterUsableEnum.GOOD.getCode();
        }
        Integer repairFlag = WaterStatusResolver.normalizeRepairFlag(command.getIsRepair());
        String status = WaterStatusResolver.deriveStatus(usable, repairFlag);
        boolean bad = WaterStatusResolver.isBad(status, usable, repairFlag);

        water.setStationId(stationId);
        water.setName(command.getName().trim());
        water.setType(command.getType());
        water.setStatus(status);
        water.setAddress(trimToNull(command.getAddress()));
        water.setLng(command.getLng());
        water.setLat(command.getLat());
        water.setExtraJson(trimToNull(command.getNotes()));
        water.setSetup(command.getSetup());
        water.setPipeDiameter(clampInt(command.getPipeDiameter(), WaterRuleConstant.PIPE_DIAMETER_MAX));
        water.setPressureMpa(clampDecimal(command.getPressureMpa(), WaterRuleConstant.PRESSURE_MAX_MPA));
        water.setFlowLps(clampDecimal(command.getFlowLps(), WaterRuleConstant.FLOW_MAX_LPS));
        water.setSupplyForm(command.getSupplyForm());
        water.setAddHeightM(clampDecimal(command.getAddHeightM(), WaterRuleConstant.ADD_HEIGHT_MAX_M));
        water.setUsable(usable);
        water.setIsRepair(repairFlag);
        water.setRepairReason(trimToNull(command.getRepairReason()));
        water.setDrainViewable(command.getDrainViewable());
        water.setDrainUsable(command.getDrainUsable());
        water.setDrainReason(trimToNull(command.getDrainReason()));
        water.setDrainNotes(trimToNull(command.getDrainNotes()));
        water.setWarmthMeasure(command.getWarmthMeasure() == null
                ? null : Integer.valueOf(command.getWarmthMeasure().booleanValue() ? 1 : 0));
        water.setWarmthReason(trimToNull(command.getWarmthReason()));
        if (command.getRetained() != null) {
            water.setRetained(Integer.valueOf(command.getRetained().booleanValue() ? 1 : 0));
        } else if (water.getRetained() == null) {
            water.setRetained(Integer.valueOf(0));
        }
        if (bad) {
            water.setReasonText(trimToNull(command.getReasonText()));
            water.setReasonCode(WaterReasonClassifier.classifyReasonCode(water.getReasonText()));
            water.setPartCode(WaterReasonClassifier.classifyPartCode(water.getReasonText()));
        } else {
            // 好用卡不得保留原因与部件，否则旧原因会被算进不好用统计
            water.setReasonText(null);
            water.setReasonCode(null);
            water.setPartCode(null);
            water.setRepairReason(null);
        }
        water.setGmtModified(now);
        return water;
    }

    /**
     * 站级不得直写正式目录，只能提交审批申请。
     *
     * @param account 账号
     */
    private void requireDirectWrite(AccountDO account) {
        if (!waterAccessService.isDirectWriter(account)) {
            throw new BizException(ErrorCodeConstant.WATER_REQUEST_REQUIRED, MSG_REQUEST_REQUIRED);
        }
    }

    private void markPending(List<WaterVO> waters, Long stationId) {
        if (waters.isEmpty()) {
            return;
        }
        List<WaterRequestDO> pending = waterRequestMapper.selectPendingByStation(stationId);
        if (pending.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<Long>();
        for (WaterRequestDO request : pending) {
            if (request.getWaterId() != null) {
                ids.add(request.getWaterId());
            }
        }
        for (WaterVO vo : waters) {
            if (vo.getId() != null && ids.contains(vo.getId())) {
                vo.setPendingApproval(Boolean.TRUE);
            }
        }
    }

    private StationDO requireStation(Long stationId) {
        StationDO station = stationMapper.selectById(stationId);
        if (station == null) {
            throw new BizException(ErrorCodeConstant.WATER_STATION_FORBIDDEN, MSG_STATION_MISSING);
        }
        return station;
    }

    private Map<Long, String> stationNameMap() {
        List<StationDO> stations = stationMapper.selectAll();
        Map<Long, String> names = new HashMap<Long, String>();
        for (StationDO station : stations) {
            names.put(station.getId(), station.getName());
        }
        return names;
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

    private WaterVO attachFiles(WaterVO water) {
        if (water == null || water.getId() == null) {
            return water;
        }
        water.setPhotos(fileObjectService.listByBizWithPreview(
                FileBizTypeConstant.WATER_PHOTO, water.getId()));
        water.setDocs(fileObjectService.listByBizWithPreview(
                FileBizTypeConstant.WATER_DOC, water.getId()));
        return water;
    }

    private WaterSourceDO requireWater(Long stationId, Long waterId) {
        WaterSourceDO water = waterSourceMapper.selectAliveById(waterId);
        if (water == null || !stationId.equals(water.getStationId())) {
            throw new BizException(ErrorCodeConstant.WATER_NOT_FOUND, MSG_WATER_MISSING);
        }
        return water;
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

    private static Integer clampInt(Integer value, int max) {
        if (value == null || value.intValue() < 0) {
            return null;
        }
        return Integer.valueOf(Math.min(value.intValue(), max));
    }

    private static BigDecimal clampDecimal(BigDecimal value, double max) {
        if (value == null || value.signum() < 0) {
            return null;
        }
        if (value.doubleValue() > max) {
            return BigDecimal.valueOf(max);
        }
        return value;
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
