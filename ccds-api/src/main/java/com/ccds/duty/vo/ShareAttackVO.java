package com.ccds.duty.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 共享态势VO（脱敏，不含电话、NFC号、账号）
 *
 * @author system
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareAttackVO {

    /**
     * 消防站名称
     */
    private String stationName;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdateTime;

    /**
     * 编组列表
     */
    private List<GroupSummary> groups;

    /**
     * 人员卡片列表
     */
    private List<PersonCard> persons;

    /**
     * 编组汇总
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupSummary {
        /**
         * 编组名称
         */
        private String groupName;

        /**
         * 人数
         */
        private Integer count;

        /**
         * 最差状态：in/warn/danger/out
         */
        private String worstStatus;
    }

    /**
     * 人员卡片
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PersonCard {
        /**
         * 姓名
         */
        private String name;

        /**
         * 编组名称
         */
        private String groupName;

        /**
         * 气瓶规格
         */
        private String cylType;

        /**
         * 当前压力(MPa)
         */
        private Double currentPressure;

        /**
         * 估算剩余时间(秒)
         */
        private Integer remainSec;

        /**
         * 状态：in/warn/danger/out
         */
        private String status;
    }
}
