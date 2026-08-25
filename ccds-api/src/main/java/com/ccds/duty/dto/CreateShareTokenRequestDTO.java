package com.ccds.duty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 创建共享令牌请求DTO
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShareTokenRequestDTO {

    /**
     * 消防站ID
     */
    @NotNull(message = "消防站ID不能为空")
    private Long stationId;

    /**
     * 有效期(小时)，默认2小时
     */
    @Min(value = 1, message = "有效期最少1小时")
    @Max(value = 72, message = "有效期最多72小时")
    private Integer expireHours;
}
