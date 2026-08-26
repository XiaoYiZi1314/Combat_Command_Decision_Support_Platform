package com.ccds.assist.assist.dto;

import jakarta.validation.constraints.Size;

import com.ccds.assist.assist.constant.AssistRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 危化品视觉/文字研判入参。图与描述只送到本后端。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HazmatVisionCommand {

    /**
     * JPEG/PNG 的 Base64，可空（纯文字研判）。
     */
    @Size(max = AssistRuleConstant.IMAGE_BASE64_MAX_LENGTH)
    private String imageBase64;

    /**
     * 图片 MIME，有图时必填。
     */
    @Size(max = 32)
    private String imageContentType;

    /**
     * 补充文字描述。
     */
    @Size(max = AssistRuleConstant.TEXT_MAX_LENGTH)
    private String text;
}
