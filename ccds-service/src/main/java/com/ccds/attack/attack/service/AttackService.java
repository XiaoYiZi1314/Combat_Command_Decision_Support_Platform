package com.ccds.attack.attack.service;

import java.util.List;

import com.ccds.attack.attack.dto.AttackEventCommand;
import com.ccds.attack.attack.vo.AttackEventResultVO;
import com.ccds.attack.attack.vo.AttackSnapshotVO;
import com.ccds.attack.attack.vo.StationAttackVO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 内攻人员卡片、事件幂等与站快照。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface AttackService {

    /**
     * 本站卡片与快照。指挥角色只读可见站。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @return 本站内攻
     */
    StationAttackVO getStationAttack(AuthPrincipal principal, Long stationId);

    /**
     * 写入场 / 复测 / 撤出等事件。重复 eventId 不生成第二张卡。
     *
     * @param principal 当前身份
     * @param stationId 站主键
     * @param command   事件
     * @return 写结果与最新本站内攻
     */
    AttackEventResultVO submitEvent(AuthPrincipal principal, Long stationId, AttackEventCommand command);

    /**
     * 指挥端快照。仅可见且正在内攻人数大于 0 的站。
     *
     * @param principal 当前身份
     * @param since     增量起点 ISO-8601，空则全量活跃站
     * @return 快照列表，不会为 null
     */
    List<AttackSnapshotVO> listSnapshots(AuthPrincipal principal, String since);
}
