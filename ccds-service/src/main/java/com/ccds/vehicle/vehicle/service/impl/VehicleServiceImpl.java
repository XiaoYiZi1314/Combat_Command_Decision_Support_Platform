package com.ccds.vehicle.vehicle.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.enums.AccountRoleEnum;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.iam.identity.service.AuthGuardService;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;
import com.ccds.vehicle.vehicle.constant.VehicleRuleConstant;
import com.ccds.vehicle.vehicle.dto.VehicleDTO;
import com.ccds.vehicle.vehicle.dto.VehicleRequestDTO;
import com.ccds.vehicle.vehicle.dto.VehicleRequestReviewDTO;
import com.ccds.vehicle.vehicle.entity.VehicleDO;
import com.ccds.vehicle.vehicle.entity.VehicleRequestDO;
import com.ccds.vehicle.vehicle.mapper.VehicleMapper;
import com.ccds.vehicle.vehicle.mapper.VehicleRequestMapper;
import com.ccds.vehicle.vehicle.service.VehicleAccessService;
import com.ccds.vehicle.vehicle.service.VehicleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 车辆档案实现：站级只读并提交申请，支队/开发直接落档并审批。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String MSG_ACTION_INVALID = "变更动作须为 add/modify/delete";

    private static final String MSG_REQUEST_PROCESSED = "该申请已处理";

    private static final String MSG_ITEM_REQUIRED = "申请内容缺少车辆信息";

    private static final String MSG_JSON = "申请内容序列化失败";

    private final VehicleMapper vehicleMapper;

    private final VehicleRequestMapper vehicleRequestMapper;

    private final VehicleAccessService vehicleAccessService;

    private final StationMapper stationMapper;

    private final AuthGuardService authGuardService;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VehicleDTO> list(AuthPrincipal principal, Long stationId, String type, String status,
                                  String keyword) {
        AccountDO account = requireAccount(principal);
        vehicleAccessService.requireVisibleStation(account, stationId);
        return filter(vehicleMapper.selectByStation(stationId), type, status, keyword,
                stationNameOf(stationId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VehicleDTO> listAll(AuthPrincipal principal, Long stationId, String type, String status,
                                     String keyword) {
        AccountDO account = requireAccount(principal);
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        if (role != AccountRoleEnum.HQ && role != AccountRoleEnum.DEVELOPER && role != AccountRoleEnum.BRIGADE) {
            throw new BizException(ErrorCodeConstant.VEHICLE_STATION_FORBIDDEN,
                    VehicleRuleConstant.MSG_FORBIDDEN);
        }
        List<Long> stationIds = stationId != null
                ? java.util.Collections.singletonList(stationId)
                : vehicleAccessService.visibleStationIds(account);
        if (stationIds.isEmpty()) {
            return new ArrayList<>();
        }
        vehicleAccessService.requireVisibleStation(account, stationIds.get(0));
        Map<Long, String> names = stationNameMap(stationIds);
        return filter(vehicleMapper.selectByStations(stationIds), type, status, keyword, names);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VehicleDTO getById(AuthPrincipal principal, Long stationId, Long vehicleId) {
        AccountDO account = requireAccount(principal);
        vehicleAccessService.requireVisibleStation(account, stationId);
        VehicleDO vehicle = requireVehicle(stationId, vehicleId);
        return toDTO(vehicle, stationNameOf(stationId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehicleDTO save(AuthPrincipal principal, Long stationId, VehicleDTO dto) {
        AccountDO account = requireAccount(principal);
        vehicleAccessService.requireDirectWriteStation(account, stationId);
        normalize(dto);
        if (dto.getPlate() != null && !dto.getPlate().isEmpty()) {
            List<VehicleDO> same = vehicleMapper.selectByStationAndPlate(stationId, dto.getPlate(),
                    dto.getId());
            if (!same.isEmpty()) {
                throw new BizException(ErrorCodeConstant.VEHICLE_INVALID, VehicleRuleConstant.MSG_PLATE_DUPLICATE);
            }
        }
        VehicleDO entity = toEntity(dto, stationId);
        if (entity.getId() == null) {
            vehicleMapper.insert(entity);
        } else {
            requireVehicle(stationId, entity.getId());
            vehicleMapper.updateById(entity);
        }
        log.info("车辆落档：stationId={}, vehicleId={}, plate={}", stationId, entity.getId(), entity.getPlate());
        return toDTO(requireVehicle(stationId, entity.getId()), stationNameOf(stationId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(AuthPrincipal principal, Long stationId, Long vehicleId) {
        AccountDO account = requireAccount(principal);
        vehicleAccessService.requireDirectWriteStation(account, stationId);
        requireVehicle(stationId, vehicleId);
        vehicleMapper.deleteById(vehicleId);
        log.info("车辆删除：stationId={}, vehicleId={}", stationId, vehicleId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehicleRequestDTO submitRequest(AuthPrincipal principal, Long stationId, VehicleRequestDTO dto) {
        AccountDO account = requireAccount(principal);
        vehicleAccessService.requireRequestStation(account, stationId);
        String action = dto == null ? null : dto.getAction();
        if (!VehicleRuleConstant.ACTION_ADD.equals(action)
                && !VehicleRuleConstant.ACTION_MODIFY.equals(action)
                && !VehicleRuleConstant.ACTION_DELETE.equals(action)) {
            throw new BizException(ErrorCodeConstant.VEHICLE_REQUEST_INVALID, MSG_ACTION_INVALID);
        }
        VehicleDTO item = dto.getItem();
        if (item == null) {
            throw new BizException(ErrorCodeConstant.VEHICLE_REQUEST_INVALID, MSG_ITEM_REQUIRED);
        }
        normalize(item);
        if (VehicleRuleConstant.ACTION_MODIFY.equals(action) && item.getId() == null) {
            throw new BizException(ErrorCodeConstant.VEHICLE_REQUEST_INVALID, VehicleRuleConstant.MSG_NOT_FOUND);
        }
        VehicleDO before = null;
        if (dto.getBefore() != null && dto.getBefore().getId() != null) {
            before = vehicleMapper.selectById(dto.getBefore().getId());
        }
        VehicleRequestDO request = VehicleRequestDO.builder()
                .stationId(stationId)
                .action(action)
                .itemJson(writeJson(item))
                .beforeJson(before != null ? writeJson(toDTO(before, null)) : null)
                .status(VehicleRuleConstant.REQUEST_PENDING)
                .createdBy(account.getId())
                .build();
        vehicleRequestMapper.insert(request);
        log.info("车辆变更申请提交：stationId={}, requestId={}, action={}", stationId, request.getId(), action);
        return toRequestDTO(request, stationNameOf(stationId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VehicleRequestDTO> listRequests(AuthPrincipal principal, Long stationId) {
        AccountDO account = requireAccount(principal);
        List<Long> stationIds;
        if (stationId != null) {
            vehicleAccessService.requireVisibleStation(account, stationId);
            stationIds = java.util.Collections.singletonList(stationId);
        } else {
            stationIds = vehicleAccessService.visibleStationIds(account);
        }
        if (stationIds.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, String> names = stationNameMap(stationIds);
        return vehicleRequestMapper.selectByStations(stationIds).stream()
                .map(request -> toRequestDTO(request, names.get(request.getStationId())))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehicleRequestDTO reviewRequest(AuthPrincipal principal, Long stationId, Long requestId,
                                            VehicleRequestReviewDTO review) {
        AccountDO account = requireAccount(principal);
        vehicleAccessService.requireDirectWriteStation(account, stationId);
        vehicleAccessService.requireReviewer(account);
        VehicleRequestDO request = vehicleRequestMapper.selectById(requestId);
        if (request == null || !stationId.equals(request.getStationId())) {
            throw new BizException(ErrorCodeConstant.VEHICLE_REQUEST_INVALID,
                    VehicleRuleConstant.MSG_NOT_FOUND);
        }
        if (!VehicleRuleConstant.REQUEST_PENDING.equals(request.getStatus())) {
            throw new BizException(ErrorCodeConstant.VEHICLE_REQUEST_INVALID, MSG_REQUEST_PROCESSED);
        }
        boolean approve = review != null && Boolean.TRUE.equals(review.getApprove());
        if (approve) {
            applyApprovedRequest(request);
        }
        request.setStatus(approve
                ? VehicleRuleConstant.REQUEST_APPROVED
                : VehicleRuleConstant.REQUEST_REJECTED);
        request.setReviewedBy(account.getId());
        request.setReviewerName(account.getUsername());
        vehicleRequestMapper.updateById(request);
        log.info("车辆变更审批：requestId={}, approve={}", requestId, approve);
        return toRequestDTO(request, stationNameOf(stationId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRequest(AuthPrincipal principal, Long stationId, Long requestId) {
        AccountDO account = requireAccount(principal);
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        boolean reviewer = role == AccountRoleEnum.HQ || role == AccountRoleEnum.DEVELOPER;
        if (reviewer) {
            vehicleAccessService.requireDirectWriteStation(account, stationId);
        } else {
            vehicleAccessService.requireRequestStation(account, stationId);
        }
        VehicleRequestDO request = vehicleRequestMapper.selectById(requestId);
        if (request == null || !stationId.equals(request.getStationId())) {
            return;
        }
        vehicleRequestMapper.deleteById(requestId);
        log.info("车辆变更申请删除：requestId={}", requestId);
    }

    private void applyApprovedRequest(VehicleRequestDO request) {
        VehicleDTO item = readJson(request.getItemJson());
        if (item == null) {
            throw new BizException(ErrorCodeConstant.VEHICLE_REQUEST_INVALID, MSG_ITEM_REQUIRED);
        }
        normalize(item);
        Long stationId = request.getStationId();
        if (VehicleRuleConstant.ACTION_DELETE.equals(request.getAction())) {
            if (item.getId() != null) {
                vehicleMapper.deleteById(item.getId());
            }
            return;
        }
        if (item.getPlate() != null && !item.getPlate().isEmpty()) {
            List<VehicleDO> same = vehicleMapper.selectByStationAndPlate(stationId, item.getPlate(), item.getId());
            if (!same.isEmpty()) {
                throw new BizException(ErrorCodeConstant.VEHICLE_INVALID, VehicleRuleConstant.MSG_PLATE_DUPLICATE);
            }
        }
        VehicleDO entity = toEntity(item, stationId);
        if (VehicleRuleConstant.ACTION_MODIFY.equals(request.getAction()) && entity.getId() != null) {
            vehicleMapper.updateById(entity);
        } else {
            entity.setId(null);
            vehicleMapper.insert(entity);
        }
    }

    private AccountDO requireAccount(AuthPrincipal principal) {
        return authGuardService.requireAccount(principal);
    }

    private VehicleDO requireVehicle(Long stationId, Long vehicleId) {
        VehicleDO vehicle = vehicleId == null ? null : vehicleMapper.selectById(vehicleId);
        if (vehicle == null || !stationId.equals(vehicle.getStationId())) {
            throw new BizException(ErrorCodeConstant.VEHICLE_NOT_FOUND, VehicleRuleConstant.MSG_NOT_FOUND);
        }
        return vehicle;
    }

    private List<VehicleDTO> filter(List<VehicleDO> source, String type, String status, String keyword,
                                     Map<Long, String> stationNames) {
        return source.stream()
                .filter(v -> type == null || type.isEmpty() || type.equals(v.getType()))
                .filter(v -> status == null || status.isEmpty() || status.equals(v.getStatus()))
                .filter(v -> matchesKeyword(v, keyword))
                .map(v -> toDTO(v, stationNames.get(v.getStationId())))
                .collect(Collectors.toList());
    }

    private List<VehicleDTO> filter(List<VehicleDO> source, String type, String status, String keyword,
                                     String stationName) {
        return source.stream()
                .filter(v -> type == null || type.isEmpty() || type.equals(v.getType()))
                .filter(v -> status == null || status.isEmpty() || status.equals(v.getStatus()))
                .filter(v -> matchesKeyword(v, keyword))
                .map(v -> toDTO(v, stationName))
                .collect(Collectors.toList());
    }

    private boolean matchesKeyword(VehicleDO v, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        return joinSearchable(v).contains(kw);
    }

    private String joinSearchable(VehicleDO v) {
        return String.join(" ",
                nz(v.getVehicleType()), nz(v.getPlate()), nz(v.getOldPlate()),
                nz(v.getModel()), nz(v.getMaker()), nz(v.getVin()),
                nz(v.getEngineNo()), nz(v.getNotes())).toLowerCase(Locale.ROOT);
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }

    private void normalize(VehicleDTO dto) {
        if (dto.getStatus() == null || dto.getStatus().isEmpty()) {
            dto.setStatus(VehicleRuleConstant.STATUS_ON_DUTY);
        }
        if (dto.getType() == null || dto.getType().isEmpty()) {
            dto.setType(VehicleRuleConstant.TYPE_OTHER);
            return;
        }
        if (!VehicleRuleConstant.VALID_TYPES.contains(dto.getType())) {
            throw new BizException(ErrorCodeConstant.VEHICLE_INVALID, VehicleRuleConstant.MSG_TYPE_INVALID);
        }
    }

    private VehicleDTO toDTO(VehicleDO entity, String stationName) {
        return VehicleDTO.builder()
                .id(entity.getId())
                .stationId(entity.getStationId())
                .type(entity.getType())
                .vehicleType(entity.getVehicleType())
                .plate(entity.getPlate())
                .oldPlate(entity.getOldPlate())
                .status(entity.getStatus())
                .model(entity.getModel())
                .maker(entity.getMaker())
                .waterCap(entity.getWaterCap())
                .foamCap(entity.getFoamCap())
                .powderCap(entity.getPowderCap())
                .workHeight(entity.getWorkHeight())
                .engineNo(entity.getEngineNo())
                .vin(entity.getVin())
                .color(entity.getColor())
                .madeDate(entity.getMadeDate())
                .equipDate(entity.getEquipDate())
                .notes(entity.getNotes())
                .stationName(stationName)
                .build();
    }

    private VehicleDO toEntity(VehicleDTO dto, Long stationId) {
        return VehicleDO.builder()
                .id(dto.getId())
                .stationId(stationId)
                .type(dto.getType())
                .vehicleType(dto.getVehicleType())
                .plate(dto.getPlate())
                .oldPlate(dto.getOldPlate())
                .status(dto.getStatus())
                .model(dto.getModel())
                .maker(dto.getMaker())
                .waterCap(dto.getWaterCap())
                .foamCap(dto.getFoamCap())
                .powderCap(dto.getPowderCap())
                .workHeight(dto.getWorkHeight())
                .engineNo(dto.getEngineNo())
                .vin(dto.getVin())
                .color(dto.getColor())
                .madeDate(dto.getMadeDate())
                .equipDate(dto.getEquipDate())
                .notes(dto.getNotes())
                .build();
    }

    private VehicleRequestDTO toRequestDTO(VehicleRequestDO request, String stationName) {
        return VehicleRequestDTO.builder()
                .id(request.getId())
                .stationId(request.getStationId())
                .action(request.getAction())
                .item(readJson(request.getItemJson()))
                .before(readJson(request.getBeforeJson()))
                .status(request.getStatus())
                .createdAt(request.getGmtCreate() == null ? null : request.getGmtCreate().toString())
                .reviewedAt(request.getReviewedAt() == null ? null : request.getReviewedAt().toString())
                .reviewer(request.getReviewerName())
                .stationName(stationName)
                .build();
    }

    private Map<Long, String> stationNameMap(List<Long> stationIds) {
        return stationMapper.selectAll().stream()
                .filter(station -> stationIds.contains(station.getId()))
                .collect(Collectors.toMap(StationDO::getId,
                        station -> station.getName() == null ? "" : station.getName()));
    }

    private String stationNameOf(Long stationId) {
        StationDO station = stationMapper.selectById(stationId);
        return station == null ? "" : station.getName();
    }

    private String writeJson(VehicleDTO dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCodeConstant.VEHICLE_REQUEST_INVALID, MSG_JSON);
        }
    }

    private VehicleDTO readJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, VehicleDTO.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}
