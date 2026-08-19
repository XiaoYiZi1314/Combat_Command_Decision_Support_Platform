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
     * 剩余秒。
     */
    private Integer remainSec;

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
