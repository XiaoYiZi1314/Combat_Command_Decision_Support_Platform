package com.ccds.water.water.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.water.water.constant.WaterRuleConstant;
import com.ccds.water.water.dto.WaterSaveCommand;
import com.ccds.water.water.service.WaterService;
import com.ccds.water.water.vo.StationWaterVO;
import com.ccds.water.water.vo.WaterImportResultVO;
import com.ccds.water.water.vo.WaterNearbyResultVO;
import com.ccds.water.water.vo.WaterVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 水源档案、表格导入导出与附近水源接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class WaterController {

    private static final String MSG_IMPORT_EMPTY = "请选择要导入的表格";

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final WaterService waterService;

    /**
     * 全市水源（登录可读，站级「全市」浏览用）。
     *
     * @param request HTTP 请求
     * @return 全市水源
     */
    @GetMapping(ApiPathConstant.WATERS_CITY)
    public ApiResultVO<List<WaterVO>> cityWaters(HttpServletRequest request) {
        return ApiResultVO.ok(waterService.listCityWaters(current(request)));
    }

    /**
     * 本站水源列表。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return 水源列表
     */
    @GetMapping(ApiPathConstant.STATION_WATERS)
    public ApiResultVO<StationWaterVO> waters(HttpServletRequest request, @PathVariable Long stationId) {
        return ApiResultVO.ok(waterService.getWaters(current(request), stationId));
    }

    /**
     * 水源详情。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param waterId   水源主键
     * @return 水源
     */
    @GetMapping(ApiPathConstant.STATION_WATER)
    public ApiResultVO<WaterVO> getWater(HttpServletRequest request,
                                         @PathVariable Long stationId,
                                         @PathVariable Long waterId) {
        return ApiResultVO.ok(waterService.getWater(current(request), stationId, waterId));
    }

    /**
     * 新建水源。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param command   水源
     * @return 新建结果
     */
    @PostMapping(ApiPathConstant.STATION_WATERS)
    public ApiResultVO<WaterVO> createWater(HttpServletRequest request,
                                            @PathVariable Long stationId,
                                            @Valid @RequestBody WaterSaveCommand command) {
        return ApiResultVO.ok(waterService.createWater(current(request), stationId, command));
    }

    /**
     * 更新水源。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param waterId   水源主键
     * @param command   水源
     * @return 更新结果
     */
    @PutMapping(ApiPathConstant.STATION_WATER)
    public ApiResultVO<WaterVO> updateWater(HttpServletRequest request,
                                            @PathVariable Long stationId,
                                            @PathVariable Long waterId,
                                            @Valid @RequestBody WaterSaveCommand command) {
        return ApiResultVO.ok(waterService.updateWater(current(request), stationId, waterId, command));
    }

    /**
     * 软删水源。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param waterId   水源主键
     * @return 空成功
     */
    @DeleteMapping(ApiPathConstant.STATION_WATER)
    public ApiResultVO<Void> deleteWater(HttpServletRequest request,
                                         @PathVariable Long stationId,
                                         @PathVariable Long waterId) {
        waterService.deleteWater(current(request), stationId, waterId);
        return ApiResultVO.ok(null);
    }

    /**
     * 导入现网检查记录表。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param file      表格
     * @return 导入结果
     */
    @PostMapping(value = ApiPathConstant.STATION_WATER_IMPORT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResultVO<WaterImportResultVO> importWaters(HttpServletRequest request,
                                                         @PathVariable Long stationId,
                                                         @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCodeConstant.WATER_IMPORT_INVALID, MSG_IMPORT_EMPTY);
        }
        try {
            return ApiResultVO.ok(waterService.importWaters(
                    current(request), stationId, file.getOriginalFilename(), file.getBytes()));
        } catch (IOException ex) {
            log.warn("water import read failed stationId={}", stationId, ex);
            throw new BizException(ErrorCodeConstant.WATER_IMPORT_INVALID, MSG_IMPORT_EMPTY);
        }
    }

    /**
     * 导出本站表格。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return xlsx
     */
    @GetMapping(ApiPathConstant.STATION_WATER_EXPORT)
    public ResponseEntity<byte[]> exportWaters(HttpServletRequest request, @PathVariable Long stationId) {
        return xlsx(WaterRuleConstant.EXPORT_FILE_PREFIX,
                waterService.exportWaters(current(request), stationId));
    }

    /**
     * 导出空模板。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return xlsx
     */
    @GetMapping(ApiPathConstant.STATION_WATER_TEMPLATE)
    public ResponseEntity<byte[]> exportTemplate(HttpServletRequest request, @PathVariable Long stationId) {
        waterService.getWaters(current(request), stationId);
        return xlsx(WaterRuleConstant.EXPORT_TEMPLATE_PREFIX, waterService.exportTemplate());
    }

    /**
     * 附近水源。
     *
     * @param request HTTP 请求
     * @param lng     经度
     * @param lat     纬度
     * @param radiusM 半径米，可省
     * @param limit   条数，可省
     * @return 附近水源
     */
    @GetMapping(ApiPathConstant.WATER_NEARBY)
    public ApiResultVO<WaterNearbyResultVO> nearby(HttpServletRequest request,
                                                   @RequestParam("lng") BigDecimal lng,
                                                   @RequestParam("lat") BigDecimal lat,
                                                   @RequestParam(value = "radiusM", required = false) Integer radiusM,
                                                   @RequestParam(value = "limit", required = false) Integer limit) {
        return ApiResultVO.ok(waterService.nearby(current(request), lng, lat, radiusM, limit));
    }

    private ResponseEntity<byte[]> xlsx(String prefix, byte[] body) {
        String fileName = prefix + LocalDate.now().format(FILE_DATE) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(WaterRuleConstant.EXPORT_CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName)
                        .build()
                        .toString())
                .body(body);
    }

    private AuthPrincipal current(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
    }
}
