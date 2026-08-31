package com.ccds.vehicle.vehicle.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 车辆档案实体。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属消防站 ID。
     */
    private Long stationId;

    /**
     * 车辆类型码。
     */
    private String type;

    /**
     * 具体类型名称。
     */
    private String vehicleType;

    /**
     * 转改后号牌。
     */
    private String plate;

    /**
     * 原号牌。
     */
    private String oldPlate;

    /**
     * 状态：执勤/报修。
     */
    private String status;

    /**
     * 厂牌型号。
     */
    private String model;

    /**
     * 生产厂家。
     */
    private String maker;

    /**
     * 载水量(t)。
     */
    private String waterCap;

    /**
     * 载泡沫量(t)。
     */
    private String foamCap;

    /**
     * 载干粉量(t)。
     */
    private String powderCap;

    /**
     * 举高高度(m)。
     */
    private String workHeight;

    /**
     * 发动机号。
     */
    private String engineNo;

    /**
     * 车辆识别代号。
     */
    private String vin;

    /**
     * 车体颜色。
     */
    private String color;

    /**
     * 出厂日期，原样文本。
     */
    private String madeDate;

    /**
     * 装备时间，原样文本。
     */
    private String equipDate;

    /**
     * 备注。
     */
    private String notes;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
