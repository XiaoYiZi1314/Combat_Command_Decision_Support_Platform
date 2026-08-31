package com.ccds.assist.assist.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccds.assist.assist.dto.AttackAdviceCommand;
import com.ccds.assist.assist.dto.ChatCommand;
import com.ccds.assist.assist.dto.ChatResultDTO;
import com.ccds.assist.assist.dto.AttackAdviceResultDTO;
import com.ccds.assist.assist.dto.DocumentCommand;
import com.ccds.assist.assist.dto.DocumentResultDTO;
import com.ccds.assist.assist.dto.SupplyCalcCommand;
import com.ccds.assist.assist.dto.SupplyCalcResultDTO;
import com.ccds.assist.assist.service.AttackAdviceService;
import com.ccds.assist.assist.service.ChatService;
import com.ccds.assist.assist.service.DocumentService;
import com.ccds.assist.assist.service.SupplyAdviceService;
import com.ccds.common.api.constant.ApiPathConstant;
import com.ccds.common.api.vo.ApiResultVO;
import com.ccds.common.web.RequestAttributeConstant;
import com.ccds.iam.identity.model.AuthPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * 内攻与供水 AI 研判接入。
 *
 * @author ccds
 * @since 0.1.0
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPathConstant.API_V1)
public class AssistController {

    private final AttackAdviceService attackAdviceService;

    private final SupplyAdviceService supplyAdviceService;

    private final ChatService chatService;

    private final DocumentService documentService;

    /**
     * 内攻辅助研判。
     *
     * @param request HTTP 请求
     * @param command 研判入参
     * @return 轮换、撤离、风险提示
     */
    @PostMapping(ApiPathConstant.ASSIST_ATTACK_ADVICE)
    public ApiResultVO<AttackAdviceResultDTO> attackAdvice(HttpServletRequest request,
                                                           @Valid @RequestBody AttackAdviceCommand command) {
        return ApiResultVO.ok(attackAdviceService.advise(current(request), command));
    }

    /**
     * 供水 AI 研判。失败时前端仍保留本地计算。
     *
     * @param request HTTP 请求
     * @param command 本地计算结果
     * @return 增强说明
     */
    @PostMapping(ApiPathConstant.ASSIST_SUPPLY_CALC)
    public ApiResultVO<SupplyCalcResultDTO> supplyCalc(HttpServletRequest request,
                                                       @Valid @RequestBody SupplyCalcCommand command) {
        return ApiResultVO.ok(supplyAdviceService.advise(current(request), command));
    }

    /**
     * AI 通用问答。
     *
     * @param request HTTP 请求
     * @param command 问答入参
     * @return AI 回答
     */
    @PostMapping(ApiPathConstant.ASSIST_CHAT)
    public ApiResultVO<ChatResultDTO> chat(HttpServletRequest request,
                                           @Valid @RequestBody ChatCommand command) {
        return ApiResultVO.ok(chatService.chat(current(request), command));
    }

    /**
     * AI 文书生成。
     *
     * @param request HTTP 请求
     * @param command 文书入参
     * @return 文书正文
     */
    @PostMapping(ApiPathConstant.ASSIST_DOCUMENT)
    public ApiResultVO<DocumentResultDTO> document(HttpServletRequest request,
                                                   @Valid @RequestBody DocumentCommand command) {
        return ApiResultVO.ok(documentService.generate(current(request), command));
    }

    private AuthPrincipal current(HttpServletRequest request) {
        return (AuthPrincipal) request.getAttribute(RequestAttributeConstant.AUTH_PRINCIPAL);
    }
}
