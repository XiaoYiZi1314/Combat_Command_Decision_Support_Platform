import './hq.css';
import { getMe, getAccessToken } from '../../stores/session.js';
import { fetchStationAttack } from '../../api/attack.js';
import { refreshSession } from '../../api/auth.js';
import { remainSecOf, resolveStatus, statusLabel, fmtMinSec, fmtElapsed, SCBA } from '../../lib/scba.js';
import { connectCommandSocket } from '../../lib/sync.js';

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

function livePerson(person, nowMs) {
  const copy = Object.assign({}, person);
  const entered = copy.enteredAt ? new Date(copy.enteredAt).getTime() : 0;
  if (copy.status === 'out' || copy.status === 'pending' || !entered) {
    copy.liveRemain = null;
    copy.liveElapsed = 0;
    copy.liveStatus = copy.status || 'pending';
    return copy;
  }
  const elapsedSec = Math.max(0, Math.floor((nowMs - entered) / 1000));
  const modified = copy.gmtModified ? new Date(copy.gmtModified).getTime() : entered;
  const sinceMeasure = Math.max(0, Math.floor((nowMs - Math.max(entered, modified)) / 1000));
  copy.liveElapsed = elapsedSec;
  copy.liveRemain = Math.max(0, remainSecOf(
    copy.currentPressure,
    copy.cylType,
    copy.workLevel,
    copy.scene,
    copy.personalK
  ) - sinceMeasure);
  copy.liveStatus = resolveStatus(copy.status, copy.currentPressure, copy.liveRemain);
  return copy;
}

export function renderHqStationPage(root, params) {
  root.innerHTML = '';
  const me = getMe() || {};
  const stationId = params && params.stationId ? String(params.stationId) : '';
  const page = el('div', 'hq-page');
  const back = el('button', 'hq-back', '返回主界面');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = '#/hq';
  });
  const head = el('div', 'hq-head');
  const title = el('h1', '', me.brigadeName || '单站态势');
  const sub = el('div', 'sub', '只读');
  head.appendChild(title);
  head.appendChild(sub);
  page.appendChild(head);
  page.appendChild(back);
  const status = el('div', 'hq-status', '连接中…');
  page.appendChild(status);
  const list = el('div', 'hq-list');
  page.appendChild(list);
  root.appendChild(page);

  let attack = null;
  let closed = false;
  let mode = 'ws';

  function setMode(next, text) {
    mode = next;
    status.textContent = text;
    status.className = next === 'ws' ? 'hq-status live' : 'hq-status poll';
  }

  function render() {
    list.innerHTML = '';
    if (!attack) {
      list.appendChild(el('div', 'hq-empty', '加载中…'));
      return;
    }
    title.textContent = attack.stationName || '单站态势';
    sub.textContent = attack.brigadeName ? `${attack.brigadeName} · 只读` : '只读';
    const now = Date.now();
    const persons = (attack.persons || []).map((p) => livePerson(p, now));
    if (!persons.length) {
      list.appendChild(el('div', 'hq-empty', '暂无内攻人员'));
      return;
    }
    persons.forEach((person) => {
      const card = el('div', 'hq-card');
      card.appendChild(el('div', 'name', `${person.displayName || ''} · ${statusLabel(person.liveStatus)}`));
      const counts = el('div', 'hq-counts');
      counts.appendChild(el('span', '', `${Number(person.currentPressure || 0).toFixed(1)} MPa`));
      counts.appendChild(el('span', '', person.liveStatus === 'pending' || person.liveStatus === 'out'
        ? '--'
        : fmtMinSec(person.liveRemain)));
      counts.appendChild(el('span', '', person.liveStatus === 'pending' ? '--' : fmtElapsed(person.liveElapsed)));
      card.appendChild(counts);
      card.appendChild(el('div', 'hq-meta', person.groupName || SCBA.ungrouped));
      list.appendChild(card);
    });
  }

  async function load() {
    if (!stationId) {
      list.innerHTML = '';
      list.appendChild(el('div', 'hq-empty', '缺少站标识'));
      return;
    }
    try {
      attack = await fetchStationAttack(stationId);
      render();
      if (mode === 'poll') {
        setMode('poll', '轮询中（WebSocket 已断开）');
      }
    } catch (err) {
      list.innerHTML = '';
      list.appendChild(el('div', 'hq-empty', err.message || '加载失败'));
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
      load();
    },
    onMessage(payload) {
      const snapshot = payload && payload.snapshot;
      if (!snapshot || String(snapshot.stationId) !== String(stationId)) {
        return;
      }
      load();
    },
    onPoll() {
      setMode('poll', '轮询中（WebSocket 已断开）');
      load();
    }
  });

  const tick = window.setInterval(() => {
    if (attack && !closed) {
      render();
    }
  }, 1000);

  return () => {
    closed = true;
    window.clearInterval(tick);
    disconnect();
  };
}
