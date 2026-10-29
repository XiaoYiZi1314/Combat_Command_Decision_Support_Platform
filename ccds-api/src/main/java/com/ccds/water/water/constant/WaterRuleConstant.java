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
     * 原因类文本最大长度（无法出水原因 / 报修说明 / 泄水阀与保暖说明）。
     */
    public static final int REASON_MAX_LENGTH = 255;

    /**
     * 水源编号最大长度。
     */
    public static final int WATER_NO_MAX_LENGTH = 32;

    /**
     * 水管网直径上限 mm。
     */
    public static final int PIPE_DIAMETER_MAX = 2000;

    /**
     * 管网压力上限 MPa。
     */
    public static final double PRESSURE_MAX_MPA = 30.0;

    /**
     * 管网流量上限 L/s。
     */
    public static final double FLOW_MAX_LPS = 500.0;

    /**
     * 加水高度上限 m。
     */
    public static final double ADD_HEIGHT_MAX_M = 100.0;

    /**
     * 月度统计标题格式中的「截止日」文案前缀。
     */
    public static final String STATS_TITLE_PREFIX = "截止";

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
     * 导入最大行数（含表头）。全市明细单站分表可达千行量级。
     */
    public static final int IMPORT_MAX_ROWS = 2000;

    /**
     * 检查表标准列数（23 列）。
     */
    public static final int INSPECTION_COLUMN_COUNT = 23;

    /**
     * 类型统计表数量：消防水鹤 / 地上消火栓 / 地下消火栓。
     */
    public static final int TYPE_STATS_SHEET_COUNT = 3;

    /**
     * 出水视频时长上限毫秒（10 秒）。超过即拒收。
     */
    public static final int VIDEO_MAX_DURATION_MS = 10_000;

    /**
     * 出水视频判定结果：通过。
     */
    public static final String VIDEO_PASSED = "passed";

    /**
     * 出水视频判定结果：不通过。
     */
    public static final String VIDEO_REJECTED = "rejected";

    /**
     * 出水视频判定结果：失败。
     */
    public static final String VIDEO_FAILED = "failed";

    /**
     * 出水视频提示文案：时长超限。
     */
    public static final String MSG_VIDEO_TOO_LONG = "出水视频限 10 秒，当前 ";

    /**
     * 出水视频提示文案：不通过须给原因。
     */
    public static final String MSG_VIDEO_REASON_REQUIRED = "判定不通过时必须填写原因";

    /**
     * 汇总统计导出名。
     */
    public static final String EXPORT_SUMMARY_PREFIX = "水源汇总统计_";

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
     * 水源估算流量：其他 L/s。
     */
    public static final double ESTIMATE_FLOW_OTHER = 10.0;

    private WaterRuleConstant() {
    }
}
