package com.ccds.vehicle.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 车辆档案 DTO。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDTO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属消防站 ID，以路径为准。
     */
    private Long stationId;

    /**
     * 车辆类型码，见 VehicleRuleConstant TYPE_*。
     */
    @NotBlank(message = "车辆类型不能为空")
    @Size(max = 50, message = "车辆类型最多50字符")
    private String type;

    /**
     * 具体类型名称，如「水罐消防车」。
     */
    @Size(max = 50, message = "具体类型最多50字符")
    private String vehicleType;

    /**
     * 转改后号牌/地方号牌。
     */
    @Size(max = 30, message = "号牌最多30字符")
    private String plate;

    /**
     * 原号牌。
     */
    @Size(max = 30, message = "原号牌最多30字符")
    private String oldPlate;

    /**
     * 状态：执勤/报修。
     */
    @Size(max = 20, message = "状态最多20字符")
    private String status;

    /**
     * 厂牌型号。
     */
    @Size(max = 100, message = "厂牌型号最多100字符")
    private String model;

    /**
     * 生产厂家。
     */
    @Size(max = 100, message = "生产厂家最多100字符")
    private String maker;

    /**
     * 载水量(t)。
     */
    @Size(max = 20, message = "载水量最多20字符")
    private String waterCap;

    /**
     * 载泡沫量(t)。
     */
    @Size(max = 20, message = "载泡沫量最多20字符")
    private String foamCap;

    /**
     * 载干粉量(t)。
     */
    @Size(max = 20, message = "载干粉量最多20字符")
    private String powderCap;

    /**
     * 举高高度(m)。
     */
    @Size(max = 20, message = "举高高度最多20字符")
    private String workHeight;

    /**
     * 发动机号。
     */
    @Size(max = 50, message = "发动机号最多50字符")
    private String engineNo;

    /**
     * 车辆识别代号。
     */
    @Size(max = 50, message = "车辆识别代号最多50字符")
    private String vin;

    /**
     * 车体颜色。
     */
    @Size(max = 20, message = "车体颜色最多20字符")
    private String color;

    /**
     * 出厂日期，原样文本。
     */
    @Size(max = 50, message = "出厂日期最多50字符")
    private String madeDate;

    /**
     * 装备时间，原样文本。
     */
    @Size(max = 50, message = "装备时间最多50字符")
    private String equipDate;

    /**
     * 备注。
     */
    @Size(max = 500, message = "备注最多500字符")
    private String notes;

    /**
     * 站点名称（列表返回时冗余）。
     */
    private String stationName;
}
