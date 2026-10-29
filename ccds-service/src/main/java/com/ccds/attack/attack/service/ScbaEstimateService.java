package com.ccds.attack.attack.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ccds.attack.attack.enums.AttackRiskStatusEnum;
import com.ccds.attack.attack.enums.AttackStatusEnum;
import com.ccds.attack.attack.model.ScbaEstimateQuery;
import com.ccds.attack.attack.model.ScbaEstimateResult;
import com.ccds.roster.roster.entity.ScbaCalibrationDO;

/**
 * 空呼剩余时间。前后端同一套常量与公式。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface ScbaEstimateService {

    /**
     * 先估当前压力，再按混合压降速率算剩余。预录入 / 已撤出剩余为空。
     *
     * @param query 估算入参，不能为空
     * @return 估算结果，不会为 null；预录入剩余秒为 0，已撤出剩余为空
     */
    ScbaEstimateResult estimate(ScbaEstimateQuery query);

    /**
     * 最近标定加权得到的代谢个人 K。无有效标定返回 null。
     *
     * @param calibrations 高强度标定，新到旧，可空
     * @return 个人 K，无法计算为 null
     */
    BigDecimal personalK(List<ScbaCalibrationDO> calibrations);

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
    AttackRiskStatusEnum resolveStatus(AttackStatusEnum current, BigDecimal pressure, Integer remainSec,
                                       LocalDateTime now, LocalDateTime enteredAt, LocalDateTime withdrawnAt);

    /**
     * 按站级阈值判定状态。站上改过阈值后，服务端与共享页都按这一份算颜色。
     *
     * @param current     当前状态
     * @param pressure    当前压力
     * @param remainSec   剩余秒，可空
     * @param enteredAt   入场时间
     * @param withdrawnAt 撤出时间
     * @param thresholds  站级阈值，为空则按一期默认值
     * @return 状态，不会为 null
     */
    AttackRiskStatusEnum resolveStatus(AttackStatusEnum current, BigDecimal pressure, Integer remainSec,
                                       LocalDateTime enteredAt, LocalDateTime withdrawnAt,
                                       com.ccds.attack.attack.model.ScbaThresholds thresholds);
}
