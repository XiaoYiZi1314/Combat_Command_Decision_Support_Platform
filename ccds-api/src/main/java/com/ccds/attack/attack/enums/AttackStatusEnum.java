package com.ccds.attack.attack.enums;

/**
 * 人员卡片状态。
 *
 * @author ccds
 * @since 0.1.0
 */
public enum AttackStatusEnum {

    /**
     * 预录入。
     */
    PENDING("pending"),

    /**
     * 内攻中·安全。
     */
    IN("in"),

    /**
     * 内攻中·预警。
     */
    WARN("warn"),

    /**
     * 内攻中·危险。
     */
    DANGER("danger"),

    /**
     * 已撤出。
     */
    OUT("out");

    private final String code;

    AttackStatusEnum(String code) {
        this.code = code;
    }

    /**
     * 库内状态码。
     *
     * @return 状态码
     */
    public String getCode() {
        return code;
    }

    /**
     * 按库内码解析。未知返回 null。
     *
     * @param code 状态码
     * @return 枚举，未知为 null
     */
    public static AttackStatusEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (AttackStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 是否未撤出。
     *
     * @return true 表示预录入或内攻中
     */
    public boolean isActive() {
        return this != OUT;
    }

    /**
     * 是否已入场（倒计时中）。
     *
     * @return true 表示安全/预警/危险
     */
    public boolean isInside() {
        return this == IN || this == WARN || this == DANGER;
    }
}
