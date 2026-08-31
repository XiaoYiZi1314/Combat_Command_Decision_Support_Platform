package com.ccds.vehicle.vehicle.constant;

/**
 * 车辆档案业务常量。
 *
 * @author ccds
 * @since 0.1.0
 */
public final class VehicleRuleConstant {

    /**
     * 车辆类型：水罐泡沫车。
     */
    public static final String TYPE_WATER_FOAM = "water_foam";

    /**
     * 车辆类型：登高平台车。
     */
    public static final String TYPE_AERIAL = "aerial";

    /**
     * 车辆类型：高喷车。
     */
    public static final String TYPE_HIGH_SPRAY = "high_spray";

    /**
     * 车辆类型：抢险救援车。
     */
    public static final String TYPE_RESCUE = "rescue";

    /**
     * 车辆类型：运兵车。
     */
    public static final String TYPE_TROOP_TRANSPORT = "troop_transport";

    /**
     * 车辆类型：供气车。
     */
    public static final String TYPE_AIR_SUPPLY = "air_supply";

    /**
     * 车辆类型：远程供水车。
     */
    public static final String TYPE_LONG_SUPPLY = "long_supply";

    /**
     * 车辆类型：干粉车。
     */
    public static final String TYPE_DRY_POWDER = "dry_powder";

    /**
     * 车辆类型：排烟车。
     */
    public static final String TYPE_SMOKE_EXHAUST = "smoke_exhaust";

    /**
     * 车辆类型：通信指挥车。
     */
    public static final String TYPE_COMMAND = "command";

    /**
     * 车辆类型：宣传车。
     */
    public static final String TYPE_PUBLICITY = "publicity";

    /**
     * 车辆类型：其他车辆。
     */
    public static final String TYPE_OTHER = "other";

    /**
     * 状态：执勤。
     */
    public static final String STATUS_ON_DUTY = "执勤";

    /**
     * 状态：报修。
     */
    public static final String STATUS_REPAIR = "报修";

    /**
     * 变更动作：新增。
     */
    public static final String ACTION_ADD = "add";

    /**
     * 变更动作：修改。
     */
    public static final String ACTION_MODIFY = "modify";

    /**
     * 变更动作：删除。
     */
    public static final String ACTION_DELETE = "delete";

    /**
     * 申请状态：待审批。
     */
    public static final String REQUEST_PENDING = "pending";

    /**
     * 申请状态：已通过。
     */
    public static final String REQUEST_APPROVED = "approved";

    /**
     * 申请状态：已驳回。
     */
    public static final String REQUEST_REJECTED = "rejected";

    /**
     * 合法车辆类型码集合，与前端 VEHICLE_TYPES 保持一致。
     */
    public static final java.util.Set<String> VALID_TYPES = java.util.Set.of(
            TYPE_WATER_FOAM, TYPE_AERIAL, TYPE_HIGH_SPRAY, TYPE_RESCUE,
            TYPE_TROOP_TRANSPORT, TYPE_AIR_SUPPLY, TYPE_LONG_SUPPLY,
            TYPE_DRY_POWDER, TYPE_SMOKE_EXHAUST, TYPE_COMMAND,
            TYPE_PUBLICITY, TYPE_OTHER);

    /**
     * 车辆类型码不合法提示。
     */
    public static final String MSG_TYPE_INVALID = "车辆类型不合法";

    /**
     * 号牌最长。
     */
    public static final int PLATE_MAX_LENGTH = 30;

    /**
     * 具体类型最长。
     */
    public static final int VEHICLE_TYPE_MAX_LENGTH = 50;

    /**
     * 备注最长。
     */
    public static final int NOTES_MAX_LENGTH = 500;

    /**
     * 号牌重复提示。
     */
    public static final String MSG_PLATE_DUPLICATE = "该号牌车辆已存在";

    /**
     * 未登录提示。
     */
    public static final String MSG_UNAUTHORIZED = "请先登录";

    /**
     * 无权提示。
     */
    public static final String MSG_FORBIDDEN = "无权操作该站车辆";

    /**
     * 未找到提示。
     */
    public static final String MSG_NOT_FOUND = "车辆不存在";

    private VehicleRuleConstant() {
    }
}
