package com.ccds.attack.attack.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ccds.attack.attack.constant.AttackRuleConstant;
import com.ccds.attack.attack.entity.AttackPersonDO;
import com.ccds.attack.attack.entity.StationSnapshotDO;
import com.ccds.attack.attack.enums.AttackStatusEnum;
import com.ccds.attack.attack.vo.AttackPersonVO;
import com.ccds.attack.attack.vo.AttackSnapshotVO;
import com.ccds.attack.attack.vo.StationAttackVO;
import com.ccds.org.org.entity.StationDO;

/**
 * 内攻 DO 转 VO。
 *
 * @author ccds
 * @since 0.1.0
 */
@Component
public class AttackViewTranslator {

    /**
     * 本站内攻视图。
     *
     * @param station  站
     * @param writable 是否可写
     * @param persons  卡片
     * @param snapshot 快照
     * @return 视图
     */
    public StationAttackVO toStationAttack(StationDO station, boolean writable, List<AttackPersonVO> persons,
                                           AttackSnapshotVO snapshot) {
        return StationAttackVO.builder()
                .stationId(station.getId())
                .stationName(station.getName())
                .writable(writable)
                .persons(persons)
                .snapshot(snapshot)
                .build();
    }

    /**
     * 卡片视图。
     *
     * @param person      卡片
     * @param now         当前时间
     * @param calibrated  是否有标定
     * @return 视图
     */
    public AttackPersonVO toPersonVo(AttackPersonDO person, LocalDateTime now, boolean calibrated) {
        AttackStatusEnum status = AttackStatusEnum.fromCode(person.getStatus());
        Integer elapsed = elapsedSec(person, now);
        return AttackPersonVO.builder()
                .id(person.getId())
                .stationId(person.getStationId())
                .profileId(person.getProfileId())
                .displayName(person.getDisplayName())
                .groupName(blankToUngrouped(person.getGroupName()))
                .cylType(person.getCylType())
                .initPressure(person.getInitPressure())
                .currentPressure(person.getCurrentPressure())
                .enteredAt(person.getEnteredAt())
                .withdrawnAt(person.getWithdrawnAt())
                .workLevel(person.getWorkLevel())
                .scene(person.getScene())
                .status(person.getStatus())
                .remainSec(status != null && status.isInside() ? person.getRemainSec() : null)
                .elapsedSec(elapsed)
                .temp(person.getProfileId() == null)
                .calibrated(calibrated)
                .clientEventId(person.getClientEventId())
                .gmtModified(person.getGmtModified())
                .build();
    }

    /**
     * 快照视图。
     *
     * @param snapshot 快照
     * @param station  站
     * @return 视图
     */
    public AttackSnapshotVO toSnapshotVo(StationSnapshotDO snapshot, StationDO station) {
        if (snapshot == null) {
            return AttackSnapshotVO.builder()
                    .stationId(station.getId())
                    .stationName(station.getName())
                    .brigadeId(station.getBrigadeId())
                    .brigadeName(station.getBrigadeName())
                    .inCount(0)
                    .warnCount(0)
                    .dangerCount(0)
                    .outCount(0)
                    .pendingCount(0)
                    .groupCount(0)
                    .build();
        }
        return AttackSnapshotVO.builder()
                .stationId(snapshot.getStationId())
                .stationName(station.getName())
                .brigadeId(station.getBrigadeId())
                .brigadeName(station.getBrigadeName())
                .inCount(nz(snapshot.getInCount()))
                .warnCount(nz(snapshot.getWarnCount()))
                .dangerCount(nz(snapshot.getDangerCount()))
                .outCount(nz(snapshot.getOutCount()))
                .pendingCount(nz(snapshot.getPendingCount()))
                .groupCount(nz(snapshot.getGroupCount()))
                .lastEventAt(snapshot.getLastEventAt())
                .gmtModified(snapshot.getGmtModified())
                .build();
    }

    /**
     * 空编组名回落到未分组。
     *
     * @param groupName 编组名
     * @return 展示名
     */
    public String blankToUngrouped(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return AttackRuleConstant.UNGROUPED_NAME;
        }
        return groupName.trim();
    }

    /**
     * 批量转卡片。
     *
     * @param persons     卡片
     * @param now         当前时间
     * @param calibrated  是否有标定，与 persons 下标对齐
     * @return 视图列表
     */
    public List<AttackPersonVO> toPersonVos(List<AttackPersonDO> persons, LocalDateTime now, List<Boolean> calibrated) {
        List<AttackPersonVO> result = new ArrayList<AttackPersonVO>();
        for (int i = 0; i < persons.size(); i++) {
            boolean flag = calibrated != null && i < calibrated.size() && Boolean.TRUE.equals(calibrated.get(i));
            result.add(toPersonVo(persons.get(i), now, flag));
        }
        return result;
    }

    private Integer elapsedSec(AttackPersonDO person, LocalDateTime now) {
        if (person.getEnteredAt() == null) {
            return 0;
        }
        LocalDateTime end = person.getWithdrawnAt() != null ? person.getWithdrawnAt() : now;
        long seconds = Duration.between(person.getEnteredAt(), end).getSeconds();
        if (seconds < 0L) {
            return 0;
        }
        if (seconds > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) seconds;
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
