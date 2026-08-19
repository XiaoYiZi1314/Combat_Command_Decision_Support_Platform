package com.ccds.attack.attack.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ccds.attack.attack.constant.AttackRuleConstant;
import com.ccds.roster.roster.constant.RosterRuleConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内攻写事件。预录入 / 入场 / 复测 / 撤出 / 删除共用。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackEventCommand {

    /**
     * 客户端幂等键。
     */
    @NotBlank
    @Size(max = AttackRuleConstant.EVENT_ID_MAX_LENGTH)
    private String eventId;

    /**
     * 事件类型：pre_add / enter / remeasure / withdraw / delete。
     */
    @NotBlank
    @Pattern(regexp = "^(pre_add|enter|remeasure|withdraw|delete)$")
    private String type;

    /**
     * 已有卡片主键。预录入可空。
     */
    private Long personId;

    /**
     * 花名册档案。临时人员为空。
     */
    private Long profileId;

    /**
     * 展示姓名。临时人员必填。
     */
    @Size(max = AttackRuleConstant.NAME_MAX_LENGTH)
    private String displayName;

    /**
     * 编组名。
     */
    @Size(max = AttackRuleConstant.GROUP_NAME_MAX_LENGTH)
    private String groupName;

    /**
     * 气瓶规格，6.8 或 9。
     */
    @Pattern(regexp = "^(6\\.8|9)[Ll]?$")
    private String cylType;

    /**
     * 压力 MPa。
     */
    @DecimalMin(AttackRuleConstant.PRESSURE_MIN)
    @DecimalMax(AttackRuleConstant.PRESSURE_MAX)
    private BigDecimal pressure;

    /**
     * 作业强度。
     */
    @Pattern(regexp = "^(light|moderate|heavy)$")
    private String workLevel;

    /**
     * 作业场景。
     */
    @Pattern(regexp = "^(flat|highrise|dark|large|search)$")
    private String scene;

    /**
     * NFC 编号，预录入时可按本站花名册匹配。
     */
    @Size(max = RosterRuleConstant.NFC_MAX_LENGTH)
    private String nfcTag;
}
