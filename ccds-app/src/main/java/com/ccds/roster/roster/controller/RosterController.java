package com.ccds.roster.roster.controller;

import java.io.IOException;
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
import com.ccds.roster.roster.constant.RosterRuleConstant;
import com.ccds.roster.roster.dto.GroupBatchSaveCommand;
import com.ccds.roster.roster.dto.ProfileSaveCommand;
import com.ccds.roster.roster.dto.ProfileTombstoneRestoreCommand;
import com.ccds.roster.roster.dto.ProfileTransferCommand;
import com.ccds.roster.roster.dto.ProfileTransferReviewCommand;
import com.ccds.roster.roster.dto.ScbaCalibrationSaveCommand;
import com.ccds.roster.roster.service.ProfileTransferService;
import com.ccds.roster.roster.service.RosterService;
import com.ccds.roster.roster.vo.ProfileTombstoneVO;
import com.ccds.roster.roster.vo.ProfileTransferVO;
import com.ccds.roster.roster.vo.ProfileVO;
import com.ccds.roster.roster.vo.RosterImportResultVO;
import com.ccds.roster.roster.vo.StationRosterVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 花名册、编组与表格导入导出接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class RosterController {

    private static final String MSG_IMPORT_EMPTY = "请选择要导入的表格";

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final RosterService rosterService;

    private final ProfileTransferService profileTransferService;

    /**
     * 本站花名册与编组。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return 花名册
     */
    @GetMapping(ApiPathConstant.STATION_ROSTER)
    public ApiResultVO<StationRosterVO> roster(HttpServletRequest request, @PathVariable Long stationId) {
        return ApiResultVO.ok(rosterService.getRoster(current(request), stationId));
    }

    /**
     * 本站档案列表（与花名册同结构，便于现网路径对齐）。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return 花名册
     */
    @GetMapping(ApiPathConstant.STATION_PROFILES)
    public ApiResultVO<StationRosterVO> listProfiles(HttpServletRequest request, @PathVariable Long stationId) {
        return ApiResultVO.ok(rosterService.getRoster(current(request), stationId));
    }

    /**
     * 档案详情。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param profileId 档案主键
     * @return 档案
     */
    @GetMapping(ApiPathConstant.STATION_PROFILE)
    public ApiResultVO<ProfileVO> getProfile(HttpServletRequest request,
                                             @PathVariable Long stationId,
                                             @PathVariable Long profileId) {
        return ApiResultVO.ok(rosterService.getProfile(current(request), stationId, profileId));
    }

    /**
     * 新建档案。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param command   档案
     * @return 新建结果
     */
    @PostMapping(ApiPathConstant.STATION_PROFILES)
    public ApiResultVO<ProfileVO> createProfile(HttpServletRequest request,
                                                @PathVariable Long stationId,
                                                @Valid @RequestBody ProfileSaveCommand command) {
        return ApiResultVO.ok(rosterService.createProfile(current(request), stationId, command));
    }

    /**
     * 更新档案。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param profileId 档案主键
     * @param command   档案
     * @return 更新结果
     */
    @PutMapping(ApiPathConstant.STATION_PROFILE)
    public ApiResultVO<ProfileVO> updateProfile(HttpServletRequest request,
                                                @PathVariable Long stationId,
                                                @PathVariable Long profileId,
                                                @Valid @RequestBody ProfileSaveCommand command) {
        return ApiResultVO.ok(rosterService.updateProfile(current(request), stationId, profileId, command));
    }

    /**
     * 软删档案。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param profileId 档案主键
     * @return 空成功
     */
    @DeleteMapping(ApiPathConstant.STATION_PROFILE)
    public ApiResultVO<Void> deleteProfile(HttpServletRequest request,
                                           @PathVariable Long stationId,
                                           @PathVariable Long profileId) {
        rosterService.deleteProfile(current(request), stationId, profileId);
        return ApiResultVO.ok(null);
    }

    /**
     * 保存个人空呼高强度标定。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param profileId 档案主键
     * @param command   标定入参
     * @return 更新后的档案详情
     */
    @PostMapping(ApiPathConstant.STATION_PROFILE_SCBA_CALIBRATIONS)
    public ApiResultVO<ProfileVO> saveScbaCalibration(HttpServletRequest request,
                                                      @PathVariable Long stationId,
                                                      @PathVariable Long profileId,
                                                      @Valid @RequestBody ScbaCalibrationSaveCommand command) {
        return ApiResultVO.ok(rosterService.saveScbaCalibration(
                current(request), stationId, profileId, command));
    }

    /**
     * 整存战斗编组。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param command   编组
     * @return 最新花名册
     */
    @PutMapping(ApiPathConstant.STATION_GROUPS)
    public ApiResultVO<StationRosterVO> saveGroups(HttpServletRequest request,
                                                   @PathVariable Long stationId,
                                                   @Valid @RequestBody GroupBatchSaveCommand command) {
        return ApiResultVO.ok(rosterService.saveGroups(current(request), stationId, command));
    }

    /**
     * 查询战斗编组。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return 花名册
     */
    @GetMapping(ApiPathConstant.STATION_GROUPS)
    public ApiResultVO<StationRosterVO> listGroups(HttpServletRequest request, @PathVariable Long stationId) {
        return ApiResultVO.ok(rosterService.getRoster(current(request), stationId));
    }

    /**
     * 导入现网列模板。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param file      表格
     * @return 导入结果
     */
    @PostMapping(value = ApiPathConstant.STATION_PROFILE_IMPORT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResultVO<RosterImportResultVO> importProfiles(HttpServletRequest request,
                                                            @PathVariable Long stationId,
                                                            @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCodeConstant.ROSTER_IMPORT_INVALID, MSG_IMPORT_EMPTY);
        }
        try {
            return ApiResultVO.ok(rosterService.importProfiles(
                    current(request), stationId, file.getOriginalFilename(), file.getBytes()));
        } catch (IOException ex) {
            log.warn("roster import read failed stationId={}", stationId, ex);
            throw new BizException(ErrorCodeConstant.ROSTER_IMPORT_INVALID, MSG_IMPORT_EMPTY);
        }
    }

    /**
     * 导出本站表格。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return xlsx
     */
    @GetMapping(ApiPathConstant.STATION_PROFILE_EXPORT)
    public ResponseEntity<byte[]> exportProfiles(HttpServletRequest request, @PathVariable Long stationId) {
        return xlsx(RosterRuleConstant.EXPORT_FILE_PREFIX, rosterService.exportProfiles(current(request), stationId));
    }

    /**
     * 导出空模板。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return xlsx
     */
    @GetMapping(ApiPathConstant.STATION_PROFILE_TEMPLATE)
    public ResponseEntity<byte[]> exportTemplate(HttpServletRequest request, @PathVariable Long stationId) {
        rosterService.getRoster(current(request), stationId);
        return xlsx(RosterRuleConstant.EXPORT_TEMPLATE_PREFIX, rosterService.exportTemplate());
    }

    /**
     * 本站已删除人员名单。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @return 删除记录列表
     */
    @GetMapping(ApiPathConstant.STATION_PROFILE_TOMBSTONES)
    public ApiResultVO<List<ProfileTombstoneVO>> listTombstones(HttpServletRequest request,
                                                                @PathVariable Long stationId) {
        return ApiResultVO.ok(rosterService.listProfileTombstones(current(request), stationId));
    }

    /**
     * 恢复已删除人员记录。scope=name 只解姓名，scope=nfc 需 confirmNfc，scope=all 全解。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键
     * @param command   恢复命令
     * @return 空成功
     */
    @PostMapping(ApiPathConstant.STATION_PROFILE_TOMBSTONE_RESTORE)
    public ApiResultVO<Void> restoreTombstone(HttpServletRequest request,
                                              @PathVariable Long stationId,
                                              @Valid @RequestBody ProfileTombstoneRestoreCommand command) {
        rosterService.restoreProfileTombstone(current(request), stationId, command.getScope(),
                command.getName(), command.getNfcTag());
        return ApiResultVO.ok(null);
    }

    /**
     * 提交人员跨站调动申请。
     *
     * @param request HTTP 请求
     * @param command 调动申请
     * @return 申请视图
     */
    @PostMapping(ApiPathConstant.PROFILE_TRANSFERS)
    public ApiResultVO<ProfileTransferVO> submitTransfer(HttpServletRequest request,
                                                         @Valid @RequestBody ProfileTransferCommand command) {
        return ApiResultVO.ok(profileTransferService.submit(current(request), command));
    }

    /**
     * 调动申请列表。
     *
     * @param request   HTTP 请求
     * @param stationId 站主键，可省
     * @param status    状态，可省
     * @return 申请列表
     */
    @GetMapping(ApiPathConstant.PROFILE_TRANSFERS)
    public ApiResultVO<List<ProfileTransferVO>> listTransfers(
            HttpServletRequest request,
            @RequestParam(value = "stationId", required = false) Long stationId,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResultVO.ok(profileTransferService.list(current(request), stationId, status));
    }

    /**
     * 审批人员调动申请。
     *
     * @param request    HTTP 请求
     * @param transferId 申请主键
     * @param command    审批命令
     * @return 申请视图
     */
    @PutMapping(ApiPathConstant.PROFILE_TRANSFER)
    public ApiResultVO<ProfileTransferVO> reviewTransfer(HttpServletRequest request,
                                                         @PathVariable Long transferId,
                                                         @Valid @RequestBody ProfileTransferReviewCommand command) {
        return ApiResultVO.ok(profileTransferService.review(current(request), transferId,
                command.getApprove(), command.getReviewReason()));
    }

    /**
     * 支队导出所有单位人员档案。站级仍只导出本站。
     *
     * @param request HTTP 请求
     * @return xlsx
     */
    @GetMapping(ApiPathConstant.PROFILES_EXPORT_ALL)
    public ResponseEntity<byte[]> exportAllProfiles(HttpServletRequest request) {
        return xlsx(RosterRuleConstant.EXPORT_ALL_FILE_PREFIX,
                rosterService.exportAllProfiles(current(request)));
    }

    private ResponseEntity<byte[]> xlsx(String prefix, byte[] body) {
        String fileName = prefix + LocalDate.now().format(FILE_DATE) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(RosterRuleConstant.EXPORT_CONTENT_TYPE))
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
