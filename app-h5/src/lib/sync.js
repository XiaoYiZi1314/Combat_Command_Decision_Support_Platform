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
  if (!lastEventAt) {
    return '';
  }
  const ts = new Date(lastEventAt).getTime();
  if (!Number.isFinite(ts)) {
    return '';
  }
  const sec = Math.max(0, Math.floor(((nowMs || Date.now()) - ts) / 1000));
  if (sec >= SYNC.staleRedSec) {
    return SYNC.staleHint;
  }
  if (sec >= SYNC.staleAmberSec) {
    return '超过 2 分钟未更新';
  }
  return '';
}
