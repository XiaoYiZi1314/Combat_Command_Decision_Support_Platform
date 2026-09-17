package com.ccds.attack.attack.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ccds.attack.attack.constant.AttackRuleConstant;
import com.ccds.attack.attack.enums.AttackSceneEnum;
import com.ccds.attack.attack.enums.AttackStatusEnum;
import com.ccds.attack.attack.enums.WorkLevelEnum;
import com.ccds.attack.attack.model.ScbaEstimateQuery;
import com.ccds.attack.attack.model.ScbaEstimateResult;
import com.ccds.attack.attack.service.ScbaEstimateService;
import com.ccds.roster.roster.constant.RosterRuleConstant;
import com.ccds.roster.roster.entity.ScbaCalibrationDO;
import com.ccds.roster.roster.enums.CylTypeEnum;

/**
 * 第七章空呼公式：默认代谢模型、加权个人 K、公式与校准混合，再按可用压力 / 压降速率估算。
 *
 * @author ccds
 * @since 0.1.0
 */
@Service
public class ScbaEstimateServiceImpl implements ScbaEstimateService {

    private static final int SCALE = 8;

    private static final BigDecimal MILLIS_PER_MINUTE = new BigDecimal("60000");

    /**
     * {@inheritDoc}
     */
    @Override
    public ScbaEstimateResult estimate(ScbaEstimateQuery query) {
        ScbaEstimateQuery safe = query == null ? ScbaEstimateQuery.builder().build() : query;
        BigDecimal personalK = personalK(safe.getCalibrations());
        AttackStatusEnum status = AttackStatusEnum.fromCode(safe.getStatus());
        BigDecimal measured = firstPositive(safe.getPressure(), safe.getInitPressure());
        if (status == AttackStatusEnum.PENDING || safe.getEnteredAt() == null) {
            return ScbaEstimateResult.builder()
                    .currentPressure(measured)
                    .remainSec(null)
                    .personalK(personalK)
                    .build();
        }
        if (status == AttackStatusEnum.OUT) {
            return ScbaEstimateResult.builder()
                    .currentPressure(measured)
                    .remainSec(null)
                    .personalK(personalK)
                    .build();
        }
        BigDecimal formulaRate = formulaRate(safe);
        BigDecimal calRate = calibrationRate(safe, personalK);
        BigDecimal blendRate = blendedRate(formulaRate, calRate);
        BigDecimal livePressure = estimatedPressure(safe, measured, blendRate);
        Integer remainSec = remainingSec(livePressure, blendRate, formulaRate);
        return ScbaEstimateResult.builder()
                .currentPressure(livePressure)
                .remainSec(remainSec)
                .personalK(personalK)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal personalK(List<ScbaCalibrationDO> calibrations) {
        if (calibrations == null || calibrations.isEmpty()) {
            return null;
        }
        int window = Math.min(AttackRuleConstant.CALIBRATION_WINDOW, calibrations.size());
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;
        for (int i = 0; i < window; i++) {
            BigDecimal itemK = kOf(calibrations.get(i));
            if (itemK == null) {
                continue;
            }
            BigDecimal weight = BigDecimal.valueOf(window - i);
            weighted = weighted.add(itemK.multiply(weight));
            weightSum = weightSum.add(weight);
        }
        if (weightSum.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal raw = weighted.divide(weightSum, SCALE, RoundingMode.HALF_UP);
        BigDecimal min = AttackRuleConstant.DEFAULT_K_PER_MET.multiply(AttackRuleConstant.PERSONAL_K_MIN_FACTOR);
        BigDecimal max = AttackRuleConstant.DEFAULT_K_PER_MET.multiply(AttackRuleConstant.PERSONAL_K_MAX_FACTOR);
        return clamp(raw, min, max);
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

    private BigDecimal formulaRate(ScbaEstimateQuery query) {
        BigDecimal rmv = rmvFromK(AttackRuleConstant.DEFAULT_K_PER_MET, query.getWorkLevel(), query.getScene());
        rmv = rmv.multiply(defaultBodyScale(query));
        return pressureRate(rmv, query.getCylType());
    }

    private BigDecimal calibrationRate(ScbaEstimateQuery query, BigDecimal personalK) {
        if (personalK == null) {
            return null;
        }
        BigDecimal rmv = rmvFromK(personalK, query.getWorkLevel(), query.getScene());
        return pressureRate(rmv, query.getCylType());
    }

    private BigDecimal blendedRate(BigDecimal formulaRate, BigDecimal calRate) {
        if (calRate == null) {
            return formulaRate;
        }
        BigDecimal mixed = formulaRate.multiply(AttackRuleConstant.BLEND_FORMULA_WITH_CAL)
                .add(calRate.multiply(AttackRuleConstant.BLEND_CAL_ONLY));
        BigDecimal min = formulaRate.multiply(AttackRuleConstant.BLEND_CAL_CLAMP_MIN);
        BigDecimal max = formulaRate.multiply(AttackRuleConstant.BLEND_CAL_CLAMP_MAX);
        return clamp(mixed, min, max);
    }

    private BigDecimal estimatedPressure(ScbaEstimateQuery query, BigDecimal measured, BigDecimal rate) {
        if (measured == null || measured.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal elapsedMin = elapsedMinutes(query);
        BigDecimal live = measured.subtract(rate.multiply(elapsedMin));
        if (live.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return live;
    }

    private Integer remainingSec(BigDecimal pressure, BigDecimal blendRate, BigDecimal formulaRate) {
        if (pressure == null || pressure.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (blendRate == null || blendRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal rawMin = pressure.divide(blendRate, SCALE, RoundingMode.HALF_UP);
        BigDecimal formulaMin = formulaRate == null || formulaRate.compareTo(BigDecimal.ZERO) <= 0
                ? rawMin
                : pressure.divide(formulaRate, SCALE, RoundingMode.HALF_UP);
        BigDecimal clamped = clampRemainMinutes(pressure, rawMin, formulaMin);
        int seconds = clamped.multiply(BigDecimal.valueOf(AttackRuleConstant.SECONDS_PER_MINUTE))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        return Math.max(0, seconds);
    }

    private BigDecimal clampRemainMinutes(BigDecimal pressure, BigDecimal rawMin, BigDecimal formulaMin) {
        BigDecimal minFactor;
        BigDecimal maxFactor;
        BigDecimal extraMin;
        if (pressure.compareTo(AttackRuleConstant.REMAIN_BAND_LOW_PRESSURE) <= 0) {
            minFactor = AttackRuleConstant.REMAIN_LOW_MIN_FACTOR;
            maxFactor = AttackRuleConstant.REMAIN_LOW_MAX_FACTOR;
            extraMin = AttackRuleConstant.REMAIN_LOW_MAX_ADD_MIN;
        } else if (pressure.compareTo(AttackRuleConstant.REMAIN_BAND_MID_PRESSURE) <= 0) {
            minFactor = AttackRuleConstant.REMAIN_MID_MIN_FACTOR;
            maxFactor = AttackRuleConstant.REMAIN_MID_MAX_FACTOR;
            extraMin = AttackRuleConstant.REMAIN_MID_MAX_ADD_MIN;
        } else {
            minFactor = AttackRuleConstant.REMAIN_MID_MIN_FACTOR;
            maxFactor = AttackRuleConstant.REMAIN_HIGH_MAX_FACTOR;
            extraMin = AttackRuleConstant.REMAIN_HIGH_MAX_ADD_MIN;
        }
        BigDecimal min = formulaMin.multiply(minFactor);
        BigDecimal max = formulaMin.multiply(maxFactor).add(extraMin);
        return clamp(rawMin, min, max);
    }

    private BigDecimal rmvFromK(BigDecimal k, String workLevel, String scene) {
        WorkLevelEnum level = WorkLevelEnum.fromCodeOrDefault(workLevel);
        AttackSceneEnum sceneEnum = AttackSceneEnum.fromCodeOrDefault(scene);
        BigDecimal rmv = k.multiply(level.getMet())
                .multiply(sceneEnum.getMetScale())
                .multiply(sceneEnum.getStress())
                .multiply(sceneEnum.getEnv());
        return clamp(rmv, AttackRuleConstant.RMV_MIN, AttackRuleConstant.RMV_MAX);
    }

    private BigDecimal pressureRate(BigDecimal rmv, String cylType) {
        BigDecimal volume = cylVolume(cylType);
        BigDecimal denom = volume.multiply(BigDecimal.valueOf(AttackRuleConstant.PRESSURE_TO_VOLUME));
        if (denom.compareTo(BigDecimal.ZERO) <= 0) {
            return AttackRuleConstant.RATE_MIN;
        }
        BigDecimal rate = rmv.divide(denom, SCALE, RoundingMode.HALF_UP);
        return clamp(rate, AttackRuleConstant.RATE_MIN, AttackRuleConstant.RATE_MAX);
    }

    private BigDecimal defaultBodyScale(ScbaEstimateQuery query) {
        BigDecimal demo = demographicFactor(query.getHeightCm(), query.getWeightKg(), query.getAge());
        BigDecimal scale = BigDecimal.ONE.add(demo.subtract(BigDecimal.ONE)
                .multiply(AttackRuleConstant.DEFAULT_BODY_BLEND));
        return clamp(scale, AttackRuleConstant.DEFAULT_BODY_MIN, AttackRuleConstant.DEFAULT_BODY_MAX);
    }

    private BigDecimal demographicFactor(Integer heightCm, Integer weightKg, Integer age) {
        if (heightCm == null || weightKg == null) {
            return BigDecimal.ONE;
        }
        if (heightCm <= AttackRuleConstant.DEMO_HEIGHT_MIN_CM
                || weightKg <= AttackRuleConstant.DEMO_WEIGHT_MIN_KG) {
            return BigDecimal.ONE;
        }
        double bsaRaw = Math.sqrt(heightCm.doubleValue() * weightKg.doubleValue()
                / AttackRuleConstant.BSA_DIVISOR);
        BigDecimal bsa = BigDecimal.valueOf(bsaRaw);
        BigDecimal heightM = BigDecimal.valueOf(heightCm.doubleValue() / 100.0D);
        BigDecimal bmi = BigDecimal.valueOf(weightKg.doubleValue())
                .divide(heightM.multiply(heightM), SCALE, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(bsa.subtract(AttackRuleConstant.BSA_REF)
                .multiply(AttackRuleConstant.BSA_COEF));
        factor = applyBmi(factor, bmi);
        factor = applyAge(factor, age);
        return clamp(factor, AttackRuleConstant.DEMO_FACTOR_MIN, AttackRuleConstant.DEMO_FACTOR_MAX);
    }

    private BigDecimal applyBmi(BigDecimal factor, BigDecimal bmi) {
        if (bmi.compareTo(AttackRuleConstant.BMI_HIGH) > 0) {
            BigDecimal add = bmi.subtract(AttackRuleConstant.BMI_HIGH).multiply(AttackRuleConstant.BMI_HIGH_COEF);
            return factor.multiply(BigDecimal.ONE.add(clamp(add, BigDecimal.ZERO,
                    AttackRuleConstant.BMI_HIGH_ADD_MAX)));
        }
        if (bmi.compareTo(AttackRuleConstant.BMI_LOW) < 0) {
            BigDecimal add = AttackRuleConstant.BMI_LOW.subtract(bmi).multiply(AttackRuleConstant.BMI_LOW_COEF);
            return factor.multiply(BigDecimal.ONE.add(clamp(add, BigDecimal.ZERO,
                    AttackRuleConstant.BMI_LOW_ADD_MAX)));
        }
        return factor;
    }

    private BigDecimal applyAge(BigDecimal factor, Integer age) {
        if (age == null) {
            return factor;
        }
        if (age > AttackRuleConstant.AGE_HIGH) {
            BigDecimal add = BigDecimal.valueOf(age - AttackRuleConstant.AGE_HIGH)
                    .multiply(AttackRuleConstant.AGE_HIGH_COEF);
            return factor.multiply(BigDecimal.ONE.add(clamp(add, BigDecimal.ZERO,
                    AttackRuleConstant.AGE_HIGH_ADD_MAX)));
        }
        if (age > 0 && age < AttackRuleConstant.AGE_YOUNG) {
            return factor.multiply(AttackRuleConstant.AGE_YOUNG_FACTOR);
        }
        return factor;
    }

    private BigDecimal kOf(ScbaCalibrationDO calibration) {
        if (calibration == null || calibration.getPressure() == null || calibration.getFullTimeSec() == null) {
            return null;
        }
        if (calibration.getPressure().compareTo(BigDecimal.ZERO) <= 0 || calibration.getFullTimeSec() <= 0) {
            return null;
        }
        BigDecimal minutes = BigDecimal.valueOf(calibration.getFullTimeSec())
                .divide(BigDecimal.valueOf(AttackRuleConstant.SECONDS_PER_MINUTE), SCALE, RoundingMode.HALF_UP);
        if (minutes.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal rmv = cylVolume(calibration.getCylType())
                .multiply(calibration.getPressure())
                .multiply(BigDecimal.valueOf(AttackRuleConstant.PRESSURE_TO_VOLUME))
                .divide(minutes, SCALE, RoundingMode.HALF_UP);
        return rmv.divide(AttackRuleConstant.MET_HEAVY, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal elapsedMinutes(ScbaEstimateQuery query) {
        LocalDateTime now = query.getNow();
        LocalDateTime anchor = query.getMeasuredAt();
        if (anchor == null || (query.getEnteredAt() != null && query.getEnteredAt().isAfter(anchor))) {
            anchor = query.getEnteredAt();
        }
        if (now == null || anchor == null || now.isBefore(anchor)) {
            return BigDecimal.ZERO;
        }
        long millis = Duration.between(anchor, now).toMillis();
        if (millis <= 0L) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(millis).divide(MILLIS_PER_MINUTE, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal cylVolume(String cylType) {
        CylTypeEnum type = CylTypeEnum.fromCodeOrDefault(cylType);
        if (RosterRuleConstant.CYL_TYPE_9.equals(type.getCode())) {
            return AttackRuleConstant.CYL_VOLUME_9;
        }
        return AttackRuleConstant.CYL_VOLUME_68;
    }

    private BigDecimal firstPositive(BigDecimal first, BigDecimal second) {
        if (first != null) {
            return first;
        }
        return second;
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }
}
