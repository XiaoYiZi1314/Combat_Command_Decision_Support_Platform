package com.ccds.duty.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 重点单位预案实体
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeyUnitDO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 所属消防站ID
     */
    private Long stationId;

    /**
     * 单位名称
     */
    private String name;

    /**
     * 地址
     */
    private String address;

    /**
     * 类别
     */
    private String category;

    /**
     * 联系人
     */
    private String contact;

    /**
     * 联系电话密文
     */
    @ToString.Exclude
    private String phoneCipher;

    /**
     * 备注
     */
    private String notes;

    /**
     * 预案正文
     */
    private String planText;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;

    /**
     * 软删除时间
     */
    private LocalDateTime deletedAt;
}
