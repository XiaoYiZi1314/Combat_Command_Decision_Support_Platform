package com.ccds.water.water.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支队用类型统计行。
 *
 * <p>某个单位总数与本月维修数都为 0 时，该单位不列出。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterTypeStatsRowVO {

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
     * 不好用数。
     */
    private Integer bad;

    /**
     * 上传出水视频数：只数在用且真有视频的卡。
     */
    private Integer videoCount;

    /**
     * 本月维修数：本月审批通过且从报修改成在用。
     */
    private Integer monthRepairCount;
}
