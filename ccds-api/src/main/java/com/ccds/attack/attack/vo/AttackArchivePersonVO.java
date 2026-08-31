package com.ccds.attack.attack.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 内攻历史归档人员快照 VO。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackArchivePersonVO {

    /**
     * 姓名或编号。
     */
    private String name;

    /**
     * 编组名。
     */
    private String group;

    /**
     * 气瓶容积(L)。
     */
    private String cylType;

    /**
     * 初始压力(MPa)。
     */
    private BigDecimal initPressure;

    /**
     * 最终压力(MPa)。
     */
    private BigDecimal finalPressure;

    /**
     * 入场时间。
     */
    private String enterTime;

    /**
     * 出场时间。
     */
    private String exitTime;

    /**
     * 强度：low/moderate/high。
     */
    private String workLevel;

    /**
     * 场景：flat/high-rise/dark/long-span/rescue。
     */
    private String scene;

    /**
     * 最终状态。
     */
    private String status;
}
