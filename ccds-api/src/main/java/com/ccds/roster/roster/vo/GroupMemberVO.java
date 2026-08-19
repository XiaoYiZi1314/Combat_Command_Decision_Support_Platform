package com.ccds.roster.roster.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 编组成员视图。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberVO {

    /**
     * 档案主键。
     */
    private Long profileId;

    /**
     * 姓名。
     */
    private String name;

    /**
     * 组内角色。
     */
    private String roleInGroup;
}
