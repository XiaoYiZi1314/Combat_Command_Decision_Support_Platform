package com.ccds.water.water.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水源档案。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterSourceDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属站。
     */
    private Long stationId;

    /**
     * 名称。
     */
    private String name;

    /**
     * 类型码。
     */
    private String type;

    /**
     * 状态码。
     */
    private String status;

    /**
     * 地址。
     */
    private String address;

    /**
     * 经度，WGS84。
     */
    private BigDecimal lng;

    /**
     * 纬度，WGS84。
     */
    private BigDecimal lat;

    /**
     * 备注与扩展（纯文本备注）。
     */
    private String extraJson;

    /**
     * 软删时间。
     */
    private LocalDateTime deletedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;

    /**
     * 水源编号，站内唯一。
     */
    private String waterNo;

    /**
     * 明确要求保留 1是0否。
     */
    private Integer retained;

    /**
     * 来源市政底表编号。
     */
    private String baseNo;

    /**
     * 设置方式 above/under。
     */
    private String setup;

    /**
     * 管网直径 mm。
     */
    private Integer pipeDiameter;

    /**
     * 管网压力 MPa。
     */
    private BigDecimal pressureMpa;

    /**
     * 管网流量 L/s。
     */
    private BigDecimal flowLps;

    /**
     * 给水形式 ring/branch。
     */
    private String supplyForm;

    /**
     * 加水高度 m。
     */
    private BigDecimal addHeightM;

    /**
     * 可使用 good/bad。
     */
    private String usable;

    /**
     * 无法出水原因原文。
     */
    private String reasonText;

    /**
     * 九原因码。
     */
    private String reasonCode;

    /**
     * 八部件码。
     */
    private String partCode;

    /**
     * 是否报修 1是0否。
     */
    private Integer isRepair;

    /**
     * 报修原因说明。
     */
    private String repairReason;

    /**
     * 泄水阀是否可查看 yes/no。
     */
    private String drainViewable;

    /**
     * 泄水阀好用情况 good/bad。
     */
    private String drainUsable;

    /**
     * 无法查看泄水阀原因。
     */
    private String drainReason;

    /**
     * 泄水阀备注。
     */
    private String drainNotes;

    /**
     * 是否有保暖措施 1是0否。
     */
    private Integer warmthMeasure;

    /**
     * 无保暖措施原因。
     */
    private String warmthReason;
}
