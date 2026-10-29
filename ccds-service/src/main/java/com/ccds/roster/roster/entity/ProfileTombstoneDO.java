package com.ccds.roster.roster.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人员删除记录（墓碑）。用于人员删除不回流、调出站后姓名与 NFC 不复用。
 *
 * @author ccds
 * @since 0.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileTombstoneDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属消防站主键。
     */
    private Long stationId;

    /**
     * 现有档案主键；迁移记录为空。
     */
    private Long profileId;

    /**
     * 旧系统档案号，如 p_1 / p_xfer_3_xxx。
     */
    private String legacyProfileId;

    /**
     * 档案号；旧系统无此概念时为空。
     */
    private String archiveNo;

    /**
     * 姓名。
     */
    private String name;

    /**
     * 姓名归一值。
     */
    private String nameNorm;

    /**
     * NFC 原始编号。
     */
    private String nfcTag;

    /**
     * NFC 归一编号（去分隔符并大写）。
     */
    private String nfcNorm;

    /**
     * 来源：user_delete/transfer_out/legacy_import。
     */
    private String source;

    /**
     * 姓名解除时间；空表示仍生效。
     */
    private LocalDateTime nameReleasedAt;

    /**
     * NFC 解除时间；空表示仍生效。
     */
    private LocalDateTime nfcReleasedAt;

    /**
     * 档案号解除时间；空表示仍生效。
     */
    private LocalDateTime archiveReleasedAt;

    /**
     * 解封人账号主键。
     */
    private Long releasedBy;

    /**
     * 创建人账号主键。
     */
    private Long createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
