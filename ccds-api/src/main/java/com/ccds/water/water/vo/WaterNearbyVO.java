package com.ccds.water.water.vo;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 附近水源条目：带直线距离与估算流量。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterNearbyVO {

    /**
     * 主键。
     */
    private Long id;

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
     * 直线距离米。
     */
    private Long distanceM;

    /**
     * 估算流量 L/s（无档案流量时的保守默认值）。
     */
    private Double estimateFlow;
}
