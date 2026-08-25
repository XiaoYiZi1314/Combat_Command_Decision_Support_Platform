package com.ccds.water.water.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水源档案。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterSourceDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属站。
     */
    private Long stationId;

    /**
     * 名称。
     */
    private String name;

    /**
     * 类型码。
     */
    private String type;

    /**
     * 状态码。
     */
    private String status;

    /**
     * 地址。
     */
    private String address;

    /**
     * 经度，WGS84。
     */
    private BigDecimal lng;

    /**
     * 纬度，WGS84。
     */
    private BigDecimal lat;

    /**
     * 备注与扩展（JSON）。
     */
    private String extraJson;

    /**
     * 软删时间。
     */
    private LocalDateTime deletedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
