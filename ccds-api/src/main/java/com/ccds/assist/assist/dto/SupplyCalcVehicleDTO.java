package com.ccds.assist.assist.dto;

import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 供水计算参战车辆。一期无车辆档案，泵量为类型默认值。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyCalcVehicleDTO {

    /**
     * 车辆类型名。
     */
    @Size(max = 64)
    private String name;

    /**
     * 默认泵量 L/s。
     */
    private Double flow;
}
