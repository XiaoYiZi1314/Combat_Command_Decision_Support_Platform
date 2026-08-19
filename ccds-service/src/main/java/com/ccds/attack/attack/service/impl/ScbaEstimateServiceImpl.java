package com.ccds.attack.attack.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.ccds.attack.attack.constant.AttackRuleConstant;
import com.ccds.attack.attack.enums.AttackSceneEnum;
import com.ccds.attack.attack.enums.AttackStatusEnum;
import com.ccds.attack.attack.enums.WorkLevelEnum;
import com.ccds.attack.attack.service.ScbaEstimateService;
import com.ccds.roster.roster.constant.RosterRuleConstant;
import com.ccds.roster.roster.entity.ScbaCalibrationDO;
import com.ccds.roster.roster.enums.CylTypeEnum;

/**
 * 剩余时间 = 压力 × 10 × 容积 / (RMV × 场景系数)。有高强度标定时用个人 K 覆盖默认 RMV。
 *
 * @author ccds
 * @since 0.1.0
 */
@Service
public class ScbaEstimateServiceImpl implements ScbaEstimateService {

    private static final int SCALE = 6;

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer remainSec(BigDecimal pressure, String cylType, String workLevel, String scene,
                             ScbaCalibrationDO calibration) {
        if (pressure == null || pressure.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal volume = cylVolume(cylType);
        BigDecimal rmv = effectiveRmv(workLevel, calibration);
        BigDecimal factor = AttackSceneEnum.fromCodeOrDefault(scene).getFactor();
        BigDecimal denom = rmv.multiply(factor);
        if (denom.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal minutes = pressure
                .multiply(BigDecimal.valueOf(AttackRuleConstant.PRESSURE_TO_VOLUME))
                .multiply(volume)
                .divide(denom, SCALE, RoundingMode.HALF_UP);
        int seconds = minutes.multiply(BigDecimal.valueOf(AttackRuleConstant.SECONDS_PER_MINUTE))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        return Math.max(0, seconds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttackStatusEnum resolveStatus(AttackStatusEnum current, BigDecimal pressure, Integer remainSec,
                                          LocalDateTime now, LocalDateTime enteredAt, LocalDateTime withdrawnAt) {
        if (withdrawnAt != null || current == AttackStatusEnum.OUT) {
            return AttackStatusEnum.OUT;
        }
        if (current == AttackStatusEnum.PENDING || enteredAt == null) {
            return AttackStatusEnum.PENDING;
        }
        BigDecimal currentPressure = pressure == null ? BigDecimal.ZERO : pressure;
        int remain = remainSec == null ? 0 : remainSec;
        if (currentPressure.compareTo(AttackRuleConstant.DANGER_PRESSURE) <= 0
                || remain <= AttackRuleConstant.DANGER_TIME_SEC) {
            return AttackStatusEnum.DANGER;
        }
        if (currentPressure.compareTo(AttackRuleConstant.WARN_PRESSURE) <= 0
                || remain <= AttackRuleConstant.WARN_TIME_SEC) {
            return AttackStatusEnum.WARN;
        }
        return AttackStatusEnum.IN;
    }

    private BigDecimal cylVolume(String cylType) {
        CylTypeEnum type = CylTypeEnum.fromCodeOrDefault(cylType);
        if (RosterRuleConstant.CYL_TYPE_9.equals(type.getCode())) {
            return AttackRuleConstant.CYL_VOLUME_9;
        }
        return AttackRuleConstant.CYL_VOLUME_68;
    }

    private BigDecimal effectiveRmv(String workLevel, ScbaCalibrationDO calibration) {
        WorkLevelEnum level = WorkLevelEnum.fromCodeOrDefault(workLevel);
        BigDecimal defaultRmv = BigDecimal.valueOf(level.getRmv());
        BigDecimal personalK = personalK(calibration);
        if (personalK == null) {
            return defaultRmv;
        }
        return defaultRmv.multiply(personalK);
    }

    private BigDecimal personalK(ScbaCalibrationDO calibration) {
        if (calibration == null || calibration.getPressure() == null || calibration.getFullTimeSec() == null) {
            return null;
        }
        if (calibration.getPressure().compareTo(BigDecimal.ZERO) <= 0 || calibration.getFullTimeSec() <= 0) {
            return null;
        }
        BigDecimal measuredMin = BigDecimal.valueOf(calibration.getFullTimeSec())
                .divide(BigDecimal.valueOf(AttackRuleConstant.SECONDS_PER_MINUTE), SCALE, RoundingMode.HALF_UP);
        BigDecimal volume = cylVolume(calibration.getCylType());
        BigDecimal formulaMin = calibration.getPressure()
                .multiply(BigDecimal.valueOf(AttackRuleConstant.PRESSURE_TO_VOLUME))
                .multiply(volume)
                .divide(BigDecimal.valueOf(AttackRuleConstant.RMV_HEAVY), SCALE, RoundingMode.HALF_UP);
        if (measuredMin.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return formulaMin.divide(measuredMin, SCALE, RoundingMode.HALF_UP);
    }
}
