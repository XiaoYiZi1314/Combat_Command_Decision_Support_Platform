package com.ccds.assist.assist.dto;

import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 供水计算附近水源摘要。不含精确坐标。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyCalcSourceDTO {

    /**
     * 水源名称。
     */
    @Size(max = 64)
    private String name;

    /**
     * 直线距离米。
     */
    private Long distanceM;

    /**
     * 估算流量 L/s。
     */
    private Double estimateFlow;
}
