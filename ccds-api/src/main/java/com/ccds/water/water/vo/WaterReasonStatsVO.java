package com.ccds.water.water.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 九原因分布。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterReasonStatsVO {

    /**
     * 无水。
     */
    private Integer noWater;

    /**
     * 漏水。
     */
    private Integer leak;

    /**
     * 压力不足。
     */
    private Integer lowPressure;

    /**
     * 锈蚀。
     */
    private Integer rust;

    /**
     * 埋压。
     */
    private Integer buried;

    /**
     * 圈占。
     */
    private Integer occupied;

    /**
     * 挪用。
     */
    private Integer diverted;

    /**
     * 塌井。
     */
    private Integer collapsedWell;

    /**
     * 毁坏（兜底）。
     */
    private Integer damaged;

    /**
     * 九项合计。
     *
     * @return 合计
     */
    public int sum() {
        return intOf(noWater) + intOf(leak) + intOf(lowPressure) + intOf(rust) + intOf(buried)
                + intOf(occupied) + intOf(diverted) + intOf(collapsedWell) + intOf(damaged);
    }

    /**
     * 累加另一份统计。
     *
     * @param other 另一份
     */
    public void add(WaterReasonStatsVO other) {
        if (other == null) {
            return;
        }
        noWater = Integer.valueOf(intOf(noWater) + intOf(other.getNoWater()));
        leak = Integer.valueOf(intOf(leak) + intOf(other.getLeak()));
        lowPressure = Integer.valueOf(intOf(lowPressure) + intOf(other.getLowPressure()));
        rust = Integer.valueOf(intOf(rust) + intOf(other.getRust()));
        buried = Integer.valueOf(intOf(buried) + intOf(other.getBuried()));
        occupied = Integer.valueOf(intOf(occupied) + intOf(other.getOccupied()));
        diverted = Integer.valueOf(intOf(diverted) + intOf(other.getDiverted()));
        collapsedWell = Integer.valueOf(intOf(collapsedWell) + intOf(other.getCollapsedWell()));
        damaged = Integer.valueOf(intOf(damaged) + intOf(other.getDamaged()));
    }

    /**
     * 按原因码自增。
     *
     * @param reasonCode 原因码
     */
    public void increment(String reasonCode) {
        if (reasonCode == null) {
            damaged = Integer.valueOf(intOf(damaged) + 1);
            return;
        }
        switch (reasonCode) {
            case "no_water":
                noWater = Integer.valueOf(intOf(noWater) + 1);
                break;
            case "leak":
                leak = Integer.valueOf(intOf(leak) + 1);
                break;
            case "low_pressure":
                lowPressure = Integer.valueOf(intOf(lowPressure) + 1);
                break;
            case "rust":
                rust = Integer.valueOf(intOf(rust) + 1);
                break;
            case "buried":
                buried = Integer.valueOf(intOf(buried) + 1);
                break;
            case "occupied":
                occupied = Integer.valueOf(intOf(occupied) + 1);
                break;
            case "diverted":
                diverted = Integer.valueOf(intOf(diverted) + 1);
                break;
            case "collapsed_well":
                collapsedWell = Integer.valueOf(intOf(collapsedWell) + 1);
                break;
            case "damaged":
            default:
                damaged = Integer.valueOf(intOf(damaged) + 1);
                break;
        }
    }

    /**
     * 建一份全 0 统计。
     *
     * @return 统计
     */
    public static WaterReasonStatsVO zero() {
        return WaterReasonStatsVO.builder()
                .noWater(Integer.valueOf(0))
                .leak(Integer.valueOf(0))
                .lowPressure(Integer.valueOf(0))
                .rust(Integer.valueOf(0))
                .buried(Integer.valueOf(0))
                .occupied(Integer.valueOf(0))
                .diverted(Integer.valueOf(0))
                .collapsedWell(Integer.valueOf(0))
                .damaged(Integer.valueOf(0))
                .build();
    }

    private static int intOf(Integer value) {
        return value == null ? 0 : value.intValue();
    }
}
