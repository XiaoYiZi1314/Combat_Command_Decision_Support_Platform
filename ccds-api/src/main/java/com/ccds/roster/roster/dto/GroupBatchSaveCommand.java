package com.ccds.roster.roster.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 整存本站战斗编组。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupBatchSaveCommand {

    /**
     * 编组列表，覆盖本站全部编组。
     */
    @NotNull
    @Valid
    private List<GroupSaveCommand> groups;
}
