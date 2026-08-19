package com.ccds.roster.roster.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 战斗编组成员。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleGroupMemberDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 编组主键。
     */
    private Long groupId;

    /**
     * 档案主键。
     */
    private Long profileId;

    /**
     * 组内角色。
     */
    private String roleInGroup;

    /**
     * 成员姓名（联查）。
     */
    private String profileName;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
