package com.ccds.water.water.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ccds.duty.dto.FileObjectDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 水源档案视图。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterVO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 水源编号。
     */
    private String waterNo;

    /**
     * 所属站。
     */
    private Long stationId;

    /**
     * 所属站名。
     */
    private String stationName;

    /**
     * 名称。
     */
    private String name;

    /**
     * 类型码。
     */
    private String type;

    /**
     * 类型展示名。
     */
    private String typeLabel;

    /**
     * 地址。
     */
    private String address;

    /**
     * 经度。
     */
    private BigDecimal lng;

    /**
     * 纬度。
     */
    private BigDecimal lat;

    /**
     * 状态码。
     */
    private String status;

    /**
     * 状态展示名。
     */
    private String statusLabel;

    /**
     * 备注。
     */
    private String notes;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;

    /**
     * 是否不好用。按唯一谓词派生，界面配色、筛选与统计都用它，不要另算。
     */
    private Boolean bad;

    /**
     * 设置方式 above/under。
     */
    private String setup;

    /**
     * 设置方式展示名。
     */
    private String setupLabel;

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
     * 给水形式展示名。
     */
    private String supplyFormLabel;

    /**
     * 加水高度 m。
     */
    private BigDecimal addHeightM;

    /**
     * 可使用 good/bad。
     */
    private String usable;

    /**
     * 可使用展示名。
     */
    private String usableLabel;

    /**
     * 无法出水原因原文。
     */
    private String reasonText;

    /**
     * 九原因码。
     */
    private String reasonCode;

    /**
     * 九原因展示名。
     */
    private String reasonLabel;

    /**
     * 八部件码。
     */
    private String partCode;

    /**
     * 八部件展示名。
     */
    private String partLabel;

    /**
     * 是否报修。
     */
    private Boolean repair;

    /**
     * 报修原因说明。
     */
    private String repairReason;

    /**
     * 明确要求保留。
     */
    private Boolean retained;

    /**
     * 待审批状态标记：站级提交了尚未通过的申请。
     */
    private Boolean pendingApproval;

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
     * 是否有保暖措施。
     */
    private Boolean warmthMeasure;

    /**
     * 无保暖措施原因。
     */
    private String warmthReason;

    /**
     * 水源照片，仅详情返回。
     */
    private List<FileObjectDTO> photos;

    /**
     * 水源档案材料，仅详情返回。
     */
    private List<FileObjectDTO> docs;
}
