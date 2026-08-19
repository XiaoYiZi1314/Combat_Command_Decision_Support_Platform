package com.ccds.roster.roster.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.ccds.roster.roster.constant.RosterRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个战斗编组。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSaveCommand {

    /**
     * 已有编组主键，新建为空。
     */
    private Long id;

    /**
     * 组名。
     */
    @NotBlank
    @Size(max = RosterRuleConstant.GROUP_NAME_MAX_LENGTH)
    private String name;

    /**
     * 展示顺序。
     */
    private Integer sortNo;

    /**
     * 成员。
     */
    @Valid
    private List<GroupMemberSaveCommand> members;
}
