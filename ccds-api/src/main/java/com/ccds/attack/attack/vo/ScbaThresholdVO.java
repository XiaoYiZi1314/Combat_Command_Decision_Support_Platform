package com.ccds.attack.attack.vo;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站级空呼阈值。共享页与指挥端都按这一份阈值算安全 / 预警 / 危险，不再各自用固定默认值。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScbaThresholdVO {

    /**
     * 压力预警阈值 MPa。
     */
    private BigDecimal warnPressure;

    /**
     * 压力危险阈值 MPa，必须低于预警阈值。
     */
    private BigDecimal dangerPressure;

    /**
     * 时间预警阈值分钟。
     */
    private Integer warnTimeMin;

    /**
     * 时间危险阈值分钟，必须低于预警阈值。
     */
    private Integer dangerTimeMin;

    /**
     * 默认作业强度 light / moderate / heavy。
     */
    private String defaultWorkLevel;

    /**
     * 是否开启语音提醒。
     */
    private Boolean voiceEnabled;
}
