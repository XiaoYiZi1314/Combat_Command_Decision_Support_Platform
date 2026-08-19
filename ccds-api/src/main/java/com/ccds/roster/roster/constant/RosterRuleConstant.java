package com.ccds.roster.roster.constant;

/**
 * 花名册字段与导入规则。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class RosterRuleConstant {

    /**
     * 姓名最大长度。
     */
    public static final int NAME_MAX_LENGTH = 10;

    /**
     * 职务最大长度。
     */
    public static final int TITLE_MAX_LENGTH = 32;

    /**
     * 衔级最大长度。
     */
    public static final int RANK_MAX_LENGTH = 32;

    /**
     * 电话最大长度。
     */
    public static final int PHONE_MAX_LENGTH = 20;

    /**
     * NFC 编号最大长度。
     */
    public static final int NFC_MAX_LENGTH = 64;

    /**
     * 编组名最大长度。
     */
    public static final int GROUP_NAME_MAX_LENGTH = 32;

    /**
     * 组内角色最大长度。
     */
    public static final int ROLE_IN_GROUP_MAX_LENGTH = 32;

    /**
     * 组内角色：组长。
     */
    public static final String ROLE_LEADER = "组长";

    /**
     * 组内角色：组员。
     */
    public static final String ROLE_MEMBER = "组员";

    /**
     * 组内角色：站指挥员。
     */
    public static final String ROLE_STATION_COMMANDER = "站指挥员";

    /**
     * 身高上限厘米。
     */
    public static final int HEIGHT_MAX_CM = 250;

    /**
     * 体重上限公斤。
     */
    public static final int WEIGHT_MAX_KG = 200;

    /**
     * 年龄上限。
     */
    public static final int AGE_MAX = 80;

    /**
     * 早检压力上限 MPa。
     */
    public static final String MORNING_PRESSURE_MAX = "30.0";

    /**
     * 6.8L 气瓶。
     */
    public static final String CYL_TYPE_68 = "6.8";

    /**
     * 9L 气瓶。
     */
    public static final String CYL_TYPE_9 = "9";

    /**
     * 指挥组名。
     */
    public static final String COMMAND_GROUP_NAME = "指挥组";

    /**
     * 指挥单元名。
     */
    public static final String COMMAND_UNIT_NAME = "指挥单元";

    /**
     * 模板示例姓名，导入时跳过。
     */
    public static final String TEMPLATE_EXAMPLE_NAME = "张三";

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
    public static final String EXPORT_SHEET_NAME = "人员档案模板";

    /**
     * 本站花名册导出名。
     */
    public static final String EXPORT_FILE_PREFIX = "人员档案_战斗编组_";

    /**
     * 空模板导出名。
     */
    public static final String EXPORT_TEMPLATE_PREFIX = "人员档案_战斗编组导入模板_";

    /**
     * 导出内容类型。
     */
    public static final String EXPORT_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private RosterRuleConstant() {
    }
}
