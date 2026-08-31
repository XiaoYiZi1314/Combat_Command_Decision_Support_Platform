package com.ccds.vehicle.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 车辆变更申请审批入参。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequestReviewDTO {

    /**
     * 是否通过。
     */
    @NotNull(message = "审批结果不能为空")
    private Boolean approve;
}
