const STATION_KEY = 'ccds_attack_station';
const CACHE_PREFIX = 'ccds_attack_cache_';
const QUEUE_KEY = 'ccds_attack_offline_queue';

function readJson(key) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  } catch (err) {
    console.warn('attack cache parse failed', key, err.name);
    return null;
  }
}

function writeJson(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch (err) {
    console.warn('attack cache write failed', key, err.name);
  }
}

export function getAttackStationId(me) {
  if (me && me.role === 'station' && me.stationId) {
    return String(me.stationId);
  }
  const saved = localStorage.getItem(STATION_KEY);
  const stations = me && Array.isArray(me.visibleStations) ? me.visibleStations : [];
  if (saved && stations.some((item) => String(item.id) === saved)) {
    return saved;
  }
  const first = stations[0];
  return first ? String(first.id) : '';
}

export function setAttackStationId(id) {
  try {
    localStorage.setItem(STATION_KEY, String(id));
  } catch (err) {
    console.warn('attack station write failed', err.name);
  }
}

export function saveAttackCache(stationId, data) {
  writeJson(CACHE_PREFIX + stationId, data);
}

export function readAttackCache(stationId) {
  return readJson(CACHE_PREFIX + stationId);
}

export function enqueueAttackEvent(stationId, body) {
  const queue = readJson(QUEUE_KEY) || [];
  queue.push({ stationId, body, queuedAt: Date.now() });
  writeJson(QUEUE_KEY, queue);
  return queue.length;
}

export function peekAttackQueue() {
  const queue = readJson(QUEUE_KEY);
  return Array.isArray(queue) ? queue : [];
}

export function shiftAttackQueue() {
  const queue = peekAttackQueue();
  if (!queue.length) {
    return null;
  }
  const first = queue.shift();
  writeJson(QUEUE_KEY, queue);
  return first;
}

export function rewriteQueuedPersonId(localId, serverId) {
  if (localId == null || serverId == null) {
    return;
  }
  const queue = peekAttackQueue();
  queue.forEach((item) => {
    if (item.body && String(item.body.personId) === String(localId)) {
      item.body.personId = serverId;
    }
  });
  writeJson(QUEUE_KEY, queue);
}

export function attackQueueLength() {
  return peekAttackQueue().length;
}
