package com.ccds.roster.roster.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ccds.roster.roster.constant.RosterRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存个人空呼高强度实测标定。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScbaCalibrationSaveCommand {

    /**
     * 实测初始压力 MPa。
     */
    @NotNull
    @DecimalMin(value = "0.1")
    @DecimalMax(value = RosterRuleConstant.MORNING_PRESSURE_MAX)
    private BigDecimal pressure;

    /**
     * 从初始压力完全用尽的实测秒数。
     */
    @NotNull
    @Min(RosterRuleConstant.SCBA_CALIBRATION_TIME_MIN_SEC)
    @Max(RosterRuleConstant.SCBA_CALIBRATION_TIME_MAX_SEC)
    private Integer fullTimeSec;

    /**
     * 实测气瓶规格。
     */
    @NotBlank
    @Pattern(regexp = "^(6\\.8|9)[Ll]?$")
    private String cylType;

    /**
     * 来源说明。
     */
    @Size(max = RosterRuleConstant.SCBA_CALIBRATION_SOURCE_MAX_LENGTH)
    private String source;
}
