package com.ccds.attack.attack.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 本站内攻主界面数据：卡片 + 快照。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationAttackVO {

    /**
     * 站主键。
     */
    private Long stationId;

    /**
     * 站名。
     */
    private String stationName;

    /**
     * 所属大队名，支队直属为空。
     */
    private String brigadeName;

    /**
     * 是否可写（站级本站）。
     */
    private Boolean writable;

    /**
     * 人员卡片。
     */
    private List<AttackPersonVO> persons;

    /**
     * 本站快照。
     */
    private AttackSnapshotVO snapshot;
}
