const STATION_KEY = 'ccds_attack_station';
const CACHE_PREFIX = 'ccds_attack_cache_';
const WRITTEN_KEY = 'ccds_attack_written_events';
const LEGACY_QUEUE_KEY = 'ccds_attack_offline_queue';
const DB_NAME = 'ccds_attack';
const DB_VERSION = 1;
const STORE_QUEUE = 'offline_queue';

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

function openDb() {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('INDEXEDDB_UNAVAILABLE'));
      return;
    }
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE_QUEUE)) {
        const store = db.createObjectStore(STORE_QUEUE, { keyPath: 'id', autoIncrement: true });
        store.createIndex('eventId', 'eventId', { unique: false });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error || new Error('INDEXEDDB_OPEN_FAILED'));
  });
}

function txDone(tx) {
  return new Promise((resolve, reject) => {
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error || new Error('INDEXEDDB_TX_FAILED'));
    tx.onabort = () => reject(tx.error || new Error('INDEXEDDB_TX_ABORTED'));
  });
}

function reqValue(req) {
  return new Promise((resolve, reject) => {
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error || new Error('INDEXEDDB_REQ_FAILED'));
  });
}

function memoryQueue() {
  const raw = readJson(LEGACY_QUEUE_KEY);
  return Array.isArray(raw) ? raw : [];
}

function writeMemoryQueue(queue) {
  writeJson(LEGACY_QUEUE_KEY, queue);
  return queue;
}

let migratePromise = null;

async function migrateLegacyQueue(db) {
  const legacy = memoryQueue();
  if (!legacy.length) {
    return;
  }
    const tx = db.transaction(STORE_QUEUE, 'readwrite');
    const done = txDone(tx);
    const store = tx.objectStore(STORE_QUEUE);
    const existing = await reqValue(store.getAll());
    const seen = {};
    (existing || []).forEach((item) => {
      if (item && item.eventId) {
        seen[item.eventId] = true;
      }
    });
    legacy.forEach((item) => {
      const eventId = item && item.body && item.body.eventId;
      if (eventId && seen[eventId]) {
        return;
      }
      if (eventId) {
        seen[eventId] = true;
      }
      store.add({
        stationId: item.stationId,
        body: item.body,
        queuedAt: item.queuedAt || Date.now(),
        attempts: Number(item.attempts) || 0,
        nextAt: item.nextAt || Date.now(),
        eventId: eventId || ''
      });
    });
    await done;
  try {
    localStorage.removeItem(LEGACY_QUEUE_KEY);
  } catch (err) {
    console.warn('attack queue migrate cleanup failed', err.name);
  }
}

async function withDb(executor) {
  try {
    const db = await openDb();
    if (!migratePromise) {
      migratePromise = migrateLegacyQueue(db).catch((err) => {
        console.warn('attack queue migrate failed', err.name);
      });
    }
    await migratePromise;
    try {
      return await executor(db);
    } finally {
      db.close();
    }
  } catch (err) {
    console.warn('attack queue indexeddb fallback', err.name);
    return executor(null);
  }
}

function fallbackPeek() {
  return memoryQueue().slice();
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

export async function enqueueAttackEvent(stationId, body) {
  const eventId = body && body.eventId;
  return withDb(async (db) => {
    if (!db) {
      const queue = fallbackPeek();
      if (eventId && queue.some((item) => item.body && item.body.eventId === eventId)) {
        return queue.length;
      }
      queue.push({
        stationId,
        body,
        queuedAt: Date.now(),
        attempts: 0,
        nextAt: Date.now(),
        eventId: eventId || ''
      });
      writeMemoryQueue(queue);
      return queue.length;
    }
    const tx = db.transaction(STORE_QUEUE, 'readwrite');
    const done = txDone(tx);
    const store = tx.objectStore(STORE_QUEUE);
    if (eventId) {
      const matched = await reqValue(store.index('eventId').getAll(eventId));
      if (matched && matched.length) {
        const all = await reqValue(store.getAll());
        await done;
        return (all || []).length;
      }
    }
    store.add({
      stationId,
      body,
      queuedAt: Date.now(),
      attempts: 0,
      nextAt: Date.now(),
      eventId: eventId || ''
    });
    const all = await reqValue(store.getAll());
    await done;
    return (all || []).length;
  });
}

export async function peekAttackQueue() {
  return withDb(async (db) => {
    if (!db) {
      return fallbackPeek();
    }
    const tx = db.transaction(STORE_QUEUE, 'readonly');
    const done = txDone(tx);
    const rows = await reqValue(tx.objectStore(STORE_QUEUE).getAll());
    await done;
    return (rows || []).sort((a, b) => (a.id || 0) - (b.id || 0));
  });
}

export async function shiftAttackQueue() {
  return withDb(async (db) => {
    if (!db) {
      const queue = fallbackPeek();
      if (!queue.length) {
        return null;
      }
      const first = queue.shift();
      writeMemoryQueue(queue);
      return first;
    }
    const tx = db.transaction(STORE_QUEUE, 'readwrite');
    const done = txDone(tx);
    const store = tx.objectStore(STORE_QUEUE);
    const rows = (await reqValue(store.getAll())).sort((a, b) => (a.id || 0) - (b.id || 0));
    if (!rows.length) {
      await done;
      return null;
    }
    const first = rows[0];
    store.delete(first.id);
    await done;
    return first;
  });
}

export async function markQueueRetry(delayMs) {
  return withDb(async (db) => {
    if (!db) {
      const queue = fallbackPeek();
      if (!queue.length) {
        return 0;
      }
      const first = queue[0];
      first.attempts = (Number(first.attempts) || 0) + 1;
      first.nextAt = Date.now() + Math.max(0, Number(delayMs) || 0);
      writeMemoryQueue(queue);
      return first.attempts;
    }
    const tx = db.transaction(STORE_QUEUE, 'readwrite');
    const done = txDone(tx);
    const store = tx.objectStore(STORE_QUEUE);
    const rows = (await reqValue(store.getAll())).sort((a, b) => (a.id || 0) - (b.id || 0));
    if (!rows.length) {
      await done;
      return 0;
    }
    const first = rows[0];
    first.attempts = (Number(first.attempts) || 0) + 1;
    first.nextAt = Date.now() + Math.max(0, Number(delayMs) || 0);
    store.put(first);
    await done;
    return first.attempts;
  });
}

export async function dropQueueHead() {
  return shiftAttackQueue();
}

export async function resetQueueBackoff() {
  return withDb(async (db) => {
    if (!db) {
      const queue = fallbackPeek();
      queue.forEach((item) => {
        item.nextAt = 0;
      });
      writeMemoryQueue(queue);
      return queue.length;
    }
    const tx = db.transaction(STORE_QUEUE, 'readwrite');
    const done = txDone(tx);
    const store = tx.objectStore(STORE_QUEUE);
    const rows = await reqValue(store.getAll());
    (rows || []).forEach((item) => {
      item.nextAt = 0;
      store.put(item);
    });
    await done;
    return (rows || []).length;
  });
}

export async function rewriteQueuedPersonId(localId, serverId) {
  if (localId == null || serverId == null) {
    return;
  }
  return withDb(async (db) => {
    if (!db) {
      const queue = fallbackPeek();
      queue.forEach((item) => {
        if (item.body && String(item.body.personId) === String(localId)) {
          item.body.personId = serverId;
        }
      });
      writeMemoryQueue(queue);
      return;
    }
    const tx = db.transaction(STORE_QUEUE, 'readwrite');
    const done = txDone(tx);
    const store = tx.objectStore(STORE_QUEUE);
    const rows = await reqValue(store.getAll());
    (rows || []).forEach((item) => {
      if (item.body && String(item.body.personId) === String(localId)) {
        item.body.personId = serverId;
        store.put(item);
      }
    });
    await done;
  });
}

export async function attackQueueLength() {
  const queue = await peekAttackQueue();
  return queue.length;
}

export function markWrittenEvent(eventId) {
  if (!eventId) {
    return;
  }
  const map = readJson(WRITTEN_KEY) || {};
  map[eventId] = Date.now();
  const keys = Object.keys(map);
  if (keys.length > 200) {
    keys.sort((a, b) => map[a] - map[b]);
    keys.slice(0, keys.length - 200).forEach((key) => {
      delete map[key];
    });
  }
  writeJson(WRITTEN_KEY, map);
}

export function writtenEventMap() {
  return readJson(WRITTEN_KEY) || {};
}
