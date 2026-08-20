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
     * 入场时间。
     */
    private LocalDateTime enteredAt;

    /**
     * 撤出时间。
     */
    private LocalDateTime withdrawnAt;

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
     * 估算剩余秒。预录入 / 已撤出为空。
     */
    private Integer remainSec;

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
     * 个人空呼 K 值。无标定时为空，H5 倒计时与后端共用。
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
