package com.ccds.water.water.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水源表格导入结果。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterImportResultVO {

    /**
     * 新增条数。
     */
    private Integer addedCount;

    /**
     * 跳过总数。
     */
    private Integer skippedCount;

    /**
     * 跳过：示例行。
     */
    private Integer skipExampleCount;

    /**
     * 跳过：类型无法识别。
     */
    private Integer skipTypeInvalidCount;

    /**
     * 跳过：缺少经纬度。
     */
    private Integer skipCoordMissingCount;

    /**
     * 跳过：重名。
     */
    private Integer skipDuplicateCount;
}
