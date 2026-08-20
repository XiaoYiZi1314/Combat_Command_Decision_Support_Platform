package com.ccds.attack.attack.service;

import com.ccds.attack.attack.vo.AttackSnapshotVO;
import com.ccds.iam.identity.model.AuthPrincipal;

/**
 * 指挥端实时推送会话。业务只依赖接口。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface CommandPushService {

    /**
     * 登记一条指挥端连接。
     *
     * @param sessionId 会话标识
     * @param principal 当前身份
     * @param sink      下发口
     */
    void register(String sessionId, AuthPrincipal principal, CommandSessionSink sink);

    /**
     * 注销连接。
     *
     * @param sessionId 会话标识
     */
    void unregister(String sessionId);

    /**
     * 向可见该站的指挥端广播快照。
     *
     * @param snapshot 快照
     */
    void broadcast(AttackSnapshotVO snapshot);
}
