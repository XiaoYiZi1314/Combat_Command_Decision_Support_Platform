package com.ccds.roster.roster.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 某站花名册与编组。
 *
 * @author ccds
 * @since 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationRosterVO {

    /**
     * 站主键。
     */
    private Long stationId;

    /**
     * 站名。
     */
    private String stationName;

    /**
     * 是否可写。
     */
    private Boolean writable;

    /**
     * 人员档案。
     */
    private List<ProfileVO> profiles;

    /**
     * 战斗编组。
     */
    private List<BattleGroupVO> groups;
}
