package com.ccds.infra.excel;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二维表格，供花名册导入导出使用。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpreadsheetTable {

    /**
     * 行列表，每行是单元格文本。
     */
    @Builder.Default
    private List<List<String>> rows = new ArrayList<List<String>>();
}
