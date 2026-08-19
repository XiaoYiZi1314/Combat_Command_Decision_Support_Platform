package com.ccds.attack.attack.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 写事件结果。重复投递返回同一卡片，不生成第二张。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackEventResultVO {

    /**
     * 是否重复投递。
     */
    private Boolean duplicate;

    /**
     * 受影响卡片。删除成功时为 null。
     */
    private AttackPersonVO person;

    /**
     * 最新本站内攻。
     */
    private StationAttackVO attack;
}
