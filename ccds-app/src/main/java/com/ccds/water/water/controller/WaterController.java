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
import com.ccds.water.water.dto.WaterRequestCommand;
import com.ccds.water.water.dto.WaterRequestReviewCommand;
import com.ccds.water.water.dto.WaterSaveCommand;
import com.ccds.water.water.dto.WaterVideoCommand;
import com.ccds.water.water.service.WaterLedgerService;
import com.ccds.water.water.service.WaterRequestService;
import com.ccds.water.water.service.WaterService;
import com.ccds.water.water.service.WaterVideoService;
import com.ccds.water.water.vo.StationWaterVO;
import com.ccds.water.water.vo.WaterImportResultVO;
import com.ccds.water.water.vo.WaterNearbyResultVO;
import com.ccds.water.water.vo.WaterRequestVO;
import com.ccds.water.water.vo.WaterStatsVO;
import com.ccds.water.water.vo.WaterVideoVO;
import com.ccds.water.water.vo.WaterVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 水源档案、表格导入导出、汇总统计、变更审批与附近水源接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class WaterController {

    private static final String MSG_IMPORT_READ_FAILED = "导入文件读取失败，请重试";

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final WaterService waterService;

    private final WaterLedgerService waterLedgerService;

    private final WaterRequestService waterRequestService;

    private final WaterVideoService waterVideoService;

    /**
     * 全市水源（按账号可见范围返回）。
     *
     * @param request HTTP 请求
     * @return 全市水源
     */
    @GetMapping(ApiPathConstant.WATERS_CITY)
    public ApiResultVO<List<WaterVO>> cityWaters(HttpServletRequest request) {
        return ApiResultVO.ok(waterService.listCityWaters(current(request)));
    }

    /**
     * 水源汇总统计。
     *
     * @param request HTTP 请求
     * @param month   月份 yyyy-MM，可省
     * @return 统计
     */
    @GetMapping(ApiPathConstant.WATERS_SUMMARY)
    public ApiResultVO<WaterStatsVO> summary(HttpServletRequest request,
                                             @RequestParam(value = "month", required = false) String month) {
        return ApiResultVO.ok(waterLedgerService.summary(current(request), month));
    }

    /**
     * 水源汇总统计导出（多工作表）。
     *
     * @param request HTTP 请求
     * @param month   月份 yyyy-MM，可省
     * @return xlsx
     */
    @GetMapping(ApiPathConstant.WATERS_SUMMARY_EXPORT)
    public ResponseEntity<byte[]> exportSummary(HttpServletRequest request,
                                                @RequestParam(value = "month", required = false) String month) {
        return xlsx(WaterRuleConstant.EXPORT_SUMMARY_PREFIX,
                waterLedgerService.exportSummary(current(request), month));
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
     * 新建水源。站级会返回须走审批申请的提示。
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
     * 更新水源。站级会返回须走审批申请的提示。
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
     * 删除水源。站级会返回须走审批申请的提示。
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
     * 导入现网检查记录表（23 列）。
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
            throw new BizException(ErrorCodeConstant.WATER_IMPORT_INVALID, "请选择要导入的表格");
        }
        try {
            return ApiResultVO.ok(waterLedgerService.importWaters(
                    current(request), stationId, file.getOriginalFilename(), file.getBytes()));
        } catch (IOException ex) {
            log.warn("water import read failed stationId={}", stationId, ex);
            throw new BizException(ErrorCodeConstant.WATER_IMPORT_INVALID, MSG_IMPORT_READ_FAILED);
        }
    }

    /**
     * 导出本站表格（23 列）。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return xlsx
     */
    @GetMapping(ApiPathConstant.STATION_WATER_EXPORT)
    public ResponseEntity<byte[]> exportWaters(HttpServletRequest request, @PathVariable Long stationId) {
        return xlsx(WaterRuleConstant.EXPORT_FILE_PREFIX,
                waterLedgerService.exportWaters(current(request), stationId));
    }

    /**
     * 导出空模板（23 列）。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键（用于权限校验）
     * @return xlsx
     */
    @GetMapping(ApiPathConstant.STATION_WATER_TEMPLATE)
    public ResponseEntity<byte[]> exportTemplate(HttpServletRequest request, @PathVariable Long stationId) {
        return xlsx(WaterRuleConstant.EXPORT_TEMPLATE_PREFIX,
                waterLedgerService.exportTemplate(current(request), stationId));
    }

    /**
     * 提交水源变更申请。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param command   申请
     * @return 申请视图
     */
    @PostMapping(ApiPathConstant.STATION_WATER_REQUESTS)
    public ApiResultVO<WaterRequestVO> submitRequest(HttpServletRequest request,
                                                     @PathVariable Long stationId,
                                                     @Valid @RequestBody WaterRequestCommand command) {
        return ApiResultVO.ok(waterRequestService.submit(current(request), stationId, command));
    }

    /**
     * 本站水源变更申请列表。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param status    状态，可省
     * @return 申请列表
     */
    @GetMapping(ApiPathConstant.STATION_WATER_REQUESTS)
    public ApiResultVO<List<WaterRequestVO>> listStationRequests(
            HttpServletRequest request,
            @PathVariable Long stationId,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResultVO.ok(waterRequestService.list(current(request), stationId, status));
    }

    /**
     * 可见范围内的水源变更申请列表（支队审批用）。
     *
     * @param request HTTP 请求
     * @param status  状态，可省
     * @return 申请列表
     */
    @GetMapping(ApiPathConstant.WATER_REQUESTS_ALL)
    public ApiResultVO<List<WaterRequestVO>> listAllRequests(
            HttpServletRequest request,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResultVO.ok(waterRequestService.list(current(request), null, status));
    }

    /**
     * 审批水源变更申请。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param requestId 申请主键
     * @param command   审批命令
     * @return 申请视图
     */
    @PutMapping(ApiPathConstant.STATION_WATER_REQUEST)
    public ApiResultVO<WaterRequestVO> reviewRequest(HttpServletRequest request,
                                                     @PathVariable Long stationId,
                                                     @PathVariable Long requestId,
                                                     @Valid @RequestBody WaterRequestReviewCommand command) {
        return ApiResultVO.ok(waterRequestService.review(current(request), stationId, requestId, command));
    }

    /**
     * 撤回水源变更申请。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param requestId 申请主键
     * @return 空成功
     */
    @DeleteMapping(ApiPathConstant.STATION_WATER_REQUEST)
    public ApiResultVO<Void> withdrawRequest(HttpServletRequest request,
                                             @PathVariable Long stationId,
                                             @PathVariable Long requestId) {
        waterRequestService.withdraw(current(request), stationId, requestId);
        return ApiResultVO.ok(null);
    }

    /**
     * 登记出水视频（时长与三帧由客户端上报）。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param waterId   水源主键
     * @param command   视频登记
     * @return 视频视图
     */
    @PostMapping(ApiPathConstant.STATION_WATER_VIDEO)
    public ApiResultVO<WaterVideoVO> submitVideo(HttpServletRequest request,
                                                 @PathVariable Long stationId,
                                                 @PathVariable Long waterId,
                                                 @Valid @RequestBody WaterVideoCommand command) {
        return ApiResultVO.ok(waterVideoService.submit(current(request), stationId, waterId, command));
    }

    /**
     * 水源出水视频列表。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param waterId   水源主键
     * @return 视频列表
     */
    @GetMapping(ApiPathConstant.STATION_WATER_VIDEO)
    public ApiResultVO<List<WaterVideoVO>> listVideos(HttpServletRequest request,
                                                      @PathVariable Long stationId,
                                                      @PathVariable Long waterId) {
        return ApiResultVO.ok(waterVideoService.list(current(request), stationId, waterId));
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
