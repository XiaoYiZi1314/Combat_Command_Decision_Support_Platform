package com.ccds.attack.attack.service;

import com.ccds.attack.attack.vo.CommandPushVO;

/**
 * 指挥端一条 WS 连接的下发口。实现放 app 层。
 *
 * @author ccds
 * @since 0.1.0
 */
public interface CommandSessionSink {

    /**
     * 下发一条推送。失败由实现自行关闭连接。
     *
     * @param push 载荷
     */
    void send(CommandPushVO push);
}
