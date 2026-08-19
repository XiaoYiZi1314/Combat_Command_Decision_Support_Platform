package com.ccds.roster.roster.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ccds.roster.roster.constant.RosterRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 编组成员。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberSaveCommand {

    /**
     * 档案主键。
     */
    @NotNull
    private Long profileId;

    /**
     * 组内角色。
     */
    @Size(max = RosterRuleConstant.ROLE_IN_GROUP_MAX_LENGTH)
    private String roleInGroup;
}
