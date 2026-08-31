package com.ccds.attack.attack.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.attack.attack.dto.AttackArchiveCommand;
import com.ccds.attack.attack.service.AttackArchiveService;
import com.ccds.attack.attack.vo.AttackArchiveVO;
import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.iam.identity.model.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 内攻历史归档接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class AttackArchiveController {

    private static final String EXPORT_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final AttackArchiveService attackArchiveService;

    /**
     * 一键归档本站当前人员。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param command   事件信息
     * @return 归档记录
     */
    @PostMapping(ApiPathConstant.STATION_ATTACK_ARCHIVES)
    public ApiResultVO<AttackArchiveVO> archive(HttpServletRequest request,
                                                @PathVariable Long stationId,
                                                @Valid @RequestBody AttackArchiveCommand command) {
        return ApiResultVO.ok(attackArchiveService.archive(current(request), stationId, command));
    }

    /**
     * 本站历史列表。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param eventKind 类型过滤，可空
     * @return 列表
     */
    @GetMapping(ApiPathConstant.STATION_ATTACK_ARCHIVES)
    public ApiResultVO<List<AttackArchiveVO>> list(HttpServletRequest request,
                                                   @PathVariable Long stationId,
                                                   @RequestParam(required = false) String eventKind) {
        return ApiResultVO.ok(attackArchiveService.list(current(request), stationId, eventKind));
    }

    /**
     * 指挥端全部历史。
     *
     * @param request   HTTP 请求
     * @param stationId 过滤站主键，可空
     * @param eventKind 类型过滤，可空
     * @return 列表
     */
    @GetMapping(ApiPathConstant.ATTACK_ARCHIVES_ALL)
    public ApiResultVO<List<AttackArchiveVO>> listAll(HttpServletRequest request,
                                                      @RequestParam(required = false) Long stationId,
                                                      @RequestParam(required = false) String eventKind) {
        return ApiResultVO.ok(attackArchiveService.listAll(current(request), stationId, eventKind));
    }

    /**
     * 更新归档基础信息。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param archiveId 归档主键
     * @param command   事件信息
     * @return 更新后的归档
     */
    @PutMapping(ApiPathConstant.STATION_ATTACK_ARCHIVE)
    public ApiResultVO<AttackArchiveVO> updateInfo(HttpServletRequest request,
                                                   @PathVariable Long stationId,
                                                   @PathVariable Long archiveId,
                                                   @Valid @RequestBody AttackArchiveCommand command) {
        return ApiResultVO.ok(attackArchiveService.updateInfo(current(request), stationId,
                archiveId, command));
    }

    /**
     * 删除归档。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param archiveId 归档主键
     * @return 空成功
     */
    @DeleteMapping(ApiPathConstant.STATION_ATTACK_ARCHIVE)
    public ApiResultVO<Void> delete(HttpServletRequest request,
                                    @PathVariable Long stationId,
                                    @PathVariable Long archiveId) {
        attackArchiveService.delete(current(request), stationId, archiveId);
        return ApiResultVO.ok(null);
    }

    /**
     * 导出明细 Excel。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键，可空
     * @param eventKind 类型过滤，可空
     * @return xlsx
     */
    @GetMapping(ApiPathConstant.ATTACK_ARCHIVES_EXPORT)
    public ResponseEntity<byte[]> export(HttpServletRequest request,
                                         @RequestParam(required = false) Long stationId,
                                         @RequestParam(required = false) String eventKind) {
        return xlsx("内攻历史记录_",
                attackArchiveService.export(current(request), stationId, eventKind));
    }

    /**
     * 导出统计 Excel。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键，可空
     * @param eventKind 类型过滤，可空
     * @return xlsx
     */
    @GetMapping(ApiPathConstant.ATTACK_ARCHIVES_EXPORT_STATS)
    public ResponseEntity<byte[]> exportStats(HttpServletRequest request,
                                              @RequestParam(required = false) Long stationId,
                                              @RequestParam(required = false) String eventKind) {
        return xlsx("内攻历史统计_",
                attackArchiveService.exportStats(current(request), stationId, eventKind));
    }

    private ResponseEntity<byte[]> xlsx(String prefix, byte[] body) {
        String fileName = prefix + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXPORT_CONTENT_TYPE))
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
