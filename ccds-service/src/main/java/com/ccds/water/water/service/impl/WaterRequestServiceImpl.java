package com.ccds.water.water.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.org.org.service.OrgQueryService;
import com.ccds.water.water.constant.WaterRuleConstant;
import com.ccds.water.water.dto.WaterRequestCommand;
import com.ccds.water.water.dto.WaterRequestReviewCommand;
import com.ccds.water.water.dto.WaterSaveCommand;
import com.ccds.water.water.entity.WaterRequestDO;
import com.ccds.water.water.entity.WaterSourceDO;
import com.ccds.water.water.mapper.WaterDeletedMapper;
import com.ccds.water.water.mapper.WaterRequestMapper;
import com.ccds.water.water.mapper.WaterSourceMapper;
import com.ccds.water.water.service.WaterAccessService;
import com.ccds.water.water.service.WaterRequestService;
import com.ccds.water.water.vo.WaterRequestVO;
import com.ccds.water.water.vo.WaterVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 水源变更审批实现。
 *
 * <p>审批落档必须单事务 + CAS：{@code reviewCas} 影响 0 行说明申请已被别人处理过。
 * 站级在审批通过前只看到「待审批」标记，统计仍按正式卡片算。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaterRequestServiceImpl implements WaterRequestService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String MSG_UNAUTHORIZED = "登录已失效，请重新登录";

    private static final String MSG_REQUEST_MISSING = "水源申请不存在";

    private static final String MSG_REQUEST_PROCESSED = "该申请已处理";

    private static final String MSG_ITEM_REQUIRED = "申请内容缺少水源信息";

    private static final String MSG_WATER_MISSING = "水源档案不存在";

    private static final String MSG_JSON = "申请内容序列化失败";

    private static final String ACTION_ADD = "add";

    private static final String ACTION_MODIFY = "modify";

    private static final String ACTION_DELETE = "delete";

    private static final String STATUS_PENDING = "pending";

    private static final String STATUS_APPROVED = "approved";

    private static final String STATUS_REJECTED = "rejected";

    private final AccountMapper accountMapper;

    private final WaterSourceMapper waterSourceMapper;

    private final WaterRequestMapper waterRequestMapper;

    private final WaterDeletedMapper waterDeletedMapper;

    private final WaterAccessService waterAccessService;

    private final StationMapper stationMapper;

    private final OrgQueryService orgQueryService;

    private final WaterViewTranslator waterViewTranslator;

    private final WaterServiceImpl waterService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaterRequestVO submit(AuthPrincipal principal, Long stationId, WaterRequestCommand command) {
        AccountDO account = requireAccount(principal);
        waterAccessService.requireRequestStation(account, stationId);
        String action = command.getAction();
        if (command.getClientRequestId() != null && !command.getClientRequestId().isBlank()) {
            WaterRequestDO exist = waterRequestMapper.selectByClientRequestId(
                    stationId, command.getClientRequestId().trim());
            if (exist != null) {
                // 提交幂等：同一客户端请求号直接返回原申请
                return toVo(exist, stationNameOf(stationId));
            }
        }
        WaterSourceDO before = null;
        WaterSaveCommand item = command.getItem();
        if (ACTION_DELETE.equals(action) || ACTION_MODIFY.equals(action)) {
            if (command.getWaterId() == null) {
                throw new BizException(ErrorCodeConstant.WATER_REQUEST_INVALID, MSG_ITEM_REQUIRED);
            }
            before = waterSourceMapper.selectAliveById(command.getWaterId());
            if (before == null || !stationId.equals(before.getStationId())) {
                throw new BizException(ErrorCodeConstant.WATER_NOT_FOUND, MSG_WATER_MISSING);
            }
        }
        if (!ACTION_DELETE.equals(action) && item == null) {
            throw new BizException(ErrorCodeConstant.WATER_REQUEST_INVALID, MSG_ITEM_REQUIRED);
        }
        String fromStatus = before == null ? null : before.getStatus();
        String toStatus = ACTION_DELETE.equals(action) ? null : deriveStatus(item);
        WaterRequestDO request = WaterRequestDO.builder()
                .stationId(stationId)
                .action(action)
                .waterId(command.getWaterId())
                .waterNo(before == null ? null : before.getWaterNo())
                .itemJson(item == null ? "{}" : writeJson(item))
                .beforeJson(before == null ? null : writeJson(toCommand(before)))
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(command.getReason().trim())
                .status(STATUS_PENDING)
                .createdBy(account.getId())
                .clientRequestId(trimToNull(command.getClientRequestId()))
                .build();
        waterRequestMapper.insert(request);
        // 审批表日志只记 id 与 action，不记坐标
        log.info("water request submitted stationId={} requestId={} action={}",
                stationId, request.getId(), action);
        return toVo(request, stationNameOf(stationId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WaterRequestVO> list(AuthPrincipal principal, Long stationId, String status) {
        AccountDO account = requireAccount(principal);
        List<Long> stationIds = new ArrayList<Long>();
        if (stationId != null) {
            waterAccessService.requireVisibleStation(account, stationId);
            stationIds.add(stationId);
        } else {
            for (StationDO station : orgQueryService.listVisibleStationEntities(account)) {
                stationIds.add(station.getId());
            }
        }
        if (stationIds.isEmpty()) {
            return new ArrayList<WaterRequestVO>();
        }
        Map<Long, String> names = stationNameMap();
        List<WaterRequestVO> out = new ArrayList<WaterRequestVO>();
        for (WaterRequestDO request : waterRequestMapper.selectByStations(stationIds, trimToNull(status))) {
            out.add(toVo(request, names.get(request.getStationId())));
        }
        return out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaterRequestVO review(AuthPrincipal principal, Long stationId, Long requestId,
                                 WaterRequestReviewCommand command) {
        AccountDO account = requireAccount(principal);
        waterAccessService.requireVisibleStation(account, stationId);
        waterAccessService.requireReviewer(account);
        WaterRequestDO request = waterRequestMapper.selectById(requestId);
        if (request == null || !stationId.equals(request.getStationId())) {
            throw new BizException(ErrorCodeConstant.WATER_REQUEST_INVALID, MSG_REQUEST_MISSING);
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new BizException(ErrorCodeConstant.WATER_REQUEST_INVALID, MSG_REQUEST_PROCESSED);
        }
        boolean approve = Boolean.TRUE.equals(command.getApprove());
        LocalDateTime now = LocalDateTime.now();
        int affected = waterRequestMapper.reviewCas(requestId, STATUS_PENDING,
                approve ? STATUS_APPROVED : STATUS_REJECTED,
                account.getId(), account.getUsername(), trimToNull(command.getReviewReason()), now);
        if (affected == 0) {
            throw new BizException(ErrorCodeConstant.WATER_REQUEST_INVALID, MSG_REQUEST_PROCESSED);
        }
        if (approve) {
            applyApproved(request, account.getId());
        }
        request.setStatus(approve ? STATUS_APPROVED : STATUS_REJECTED);
        request.setReviewerName(account.getUsername());
        request.setReviewedAt(now);
        log.info("water request reviewed requestId={} approve={} action={}",
                requestId, Boolean.valueOf(approve), request.getAction());
        return toVo(request, stationNameOf(stationId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(AuthPrincipal principal, Long stationId, Long requestId) {
        AccountDO account = requireAccount(principal);
        waterAccessService.requireRequestStation(account, stationId);
        WaterRequestDO request = waterRequestMapper.selectById(requestId);
        if (request == null || !stationId.equals(request.getStationId())) {
            throw new BizException(ErrorCodeConstant.WATER_REQUEST_INVALID, MSG_REQUEST_MISSING);
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new BizException(ErrorCodeConstant.WATER_REQUEST_INVALID, MSG_REQUEST_PROCESSED);
        }
        waterRequestMapper.deletePending(requestId);
        log.info("water request withdrawn requestId={}", requestId);
    }

    /**
     * 审批通过落档：新增分配编号后写入，修改覆盖原卡，删除写删除记录后软删。
     *
     * @param request    申请
     * @param operatorId 审批人账号主键
     */
    private void applyApproved(WaterRequestDO request, Long operatorId) {
        Long stationId = request.getStationId();
        if (ACTION_DELETE.equals(request.getAction())) {
            WaterSourceDO water = waterSourceMapper.selectAliveById(request.getWaterId());
            if (water != null) {
                waterService.softDeleteWithTombstone(water, operatorId,
                        WaterServiceImpl.SOURCE_APPROVAL);
            }
            return;
        }
        WaterSaveCommand item = readJson(request.getItemJson());
        if (item == null) {
            throw new BizException(ErrorCodeConstant.WATER_REQUEST_INVALID, MSG_ITEM_REQUIRED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (ACTION_MODIFY.equals(request.getAction())) {
            WaterSourceDO water = waterSourceMapper.selectAliveById(request.getWaterId());
            if (water == null) {
                throw new BizException(ErrorCodeConstant.WATER_NOT_FOUND, MSG_WATER_MISSING);
            }
            waterService.applyCommand(water, stationId, item, now);
            waterSourceMapper.update(water);
            return;
        }
        WaterSourceDO water = waterService.applyCommand(new WaterSourceDO(), stationId, item, now);
        water.setWaterNo(waterService.nextWaterNo(stationId));
        water.setGmtCreate(now);
        water.setGmtModified(now);
        waterSourceMapper.insert(water);
        // 审批通过新增：解除该编号的删除记录
        waterDeletedMapper.release(stationId, water.getWaterNo(), operatorId);
    }

    private String deriveStatus(WaterSaveCommand item) {
        if (item == null) {
            return null;
        }
        String usable = item.getUsable() == null || item.getUsable().isBlank()
                ? "good" : item.getUsable();
        Integer repairFlag = WaterStatusResolver.normalizeRepairFlag(item.getIsRepair());
        return WaterStatusResolver.deriveStatus(usable, repairFlag);
    }

    private WaterSaveCommand toCommand(WaterSourceDO water) {
        return WaterSaveCommand.builder()
                .name(water.getName())
                .type(water.getType())
                .address(water.getAddress())
                .lng(water.getLng())
                .lat(water.getLat())
                .status(water.getStatus())
                .notes(water.getExtraJson())
                .setup(water.getSetup())
                .pipeDiameter(water.getPipeDiameter())
                .pressureMpa(water.getPressureMpa())
                .flowLps(water.getFlowLps())
                .supplyForm(water.getSupplyForm())
                .addHeightM(water.getAddHeightM())
                .usable(water.getUsable())
                .reasonText(water.getReasonText())
                .isRepair(Boolean.valueOf(water.getIsRepair() != null && water.getIsRepair().intValue() == 1))
                .repairReason(water.getRepairReason())
                .retained(Boolean.valueOf(water.getRetained() != null && water.getRetained().intValue() == 1))
                .drainViewable(water.getDrainViewable())
                .drainUsable(water.getDrainUsable())
                .drainReason(water.getDrainReason())
                .drainNotes(water.getDrainNotes())
                .warmthMeasure(water.getWarmthMeasure() == null
                        ? null : Boolean.valueOf(water.getWarmthMeasure().intValue() == 1))
                .warmthReason(water.getWarmthReason())
                .build();
    }

    private WaterRequestVO toVo(WaterRequestDO request, String stationName) {
        WaterSaveCommand after = ACTION_DELETE.equals(request.getAction())
                ? null : readJson(request.getItemJson());
        WaterSaveCommand before = request.getBeforeJson() == null ? null : readJson(request.getBeforeJson());
        return WaterRequestVO.builder()
                .id(request.getId())
                .stationId(request.getStationId())
                .stationName(stationName)
                .action(request.getAction())
                .waterId(request.getWaterId())
                .waterNo(request.getWaterNo())
                .before(before == null ? null : toBriefVo(before, request.getWaterId(), request.getWaterNo()))
                .after(after == null ? null : toBriefVo(after, request.getWaterId(), request.getWaterNo()))
                .fromStatus(request.getFromStatus())
                .toStatus(request.getToStatus())
                .reason(request.getReason())
                .status(request.getStatus())
                .reviewReason(request.getReviewReason())
                .reviewerName(request.getReviewerName())
                .reviewedAt(request.getReviewedAt())
                .gmtCreate(request.getGmtCreate())
                .build();
    }

    private WaterVO toBriefVo(WaterSaveCommand command, Long waterId, String waterNo) {
        return WaterVO.builder()
                .id(waterId)
                .waterNo(waterNo)
                .name(command.getName())
                .type(command.getType())
                .address(command.getAddress())
                .lng(command.getLng())
                .lat(command.getLat())
                .status(command.getStatus())
                .notes(command.getNotes())
                .setup(command.getSetup())
                .pipeDiameter(command.getPipeDiameter())
                .pressureMpa(command.getPressureMpa())
                .flowLps(command.getFlowLps())
                .supplyForm(command.getSupplyForm())
                .addHeightM(command.getAddHeightM())
                .usable(command.getUsable())
                .reasonText(command.getReasonText())
                .build();
    }

    private String writeJson(WaterSaveCommand command) {
        try {
            return MAPPER.writeValueAsString(command);
        } catch (JsonProcessingException ex) {
            log.error("water request serialize failed", ex);
            throw new BizException(ErrorCodeConstant.WATER_REQUEST_INVALID, MSG_JSON);
        }
    }

    private WaterSaveCommand readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, WaterSaveCommand.class);
        } catch (JsonProcessingException ex) {
            log.warn("water request parse failed length={}", Integer.valueOf(json.length()), ex);
            return null;
        }
    }

    private Map<Long, String> stationNameMap() {
        Map<Long, String> names = new LinkedHashMap<Long, String>();
        for (StationDO station : stationMapper.selectAll()) {
            names.put(station.getId(), station.getName());
        }
        return names;
    }

    private String stationNameOf(Long stationId) {
        StationDO station = stationMapper.selectById(stationId);
        return station == null ? null : station.getName();
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

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
