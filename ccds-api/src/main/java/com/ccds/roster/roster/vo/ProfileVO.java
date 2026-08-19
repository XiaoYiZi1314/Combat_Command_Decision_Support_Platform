package com.ccds.roster.roster.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 人员档案视图。电话明文仅站级可写账号返回，其余只给脱敏值。日志禁止打印。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "phone")
public class ProfileVO {

    /**
     * 档案主键。
     */
    private Long id;

    /**
     * 所属站。
     */
    private Long stationId;

    /**
     * 所属站名。
     */
    private String stationName;

    /**
     * 姓名。
     */
    private String name;

    /**
     * 职务。
     */
    private String title;

    /**
     * 衔级。
     */
    private String rankName;

    /**
     * 电话明文。
     */
    private String phone;

    /**
     * 电话脱敏值。
     */
    private String phoneMasked;

    /**
     * 气瓶规格。
     */
    private String cylType;

    /**
     * NFC 编号。
     */
    private String nfcTag;

    /**
     * 身高厘米。
     */
    private Integer heightCm;

    /**
     * 体重公斤。
     */
    private Integer weightKg;

    /**
     * 年龄。
     */
    private Integer age;

    /**
     * 早检压力 MPa。
     */
    private BigDecimal morningPressure;

    /**
     * 空呼标定条数。
     */
    private Integer calibrationCount;

    /**
     * 空呼标定摘要。
     */
    private List<ScbaCalibrationVO> calibrations;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
