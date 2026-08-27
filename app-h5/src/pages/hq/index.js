import './hq.css';
import { getMe, getAccessToken } from '../../stores/session.js';
import { fetchAttackSnapshots } from '../../api/attack.js';
import { refreshSession } from '../../api/auth.js';
import { shouldDeferBackgroundPaint } from '../../lib/device-compat.js';
import { confirmLeaveMain, setPageBackHandler } from '../../lib/host.js';
import {
  connectCommandSocket,
  snapshotActiveCount,
  snapshotStaleClass,
  snapshotStaleSec,
  snapshotStaleText
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

function headTitle(me) {
  if (me.role === 'brigade') {
    return me.brigadeName || '大队汇总';
  }
  if (me.role === 'developer') {
    return '指挥汇总';
  }
  return me.brigadeName || '支队汇总';
}

export function renderHqPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'hq-page');
  const head = el('div', 'hq-head');
  const text = el('div', 'hq-head-text');
  text.appendChild(el('h1', '', headTitle(me)));
  text.appendChild(el('div', 'sub', '只列出有未撤出人员的站'));
  head.appendChild(text);
  const settingsBtn = el('button', 'btn-icon', '⚙');
  settingsBtn.type = 'button';
  settingsBtn.addEventListener('click', () => {
    window.location.hash = '#/settings';
  });
  head.appendChild(settingsBtn);
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
    closed: false
  };

  function visibleRows() {
    return Object.keys(state.rows)
      .map((id) => state.rows[id])
      .filter((row) => snapshotActiveCount(row) > 0)
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
      const staleSec = snapshotStaleSec(row.lastEventAt, now);
      const card = el('div', `hq-card ${snapshotStaleClass(staleSec)}`.trim());
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

  async function token() {
    let access = getAccessToken();
    if (!access) {
      const refreshed = await refreshSession();
      access = refreshed && refreshed.accessToken ? refreshed.accessToken : getAccessToken();
    }
    return access;
  }

  const disconnect = connectCommandSocket({
    token,
    onOpen() {
      setMode('ws', '实时推送已连接');
      poll(Boolean(state.since));
    },
    onMessage(payload) {
      if (payload && payload.snapshot) {
        applyRows([payload.snapshot]);
      }
    },
    onPoll(incremental) {
      setMode('poll', '轮询中（WebSocket 已断开）');
      poll(incremental);
    }
  });

  const tick = window.setInterval(() => {
    if (shouldDeferBackgroundPaint()) {
      return;
    }
    renderList();
  }, 1000);

  setPageBackHandler(() => !confirmLeaveMain());

  return () => {
    setPageBackHandler(null);
    state.closed = true;
    window.clearInterval(tick);
    disconnect();
  };
}
