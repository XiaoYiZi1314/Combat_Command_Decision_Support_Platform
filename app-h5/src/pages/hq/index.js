import './hq.css';
import { buildDrawer } from '../shared/drawer.js';
import '../shared/drawer.css';
import { getMe } from '../../stores/session.js';
import { fetchAttackSnapshots } from '../../api/attack.js';
import { issueWebSocketTicket } from '../../api/auth.js';
import { shouldDeferBackgroundPaint } from '../../lib/device-compat.js';
import { confirmLeaveMain, setPageBackHandler } from '../../lib/host.js';
import { buildOutdoorToggle } from '../../lib/theme.js';
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

function headTitle(me, brigade) {
  if (brigade) {
    return brigade.name || '大队汇总';
  }
  if (me.role === 'brigade') {
    return me.brigadeName || '大队汇总';
  }
  if (me.role === 'developer') {
    return '指挥汇总';
  }
  return '支队汇总';
}

/**
 * 2.1：支队首页先显示大队卡片，点一个大队再进入它所属的站。
 * 大队账号不渲染大队层，直接看所属站。
 */
export function renderHqPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'hq-page');
  const head = el('div', 'hq-head');
  const text = el('div', 'hq-head-text');
  const titleEl = el('h1', '', headTitle(me, null));
  text.appendChild(titleEl);
  const subEl = el('div', 'sub', '只列出出警且已有人入场的站');
  text.appendChild(subEl);
  head.appendChild(text);
  const outdoorBtn = buildOutdoorToggle();
  head.appendChild(outdoorBtn);
  const settingsBtn = el('button', 'btn-icon', '⚙');
  settingsBtn.type = 'button';
  settingsBtn.addEventListener('click', () => {
    window.location.hash = '#/settings';
  });
  head.appendChild(settingsBtn);
  const moreBtn = el('button', 'btn-icon', '☰');
  moreBtn.type = 'button';
  moreBtn.setAttribute('aria-label', '更多功能');
  const drawerWrap = el('div', 'hq-drawer-root');
  const drawerNode = buildDrawer(() => setDrawer(false));
  drawerWrap.appendChild(drawerNode);
  const drawerEl = drawerWrap.querySelector('.drawer');
  const maskEl = drawerWrap.querySelector('.drawer-mask');
  drawerEl.classList.remove('drawer');
  drawerEl.classList.add('hq-drawer-panel');
  function setDrawer(open) {
    drawerEl.classList.toggle('show', open);
    maskEl.classList.toggle('show', open);
  }
  moreBtn.addEventListener('click', () => setDrawer(true));
  head.appendChild(moreBtn);
  page.appendChild(head);
  const status = el('div', 'hq-status', '连接中…');
  page.appendChild(status);
  const list = el('div', 'hq-list');
  page.appendChild(list);
  page.appendChild(drawerWrap);
  root.appendChild(page);

  const state = {
    rows: {},
    mode: 'ws',
    since: '',
    closed: false,
    // 当前进入的大队；空表示在大队列表层
    brigade: null
  };

  /**
   * 可见站过滤：出警模式且已有人入场（不含预录入）。
   * 与后端同一表达式：workMode == 'dispatch' && (in + warn + danger) > 0
   */
  function visibleRows() {
    return Object.keys(state.rows)
      .map((id) => state.rows[id])
      .filter((row) => {
        if (row.workMode && row.workMode !== 'dispatch') {
          return false;
        }
        return dispatchCount(row) > 0;
      })
      .sort((a, b) => {
        const ta = a.lastEventAt ? new Date(a.lastEventAt).getTime() : 0;
        const tb = b.lastEventAt ? new Date(b.lastEventAt).getTime() : 0;
        return tb - ta;
      });
  }

  /**
   * 出警且入场人数：不含 pending（预录入）。
   */
  function dispatchCount(row) {
    return Number(row.inCount || 0) + Number(row.warnCount || 0) + Number(row.dangerCount || 0);
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
    if (state.brigade) {
      renderStationsInBrigade();
      return;
    }
    if (me.role === 'brigade') {
      renderStationsInBrigade();
      return;
    }
    renderBrigades();
  }

  /**
   * 大队卡片层：按 brigadeId 聚合，卡上写所属站数或内攻组数，以及安全/预警/危险人数。
   */
  function renderBrigades() {
    const rows = visibleRows();
    if (!rows.length) {
      list.appendChild(el('div', 'hq-empty', '暂无正在出警的站'));
      return;
    }
    const byBrigade = new Map();
    rows.forEach((row) => {
      const key = row.brigadeId == null ? 'direct' : String(row.brigadeId);
      let node = byBrigade.get(key);
      if (!node) {
        node = {
          id: key,
          name: row.brigadeName || '支队直属',
          stationIds: new Set(),
          groupCount: 0,
          inCount: 0,
          warnCount: 0,
          dangerCount: 0,
          lastEventAt: row.lastEventAt
        };
        byBrigade.set(key, node);
      }
      node.stationIds.add(String(row.stationId));
      node.groupCount += Number(row.groupCount || 0);
      node.inCount += Number(row.inCount || 0);
      node.warnCount += Number(row.warnCount || 0);
      node.dangerCount += Number(row.dangerCount || 0);
      if (row.lastEventAt && (!node.lastEventAt || row.lastEventAt > node.lastEventAt)) {
        node.lastEventAt = row.lastEventAt;
      }
    });
    const nodes = Array.from(byBrigade.values()).sort((a, b) => b.stationIds.size - a.stationIds.size);
    nodes.forEach((node) => {
      const card = el('div', 'hq-card hq-brigade-card');
      card.appendChild(el('div', 'name', node.name));
      card.appendChild(el('div', 'brigade', `所属站 ${node.stationIds.size} 个 · 内攻组 ${node.groupCount}`));
      const counts = el('div', 'hq-counts');
      counts.appendChild(el('span', 'in', `安全 ${node.inCount}`));
      counts.appendChild(el('span', 'warn', `预警 ${node.warnCount}`));
      counts.appendChild(el('span', 'danger', `危险 ${node.dangerCount}`));
      card.appendChild(counts);
      card.appendChild(el('div', 'hq-meta', `更新 ${fmtTime(node.lastEventAt)}`));
      card.addEventListener('click', () => {
        state.brigade = { id: node.id, name: node.name };
        titleEl.textContent = node.name;
        subEl.textContent = '只列出出警且已有人入场的站';
        renderList();
      });
      list.appendChild(card);
    });
  }

  /**
   * 站卡片层：大队账号或已点入某个大队后显示。
   */
  function renderStationsInBrigade() {
    let rows = visibleRows();
    if (state.brigade) {
      const brigadeId = state.brigade.id;
      rows = rows.filter((row) => {
        const key = row.brigadeId == null ? 'direct' : String(row.brigadeId);
        return key === brigadeId;
      });
      const backBtn = el('button', 'hq-back-inline', '返回大队列表');
      backBtn.type = 'button';
      backBtn.addEventListener('click', () => {
        state.brigade = null;
        titleEl.textContent = headTitle(me, null);
        renderList();
      });
      list.appendChild(backBtn);
    }
    if (!rows.length) {
      list.appendChild(el('div', 'hq-empty', '该大队暂无正在出警的站'));
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

  async function ticket() {
    const issued = await issueWebSocketTicket();
    return issued && issued.ticket ? issued.ticket : '';
  }

  const disconnect = connectCommandSocket({
    ticket,
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

  setPageBackHandler(() => {
    if (state.brigade) {
      state.brigade = null;
      titleEl.textContent = headTitle(me, null);
      renderList();
      return true;
    }
    return !confirmLeaveMain();
  });

  return () => {
    setPageBackHandler(null);
    state.closed = true;
    window.clearInterval(tick);
    disconnect();
  };
}
