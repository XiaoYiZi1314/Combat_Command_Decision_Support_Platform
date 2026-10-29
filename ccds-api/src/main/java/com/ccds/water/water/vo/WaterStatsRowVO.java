package com.ccds.water.water.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水源按单位汇总行。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterStatsRowVO {

    /**
     * 单位主键。
     */
    private Long stationId;

    /**
     * 单位名称。
     */
    private String stationName;

    /**
     * 大队名称。
     */
    private String brigadeName;

    /**
     * 总数。
     */
    private Integer total;

    /**
     * 好用数。
     */
    private Integer good;

    /**
     * 好用率百分比，保留一位小数。
     */
    private Double goodRate;

    /**
     * 不好用数。
     */
    private Integer bad;

    /**
     * 地上数量（消火栓行用）。
     */
    private Integer groundTotal;

    /**
     * 地上好用数。
     */
    private Integer groundGood;

    /**
     * 地上不好用数。
     */
    private Integer groundBad;

    /**
     * 地下数量（消火栓行用）。
     */
    private Integer undergroundTotal;

    /**
     * 地下好用数。
     */
    private Integer undergroundGood;

    /**
     * 地下不好用数。
     */
    private Integer undergroundBad;

    /**
     * 九原因个数。
     */
    private WaterReasonStatsVO reasons;
}
