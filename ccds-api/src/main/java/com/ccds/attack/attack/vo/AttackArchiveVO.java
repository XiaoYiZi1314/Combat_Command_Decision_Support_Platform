package com.ccds.attack.attack.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内攻历史归档 VO。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackArchiveVO {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 站主键。
     */
    private Long stationId;

    /**
     * 站名。
     */
    private String stationName;

    /**
     * 类型：drill演练 / dispatch出警。
     */
    private String eventKind;

    /**
     * 事件名称。
     */
    private String eventName;

    /**
     * 地点。
     */
    private String location;

    /**
     * 本次内攻开始时间。
     */
    private String startedAt;

    /**
     * 归档完成时间。
     */
    private String finishedAt;

    /**
     * 记录人数。
     */
    private Integer personCount;

    /**
     * 人员快照列表。
     */
    private List<AttackArchivePersonVO> persons;
}
