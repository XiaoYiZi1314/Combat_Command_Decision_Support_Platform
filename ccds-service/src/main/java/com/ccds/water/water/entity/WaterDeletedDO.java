package com.ccds.water.water.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已删除水源记录。
 *
 * <p>删除时先写本表再软删。任何同步、市政底表合并、表格导入都必须按编号拦截，
 * 防止删掉的卡被旧目录灌回来。本表只增不删，只有用户主动新建或审批通过新增才解除。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterDeletedDO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 所属站主键。
     */
    private Long stationId;

    /**
     * 已删除水源编号。
     */
    private String waterNo;

    /**
     * 删除时名称。
     */
    private String name;

    /**
     * 删除时类型。
     */
    private String type;

    /**
     * 来源：user_delete / approval / legacy_import。
     */
    private String source;

    /**
     * 解除时间；空表示仍生效。
     */
    private LocalDateTime releasedAt;

    /**
     * 解除人账号主键。
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
