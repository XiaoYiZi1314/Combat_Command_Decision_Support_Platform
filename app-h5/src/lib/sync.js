export const SYNC = {
  pollAfterDisconnectMs: 3000,
  pollIntervalMs: 5000,
  retryMax: 5,
  retryBaseMs: 1000,
  retryMaxMs: 30000,
  staleAmberSec: 120,
  staleRedSec: 300,
  staleHint: '请向现场核实',
  cloudOverride: '已用云端更新覆盖',
  connected: '已连接',
  wsPath: '/ws/command'
};

export function retryDelayMs(attempt) {
  const n = Math.max(0, Number(attempt) || 0);
  const delay = SYNC.retryBaseMs * Math.pow(2, n);
  return Math.min(SYNC.retryMaxMs, delay);
}

export function wsUrl(token) {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const encoded = encodeURIComponent(token || '');
  return `${proto}//${window.location.host}${SYNC.wsPath}?token=${encoded}`;
}

export function syncStatusText(online, pending) {
  const n = Number(pending) || 0;
  if (!online) {
    return n > 0 ? `离线待传 ${n} 条` : '离线';
  }
  return n > 0 ? `已连接，待传 ${n} 条` : SYNC.connected;
}

export function isTransient(err) {
  if (!err) {
    return false;
  }
  return err.code === 'NETWORK' || err.code === 'HTTP' || err.code === 'SYSTEM_ERROR';
}

export function isDropEvent(err) {
  if (!err) {
    return false;
  }
  return err.code === 'ATTACK_PERSON_DUPLICATE'
    || err.code === 'ATTACK_EVENT_INVALID'
    || err.code === 'ATTACK_STATUS_INVALID'
    || err.code === 'ATTACK_PERSON_NOT_FOUND'
    || err.code === 'ATTACK_NFC_NOT_FOUND';
}

export function personTimeMs(person) {
  if (!person) {
    return 0;
  }
  const modified = person.gmtModified ? new Date(person.gmtModified).getTime() : 0;
  return Number.isFinite(modified) ? modified : 0;
}

export function shouldOverrideLocal(localPerson, remotePerson, justWrittenEventId) {
  if (!remotePerson) {
    return false;
  }
  if (!localPerson) {
    return true;
  }
  if (justWrittenEventId && remotePerson.clientEventId === justWrittenEventId) {
    return false;
  }
  return personTimeMs(remotePerson) > personTimeMs(localPerson);
}

export function mergeAttack(localAttack, remoteAttack, justWrittenEventIds) {
  const written = justWrittenEventIds || {};
  if (!remoteAttack) {
    return { attack: localAttack, overridden: false };
  }
  if (!localAttack || !Array.isArray(localAttack.persons)) {
    return { attack: remoteAttack, overridden: false };
  }
  const localMap = {};
  (localAttack.persons || []).forEach((p) => {
    localMap[String(p.id)] = p;
  });
  let overridden = false;
  const merged = (remoteAttack.persons || []).map((remote) => {
    const local = localMap[String(remote.id)];
    const eventId = remote.clientEventId;
    if (shouldOverrideLocal(local, remote, eventId && written[eventId] ? eventId : '')) {
      if (local && personTimeMs(remote) > personTimeMs(local) && (!eventId || !written[eventId])) {
        overridden = true;
      }
      return remote;
    }
    return local || remote;
  });
  const remoteIds = {};
  merged.forEach((p) => {
    remoteIds[String(p.id)] = true;
  });
  (localAttack.persons || []).forEach((local) => {
    const id = String(local.id);
    if (remoteIds[id]) {
      return;
    }
    if (String(id).indexOf('local_') !== 0) {
      return;
    }
    const covered = (remoteAttack.persons || []).some((remote) => {
      return remote.clientEventId && local.clientEventId && remote.clientEventId === local.clientEventId;
    });
    if (!covered) {
      merged.push(local);
    }
  });
  return {
    attack: Object.assign({}, remoteAttack, { persons: merged }),
    overridden
  };
}

export function snapshotStaleSec(lastEventAt, nowMs) {
  if (!lastEventAt) {
    return null;
  }
  const ts = new Date(lastEventAt).getTime();
  if (!Number.isFinite(ts)) {
    return null;
  }
  return Math.max(0, Math.floor(((nowMs || Date.now()) - ts) / 1000));
}

export function snapshotStaleClass(staleSec) {
  const n = Number(staleSec);
  if (!Number.isFinite(n)) {
    return '';
  }
  if (n >= SYNC.staleRedSec) {
    return 'stale-red';
  }
  if (n >= SYNC.staleAmberSec) {
    return 'stale-amber';
  }
  return '';
}

export function snapshotStaleText(lastEventAt, nowMs) {
  const sec = snapshotStaleSec(lastEventAt, nowMs);
  if (sec == null) {
    return '';
  }
  if (sec >= SYNC.staleRedSec) {
    return `超过 5 分钟未更新 · ${SYNC.staleHint}`;
  }
  if (sec >= SYNC.staleAmberSec) {
    return '超过 2 分钟未更新';
  }
  return '';
}

export function snapshotActiveCount(row) {
  if (!row) {
    return 0;
  }
  return Number(row.inCount || 0)
    + Number(row.warnCount || 0)
    + Number(row.dangerCount || 0)
    + Number(row.pendingCount || 0);
}

export function connectCommandSocket(handlers) {
  const cbs = handlers || {};
  const state = {
    socket: null,
    pollTimer: null,
    fallbackTimer: null,
    closed: false
  };

  function stopPoll() {
    if (state.pollTimer) {
      window.clearInterval(state.pollTimer);
      state.pollTimer = null;
    }
  }

  function startPoll() {
    if (state.pollTimer || state.closed) {
      return;
    }
    if (typeof cbs.onPoll === 'function') {
      cbs.onPoll(true);
    }
    state.pollTimer = window.setInterval(() => {
      if (typeof cbs.onPoll === 'function') {
        cbs.onPoll(true);
      }
    }, SYNC.pollIntervalMs);
  }

  function closeSocket() {
    if (state.socket) {
      try {
        state.socket.close();
      } catch (err) {
        console.warn('hq ws close failed', err.name);
      }
      state.socket = null;
    }
  }

  async function connectWs() {
    if (state.closed) {
      return;
    }
    let token = '';
    if (typeof cbs.token === 'function') {
      token = await cbs.token();
    }
    if (!token) {
      startPoll();
      return;
    }
    closeSocket();
    const socket = new WebSocket(wsUrl(token));
    state.socket = socket;
    socket.addEventListener('open', () => {
      if (state.fallbackTimer) {
        window.clearTimeout(state.fallbackTimer);
        state.fallbackTimer = null;
      }
      stopPoll();
      if (typeof cbs.onOpen === 'function') {
        cbs.onOpen();
      }
    });
    socket.addEventListener('message', (event) => {
      try {
        const payload = JSON.parse(event.data);
        if (typeof cbs.onMessage === 'function') {
          cbs.onMessage(payload);
        }
      } catch (err) {
        console.warn('hq ws payload invalid', err.name);
      }
    });
    socket.addEventListener('close', () => {
      if (state.closed) {
        return;
      }
      state.socket = null;
      if (state.fallbackTimer) {
        window.clearTimeout(state.fallbackTimer);
      }
      state.fallbackTimer = window.setTimeout(() => {
        startPoll();
        connectWs();
      }, SYNC.pollAfterDisconnectMs);
    });
    socket.addEventListener('error', () => {
      if (socket.readyState !== WebSocket.OPEN) {
        socket.close();
      }
    });
  }

  if (typeof cbs.onPoll === 'function') {
    cbs.onPoll(false);
  }
  connectWs();

  return () => {
    state.closed = true;
    stopPoll();
    if (state.fallbackTimer) {
      window.clearTimeout(state.fallbackTimer);
    }
    closeSocket();
  };
}
