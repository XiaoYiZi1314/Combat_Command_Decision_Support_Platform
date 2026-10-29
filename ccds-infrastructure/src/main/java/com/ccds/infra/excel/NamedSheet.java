package com.ccds.infra.excel;

/**
 * 命名工作表，供多 sheet 导出使用。
 *
 * @author ccds
 * @since 0.1.2
 */
public class NamedSheet {

    private final String name;

    private final SpreadsheetTable table;

    /**
     * 构造命名工作表。
     *
     * @param name  表名
     * @param table 行数据
     */
    public NamedSheet(String name, SpreadsheetTable table) {
        this.name = name;
        this.table = table;
    }

    /**
     * 表名。
     *
     * @return 表名
     */
    public String getName() {
        return name;
    }

    /**
     * 行数据。
     *
     * @return 表格
     */
    public SpreadsheetTable getTable() {
        return table;
    }
}
