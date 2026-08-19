package com.ccds.attack.attack.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ccds.attack.attack.enums.AttackStatusEnum;
import com.ccds.roster.roster.entity.ScbaCalibrationDO;

/**
 * 空呼剩余时间。前后端同一套常量。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface ScbaEstimateService {

    /**
     * 估算剩余秒。预录入 / 已撤出返回 null。
     *
     * @param pressure    当前压力
     * @param cylType     气瓶规格
     * @param workLevel   作业强度
     * @param scene       场景
     * @param calibration 高强度标定，可空
     * @return 剩余秒，无法估算为 null
     */
    Integer remainSec(BigDecimal pressure, String cylType, String workLevel, String scene,
                      ScbaCalibrationDO calibration);

    /**
     * 按压力与剩余时间判定状态。先到先得，取更严重。
     *
     * @param current     当前状态
     * @param pressure    当前压力
     * @param remainSec   剩余秒，可空
     * @param now         当前时间
     * @param enteredAt   入场时间
     * @param withdrawnAt 撤出时间
     * @return 状态，不会为 null
     */
    AttackStatusEnum resolveStatus(AttackStatusEnum current, BigDecimal pressure, Integer remainSec,
                                   LocalDateTime now, LocalDateTime enteredAt, LocalDateTime withdrawnAt);
}
