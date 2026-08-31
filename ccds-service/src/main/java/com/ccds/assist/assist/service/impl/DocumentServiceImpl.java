package com.ccds.assist.assist.service.impl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ccds.assist.assist.constant.AssistRuleConstant;
import com.ccds.assist.assist.dto.DocumentCommand;
import com.ccds.assist.assist.dto.DocumentResultDTO;
import com.ccds.assist.assist.service.AssistAccessService;
import com.ccds.assist.assist.service.DocumentService;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.model.ModelGateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 文书生成实现。模板固定四种，正文由服务端模型生成。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final String TEMPLATE_FIRE_REPORT = "fire_report";

    private static final String TEMPLATE_RESCUE_REPORT = "rescue_report";

    private static final String TEMPLATE_DRILL_REPORT = "drill_report";

    private static final String TEMPLATE_INSPECT_REPORT = "inspect_report";

    private static final String MSG_TEMPLATE_INVALID = "文书类型须为 fire_report/rescue_report/drill_report/inspect_report";

    private static final Map<String, String> TEMPLATE_TITLES = new HashMap<String, String>();

    static {
        TEMPLATE_TITLES.put(TEMPLATE_FIRE_REPORT, "火灾扑救报告");
        TEMPLATE_TITLES.put(TEMPLATE_RESCUE_REPORT, "抢险救援报告");
        TEMPLATE_TITLES.put(TEMPLATE_DRILL_REPORT, "演练记录");
        TEMPLATE_TITLES.put(TEMPLATE_INSPECT_REPORT, "内攻安全检查记录");
    }

    private static final String SYSTEM_PROMPT = "你是作战指挥辅助决策平台的消防文书助手。"
            + "请按指定文书类型生成结构完整、语言规范的公文正文。"
            + "只依据给定现场摘要与填写信息行文，不得编造摘要中不存在的人员、车辆或数据。"
            + "不要使用 Markdown 语法，直接输出公文正文文本。"
            + "文末另起一行声明：本文书由辅助系统生成，仅供内部参考，正式行文需人工核签。";

    private final AssistAccessService assistAccessService;

    private final ModelGateway modelGateway;

    /**
     * {@inheritDoc}
     */
    @Override
    public DocumentResultDTO generate(AuthPrincipal principal, DocumentCommand command) {
        assistAccessService.requireAccount(principal);
        if (command == null || command.getTemplate() == null || command.getTemplate().isBlank()) {
            throw new BizException(ErrorCodeConstant.ASSIST_DOCUMENT_INVALID, "文书类型不能为空");
        }
        String template = command.getTemplate().trim();
        String title = TEMPLATE_TITLES.get(template);
        if (title == null) {
            throw new BizException(ErrorCodeConstant.ASSIST_DOCUMENT_INVALID, MSG_TEMPLATE_INVALID);
        }
        String prompt = buildPrompt(title, command);
        try {
            String answer = modelGateway.completeText(SYSTEM_PROMPT, prompt);
            return DocumentResultDTO.builder()
                    .content(answer == null ? "" : answer.trim())
                    .disclaimer(AssistRuleConstant.AUXILIARY_DISCLAIMER)
                    .build();
        } catch (IllegalStateException ex) {
            log.error("AI 文书生成模型不可用", ex);
            throw new BizException(ErrorCodeConstant.ASSIST_DOCUMENT_UNAVAILABLE,
                    AssistRuleConstant.MSG_MODEL_UNAVAILABLE, ex);
        }
    }

    private String buildPrompt(String title, DocumentCommand command) {
        StringBuilder builder = new StringBuilder("请生成一份专业的消防");
        builder.append(title);
        builder.append("。\n填写信息：");
        builder.append("\n日期：").append(command.getDate() == null || command.getDate().isBlank()
                ? LocalDate.now().toString() : command.getDate().trim());
        if (command.getEventName() != null && !command.getEventName().isBlank()) {
            builder.append("\n名称：").append(command.getEventName().trim());
        }
        if (command.getLocation() != null && !command.getLocation().isBlank()) {
            builder.append("\n地点：").append(command.getLocation().trim());
        }
        if (command.getContext() != null && !command.getContext().isBlank()) {
            builder.append("\n现场摘要：\n").append(command.getContext().trim());
        }
        builder.append("\n请包含标题、基本情况、处置（或检查/演练）过程、结果与建议等章节，正文用纯文本段落。");
        return builder.toString();
    }
}
