package com.ccds.roster.roster.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 战斗编组删除记录。
 *
 * <p>只记精确的组名与组编号，禁止使用会连到整类编组的通配键：
 * 删「灭火 1 组」不得连带删掉「灭火救援 1 组」。本表只增不删，只有用户重新新建同名组才解除。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleGroupTombstoneDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属消防站主键。
     */
    private Long stationId;

    /**
     * 组名原文。
     */
    private String groupName;

    /**
     * 组名归一值。
     */
    private String nameNorm;

    /**
     * 删除时组主键。
     */
    private Long groupId;

    /**
     * 槽位号；仅固定卡有值。
     */
    private Integer slotNo;

    /**
     * 来源：user_delete / legacy_bug。
     */
    private String source;

    /**
     * 解除时间；空表示仍生效。
     */
    private LocalDateTime releasedAt;

    /**
     * 解除人账号主键。
     */
    private Long releasedBy;

    /**
     * 创建人账号主键。
     */
    private Long createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
