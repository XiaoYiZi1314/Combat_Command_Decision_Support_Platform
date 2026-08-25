import '../attack/attack.css';
import { getMe, getAccessToken } from '../../stores/session.js';
import { fetchStationAttack } from '../../api/attack.js';
import { refreshSession } from '../../api/auth.js';
import { connectCommandSocket } from '../../lib/sync.js';
import {
  el,
  livePersons,
  paintPeopleAndGroups,
  paintPersonCard,
  paintStats
} from '../attack/board.js';

export function renderHqStationPage(root, params) {
  root.innerHTML = '';
  const me = getMe() || {};
  const stationId = params && params.stationId ? String(params.stationId) : '';
  const page = el('div', 'attack-page command-readonly');
  const state = {
    attack: null,
    filter: '',
    mode: 'ws'
  };

  const head = el('div', 'attack-head');
  const text = el('div', 'attack-head-text');
  const title = el('h1', '', me.brigadeName || '单站态势');
  const sub = el('div', 'sub', '只读');
  text.appendChild(title);
  text.appendChild(sub);
  head.appendChild(text);
  const back = el('button', 'hq-back-inline', '返回主界面');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = '#/hq';
  });
  head.appendChild(back);
  page.appendChild(head);

  const status = el('div', 'hq-status', '连接中…');
  page.appendChild(status);

  const stats = el('div', 'stats');
  page.appendChild(stats);

  const list = el('div', 'list main-pager');
  const peoplePage = el('div', 'main-page main-people-page');
  const groupPage = el('div', 'main-page main-group-page');
  list.appendChild(peoplePage);
  list.appendChild(groupPage);
  page.appendChild(list);
  root.appendChild(page);

  function currentPersons() {
    return livePersons(state.attack);
  }

  function renderStats() {
    paintStats({
      statsEl: stats,
      filter: state.filter,
      persons: currentPersons(),
      onFilter(next) {
        state.filter = next;
        renderCards();
        renderStats();
      }
    });
  }

  function renderCards() {
    paintPeopleAndGroups({
      peoplePage,
      groupPage,
      persons: currentPersons(),
      filter: state.filter,
      stationName: (state.attack && state.attack.stationName) || '本站',
      focusId: '',
      buildCard(person) {
        return paintPersonCard(person, '');
      }
    });
  }

  function paint() {
    if (state.attack && state.attack.stationName) {
      title.textContent = state.attack.stationName;
      sub.textContent = state.attack.brigadeName
        ? `${state.attack.brigadeName} · 只读`
        : '只读';
    }
    renderStats();
    renderCards();
  }

  function setMode(mode, text) {
    state.mode = mode;
    status.textContent = text;
    status.className = mode === 'ws' ? 'hq-status live' : 'hq-status poll';
  }

  async function load() {
    if (!stationId) {
      peoplePage.textContent = '缺少站标识';
      return;
    }
    try {
      state.attack = await fetchStationAttack(stationId);
      paint();
      if (state.mode === 'poll') {
        setMode('poll', '轮询中（WebSocket 已断开）');
      }
    } catch (err) {
      peoplePage.textContent = err.message || '加载失败';
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
      if (!snapshot || String(snapshot.stationId) !== stationId) {
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
    if (state.attack) {
      paint();
    }
  }, 1000);

  return () => {
    window.clearInterval(tick);
    disconnect();
  };
}
