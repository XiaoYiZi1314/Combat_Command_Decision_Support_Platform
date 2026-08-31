package com.ccds.vehicle.vehicle.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.vehicle.vehicle.dto.VehicleDTO;
import com.ccds.vehicle.vehicle.dto.VehicleRequestDTO;
import com.ccds.vehicle.vehicle.dto.VehicleRequestReviewDTO;
import com.ccds.vehicle.vehicle.service.VehicleService;

import lombok.RequiredArgsConstructor;

/**
 * 车辆档案接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * 本站车辆列表。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param type      类型码，可空
     * @param status    状态，可空
     * @param keyword   关键字，可空
     * @return 列表
     */
    @GetMapping(ApiPathConstant.STATION_VEHICLES)
    public ApiResultVO<List<VehicleDTO>> list(HttpServletRequest request,
                                              @PathVariable Long stationId,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String keyword) {
        return ApiResultVO.ok(vehicleService.list(current(request), stationId, type, status, keyword));
    }

    /**
     * 指挥端全部车辆。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键，可空
     * @param type      类型码，可空
     * @param status    状态，可空
     * @param keyword   关键字，可空
     * @return 列表
     */
    @GetMapping(ApiPathConstant.VEHICLES_ALL)
    public ApiResultVO<List<VehicleDTO>> listAll(HttpServletRequest request,
                                                 @RequestParam(required = false) Long stationId,
                                                 @RequestParam(required = false) String type,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword) {
        return ApiResultVO.ok(vehicleService.listAll(current(request), stationId, type, status, keyword));
    }

    /**
     * 单车详情。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param vehicleId 车辆主键
     * @return 车辆
     */
    @GetMapping(ApiPathConstant.STATION_VEHICLE)
    public ApiResultVO<VehicleDTO> getById(HttpServletRequest request,
                                           @PathVariable Long stationId,
                                           @PathVariable Long vehicleId) {
        return ApiResultVO.ok(vehicleService.getById(current(request), stationId, vehicleId));
    }

    /**
     * 支队/开发直接落档（新增或修改）。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param dto       车辆内容
     * @return 保存结果
     */
    @PutMapping(ApiPathConstant.STATION_VEHICLE)
    public ApiResultVO<VehicleDTO> save(HttpServletRequest request,
                                        @PathVariable Long stationId,
                                        @Valid @RequestBody VehicleDTO dto) {
        return ApiResultVO.ok(vehicleService.save(current(request), stationId, dto));
    }

    /**
     * 新增车辆（落档路径同 PUT 单车，此处为无主键新增入口）。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param dto       车辆内容
     * @return 保存结果
     */
    @PostMapping(ApiPathConstant.STATION_VEHICLES)
    public ApiResultVO<VehicleDTO> create(HttpServletRequest request,
                                          @PathVariable Long stationId,
                                          @Valid @RequestBody VehicleDTO dto) {
        dto.setId(null);
        return ApiResultVO.ok(vehicleService.save(current(request), stationId, dto));
    }

    /**
     * 支队/开发直接删除。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param vehicleId 车辆主键
     * @return 空成功
     */
    @DeleteMapping(ApiPathConstant.STATION_VEHICLE)
    public ApiResultVO<Void> delete(HttpServletRequest request,
                                    @PathVariable Long stationId,
                                    @PathVariable Long vehicleId) {
        vehicleService.delete(current(request), stationId, vehicleId);
        return ApiResultVO.ok(null);
    }

    /**
     * 站级提交变更申请。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param dto       申请内容
     * @return 申请
     */
    @PostMapping(ApiPathConstant.STATION_VEHICLE_REQUESTS)
    public ApiResultVO<VehicleRequestDTO> submitRequest(HttpServletRequest request,
                                                        @PathVariable Long stationId,
                                                        @RequestBody VehicleRequestDTO dto) {
        return ApiResultVO.ok(vehicleService.submitRequest(current(request), stationId, dto));
    }

    /**
     * 变更申请列表。路径限定单站；指挥端可用 /vehicle-requests 查全部。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return 列表
     */
    @GetMapping(ApiPathConstant.STATION_VEHICLE_REQUESTS)
    public ApiResultVO<List<VehicleRequestDTO>> listRequests(HttpServletRequest request,
                                                             @PathVariable Long stationId) {
        return ApiResultVO.ok(vehicleService.listRequests(current(request), stationId));
    }

    /**
     * 全部变更申请（指挥端）。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键，可空
     * @return 列表
     */
    @GetMapping(ApiPathConstant.VEHICLE_REQUESTS_ALL)
    public ApiResultVO<List<VehicleRequestDTO>> listAllRequests(HttpServletRequest request,
                                                                @RequestParam(required = false) Long stationId) {
        return ApiResultVO.ok(vehicleService.listRequests(current(request), stationId));
    }

    /**
     * 审批变更申请。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param requestId 申请主键
     * @param review    审批结果
     * @return 更新后的申请
     */
    @PostMapping(ApiPathConstant.STATION_VEHICLE_REQUEST)
    public ApiResultVO<VehicleRequestDTO> reviewRequest(HttpServletRequest request,
                                                        @PathVariable Long stationId,
                                                        @PathVariable Long requestId,
                                                        @Valid @RequestBody VehicleRequestReviewDTO review) {
        return ApiResultVO.ok(vehicleService.reviewRequest(current(request), stationId, requestId, review));
    }

    /**
     * 删除变更申请记录。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param requestId 申请主键
     * @return 空成功
     */
    @DeleteMapping(ApiPathConstant.STATION_VEHICLE_REQUEST)
    public ApiResultVO<Void> deleteRequest(HttpServletRequest request,
                                           @PathVariable Long stationId,
                                           @PathVariable Long requestId) {
        vehicleService.deleteRequest(current(request), stationId, requestId);
        return ApiResultVO.ok(null);
    }

    private AuthPrincipal current(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
    }
}
