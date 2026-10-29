package com.ccds.attack.attack.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站级空呼阈值（领域模型）。
 *
 * <p>判定安全 / 预警 / 危险必须用这份阈值：站上改过的值随实时状态下发，
 * 共享页与指挥端不再各用一套固定默认值。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScbaThresholds {

    /**
     * 压力预警阈值 MPa。
     */
    private BigDecimal warnPressure;

    /**
     * 压力危险阈值 MPa。
     */
    private BigDecimal dangerPressure;

    /**
     * 时间预警阈值秒。
     */
    private Integer warnTimeSec;

    /**
     * 时间危险阈值秒。
     */
    private Integer dangerTimeSec;

    /**
     * 默认作业强度。
     */
    private String defaultWorkLevel;

    /**
     * 是否开启语音提醒。
     */
    private Boolean voiceEnabled;
}
