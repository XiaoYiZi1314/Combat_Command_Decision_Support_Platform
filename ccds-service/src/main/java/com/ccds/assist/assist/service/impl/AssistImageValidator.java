package com.ccds.assist.assist.service.impl;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.ccds.assist.assist.constant.AssistRuleConstant;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.duty.constant.FileBizTypeConstant;
import com.ccds.duty.constant.FileRuleConstant;

/**
 * 图片入参校验与清洗。危化品拍照与 AI 问答附图共用同一套规则，避免两处各写一份。
 *
 * @author ccds
 * @since 0.1.2
 */
@Component
public class AssistImageValidator {

    private static final Pattern BASE64_CHARS = Pattern.compile("^[A-Za-z0-9+/=\\r\\n]+$");

    /**
     * 校验图片并返回去掉 data: 前缀后的纯 base64。
     *
     * @param imageBase64       图片 base64，可带 data: 前缀
     * @param imageContentType  MIME 类型
     * @param bizType           业务类型，用于 MIME 白名单
     * @return 纯 base64
     */
    public String validateAndStrip(String imageBase64, String imageContentType, String bizType) {
        String mime = imageContentType == null ? "" : imageContentType.trim().toLowerCase(Locale.ROOT);
        if (!FileRuleConstant.isAllowedMime(bizType, mime)) {
            throw new BizException(ErrorCodeConstant.HAZMAT_VISION_INVALID, AssistRuleConstant.MSG_VISION_INVALID);
        }
        String payload = stripDataUrl(imageBase64);
        if (payload.length() > AssistRuleConstant.IMAGE_BASE64_MAX_LENGTH
                || !BASE64_CHARS.matcher(payload).matches()) {
            throw new BizException(ErrorCodeConstant.HAZMAT_VISION_INVALID, AssistRuleConstant.MSG_VISION_INVALID);
        }
        return payload;
    }

    /**
     * 去掉 data: 前缀。
     *
     * @param imageBase64 图片 base64
     * @return 纯 base64
     */
    public String stripDataUrl(String imageBase64) {
        if (imageBase64 == null) {
            return "";
        }
        String raw = imageBase64.trim();
        int comma = raw.indexOf(',');
        if (raw.startsWith("data:") && comma > 0) {
            return raw.substring(comma + 1);
        }
        return raw;
    }

    /**
     * 是否带了图片。
     *
     * @param imageBase64 图片 base64
     * @return 有图为 true
     */
    public boolean hasImage(String imageBase64) {
        return imageBase64 != null && !imageBase64.isBlank();
    }

    /**
     * 危化品图片的业务类型，供调用方复用。
     *
     * @return 业务类型
     */
    public String hazmatBizType() {
        return FileBizTypeConstant.HAZMAT_IMAGE;
    }

    /**
     * AI 问答附图的业务类型。
     *
     * @return 业务类型
     */
    public String assistBizType() {
        return FileBizTypeConstant.HAZMAT_IMAGE;
    }
}
