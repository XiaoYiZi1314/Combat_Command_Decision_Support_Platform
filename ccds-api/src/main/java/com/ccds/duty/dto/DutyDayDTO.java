package com.ccds.duty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
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
}
