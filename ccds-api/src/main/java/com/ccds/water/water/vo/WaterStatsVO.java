package com.ccds.water.water.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水源汇总统计。
 *
 * <p>屏幕与导出共用本 VO，保证「导出的表和屏幕上的数一致」。不变量：
 * 好用 + 不好用 = 总数；九原因合计 = 不好用数；地上 + 地下 = 消火栓总数。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterStatsVO {

    /**
     * 统计标题，如「截止 2026-10-31」。
     */
    private String title;

    /**
     * 统计月份，格式 yyyy-MM。
     */
    private String month;

    /**
     * 消防水鹤按单位汇总。
     */
    private List<WaterStatsRowVO> craneRows;

    /**
     * 消火栓按单位汇总（含地上/地下拆分）。
     */
    private List<WaterStatsRowVO> hydrantRows;

    /**
     * 消防水鹤类型统计（支队用）。
     */
    private List<WaterTypeStatsRowVO> craneTypeRows;

    /**
     * 地上消火栓类型统计（支队用）。
     */
    private List<WaterTypeStatsRowVO> groundTypeRows;

    /**
     * 地下消火栓类型统计（支队用）。
     */
    private List<WaterTypeStatsRowVO> undergroundTypeRows;

    /**
     * 九原因合计（水鹤 + 消火栓）。
     */
    private WaterReasonStatsVO reasonTotals;

    /**
     * 应急加水点数量。本期恒 0，不用消火栓填。
     */
    private Integer emergencyPoints;

    /**
     * 消防水塔数量。本期恒 0。
     */
    private Integer waterTowers;

    /**
     * 其他水源数量。本期恒 0。
     */
    private Integer otherSources;
}
