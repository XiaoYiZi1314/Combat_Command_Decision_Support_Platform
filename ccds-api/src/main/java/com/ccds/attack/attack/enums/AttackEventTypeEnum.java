package com.ccds.attack.attack.enums;

/**
 * 内攻事件类型。
 *
 * @author ccds
 * @since 0.1.0
 */
public enum AttackEventTypeEnum {

    /**
     * 预录入。
     */
    PRE_ADD("pre_add"),

    /**
     * 入场。
     */
    ENTER("enter"),

    /**
     * 复测压力。
     */
    REMEASURE("remeasure"),

    /**
     * 撤出。
     */
    WITHDRAW("withdraw"),

    /**
     * 删除预录入或误录。
     */
    DELETE("delete");

    private final String code;

    AttackEventTypeEnum(String code) {
        this.code = code;
    }

    /**
     * 库内事件码。
     *
     * @return 事件码
     */
    public String getCode() {
        return code;
    }

    /**
     * 按库内码解析。未知返回 null。
     *
     * @param code 事件码
     * @return 枚举，未知为 null
     */
    public static AttackEventTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (AttackEventTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}
