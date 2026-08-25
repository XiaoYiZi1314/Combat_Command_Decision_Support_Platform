package com.ccds.water.water.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站级水源列表。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationWaterVO {

    /**
     * 站主键。
     */
    private Long stationId;

    /**
     * 站名。
     */
    private String stationName;

    /**
     * 是否可写。
     */
    private Boolean writable;

    /**
     * 水源列表。
     */
    private List<WaterVO> waters;
}
