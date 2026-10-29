package com.ccds.attack.attack.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccds.attack.attack.constant.AttackRuleConstant;
import com.ccds.attack.attack.entity.StationScbaSettingDO;
import com.ccds.attack.attack.entity.StationWorkModeDO;
import com.ccds.attack.attack.mapper.StationScbaSettingMapper;
import com.ccds.attack.attack.mapper.StationWorkModeMapper;
import com.ccds.attack.attack.model.ScbaThresholds;
import com.ccds.attack.attack.service.StationStateService;
import com.ccds.attack.attack.vo.ScbaThresholdVO;
import com.ccds.common.api.constant.ErrorCodeConstant;
import com.ccds.common.api.exception.BizException;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.enums.AccountRoleEnum;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.org.org.entity.StationDO;
import com.ccds.org.org.mapper.StationMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 站级作业模式与空呼阈值实现。
 *
 * <p>模式与阈值都存站级表，随实时状态下发；指挥端只读，站上改了以后共享页与指挥端颜色立刻一致。</p>
 *
 * @author ccds
 * @since 0.1.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StationStateServiceImpl implements StationStateService {

    private static final String MODE_TEST = "test";

    private static final String MODE_TRAINING = "training";

    private static final String MODE_DISPATCH = "dispatch";

    private static final String MSG_UNAUTHORIZED = "登录已失效，请重新登录";

    private static final String MSG_MODE_INVALID = "作业模式须为 test/training/dispatch";

    private static final String MSG_THRESHOLD_INVALID = "压力与时间的危险阈值都必须低于预警阈值";

    private static final String MSG_WRITE_FORBIDDEN = "仅本站账号可改本站模式与阈值";

    private static final String MSG_STATION_MISSING = "单位不存在";

    private static final int SECONDS_PER_MINUTE = 60;

    private final StationWorkModeMapper stationWorkModeMapper;

    private final StationScbaSettingMapper stationScbaSettingMapper;

    private final AccountMapper accountMapper;

    private final StationMapper stationMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWorkMode(Long stationId) {
        StationWorkModeDO row = stationWorkModeMapper.selectByStationId(stationId);
        return normalizeMode(row == null ? null : row.getWorkMode());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.Map<Long, String> getWorkModes(java.util.List<Long> stationIds) {
        java.util.Map<Long, String> out = new java.util.HashMap<Long, String>();
        if (stationIds == null || stationIds.isEmpty()) {
            return out;
        }
        for (StationWorkModeDO row : stationWorkModeMapper.selectByStationIds(stationIds)) {
            out.put(row.getStationId(), normalizeMode(row.getWorkMode()));
        }
        return out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.Map<Long, ScbaThresholdVO> getThresholdVos(java.util.List<Long> stationIds) {
        java.util.Map<Long, ScbaThresholdVO> out = new java.util.HashMap<Long, ScbaThresholdVO>();
        if (stationIds == null || stationIds.isEmpty()) {
            return out;
        }
        for (StationScbaSettingDO row : stationScbaSettingMapper.selectByStationIds(stationIds)) {
            out.put(row.getStationId(), toThresholdVo(row));
        }
        return out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScbaThresholds getThresholds(Long stationId) {
        StationScbaSettingDO row = stationScbaSettingMapper.selectByStationId(stationId);
        if (row == null) {
            // 未配置的站按一期默认值，与站端本机默认值保持一致
            return ScbaThresholds.builder()
                    .warnPressure(AttackRuleConstant.WARN_PRESSURE)
                    .dangerPressure(AttackRuleConstant.DANGER_PRESSURE)
                    .warnTimeSec(Integer.valueOf(AttackRuleConstant.WARN_TIME_SEC))
                    .dangerTimeSec(Integer.valueOf(AttackRuleConstant.DANGER_TIME_SEC))
                    .defaultWorkLevel("moderate")
                    .voiceEnabled(Boolean.TRUE)
                    .build();
        }
        return ScbaThresholds.builder()
                .warnPressure(row.getWarnPressure() == null
                        ? AttackRuleConstant.WARN_PRESSURE : row.getWarnPressure())
                .dangerPressure(row.getDangerPressure() == null
                        ? AttackRuleConstant.DANGER_PRESSURE : row.getDangerPressure())
                .warnTimeSec(Integer.valueOf((row.getWarnTimeMin() == null
                        ? 12 : row.getWarnTimeMin().intValue()) * SECONDS_PER_MINUTE))
                .dangerTimeSec(Integer.valueOf((row.getDangerTimeMin() == null
                        ? 5 : row.getDangerTimeMin().intValue()) * SECONDS_PER_MINUTE))
                .defaultWorkLevel(row.getWorkLevel() == null ? "moderate" : row.getWorkLevel())
                .voiceEnabled(Boolean.valueOf(row.getVoiceEnabled() != null
                        && row.getVoiceEnabled().intValue() == 1))
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScbaThresholdVO getThresholdVo(Long stationId) {
        return toThresholdVo(stationScbaSettingMapper.selectByStationId(stationId));
    }

    private ScbaThresholdVO toThresholdVo(StationScbaSettingDO row) {
        if (row == null) {
            return ScbaThresholdVO.builder()
                    .warnPressure(AttackRuleConstant.WARN_PRESSURE)
                    .dangerPressure(AttackRuleConstant.DANGER_PRESSURE)
                    .warnTimeMin(Integer.valueOf(AttackRuleConstant.WARN_TIME_SEC / SECONDS_PER_MINUTE))
                    .dangerTimeMin(Integer.valueOf(AttackRuleConstant.DANGER_TIME_SEC / SECONDS_PER_MINUTE))
                    .defaultWorkLevel("moderate")
                    .voiceEnabled(Boolean.TRUE)
                    .build();
        }
        return ScbaThresholdVO.builder()
                .warnPressure(row.getWarnPressure() == null
                        ? AttackRuleConstant.WARN_PRESSURE : row.getWarnPressure())
                .dangerPressure(row.getDangerPressure() == null
                        ? AttackRuleConstant.DANGER_PRESSURE : row.getDangerPressure())
                .warnTimeMin(row.getWarnTimeMin() == null ? Integer.valueOf(12) : row.getWarnTimeMin())
                .dangerTimeMin(row.getDangerTimeMin() == null ? Integer.valueOf(5) : row.getDangerTimeMin())
                .defaultWorkLevel(row.getWorkLevel() == null ? "moderate" : row.getWorkLevel())
                .voiceEnabled(Boolean.valueOf(row.getVoiceEnabled() != null
                        && row.getVoiceEnabled().intValue() == 1))
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requireVisible(AuthPrincipal principal, Long stationId) {
        AccountDO account = requireAccount(principal);
        StationDO station = stationMapper.selectById(stationId);
        if (station == null) {
            throw new BizException(ErrorCodeConstant.ATTACK_STATION_FORBIDDEN, MSG_STATION_MISSING);
        }
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        if (role == null) {
            throw new BizException(ErrorCodeConstant.ATTACK_STATION_FORBIDDEN, MSG_WRITE_FORBIDDEN);
        }
        switch (role) {
            case STATION:
                if (account.getStationId() != null && account.getStationId().equals(stationId)) {
                    return;
                }
                break;
            case BRIGADE:
                if (account.getBrigadeId() != null && account.getBrigadeId().equals(station.getBrigadeId())) {
                    return;
                }
                break;
            case HQ:
            case DEVELOPER:
                return;
            default:
                break;
        }
        throw new BizException(ErrorCodeConstant.ATTACK_STATION_FORBIDDEN, MSG_WRITE_FORBIDDEN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateWorkMode(AuthPrincipal principal, Long stationId, String workMode) {
        AccountDO account = requireAccount(principal);
        requireWritable(account, stationId);
        String mode = normalizeModeStrict(workMode);
        LocalDateTime now = LocalDateTime.now();
        stationWorkModeMapper.insertOrUpdate(StationWorkModeDO.builder()
                .stationId(stationId)
                .workMode(mode)
                .updatedBy(account.getId())
                .gmtCreate(now)
                .gmtModified(now)
                .build());
        log.info("站级作业模式变更：stationId={}, workMode={}", stationId, mode);
        return mode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScbaThresholdVO saveThresholds(AuthPrincipal principal, Long stationId, ScbaThresholdVO command) {
        AccountDO account = requireAccount(principal);
        requireWritable(account, stationId);
        if (command == null || command.getWarnPressure() == null || command.getDangerPressure() == null) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_THRESHOLD_INVALID);
        }
        // 危险阈值必须低于预警阈值
        if (command.getDangerPressure().compareTo(command.getWarnPressure()) >= 0) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_THRESHOLD_INVALID);
        }
        Integer warnTimeMin = command.getWarnTimeMin() == null ? Integer.valueOf(12) : command.getWarnTimeMin();
        Integer dangerTimeMin = command.getDangerTimeMin() == null ? Integer.valueOf(5) : command.getDangerTimeMin();
        if (dangerTimeMin.intValue() >= warnTimeMin.intValue()) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_THRESHOLD_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        stationScbaSettingMapper.insertOrUpdate(StationScbaSettingDO.builder()
                .stationId(stationId)
                .warnPressure(command.getWarnPressure())
                .dangerPressure(command.getDangerPressure())
                .warnTimeMin(warnTimeMin)
                .dangerTimeMin(dangerTimeMin)
                .workLevel(command.getDefaultWorkLevel() == null ? "moderate" : command.getDefaultWorkLevel())
                .scene("flat")
                .voiceEnabled(Integer.valueOf(Boolean.TRUE.equals(command.getVoiceEnabled()) ? 1 : 0))
                .updatedBy(account.getId())
                .gmtCreate(now)
                .gmtModified(now)
                .build());
        log.info("站级空呼阈值保存：stationId={}, warnPressure={}, dangerPressure={}",
                stationId, command.getWarnPressure(), command.getDangerPressure());
        return getThresholdVo(stationId);
    }

    private void requireWritable(AccountDO account, Long stationId) {
        StationDO station = stationMapper.selectById(stationId);
        if (station == null) {
            throw new BizException(ErrorCodeConstant.ATTACK_STATION_FORBIDDEN, MSG_STATION_MISSING);
        }
        AccountRoleEnum role = AccountRoleEnum.fromCode(account.getRole());
        // 指挥端只读：只有站级本站可以改模式与阈值
        if (role != AccountRoleEnum.STATION || account.getStationId() == null
                || !account.getStationId().equals(stationId)) {
            throw new BizException(ErrorCodeConstant.ATTACK_STATION_FORBIDDEN, MSG_WRITE_FORBIDDEN);
        }
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return MODE_TEST;
        }
        String value = mode.trim().toLowerCase(Locale.ROOT);
        if (MODE_TRAINING.equals(value) || MODE_DISPATCH.equals(value) || MODE_TEST.equals(value)) {
            return value;
        }
        return MODE_TEST;
    }

    private String normalizeModeStrict(String mode) {
        if (mode == null || mode.isBlank()) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_MODE_INVALID);
        }
        String value = mode.trim().toLowerCase(Locale.ROOT);
        if (!MODE_TRAINING.equals(value) && !MODE_DISPATCH.equals(value) && !MODE_TEST.equals(value)) {
            throw new BizException(ErrorCodeConstant.PARAM_INVALID, MSG_MODE_INVALID);
        }
        return value;
    }

    private AccountDO requireAccount(AuthPrincipal principal) {
        if (principal == null || principal.getAccountId() == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        AccountDO account = accountMapper.selectById(principal.getAccountId());
        if (account == null) {
            throw new BizException(ErrorCodeConstant.AUTH_UNAUTHORIZED, MSG_UNAUTHORIZED);
        }
        return account;
    }
}
