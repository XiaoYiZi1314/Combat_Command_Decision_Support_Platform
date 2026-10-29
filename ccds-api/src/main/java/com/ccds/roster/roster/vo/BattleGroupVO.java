package com.ccds.roster.roster.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 战斗编组视图。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleGroupVO {

    /**
     * 编组主键。
     */
    private Long id;

    /**
     * 组名。
     */
    private String name;

    /**
     * 展示顺序。
     */
    private Integer sortNo;

    /**
     * 是否指挥组 / 指挥单元。
     */
    private Boolean commandGroup;

    /**
     * 固定槽位：1 指挥员 / 2 安全员 / 3 通信员，作战组为空。
     */
    private Integer slotNo;

    /**
     * 是否固定编制卡（不可删除）。
     */
    private Boolean fixed;

    /**
     * 成员。
     */
    private List<GroupMemberVO> members;
}
