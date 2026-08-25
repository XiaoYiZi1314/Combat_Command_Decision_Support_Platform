package com.ccds.water.water.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ccds.water.water.constant.WaterRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新建或更新水源档案。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterSaveCommand {

    /**
     * 名称。
     */
    @NotBlank
    @Size(max = WaterRuleConstant.NAME_MAX_LENGTH)
    private String name;

    /**
     * 类型码：crane / ground / underground。
     */
    @NotBlank
    @Pattern(regexp = "^(crane|ground|underground)$")
    private String type;

    /**
     * 地址。
     */
    @Size(max = WaterRuleConstant.ADDRESS_MAX_LENGTH)
    private String address;

    /**
     * 经度，WGS84。
     */
    @NotNull
    @DecimalMin("73.0")
    @DecimalMax("136.0")
    private BigDecimal lng;

    /**
     * 纬度，WGS84。
     */
    @NotNull
    @DecimalMin("3.0")
    @DecimalMax("54.0")
    private BigDecimal lat;

    /**
     * 状态码：active / repair。
     */
    @NotBlank
    @Pattern(regexp = "^(active|repair)$")
    private String status;

    /**
     * 备注。
     */
    @Size(max = WaterRuleConstant.NOTES_MAX_LENGTH)
    private String notes;
}
