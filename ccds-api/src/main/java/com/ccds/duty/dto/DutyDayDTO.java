package com.ccds.duty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 值班日历DTO
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DutyDayDTO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 消防站ID，以路径为准。
     */
    private Long stationId;

    /**
     * 值班日期
     */
    @NotNull(message = "值班日期不能为空")
    private LocalDate dutyDate;

    /**
     * 主官档案ID
     */
    private Long mainOfficerId;

    /**
     * 主官姓名
     */
    private String mainOfficerName;

    /**
     * 副职档案ID
     */
    private Long deputyOfficerId;

    /**
     * 副职姓名
     */
    private String deputyOfficerName;

    /**
     * 备注
     */
    private String note;

    /**
     * 当日战斗编组快照JSON
     */
    private String groupSnapshotJson;

    /**
     * 当日车辆快照JSON
     */
    @Size(max = 2000, message = "车辆快照最多2000字符")
    private String vehicleSnapshotJson;

    /**
     * 编组写入模式：preserve 保持原快照 / replace 用本次提交覆盖。
     * 历史日期服务端强制 preserve，防止用今天的花名册覆盖那一天。
     */
    private String groupSnapshotMode;

    /**
     * 是否保留原主官与副职（页面上没有对应选框时传 true，避免存成空）。
     */
    private Boolean keepOfficers;
}
