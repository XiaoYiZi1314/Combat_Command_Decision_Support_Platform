package com.ccds.water.water.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水源档案视图。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterVO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属站。
     */
    private Long stationId;

    /**
     * 所属站名。
     */
    private String stationName;

    /**
     * 名称。
     */
    private String name;

    /**
     * 类型码。
     */
    private String type;

    /**
     * 类型展示名。
     */
    private String typeLabel;

    /**
     * 地址。
     */
    private String address;

    /**
     * 经度。
     */
    private BigDecimal lng;

    /**
     * 纬度。
     */
    private BigDecimal lat;

    /**
     * 状态码。
     */
    private String status;

    /**
     * 状态展示名。
     */
    private String statusLabel;

    /**
     * 备注。
     */
    private String notes;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
