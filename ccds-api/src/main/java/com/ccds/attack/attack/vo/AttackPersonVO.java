package com.ccds.attack.attack.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人员卡片视图。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackPersonVO {

    /**
     * 卡片主键。
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
     * 最近确认压力锚点。前端以该值和锚点时间重建倒计时，避免重复衰减。
     */
    private BigDecimal pressureAnchor;

    /**
     * 最近确认压力时间。
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
     * 累计暂停秒数。
     */
    private Integer pausedSeconds;

    /**
     * 最近压力锚点后的累计暂停秒数。
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
     * 持久化生命周期状态：pending / in / out / scanout。
     */
    private String lifecycleStatus;

    /**
     * 估算剩余秒。预录入为 0，已撤出为空。
     */
    private Integer remainSec;

    /**
     * 服务端本次计算的风险展示态：in / warn / danger / out。
     */
    private String riskStatus;

    /**
     * 服务端运行时计算时刻。
     */
    private LocalDateTime runtimeAt;

    /**
     * 已作业秒。预录入 / 已撤出为 0。
     */
    private Integer elapsedSec;

    /**
     * 是否临时人员。
     */
    private Boolean temp;

    /**
     * 是否有高强度空呼标定。
     */
    private Boolean calibrated;

    /**
     * 身高厘米，供前端按体型估算。
     */
    private Integer heightCm;

    /**
     * 体重公斤，供前端按体型估算。
     */
    private Integer weightKg;

    /**
     * 年龄，供前端按体型估算。
     */
    private Integer age;

    /**
     * 代谢个人 K。无标定时为空，H5 倒计时与后端共用。
     */
    private BigDecimal personalK;

    /**
     * 最近客户端事件幂等键。
     */
    private String clientEventId;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
