package com.ccds.duty.controller;

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
import com.ccds.duty.dto.KeyUnitDTO;
import com.ccds.duty.service.KeyUnitService;
import com.ccds.iam.identity.model.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 重点单位预案接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class KeyUnitController {

    private final KeyUnitService keyUnitService;

    /**
     * 本站重点单位列表。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param category  类别，可空
     * @param keyword   名称前缀，可空
     * @return 列表
     */
    @GetMapping(ApiPathConstant.STATION_KEY_UNITS)
    public ApiResultVO<List<KeyUnitDTO>> list(HttpServletRequest request,
                                              @PathVariable Long stationId,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) String keyword) {
        return ApiResultVO.ok(keyUnitService.list(current(request), stationId, category, keyword));
    }

    /**
     * 详情。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param id        重点单位主键
     * @return 重点单位
     */
    @GetMapping(ApiPathConstant.STATION_KEY_UNIT)
    public ApiResultVO<KeyUnitDTO> getById(HttpServletRequest request,
                                           @PathVariable Long stationId,
                                           @PathVariable Long id) {
        return ApiResultVO.ok(keyUnitService.getById(current(request), stationId, id));
    }

    /**
     * 创建。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param dto       入参
     * @return 创建结果
     */
    @PostMapping(ApiPathConstant.STATION_KEY_UNITS)
    public ApiResultVO<KeyUnitDTO> create(HttpServletRequest request,
                                          @PathVariable Long stationId,
                                          @Valid @RequestBody KeyUnitDTO dto) {
        return ApiResultVO.ok(keyUnitService.create(current(request), stationId, dto));
    }

    /**
     * 更新。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param id        重点单位主键
     * @param dto       入参
     * @return 更新结果
     */
    @PutMapping(ApiPathConstant.STATION_KEY_UNIT)
    public ApiResultVO<KeyUnitDTO> update(HttpServletRequest request,
                                          @PathVariable Long stationId,
                                          @PathVariable Long id,
                                          @Valid @RequestBody KeyUnitDTO dto) {
        return ApiResultVO.ok(keyUnitService.update(current(request), stationId, id, dto));
    }

    /**
     * 软删除。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param id        重点单位主键
     * @return 空成功
     */
    @DeleteMapping(ApiPathConstant.STATION_KEY_UNIT)
    public ApiResultVO<Void> delete(HttpServletRequest request,
                                    @PathVariable Long stationId,
                                    @PathVariable Long id) {
        keyUnitService.delete(current(request), stationId, id);
        return ApiResultVO.ok(null);
    }

    private AuthPrincipal current(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
    }
}
