package com.ccds.attack.attack.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内攻人员卡片。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackPersonDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属站。
     */
    private Long stationId;

    /**
     * 花名册档案，临时人员为空。
     */
    private Long profileId;

    /**
     * 展示姓名。
     */
    private String displayName;

    /**
     * 编组名。
     */
    private String groupName;

    /**
     * 气瓶规格。
     */
    private String cylType;

    /**
     * 入场压力。
     */
    private BigDecimal initPressure;

    /**
     * 当前压力。
     */
    private BigDecimal currentPressure;

    /**
     * 最近确认压力的锚点时间。
     */
    private LocalDateTime manualPressureAt;

    /**
     * 入场时间。
     */
    private LocalDateTime enteredAt;

    /**
     * 撤出时间。
     */
    private LocalDateTime withdrawnAt;

    /**
     * 暂停开始时间，空表示未暂停。
     */
    private LocalDateTime timerPausedAt;

    /**
     * 已累计暂停秒数。
     */
    private Integer pausedSeconds;

    /**
     * 最近压力锚点后的累计暂停秒数，用于避免复测后重复扣除历史暂停。
     */
    private Integer pressurePausedSeconds;

    /**
     * 作业强度。
     */
    private String workLevel;

    /**
     * 作业场景。
     */
    private String scene;

    /**
     * 状态。
     */
    private String status;

    /**
     * 剩余秒。
     */
    private Integer remainSec;

    /**
     * 最近运行时剩余时间锚点秒数。
     */
    private Integer remainAnchorSec;

    /**
     * 当前请求计算出的压力，仅用于视图。
     */
    private BigDecimal runtimePressure;

    /**
     * 当前请求计算出的风险展示态，仅用于视图。
     */
    private String runtimeStatus;

    /**
     * 客户端事件幂等键。
     */
    private String clientEventId;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
