package com.ccds.roster.roster.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
import com.ccds.roster.roster.service.RosterService;
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
        return xlsx(rosterService.exportProfiles(current(request), stationId));
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
        return xlsx(rosterService.exportTemplate());
    }

    private ResponseEntity<byte[]> xlsx(byte[] body) {
        String fileName = "roster-template-" + LocalDate.now().format(FILE_DATE) + ".xlsx";
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
