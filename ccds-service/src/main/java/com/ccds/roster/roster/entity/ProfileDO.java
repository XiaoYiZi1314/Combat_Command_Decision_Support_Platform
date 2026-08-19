package com.ccds.roster.roster.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 人员档案。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "phoneCipher")
public class ProfileDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属站。
     */
    private Long stationId;

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
     * 电话密文。
     */
    private String phoneCipher;

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
     * 早检压力。
     */
    private BigDecimal morningPressure;

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
     * 空呼标定条数（联查）。
     */
    private Integer calibrationCount;
}
