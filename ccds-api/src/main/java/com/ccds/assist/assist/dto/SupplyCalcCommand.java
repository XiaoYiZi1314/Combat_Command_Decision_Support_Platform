package com.ccds.assist.assist.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ccds.assist.assist.constant.AssistRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 供水 AI 研判入参。本地计算结果必须一并提交，失败时前端仍保留本地结果。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyCalcCommand {

    /**
     * 火灾类型码。
     */
    @NotNull
    @Size(max = 32)
    private String fireType;

    /**
     * 燃烧面积 m²。
     */
    @NotNull
    @DecimalMin("1")
    @DecimalMax("100000")
    private Double area;

    /**
     * 经验系数。
     */
    @NotNull
    @DecimalMin("1.1")
    @DecimalMax("1.4")
    private Double experience;

    /**
     * 本地计算的需求流量 L/s。
     */
    @NotNull
    private Double requiredFlow;

    /**
     * 车辆默认泵量合计 L/s。
     */
    @NotNull
    private Double vehicleFlow;

    /**
     * 附近水源估算流量合计 L/s。
     */
    @NotNull
    private Double sourceFlow;

    /**
     * 余量 L/s（可负）。
     */
    @NotNull
    private Double gap;

    /**
     * 干线压力损失估算 MPa。
     */
    private Double hoseLoss;

    /**
     * 参战车辆摘要。
     */
    @Valid
    @Size(max = AssistRuleConstant.SUPPLY_VEHICLE_MAX)
    private List<SupplyCalcVehicleDTO> vehicles;

    /**
     * 附近水源摘要。
     */
    @Valid
    @Size(max = AssistRuleConstant.SUPPLY_SOURCE_MAX)
    private List<SupplyCalcSourceDTO> sources;
}
