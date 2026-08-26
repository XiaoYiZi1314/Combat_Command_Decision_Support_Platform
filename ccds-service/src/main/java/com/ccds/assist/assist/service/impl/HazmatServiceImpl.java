package com.ccds.assist.assist.service.impl;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.ccds.assist.assist.constant.AssistRuleConstant;
import com.ccds.assist.assist.dto.HazmatSearchResultDTO;
import com.ccds.assist.assist.dto.HazmatVisionCommand;
import com.ccds.assist.assist.dto.HazmatVisionResultDTO;
import com.ccds.assist.assist.service.AssistAccessService;
import com.ccds.assist.assist.service.HazmatService;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.constant.FileBizTypeConstant;
import com.ccds.duty.constant.FileRuleConstant;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.infra.hazmat.HazmatSearchClient;
import com.ccds.infra.model.ModelGateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 危化品检索与研判。图和描述只送到本后端。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HazmatServiceImpl implements HazmatService {

    private static final Pattern BASE64_CHARS = Pattern.compile("^[A-Za-z0-9+/=\\r\\n]+$");

    private final AssistAccessService assistAccessService;

    private final HazmatSearchClient hazmatSearchClient;

    private final ModelGateway modelGateway;

    /**
     * {@inheritDoc}
     */
    @Override
    public HazmatSearchResultDTO search(AuthPrincipal principal, String query) {
        assistAccessService.requireAccount(principal);
        String keyword = normalizeQuery(query);
        try {
            return hazmatSearchClient.search(keyword, AssistRuleConstant.SEARCH_DEFAULT_LIMIT);
        } catch (IllegalStateException ex) {
            log.warn("危化品检索未配置");
            throw new BizException(ErrorCodeConstant.HAZMAT_SEARCH_UNAVAILABLE,
                    AssistRuleConstant.MSG_SEARCH_UNAVAILABLE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HazmatVisionResultDTO vision(AuthPrincipal principal, HazmatVisionCommand command) {
        assistAccessService.requireAccount(principal);
        HazmatVisionCommand safe = command == null ? new HazmatVisionCommand() : command;
        boolean hasImage = hasText(safe.getImageBase64());
        boolean hasText = hasText(safe.getText());
        if (!hasImage && !hasText) {
            throw new BizException(ErrorCodeConstant.HAZMAT_VISION_INVALID, AssistRuleConstant.MSG_VISION_INVALID);
        }
        if (hasImage) {
            validateImage(safe.getImageBase64(), safe.getImageContentType());
        }
        try {
            String raw = modelGateway.completeVision(
                    AssistPromptBuilder.hazmatSystem(),
                    AssistPromptBuilder.hazmatUser(safe.getText()),
                    hasImage ? stripDataUrl(safe.getImageBase64()) : null,
                    hasImage ? safe.getImageContentType() : null);
            return parseVision(raw);
        } catch (IllegalStateException ex) {
            log.warn("危化品研判模型不可用");
            throw new BizException(ErrorCodeConstant.ASSIST_MODEL_UNAVAILABLE, AssistRuleConstant.MSG_MODEL_UNAVAILABLE);
        }
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            throw new BizException(ErrorCodeConstant.HAZMAT_QUERY_INVALID, AssistRuleConstant.MSG_QUERY_INVALID);
        }
        String keyword = query.trim();
        if (keyword.length() < AssistRuleConstant.QUERY_MIN_LENGTH
                || keyword.length() > AssistRuleConstant.QUERY_MAX_LENGTH) {
            throw new BizException(ErrorCodeConstant.HAZMAT_QUERY_INVALID, AssistRuleConstant.MSG_QUERY_INVALID);
        }
        return keyword;
    }

    private void validateImage(String imageBase64, String contentType) {
        String mime = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        if (!FileRuleConstant.isAllowedMime(FileBizTypeConstant.HAZMAT_IMAGE, mime)) {
            throw new BizException(ErrorCodeConstant.HAZMAT_VISION_INVALID, AssistRuleConstant.MSG_VISION_INVALID);
        }
        String payload = stripDataUrl(imageBase64);
        if (payload.length() > AssistRuleConstant.IMAGE_BASE64_MAX_LENGTH || !BASE64_CHARS.matcher(payload).matches()) {
            throw new BizException(ErrorCodeConstant.HAZMAT_VISION_INVALID, AssistRuleConstant.MSG_VISION_INVALID);
        }
    }

    private String stripDataUrl(String imageBase64) {
        String raw = imageBase64.trim();
        int comma = raw.indexOf(',');
        if (raw.startsWith("data:") && comma > 0) {
            return raw.substring(comma + 1);
        }
        return raw;
    }

    private HazmatVisionResultDTO parseVision(String raw) {
        String text = raw == null ? "" : raw.trim();
        return HazmatVisionResultDTO.builder()
                .possibleName("")
                .hazardSummary(text)
                .advice("")
                .disclaimer(AssistRuleConstant.AUXILIARY_DISCLAIMER)
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
