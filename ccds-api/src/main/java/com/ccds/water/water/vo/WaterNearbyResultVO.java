package com.ccds.water.water.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 附近水源结果。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterNearbyResultVO {

    /**
     * 检索半径米。
     */
    private Integer radiusM;

    /**
     * 命中条数。
     */
    private Integer count;

    /**
     * 按距离升序的水源。
     */
    private List<WaterNearbyVO> waters;
}
