package com.ccds.attack.attack.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站级空呼阈值。
 *
 * @author ccds
 * @since 0.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationScbaSettingDO {

    /**
     * 消防站主键。
     */
    private Long stationId;

    /**
     * 压力预警阈值 MPa。
     */
    private BigDecimal warnPressure;

    /**
     * 压力危险阈值 MPa。
     */
    private BigDecimal dangerPressure;

    /**
     * 时间预警阈值分钟。
     */
    private Integer warnTimeMin;

    /**
     * 时间危险阈值分钟。
     */
    private Integer dangerTimeMin;

    /**
     * 默认作业强度 light/moderate/heavy。
     */
    private String workLevel;

    /**
     * 默认场景 flat/highrise/dark/large/search。
     */
    private String scene;

    /**
     * 语音提醒 1 开 0 关。
     */
    private Integer voiceEnabled;

    /**
     * 最后修改人账号主键。
     */
    private Long updatedBy;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
