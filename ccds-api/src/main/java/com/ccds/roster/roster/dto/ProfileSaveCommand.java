package com.ccds.roster.roster.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.ccds.roster.roster.constant.RosterRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 新建或更新人员档案。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "phone")
public class ProfileSaveCommand {

    /**
     * 姓名。
     */
    @NotBlank
    @Size(max = RosterRuleConstant.NAME_MAX_LENGTH)
    private String name;

    /**
     * 职务。
     */
    @Size(max = RosterRuleConstant.TITLE_MAX_LENGTH)
    private String title;

    /**
     * 衔级。
     */
    @Size(max = RosterRuleConstant.RANK_MAX_LENGTH)
    private String rankName;

    /**
     * 电话明文，仅传输。
     */
    @Size(max = RosterRuleConstant.PHONE_MAX_LENGTH)
    private String phone;

    /**
     * 气瓶规格，6.8 或 9。
     */
    private String cylType;

    /**
     * NFC 编号。
     */
    @Size(max = RosterRuleConstant.NFC_MAX_LENGTH)
    private String nfcTag;

    /**
     * 身高厘米。
     */
    @Min(0)
    @Max(RosterRuleConstant.HEIGHT_MAX_CM)
    private Integer heightCm;

    /**
     * 体重公斤。
     */
    @Min(0)
    @Max(RosterRuleConstant.WEIGHT_MAX_KG)
    private Integer weightKg;

    /**
     * 年龄。
     */
    @Min(0)
    @Max(RosterRuleConstant.AGE_MAX)
    private Integer age;

    /**
     * 早检压力 MPa。
     */
    @DecimalMin("0.0")
    @DecimalMax(RosterRuleConstant.MORNING_PRESSURE_MAX)
    private BigDecimal morningPressure;
}
