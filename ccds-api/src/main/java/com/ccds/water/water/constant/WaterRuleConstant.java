package com.ccds.water.water.constant;

/**
 * 水源字段与导入规则。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class WaterRuleConstant {

    /**
     * 水源名称最大长度。
     */
    public static final int NAME_MAX_LENGTH = 64;

    /**
     * 地址最大长度。
     */
    public static final int ADDRESS_MAX_LENGTH = 128;

    /**
     * 备注最大长度。
     */
    public static final int NOTES_MAX_LENGTH = 500;

    /**
     * extra_json 最大长度。
     */
    public static final int EXTRA_MAX_LENGTH = 2000;

    /**
     * 附近水源默认半径米。
     */
    public static final int NEARBY_DEFAULT_RADIUS_M = 3000;

    /**
     * 附近水源半径上限米。
     */
    public static final int NEARBY_MAX_RADIUS_M = 10000;

    /**
     * 附近水源默认条数。
     */
    public static final int NEARBY_DEFAULT_LIMIT = 30;

    /**
     * 附近水源条数上限。
     */
    public static final int NEARBY_MAX_LIMIT = 50;

    /**
     * 全市扫描条数上限（nearby 内存过滤前的加载上限）。
     */
    public static final int NEARBY_SCAN_MAX = 5000;

    /**
     * 导入文件最大字节。
     */
    public static final long IMPORT_MAX_BYTES = 2_097_152L;

    /**
     * 导入最大行数（含表头）。
     */
    public static final int IMPORT_MAX_ROWS = 500;

    /**
     * 导出工作表名。
     */
    public static final String EXPORT_SHEET_NAME = "水源检查记录表";

    /**
     * 本站水源导出名。
     */
    public static final String EXPORT_FILE_PREFIX = "水源检查记录_";

    /**
     * 空模板导出名。
     */
    public static final String EXPORT_TEMPLATE_PREFIX = "水源检查记录导入模板_";

    /**
     * 导出内容类型。
     */
    public static final String EXPORT_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * 模板示例名称（导入时跳过示例行）。
     */
    public static final String TEMPLATE_EXAMPLE_NAME = "示例";

    /**
     * 水源估算流量：消防水鹤 L/s。
     */
    public static final double ESTIMATE_FLOW_CRANE = 45.0;

    /**
     * 水源估算流量：消火栓 L/s。
     */
    public static final double ESTIMATE_FLOW_HYDRANT = 15.0;

    /**
     * 水源估算流量：天然/取水 L/s。
     */
    public static final double ESTIMATE_FLOW_NATURAL = 30.0;

    /**
     * 水源估算流量：其他 L/s。
     */
    public static final double ESTIMATE_FLOW_OTHER = 10.0;

    private WaterRuleConstant() {
    }
}
