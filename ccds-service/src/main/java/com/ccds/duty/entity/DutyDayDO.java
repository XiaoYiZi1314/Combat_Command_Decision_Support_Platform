package com.ccds.duty.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 值班日历实体
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DutyDayDO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 消防站ID
     */
    private Long stationId;

    /**
     * 值班日期
     */
    private LocalDate dutyDate;

    /**
     * 主官档案ID
     */
    private Long mainOfficerId;

    /**
     * 副职档案ID
     */
    private Long deputyOfficerId;

    /**
     * 备注
     */
    private String note;

    /**
     * 当日战斗编组快照JSON
     */
    private String groupSnapshotJson;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;
}
