package com.ccds.roster.roster.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 战斗编组。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleGroupDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属站。
     */
    private Long stationId;

    /**
     * 组名。
     */
    private String name;

    /**
     * 展示顺序。
     */
    private Integer sortNo;

    /**
     * 固定槽位：1 指挥员 / 2 安全员 / 3 通信员，作战组为空。
     */
    private Integer slotNo;

    /**
     * 是否固定编制卡。
     */
    private Integer isFixed;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
