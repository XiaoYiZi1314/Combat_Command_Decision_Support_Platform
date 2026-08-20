import './hq.css';
import { getMe, getAccessToken } from '../../stores/session.js';
import { fetchAttackSnapshots } from '../../api/attack.js';
import { refreshSession } from '../../api/auth.js';
import {
  SYNC,
  snapshotStaleClass,
  snapshotStaleText,
  wsUrl
} from '../../lib/sync.js';

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  if (text) {
    node.textContent = text;
  }
  return node;
}

function activeCount(row) {
  return Number(row.inCount || 0) + Number(row.warnCount || 0) + Number(row.dangerCount || 0);
}

function fmtTime(value) {
  if (!value) {
    return '--';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '--';
  }
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  const ss = String(date.getSeconds()).padStart(2, '0');
  return `${hh}:${mm}:${ss}`;
}

export function renderHqPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'hq-page');
  const head = el('div', 'hq-head');
  head.appendChild(el('h1', '', me.brigadeName || '指挥汇总'));
  head.appendChild(el('div', 'sub', '只列出有未撤出人员的站'));
  page.appendChild(head);
  const status = el('div', 'hq-status', '连接中…');
  page.appendChild(status);
  const list = el('div', 'hq-list');
  page.appendChild(list);
  root.appendChild(page);

  const state = {
    rows: {},
    mode: 'ws',
    since: '',
    socket: null,
    pollTimer: null,
    fallbackTimer: null,
    closed: false
  };

  function visibleRows() {
    return Object.keys(state.rows)
      .map((id) => state.rows[id])
      .filter((row) => activeCount(row) > 0)
      .sort((a, b) => {
        const ta = a.lastEventAt ? new Date(a.lastEventAt).getTime() : 0;
        const tb = b.lastEventAt ? new Date(b.lastEventAt).getTime() : 0;
        return tb - ta;
      });
  }

  function applyRows(rows) {
    (rows || []).forEach((row) => {
      if (!row || row.stationId == null) {
        return;
      }
      state.rows[String(row.stationId)] = row;
      if (row.gmtModified && (!state.since || row.gmtModified > state.since)) {
        state.since = row.gmtModified;
      }
    });
    renderList();
  }

  function renderList() {
    list.innerHTML = '';
    const rows = visibleRows();
    if (!rows.length) {
      list.appendChild(el('div', 'hq-empty', '暂无正在内攻的站'));
      return;
    }
    const now = Date.now();
    rows.forEach((row) => {
      const card = el('div', `hq-card ${snapshotStaleClass(row.staleSec)}`);
      card.appendChild(el('div', 'name', row.stationName || '消防站'));
      if (row.brigadeName) {
        card.appendChild(el('div', 'brigade', row.brigadeName));
      }
      const counts = el('div', 'hq-counts');
      counts.appendChild(el('span', 'in', `安全 ${row.inCount || 0}`));
      counts.appendChild(el('span', 'warn', `预警 ${row.warnCount || 0}`));
      counts.appendChild(el('span', 'danger', `危险 ${row.dangerCount || 0}`));
      counts.appendChild(el('span', '', `组 ${row.groupCount || 0}`));
      card.appendChild(counts);
      const hint = snapshotStaleText(row.lastEventAt, now);
      const meta = el('div', 'hq-meta', hint || `更新 ${fmtTime(row.lastEventAt)}`);
      card.appendChild(meta);
      card.addEventListener('click', () => {
        window.location.hash = `#/hq/station/${row.stationId}`;
      });
      list.appendChild(card);
    });
  }

  function setMode(mode, text) {
    state.mode = mode;
    status.textContent = text;
    status.className = mode === 'ws' ? 'hq-status live' : 'hq-status poll';
  }

  async function poll(incremental) {
    try {
      const since = incremental ? state.since : '';
      const rows = await fetchAttackSnapshots(since);
      applyRows(rows);
      if (state.mode === 'poll') {
        setMode('poll', '轮询中（WebSocket 已断开）');
      }
    } catch (err) {
      if (err.code === 'NETWORK') {
        setMode('poll', '离线，显示本地快照');
        return;
      }
      setMode('poll', err.message || '拉取失败');
    }
  }

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
    setMode('poll', '轮询中（WebSocket 已断开）');
    poll(true);
    state.pollTimer = window.setInterval(() => poll(true), SYNC.pollIntervalMs);
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
    let token = getAccessToken();
    if (!token) {
      const refreshed = await refreshSession();
      token = refreshed && refreshed.accessToken ? refreshed.accessToken : getAccessToken();
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
      setMode('ws', '实时推送已连接');
      poll(false);
    });
    socket.addEventListener('message', (event) => {
      try {
        const payload = JSON.parse(event.data);
        if (payload && payload.snapshot) {
          applyRows([payload.snapshot]);
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

  const tick = window.setInterval(renderList, 1000);
  poll(false).then(() => connectWs());

  return () => {
    state.closed = true;
    window.clearInterval(tick);
    stopPoll();
    if (state.fallbackTimer) {
      window.clearTimeout(state.fallbackTimer);
    }
    closeSocket();
  };
}
