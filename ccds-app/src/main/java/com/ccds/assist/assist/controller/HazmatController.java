package com.ccds.assist.assist.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.assist.assist.constant.AssistRuleConstant;
import com.ccds.assist.assist.dto.HazmatSearchResultDTO;
import com.ccds.assist.assist.dto.HazmatVisionCommand;
import com.ccds.assist.assist.dto.HazmatVisionResultDTO;
import com.ccds.assist.assist.service.HazmatService;
import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.iam.identity.model.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 危化品检索与视觉/文字研判接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class HazmatController {

    private final HazmatService hazmatService;

    /**
     * 按名称 / 俗称 / UN / CAS 检索。
     *
     * @param request HTTP 请求
     * @param query   检索词
     * @return 检索结果
     */
    @GetMapping(ApiPathConstant.HAZMAT_SEARCH)
    public ApiResultVO<HazmatSearchResultDTO> search(
            HttpServletRequest request,
            @RequestParam("q") @NotBlank @Size(max = AssistRuleConstant.QUERY_MAX_LENGTH) String query) {
        return ApiResultVO.ok(hazmatService.search(current(request), query));
    }

    /**
     * 拍照 / 图库 / 文字研判。
     *
     * @param request HTTP 请求
     * @param command 图+文
     * @return 研判结果
     */
    @PostMapping(ApiPathConstant.ASSIST_HAZMAT_VISION)
    public ApiResultVO<HazmatVisionResultDTO> vision(HttpServletRequest request,
                                                     @Valid @RequestBody HazmatVisionCommand command) {
        return ApiResultVO.ok(hazmatService.vision(current(request), command));
    }

    private AuthPrincipal current(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
    }
}
