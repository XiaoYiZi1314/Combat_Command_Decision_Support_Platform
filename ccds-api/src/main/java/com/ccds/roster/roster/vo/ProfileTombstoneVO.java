package com.ccds.roster.roster.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人员删除记录视图。
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileTombstoneVO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 档案号。
     */
    private String archiveNo;

    /**
     * 姓名。
     */
    private String name;

    /**
     * NFC 编号。
     */
    private String nfcTag;

    /**
     * 来源：user_delete / transfer_out / legacy_import。
     */
    private String source;

    /**
     * 姓名是否仍被拦截。
     */
    private Boolean nameBlocked;

    /**
     * NFC 是否仍被拦截。
     */
    private Boolean nfcBlocked;

    /**
     * 档案号是否仍被拦截。
     */
    private Boolean archiveBlocked;

    /**
     * 删除时间。
     */
    private LocalDateTime gmtCreate;
}
