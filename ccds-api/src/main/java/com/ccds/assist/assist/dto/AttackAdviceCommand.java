package com.ccds.assist.assist.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ccds.assist.assist.constant.AssistRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内攻 AI 研判入参。人员摘要由后端按白名单重装，前端字段仅作参考。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackAdviceCommand {

    /**
     * 目标站。空则用站级本站。
     */
    @NotNull
    private Long stationId;

    /**
     * 天气摘要，可空。后端再截断。
     */
    @Size(max = AssistRuleConstant.WEATHER_SUMMARY_MAX_LENGTH)
    private String weatherSummary;

    /**
     * 火点/现场描述，可空。
     */
    @Size(max = AssistRuleConstant.SCENE_DESC_MAX_LENGTH)
    private String sceneDesc;

    /**
     * 前端可改的人员摘要。后端只保留白名单字段；空则改用本站未撤出卡片。
     */
    @Valid
    @Size(max = AssistRuleConstant.ATTACK_PERSON_MAX)
    private List<AttackAdvicePersonDTO> persons;
}
