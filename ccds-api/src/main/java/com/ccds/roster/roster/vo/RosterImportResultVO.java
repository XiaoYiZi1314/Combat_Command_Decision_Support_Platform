package com.ccds.roster.roster.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 花名册导入结果。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RosterImportResultVO {

    /**
     * 新增人数。
     */
    private Integer addedCount;

    /**
     * 按姓名更新人数。
     */
    private Integer updatedCount;

    /**
     * 跳过行数。
     */
    private Integer skippedCount;

    /**
     * 新增编组数。
     */
    private Integer addedGroupCount;

    /**
     * 是否保留了原指挥组。
     */
    private Boolean commandGroupPreserved;
}
