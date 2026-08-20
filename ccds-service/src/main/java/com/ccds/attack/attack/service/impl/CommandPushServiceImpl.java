package com.ccds.attack.attack.service.impl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.ccds.attack.attack.service.CommandPushService;
import com.ccds.attack.attack.service.CommandSessionSink;
import com.ccds.attack.attack.vo.AttackSnapshotVO;
import com.ccds.attack.attack.vo.CommandPushVO;
import com.ccds.iam.identity.entity.AccountDO;
import com.ccds.iam.identity.mapper.AccountMapper;
import com.ccds.iam.identity.model.AuthPrincipal;
import com.ccds.iam.identity.vo.StationVO;
import com.ccds.org.org.service.OrgQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 内存登记指挥端会话，按可见站过滤后再下发。
 *
 * @author ccds
 * @since 0.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandPushServiceImpl implements CommandPushService {

    private static final String TYPE_SNAPSHOT = "snapshot";

    private final AccountMapper accountMapper;

    private final OrgQueryService orgQueryService;

    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<String, SessionEntry>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(String sessionId, AuthPrincipal principal, CommandSessionSink sink) {
        if (sessionId == null || sessionId.isBlank() || principal == null || principal.getAccountId() == null) {
            return;
        }
        AccountDO account = accountMapper.selectById(principal.getAccountId());
        if (account == null) {
            return;
        }
        Set<Long> visible = ConcurrentHashMap.newKeySet();
        for (StationVO station : orgQueryService.listVisibleStations(account)) {
            if (station.getId() != null) {
                visible.add(station.getId());
            }
        }
        sessions.put(sessionId, new SessionEntry(visible, sink));
        log.info("command ws register sessionHash={} stationCount={}", Integer.valueOf(sessionId.hashCode()),
                Integer.valueOf(visible.size()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregister(String sessionId) {
        if (sessionId == null) {
            return;
        }
        sessions.remove(sessionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcast(AttackSnapshotVO snapshot) {
        if (snapshot == null || snapshot.getStationId() == null) {
            return;
        }
        CommandPushVO push = CommandPushVO.builder()
                .type(TYPE_SNAPSHOT)
                .snapshot(snapshot)
                .build();
        Long stationId = snapshot.getStationId();
        for (SessionEntry session : sessions.values()) {
            if (!session.visibleStationIds.contains(stationId) || session.sink == null) {
                continue;
            }
            try {
                session.sink.send(push);
            } catch (RuntimeException ex) {
                log.warn("command ws send failed stationId={}", stationId, ex);
            }
        }
    }

    private static final class SessionEntry {

        private final Set<Long> visibleStationIds;

        private final CommandSessionSink sink;

        private SessionEntry(Set<Long> visibleStationIds, CommandSessionSink sink) {
            this.visibleStationIds = visibleStationIds;
            this.sink = sink;
        }
    }
}
