package com.ccds.attack.attack.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 空呼估算结果。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScbaEstimateResult {

    /**
     * 估算当前压力。
     */
    private BigDecimal currentPressure;

    /**
     * 估算剩余秒。预录入 / 已撤出为空。
     */
    private Integer remainSec;

    /**
     * 代谢个人 K。无有效标定时为空。
     */
    private BigDecimal personalK;
}
