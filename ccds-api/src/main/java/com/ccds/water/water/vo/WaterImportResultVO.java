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
     * 更新条数。
     */
    private Integer skippedCount;
}
