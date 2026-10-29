package com.ccds.attack.attack.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站级作业模式（测试/训练/出警）。
 *
 * @author ccds
 * @since 0.1.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationWorkModeDO {

    /**
     * 消防站主键。
     */
    private Long stationId;

    /**
     * 作业模式 test/training/dispatch。
     */
    private String workMode;

    /**
     * 最后修改人账号主键。
     */
    private Long updatedBy;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间。
     */
    private LocalDateTime gmtModified;
}
