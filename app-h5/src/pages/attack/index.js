import './attack.css';
import { buildDrawer } from '../shared/drawer.js';
import '../shared/drawer.css';
import { getMe } from '../../stores/session.js';
import { getScbaSettings } from '../../stores/settings.js';
import { fetchRoster } from '../../api/roster.js';
import { fetchStationAttack, submitAttackEvent } from '../../api/attack.js';
import { bridge } from '../../bridge/index.js';
import { tagMatchesProfile } from '../../stores/nfc.js';
import { archiveStation } from '../../api/archive.js';
import { shouldDeferBackgroundPaint } from '../../lib/device-compat.js';
import { confirmLeaveMain, setPageBackHandler } from '../../lib/host.js';
import {
  attackQueueLength,
  dropQueueHead,
  enqueueAttackEvent,
  getAttackStationId,
  markQueueRetry,
  markWrittenEvent,
  peekAttackQueue,
  resetQueueBackoff,
  readAttackCache,
  rewriteQueuedPersonId,
  saveAttackCache,
  setAttackStationId,
  writtenEventMap
} from '../../stores/attack.js';
import {
  SYNC,
  isDropEvent,
  isTransient,
  mergeAttack,
  retryDelayMs,
  syncStatusText
} from '../../lib/sync.js';
import {
  SCBA,
  newEventId
} from '../../lib/scba.js';
import {
  el,
  livePersons,
  paintPeopleAndGroups,
  paintPersonCard,
  paintStats
} from './board.js';

function visibleStations() {
  const me = getMe() || {};
  return Array.isArray(me.visibleStations) ? me.visibleStations : [];
}

const WORK_LEVEL_OPTIONS = [
  ['light', '低强度'],
  ['moderate', '中强度'],
  ['heavy', '高强度']
];

const SCENE_OPTIONS = [
  ['flat', '平地'],
  ['highrise', '高层'],
  ['search', '搜救'],
  ['large', '大跨度空间'],
  ['dark', '黑暗']
];

function headSub(me, attack) {
  const stationName = (attack && attack.stationName) || me.stationName;
  const brigadeName = (attack && attack.brigadeName) || me.brigadeName;
  if (brigadeName && stationName && brigadeName !== stationName) {
    return `${brigadeName} / ${stationName}`;
  }
  return brigadeName || stationName || '作战指挥辅助决策平台';
}

function ignoreBackground(err) {
  if (err) {
    console.warn('attack background task failed', err.code || err.name);
  }
}

export function renderAttackPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const writableStation = me.role === 'station';
  const page = el('div', 'attack-page');
  const state = {
    stationId: getAttackStationId(me),
    attack: null,
    roster: null,
    filter: '',
    openEnterId: '',
    openUpdateId: '',
    pressureDrafts: {},
    selectedIds: {},
    groupTab: '全部',
    focusId: '',
    nfcReady: bridge.hasNfc(),
    syncing: false,
    flushPromise: null,
    online: typeof navigator === 'undefined' ? true : navigator.onLine,
    riskInitialized: false,
    riskAlertAt: {}
  };

  const head = el('div', 'attack-head');
  const text = el('div', 'attack-head-text');
  const title = el('h1', '', me.stationName || me.brigadeName || '作战指挥辅助决策平台');
  const sub = el('div', 'sub', headSub(me, null));
  text.appendChild(title);
  text.appendChild(sub);
  head.appendChild(text);
  if (!writableStation && visibleStations().length) {
    const select = el('select');
    visibleStations().forEach((station) => {
      const opt = document.createElement('option');
      opt.value = String(station.id);
      opt.textContent = station.name;
      select.appendChild(opt);
    });
    select.value = state.stationId;
    select.addEventListener('change', () => {
      setAttackStationId(select.value);
      state.stationId = select.value;
      load();
    });
    head.appendChild(select);
  }
  const settingsBtn = el('button', 'btn-icon', '⚙');
  settingsBtn.type = 'button';
  settingsBtn.addEventListener('click', () => {
    window.location.hash = '#/settings';
  });
  head.appendChild(settingsBtn);
  page.appendChild(head);

  const stats = el('div', 'stats');
  page.appendChild(stats);

  const actions = el('div', 'action-bar');
  const quickBtn = el('button', 'btn-glass primary', '快速录入');
  const syncBtn = el('button', 'btn-glass accent', '云同步');
  const moreBtn = el('button', 'btn-glass', '更多功能');
  const nfcBtn = el('button', 'btn-glass', state.nfcReady ? 'NFC扫描' : 'NFC不可用');
  const syncHint = el('div', 'sync-hint');
  quickBtn.type = 'button';
  syncBtn.type = 'button';
  moreBtn.type = 'button';
  nfcBtn.type = 'button';
  actions.appendChild(quickBtn);
  actions.appendChild(syncBtn);
  actions.appendChild(moreBtn);
  actions.appendChild(nfcBtn);
  page.appendChild(actions);
  page.appendChild(syncHint);

  const list = el('div', 'list main-pager');
  const peoplePage = el('div', 'main-page main-people-page');
  const groupPage = el('div', 'main-page main-group-page');
  list.appendChild(peoplePage);
  list.appendChild(groupPage);
  page.appendChild(list);

  const toast = el('div', 'attack-toast');
  page.appendChild(toast);

  const nfcBar = el('div', 'nfc-bar');
  const bulkWorkBtn = el('button', '', '全员强度');
  const bulkSceneBtn = el('button', '', '全员场景');
  const bulkEnterBtn = el('button', '', '全员入场');
  const archiveBtn = el('button', '', '归档');
  [bulkWorkBtn, bulkSceneBtn, bulkEnterBtn, archiveBtn].forEach((button) => {
    button.type = 'button';
    nfcBar.appendChild(button);
  });
  page.appendChild(nfcBar);

  const bulkPanel = el('div', 'bulk-panel');
  page.appendChild(bulkPanel);

  /* buildDrawer 返回的 fragment 内含 .drawer-mask 与 aside.drawer，直接挂 page，避免双层嵌套 */
  const drawerRoot = el('div', 'drawer-root');
  drawerRoot.appendChild(buildDrawer(() => setDrawer(false)));
  const drawer = drawerRoot.querySelector('.drawer');
  const overlay = drawerRoot.querySelector('.drawer-mask');
  const quick = el('div', 'quick-panel');
  page.appendChild(drawerRoot);
  page.appendChild(quick);
  root.appendChild(page);

  function showToast(message) {
    toast.textContent = message;
    toast.classList.add('show');
    window.setTimeout(() => toast.classList.remove('show'), 2200);
  }

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
    const persons = currentPersons();
    announceRiskTransitions(persons);
    paintPeopleAndGroups({
      peoplePage,
      groupPage,
      persons,
      filter: state.filter,
      stationName: (state.attack && state.attack.stationName) || '本站',
      focusId: state.focusId,
      buildCard: renderCard
    });
  }

  function announceRiskTransitions(persons) {
    const now = Date.now();
    if (!state.riskInitialized) {
      persons.forEach((person) => { state.riskAlertAt[String(person.id)] = person.liveStatus; });
      state.riskInitialized = true;
      return;
    }
    persons.forEach((person) => {
      const id = String(person.id);
      const previous = state.riskAlertAt[id];
      state.riskAlertAt[id] = person.liveStatus;
      if (!previous || previous === person.liveStatus
          || (person.liveStatus !== 'warn' && person.liveStatus !== 'danger')) {
        return;
      }
      const lastAlert = Number(state.riskAlertAt[`${id}:alertAt`] || 0);
      if (now - lastAlert < 45000) {
        return;
      }
      state.riskAlertAt[`${id}:alertAt`] = now;
      const frequency = person.liveStatus === 'danger' ? 1800 : 1200;
      bridge.alarm(frequency, 700).catch(() => {});
      bridge.vibrate([0, 250, 120, 250]).catch(() => {});
      if (getScbaSettings().voiceEnabled) {
        bridge.speak(`${person.displayName || '内攻人员'}气瓶压力${person.liveStatus === 'danger' ? '危险' : '预警'}，当前${Number(person.livePressure || 0).toFixed(1)}兆帕`).catch(() => {});
      }
    });
  }
  function renderCard(person) {
    const card = paintPersonCard(person, state.focusId);
    if (person.liveStatus !== 'out') {
      card.appendChild(contextControls(person));
    }
    if (!writableStation) {
      return card;
    }
    const actionsRow = el('div', 'actions');
    if (person.liveStatus === 'pending') {
      const enter = el('button', 'btn-card btn-update', '入场');
      enter.type = 'button';
      enter.addEventListener('click', () => {
        state.openEnterId = state.openEnterId === person.id ? '' : person.id;
        state.openUpdateId = '';
        renderCards();
      });
      const del = el('button', 'btn-card btn-del', '删除');
      del.type = 'button';
      del.addEventListener('click', () => submit({
        eventId: newEventId(),
        type: 'delete',
        personId: person.id,
        clientUpdatedAt: person.gmtModified || null
      }, '已删除'));
      actionsRow.appendChild(enter);
      actionsRow.appendChild(del);
    } else if (person.liveStatus !== 'out') {
      const paused = Boolean(person.timerPausedAt);
      const timerButton = el('button', 'btn-card btn-update', paused ? '恢复计时' : '暂停计时');
      timerButton.type = 'button';
      timerButton.addEventListener('click', () => submit({
        eventId: newEventId(),
        type: paused ? 'resume' : 'pause',
        personId: person.id,
        clientUpdatedAt: person.gmtModified || null
      }, paused ? `${person.displayName} 已恢复计时` : `${person.displayName} 已暂停计时`));
      actionsRow.appendChild(timerButton);
      const update = el('button', 'btn-card btn-update', '复测压力');
      update.type = 'button';
      update.addEventListener('click', () => {
        state.openUpdateId = state.openUpdateId === person.id ? '' : person.id;
        state.openEnterId = '';
        renderCards();
      });
      const out = el('button', 'btn-card btn-out', '撤出');
      out.type = 'button';
      out.addEventListener('click', () => submit({
        eventId: newEventId(),
        type: 'withdraw',
        personId: person.id,
        clientUpdatedAt: person.gmtModified || null
      }, `${person.displayName} 已撤出`));
      actionsRow.appendChild(update);
      actionsRow.appendChild(out);
    }
    card.appendChild(actionsRow);
    if (state.openEnterId === person.id) {
      card.appendChild(enterPanel(person));
    }
    if (state.openUpdateId === person.id) {
      card.appendChild(updatePanel(person));
    }
    return card;
  }

  function contextControls(person) {
    const row = el('div', 'work-context');
    const work = selectControl('强度', WORK_LEVEL_OPTIONS,
      person.workLevel || getScbaSettings().defaultWorkLevel);
    const scene = selectControl('场景', SCENE_OPTIONS, person.scene || 'flat');
    const changed = () => submit({
      eventId: newEventId(),
      type: 'context',
      personId: person.id,
      workLevel: work.input.value,
      scene: scene.input.value,
      clientUpdatedAt: person.gmtModified || null
    }, `${person.displayName} 的作业参数已更新`);
    work.input.addEventListener('change', changed);
    scene.input.addEventListener('change', changed);
    row.appendChild(work.row);
    row.appendChild(scene.row);
    return row;
  }

  function selectControl(label, options, value) {
    const row = el('label', 'context-field');
    row.appendChild(el('span', '', label));
    const input = document.createElement('select');
    options.forEach((item) => {
      const option = el('option', '', item[1]);
      option.value = item[0];
      input.appendChild(option);
    });
    input.value = value;
    input.disabled = !writableStation;
    row.appendChild(input);
    return { row, input };
  }

  function pressureControl(id, value) {
    const draftKey = `${id}:${state.openEnterId || state.openUpdateId || ''}`;
    const draft = state.pressureDrafts[draftKey];
    const initialValue = draft == null ? value : draft;
    const wrap = el('div', 'pressure-control');
    const headRow = el('div', 'pressure-head');
    headRow.appendChild(el('span', '', '压力'));
    const label = el('b', '', `${Number(initialValue).toFixed(1)} MPa`);
    headRow.appendChild(label);
    const input = document.createElement('input');
    input.type = 'range';
    input.className = 'pressure-range';
    input.min = String(SCBA.pressureMin);
    input.max = String(SCBA.pressureMax);
    input.step = String(SCBA.pressureStep);
    input.value = String(initialValue);
    input.addEventListener('input', () => {
      state.pressureDrafts[draftKey] = Number(input.value);
      label.textContent = `${Number(input.value).toFixed(1)} MPa`;
    });
    wrap.appendChild(headRow);
    wrap.appendChild(input);
    wrap.input = input;
    return wrap;
  }

  function cylSelect(value) {
    const select = document.createElement('select');
    ['6.8', '9'].forEach((item) => {
      const opt = document.createElement('option');
      opt.value = item;
      opt.textContent = `${item}L`;
      select.appendChild(opt);
    });
    select.value = value || '6.8';
    return select;
  }

  function enterPanel(person) {
    const row = el('div', 'enter-row');
    const pressure = pressureControl('enter', person.currentPressure ?? SCBA.defaultPressure);
    const cyl = cylSelect(person.cylType);
    const go = el('button', 'btn-enter-go', '确认入场');
    go.type = 'button';
    go.addEventListener('click', () => submit({
      eventId: newEventId(),
      type: 'enter',
      personId: person.id,
      pressure: Number(pressure.input.value),
      cylType: cyl.value
    }, `${person.displayName} 已入场`));
    row.appendChild(pressure);
    row.appendChild(cyl);
    row.appendChild(go);
    return row;
  }

  function updatePanel(person) {
    const row = el('div', 'upd-row');
    const pressure = pressureControl('upd', person.currentPressure ?? SCBA.defaultPressure);
    const go = el('button', 'btn-ok', '确认复测');
    go.type = 'button';
    go.addEventListener('click', () => submit({
      eventId: newEventId(),
      type: 'remeasure',
      personId: person.id,
      pressure: Number(pressure.input.value)
    }, `${person.displayName} 压力已更新`));
    row.appendChild(pressure);
    row.appendChild(go);
    return row;
  }

  function setDrawer(open) {
    overlay.classList.toggle('show', open);
    drawer.classList.toggle('show', open);
  }


  function setQuick(open) {
    if (open) {
      renderQuick();
      quick.style.height = '72vh';
      quick.classList.add('show');
      return;
    }
    quick.classList.remove('show');
  }

  function setBulk(open, kind) {
    if (!open) {
      bulkPanel.classList.remove('show');
      return;
    }
    renderBulk(kind);
    bulkPanel.classList.add('show');
  }

  function renderBulk(kind) {
    bulkPanel.innerHTML = '';
    const head = el('div', 'bulk-head');
    head.appendChild(el('h2', '', kind === 'work' ? '统一设置强度' : kind === 'scene' ? '统一设置场景' : '全员入场'));
    const close = el('button', 'bulk-close', '×');
    close.type = 'button';
    close.addEventListener('click', () => setBulk(false));
    head.appendChild(close);
    bulkPanel.appendChild(head);
    const body = el('div', 'bulk-body');
    const people = currentPersons().filter((person) => person.liveStatus !== 'out');
    const pending = people.filter((person) => person.liveStatus === 'pending');
    if (kind === 'enter') {
      body.appendChild(el('p', 'bulk-help', pending.length
        ? `将让 ${pending.length} 名预录入人员按当前压力和参数入场`
        : '当前没有可入场的预录入人员'));
    } else {
      const options = kind === 'work' ? WORK_LEVEL_OPTIONS : SCENE_OPTIONS;
      const field = selectControl(kind === 'work' ? '统一强度' : '统一场景', options,
        kind === 'work' ? getScbaSettings().defaultWorkLevel : 'flat');
      body.appendChild(field.row);
      body.appendChild(el('p', 'bulk-help', `将应用到 ${people.length} 名未撤出人员，包含预录入人员`));
      const apply = el('button', 'bulk-confirm', '应用到全员');
      apply.type = 'button';
      apply.addEventListener('click', () => applyBulkContext(kind, field.input.value));
      body.appendChild(apply);
    }
    if (kind === 'enter') {
      const enter = el('button', 'bulk-confirm', '确认全员入场');
      enter.type = 'button';
      enter.disabled = pending.length === 0;
      enter.addEventListener('click', enterAll);
      body.appendChild(enter);
    }
    bulkPanel.appendChild(body);
  }

  async function applyBulkContext(kind, value) {
    const people = currentPersons().filter((person) => person.liveStatus !== 'out');
    if (!people.length) {
      showToast('暂无可调整人员');
      setBulk(false);
      return;
    }
    for (const person of people) {
      await submit({
        eventId: newEventId(),
        type: 'context',
        personId: person.id,
        workLevel: kind === 'work' ? value : (person.workLevel || getScbaSettings().defaultWorkLevel),
        scene: kind === 'scene' ? value : (person.scene || 'flat'),
        clientUpdatedAt: person.gmtModified || null
      });
    }
    setBulk(false);
    showToast(`已更新 ${people.length} 名人员的${kind === 'work' ? '强度' : '场景'}`);
  }

  async function enterAll() {
    const pending = currentPersons().filter((person) => person.liveStatus === 'pending');
    if (!pending.length) {
      showToast('暂无预录入人员');
      setBulk(false);
      return;
    }
    if (!window.confirm(`确认让 ${pending.length} 名人员全部入场？`)) {
      return;
    }
    for (const person of pending) {
      await submit({
        eventId: newEventId(),
        type: 'enter',
        personId: person.id,
        pressure: Number(person.currentPressure || person.initPressure || SCBA.defaultPressure),
        cylType: person.cylType || '6.8',
        workLevel: person.workLevel || getScbaSettings().defaultWorkLevel,
        scene: person.scene || 'flat',
        clientUpdatedAt: person.gmtModified || null
      });
    }
    setBulk(false);
    showToast(`已让 ${pending.length} 名人员入场`);
  }

  function bindQuickHandle(handle) {
    let startY = 0;
    let startH = 0;
    const onMove = (event) => {
      const point = event.touches ? event.touches[0] : event;
      const next = Math.min(window.innerHeight * 0.92, Math.max(window.innerHeight * 0.4, startH + (startY - point.clientY)));
      quick.style.height = `${Math.round(next)}px`;
    };
    const onUp = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
      window.removeEventListener('touchmove', onMove);
      window.removeEventListener('touchend', onUp);
    };
    const onDown = (event) => {
      const point = event.touches ? event.touches[0] : event;
      startY = point.clientY;
      startH = quick.getBoundingClientRect().height;
      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
      window.addEventListener('touchmove', onMove, { passive: true });
      window.addEventListener('touchend', onUp);
    };
    handle.addEventListener('pointerdown', onDown);
    handle.addEventListener('touchstart', onDown, { passive: true });
  }

  function activeIds() {
    const set = {};
    currentPersons().forEach((p) => {
      if (p.profileId && p.liveStatus !== 'out') {
        set[String(p.profileId)] = true;
      }
    });
    return set;
  }

  function renderQuick() {
    quick.innerHTML = '';
    const handle = el('div', 'qp-handle');
    bindQuickHandle(handle);
    const header = el('div', 'qp-header');
    header.appendChild(el('h2', '', '快速录入'));
    const close = el('button', 'qp-close', '✕');
    close.type = 'button';
    close.addEventListener('click', () => setQuick(false));
    header.appendChild(close);
    const body = el('div', 'qp-body');
    const tabs = el('div', 'group-tabs');
    const groups = [{ name: '全部' }].concat((state.roster && state.roster.groups) || []);
    groups.forEach((group) => {
      const tab = el('button', state.groupTab === group.name ? 'gtab active' : 'gtab', group.name);
      tab.type = 'button';
      tab.addEventListener('click', () => {
        state.groupTab = group.name;
        renderQuick();
      });
      tabs.appendChild(tab);
    });
    body.appendChild(tabs);
    const inField = activeIds();
    const profiles = ((state.roster && state.roster.profiles) || []).filter((prof) => {
      if (state.groupTab === '全部') {
        return true;
      }
      const group = ((state.roster && state.roster.groups) || []).find((item) => item.name === state.groupTab);
      return group && (group.members || []).some((m) => String(m.profileId) === String(prof.id));
    });
    if (state.groupTab !== '全部') {
      const pre = el('button', 'btn-group-pre', '一键预录入本组全部人员');
      pre.type = 'button';
      pre.addEventListener('click', () => preAddGroup(profiles, inField));
      body.appendChild(pre);
    }
    body.appendChild(el('div', 'roster-label', '点选人员（可多选）'));
    const grid = el('div', 'roster-grid');
    profiles.forEach((prof) => {
      const chip = el('button', 'roster-chip', prof.name);
      chip.type = 'button';
      if (inField[String(prof.id)]) {
        chip.classList.add('already-in');
      } else if (state.selectedIds[String(prof.id)]) {
        chip.classList.add('selected');
      }
      chip.addEventListener('click', () => {
        if (inField[String(prof.id)]) {
          showToast('该人员已有未撤出卡片');
          return;
        }
        if (state.selectedIds[String(prof.id)]) {
          delete state.selectedIds[String(prof.id)];
        } else {
          state.selectedIds[String(prof.id)] = true;
        }
        renderQuick();
      });
      grid.appendChild(chip);
    });
    if (!profiles.length) {
      grid.appendChild(el('div', 'roster-empty', '本组暂无可录入人员'));
    }
    body.appendChild(grid);

    const inputRow = el('div', 'input-row');
    const defaults = selectedDefaults();
    const pressure = pressureControl('quick', defaults.pressure);
    const cyl = cylSelect(defaults.cylType);
    inputRow.appendChild(pressure);
    inputRow.appendChild(cyl);
    body.appendChild(inputRow);
    const confirm = el('button', 'btn-confirm-add', '确认预录入');
    confirm.type = 'button';
    confirm.addEventListener('click', () => preAddSelected(pressure.input.value, cyl.value));
    body.appendChild(confirm);

    const temp = el('div', 'temp-section');
    temp.appendChild(el('div', 'temp-label', '添加临时人员'));
    const tempRow = el('div', 'temp-row');
    const nameInput = document.createElement('input');
    nameInput.type = 'text';
    nameInput.maxLength = 10;
    nameInput.placeholder = '姓名';
    const tempPressure = pressureControl('temp', SCBA.defaultPressure);
    const tempCyl = cylSelect('6.8');
    const addTemp = el('button', 'btn-temp-add', '添加');
    addTemp.type = 'button';
    addTemp.addEventListener('click', () => {
      const name = (nameInput.value || '').trim();
      if (!name) {
        showToast('请填写临时人员姓名');
        return;
      }
      submit({
        eventId: newEventId(),
        type: 'pre_add',
        displayName: name,
        pressure: Number(tempPressure.input.value),
        cylType: tempCyl.value
      }, `${name} 已预录入`);
    });
    tempRow.appendChild(nameInput);
    tempRow.appendChild(tempPressure);
    tempRow.appendChild(tempCyl);
    tempRow.appendChild(addTemp);
    temp.appendChild(tempRow);
    body.appendChild(temp);

    quick.appendChild(handle);
    quick.appendChild(header);
    quick.appendChild(body);
  }

  function selectedDefaults() {
    const ids = Object.keys(state.selectedIds);
    const first = ((state.roster && state.roster.profiles) || []).find((item) => String(item.id) === ids[0]);
    if (!first) {
      return { pressure: SCBA.defaultPressure, cylType: '6.8' };
    }
    return {
      pressure: Number(first.morningPressure || SCBA.defaultPressure),
      cylType: first.cylType || '6.8'
    };
  }

  function focusPerson(personId) {
    state.focusId = personId;
    renderCards();
    window.setTimeout(() => {
      const node = peoplePage.querySelector(`[data-person-id="${personId}"]`);
      if (node && node.scrollIntoView) {
        node.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }, 0);
    window.setTimeout(() => {
      if (String(state.focusId) === String(personId)) {
        state.focusId = '';
        renderCards();
      }
    }, 3200);
  }

  function findActiveByProfile(profileId) {
    return currentPersons().find((p) => String(p.profileId) === String(profileId) && p.liveStatus !== 'out');
  }

  function findActiveByName(name) {
    return currentPersons().find((p) => p.temp && p.displayName === name && p.liveStatus !== 'out');
  }

  function groupNameOf(profileId) {
    if (!profileId) {
      return SCBA.ungrouped;
    }
    const groups = (state.roster && state.roster.groups) || [];
    for (let i = 0; i < groups.length; i += 1) {
      const group = groups[i];
      const hit = (group.members || []).some((m) => String(m.profileId) === String(profileId));
      if (hit) {
        return group.name;
      }
    }
    return SCBA.ungrouped;
  }

  function applyLocalEvent(body) {
    if (body.type === 'pre_add') {
      const existing = body.profileId ? findActiveByProfile(body.profileId) : findActiveByName(body.displayName);
      if (existing) {
        return existing;
      }
      const prof = body.profileId
        ? ((state.roster && state.roster.profiles) || []).find((item) => String(item.id) === String(body.profileId))
        : null;
      const groupName = body.groupName || groupNameOf(body.profileId);
      const local = {
        id: `local_${body.eventId}`,
        stationId: Number(state.stationId),
        profileId: body.profileId || null,
        displayName: body.displayName || (prof && prof.name) || '',
        groupName: groupName || SCBA.ungrouped,
        cylType: body.cylType || '6.8',
        initPressure: body.pressure,
        currentPressure: body.pressure,
        enteredAt: null,
        withdrawnAt: null,
        workLevel: body.workLevel || getScbaSettings().defaultWorkLevel,
        scene: body.scene || 'flat',
        status: 'pending',
        lifecycleStatus: 'pending',
        pressureAnchor: body.pressure,
        manualPressureAt: null,
        timerPausedAt: null,
        pausedSeconds: 0,
        pressurePausedSeconds: 0,
        remainSec: 0,
        elapsedSec: 0,
        temp: !body.profileId,
        calibrated: false,
        personalK: null,
        clientEventId: body.eventId,
        gmtModified: new Date().toISOString()
      };
      const base = state.attack || { stationId: Number(state.stationId), persons: [] };
      applyAttack(Object.assign({}, base, { persons: (base.persons || []).concat([local]) }));
      return local;
    }
    const persons = ((state.attack && state.attack.persons) || []).slice();
    const idx = persons.findIndex((p) => String(p.id) === String(body.personId));
    if (idx < 0) {
      return null;
    }
    const next = Object.assign({}, persons[idx]);
    if (body.type === 'enter') {
      const enteredAt = new Date().toISOString();
      next.status = 'in';
      next.lifecycleStatus = 'in';
      next.enteredAt = enteredAt;
      next.currentPressure = body.pressure;
      next.initPressure = body.pressure;
      next.pressureAnchor = body.pressure;
      next.manualPressureAt = enteredAt;
      next.timerPausedAt = null;
      next.pausedSeconds = 0;
      next.pressurePausedSeconds = 0;
      next.workLevel = body.workLevel || next.workLevel || getScbaSettings().defaultWorkLevel;
      next.scene = body.scene || next.scene || 'flat';
      next.cylType = body.cylType || next.cylType;
    } else if (body.type === 'context') {
      next.workLevel = body.workLevel || next.workLevel || getScbaSettings().defaultWorkLevel;
      next.scene = body.scene || next.scene || 'flat';
      next.gmtModified = new Date().toISOString();
    } else if (body.type === 'remeasure') {
      next.currentPressure = body.pressure;
      next.pressureAnchor = body.pressure;
      next.manualPressureAt = new Date().toISOString();
      next.pressurePausedSeconds = 0;
      next.gmtModified = new Date().toISOString();
    } else if (body.type === 'pause') {
      next.timerPausedAt = new Date().toISOString();
    } else if (body.type === 'resume') {
      const pausedAt = next.timerPausedAt ? new Date(next.timerPausedAt).getTime() : Date.now();
      const pauseSeconds = Math.max(0, Math.floor((Date.now() - pausedAt) / 1000));
      next.pausedSeconds = Number(next.pausedSeconds || 0) + pauseSeconds;
      next.pressurePausedSeconds = Number(next.pressurePausedSeconds || 0) + pauseSeconds;
      next.timerPausedAt = null;
    } else if (body.type === 'withdraw' || body.type === 'scanout') {
      const pausedAt = next.timerPausedAt ? new Date(next.timerPausedAt).getTime() : 0;
      if (pausedAt) {
        const pauseSeconds = Math.max(0, Math.floor((Date.now() - pausedAt) / 1000));
        next.pausedSeconds = Number(next.pausedSeconds || 0) + pauseSeconds;
        next.pressurePausedSeconds = Number(next.pressurePausedSeconds || 0) + pauseSeconds;
      }
      next.status = 'out';
      next.lifecycleStatus = body.type === 'scanout' ? 'scanout' : 'out';
      next.withdrawnAt = new Date().toISOString();
    } else if (body.type === 'delete') {
      persons.splice(idx, 1);
      const attack = Object.assign({}, state.attack, { persons });
      applyAttack(attack);
      return null;
    }
    next.clientEventId = body.eventId;
    persons[idx] = next;
    applyAttack(Object.assign({}, state.attack, { persons }));
    return next;
  }

  async function preAddSelected(pressure, cylType) {
    const ids = Object.keys(state.selectedIds);
    if (!ids.length) {
      showToast('请先点选人员');
      return;
    }
    for (const id of ids) {
      const prof = ((state.roster && state.roster.profiles) || []).find((item) => String(item.id) === id);
      await submit({
        eventId: newEventId(),
        type: 'pre_add',
        profileId: Number(id),
        displayName: prof ? prof.name : '',
        pressure: Number(pressure),
        cylType: cylType || (prof && prof.cylType) || '6.8'
      });
    }
    state.selectedIds = {};
    showToast('已预录入');
    setQuick(false);
  }

  async function preAddGroup(profiles, inField) {
    const pending = profiles.filter((prof) => !inField[String(prof.id)]);
    if (!pending.length) {
      showToast('本组人员均已在场');
      return;
    }
    for (const prof of pending) {
      await submit({
        eventId: newEventId(),
        type: 'pre_add',
        profileId: prof.id,
        displayName: prof.name,
        pressure: Number(prof.morningPressure || SCBA.defaultPressure),
        cylType: prof.cylType || '6.8'
      });
    }
    showToast('本组未在场人员已预录入');
  }

  function withClientUpdatedAt(body) {
    const next = Object.assign({}, body);
    if (next.clientUpdatedAt) {
      return next;
    }
    if (next.personId == null) {
      return next;
    }
    const local = ((state.attack && state.attack.persons) || []).find((p) => String(p.id) === String(next.personId));
    if (local && local.gmtModified) {
      next.clientUpdatedAt = local.gmtModified;
    }
    return next;
  }

  async function submit(body, okText) {
    if (!state.stationId) {
      showToast('没有可见单位');
      return null;
    }
    const payload = withClientUpdatedAt(body);
    const local = applyLocalEvent(payload);
    await enqueueAttackEvent(state.stationId, payload);
    state.openEnterId = '';
    state.openUpdateId = '';
    delete state.pressureDrafts[`enter:${payload.personId || ''}`];
    delete state.pressureDrafts[`upd:${payload.personId || ''}`];
    renderStats();
    renderCards();
    try {
      await flushQueue();
    } catch (err) {
      if (err.code === 'ATTACK_PERSON_DUPLICATE') {
        showToast(err.message || '该人员已有未撤出卡片');
        await load();
        const existing = payload.profileId ? findActiveByProfile(payload.profileId) : findActiveByName(payload.displayName);
        if (existing) {
          focusPerson(existing.id);
        }
        return existing;
      }
      if (err.code !== 'NETWORK') {
        showToast(err.message || '操作失败');
        return local;
      }
    }
    const current = payload.profileId ? findActiveByProfile(payload.profileId) : local;
    if (current && payload.type === 'pre_add') {
      focusPerson(current.id);
    }
    if (okText) {
      showToast(okText);
    }
    return current;
  }

  function applyAttack(data, options) {
    const opts = options || {};
    let next = data;
    if (opts.merge && state.attack) {
      const merged = mergeAttack(state.attack, data, writtenEventMap());
      next = merged.attack;
      if (merged.overridden) {
        showToast(SYNC.cloudOverride);
      }
    }
    state.attack = next;
    saveAttackCache(state.stationId, next);
    if (next && next.stationName) {
      title.textContent = next.stationName;
      sub.textContent = headSub(me, next);
    }
    renderSyncHint().catch(ignoreBackground);
  }

  async function renderSyncHint() {
    const n = await attackQueueLength();
    syncHint.textContent = `同步状态：${syncStatusText(state.online, n)}`;
    syncHint.className = state.online && n === 0 ? 'sync-hint ok' : 'sync-hint';
  }

  async function flushQueue() {
    if (state.flushPromise) {
      return state.flushPromise;
    }
    state.flushPromise = drainQueue();
    try {
      return await state.flushPromise;
    } finally {
      state.flushPromise = null;
    }
  }

  async function drainQueue() {
    while (true) {
      const queue = await peekAttackQueue();
      if (!queue.length) {
        break;
      }
      const item = queue[0];
      const wait = Number(item.nextAt || 0) - Date.now();
      if (wait > 0) {
        return;
      }
      try {
        const result = await submitAttackEvent(item.stationId, item.body);
        await dropQueueHead();
        if (item.body && item.body.eventId) {
          markWrittenEvent(item.body.eventId);
        }
        if (item.body && item.body.type === 'pre_add' && result.person && item.body.eventId) {
          await rewriteQueuedPersonId(`local_${item.body.eventId}`, result.person.id);
        }
        if (String(item.stationId) === String(state.stationId) && result.attack) {
          applyAttack(result.attack, { merge: true });
        }
        if (result && result.cloudOverride) {
          showToast(SYNC.cloudOverride);
        }
        await renderSyncHint();
      } catch (err) {
        if (isTransient(err) || err.code === 'NETWORK') {
          state.online = false;
          const attempts = await markQueueRetry(retryDelayMs(item.attempts || 0));
          if (attempts >= SYNC.retryMax) {
            showToast(`待传失败 ${attempts} 次，点云同步重试`);
          }
          await renderSyncHint();
          return;
        }
        if (isDropEvent(err)) {
          await dropQueueHead();
          await renderSyncHint();
          throw err;
        }
        state.online = false;
        await markQueueRetry(retryDelayMs(item.attempts || 0));
        await renderSyncHint();
        return;
      }
    }
    state.online = true;
    await renderSyncHint();
  }

  async function load() {
    if (!state.stationId) {
      peoplePage.textContent = '没有可见单位';
      return;
    }
    const cached = readAttackCache(state.stationId);
    if (cached) {
      applyAttack(cached);
      renderStats();
      renderCards();
    }
    try {
      const data = await fetchStationAttack(state.stationId);
      state.online = true;
      applyAttack(data, { merge: Boolean(cached) });
      if (writableStation) {
        state.roster = await fetchRoster(state.stationId);
      }
      await flushQueue();
      renderStats();
      renderCards();
    } catch (err) {
      state.online = err.code === 'NETWORK' ? false : state.online;
      await renderSyncHint();
      if (!cached) {
        peoplePage.textContent = err.message || '加载失败';
      } else {
        showToast('离线显示本地缓存');
      }
    }
  }

  quickBtn.addEventListener('click', () => {
    if (!writableStation) {
      showToast('指挥端只读');
      return;
    }
    setQuick(true);
  });
  moreBtn.addEventListener('click', () => setDrawer(true));
  overlay.addEventListener('click', () => setDrawer(false));
  syncBtn.addEventListener('click', async () => {
    if (state.syncing) {
      return;
    }
    state.syncing = true;
    syncBtn.textContent = '同步中…';
    await resetQueueBackoff();
    try {
      await flushQueue();
      await load();
      const n = await attackQueueLength();
      showToast(n ? `仍有 ${n} 条待传` : '已同步');
    } finally {
      state.syncing = false;
      syncBtn.textContent = '云同步';
      await renderSyncHint();
    }
  });
  nfcBtn.addEventListener('click', async () => {
    if (!writableStation) {
      return;
    }
    if (!state.nfcReady) {
      setQuick(true);
      return;
    }
    const result = await bridge.nfcRead();
    if (!result || !result.ok || !result.data || !result.data.tag) {
      const code = result && result.errorCode ? result.errorCode : '';
      if (code === 'NO_NFC' || code === 'UNSUPPORTED') {
        showToast('本机无 NFC，请用快速录入');
        setQuick(true);
        return;
      }
      showToast('未读到标签，请重试或用手选');
      return;
    }
    const tag = String(result.data.tag || '').replace(/[\s:：\-_]/g, '').toUpperCase();
    const roster = state.roster || {};
    const matched = ((roster.profiles) || []).find((prof) => tagMatchesProfile(prof.nfcTag, tag));
    if (!matched) {
      showToast('未找到对应花名册');
      setQuick(true);
      return;
    }
    const existing = currentPersons().find((p) => String(p.profileId) === String(matched.id) && p.liveStatus !== 'out');
    if (existing) {
      if (existing.liveStatus === 'pending') {
        state.openEnterId = existing.id;
        state.openUpdateId = '';
        renderCards();
        showToast(`${matched.name} 已预录入，请确认入场`);
        return;
      }
      if (existing.liveStatus === 'out') {
        return;
      }
      await submit({
        eventId: newEventId(),
        type: 'scanout',
        personId: existing.id,
        nfcTag: tag,
        clientUpdatedAt: existing.gmtModified || null
      }, `${matched.name} 已扫码撤出`);
      return;
    }
    const pressure = Number(matched.morningPressure || SCBA.defaultPressure);
    const cylType = matched.cylType || '6.8';
    if (matched.morningPressure) {
      const pre = await submit({
        eventId: newEventId(),
        type: 'pre_add',
        profileId: matched.id,
        nfcTag: tag,
        pressure,
        cylType
      });
      const card = pre || findActiveByProfile(matched.id);
      if (card && (card.liveStatus === 'pending' || card.status === 'pending')) {
        await submit({
          eventId: newEventId(),
          type: 'enter',
          personId: card.id,
          pressure,
          cylType
        }, `${matched.name} 已入场`);
        return;
      }
      return;
    }
    await submit({
      eventId: newEventId(),
      type: 'pre_add',
      profileId: matched.id,
      nfcTag: tag,
      pressure,
      cylType
    }, `${matched.name} 已预录入`);
  });

  bulkWorkBtn.addEventListener('click', () => {
    if (writableStation) {
      setBulk(true, 'work');
    }
  });
  bulkSceneBtn.addEventListener('click', () => {
    if (writableStation) {
      setBulk(true, 'scene');
    }
  });
  bulkEnterBtn.addEventListener('click', () => {
    if (writableStation) {
      setBulk(true, 'enter');
    }
  });

  archiveBtn.addEventListener('click', async () => {
    if (!writableStation) {
      showToast('指挥端只读');
      return;
    }
    let persons = currentPersons();
    if (!persons.length) {
      showToast('暂无内攻人员卡片');
      return;
    }
    let active = persons.filter((p) => p.liveStatus !== 'out');
    if (active.length) {
      showToast(`仍有 ${active.length} 人未撤出，全部撤出后才能归档`);
      return;
    }
    try {
      await resetQueueBackoff();
      await flushQueue();
    } catch (err) {
      showToast(err.message || '操作同步失败，请重试');
      return;
    }
    const pending = await attackQueueLength(state.stationId);
    if (pending > 0) {
      showToast(`仍有 ${pending} 条操作待同步，请检查网络后重试`);
      return;
    }
    try {
      const cloudAttack = await fetchStationAttack(state.stationId);
      applyAttack(cloudAttack);
      renderStats();
      renderCards();
    } catch (err) {
      showToast(err.message || '无法确认人员撤出状态，请重试');
      return;
    }
    persons = currentPersons();
    active = persons.filter((p) => p.liveStatus !== 'out');
    if (active.length) {
      showToast(`仍有 ${active.length} 人未撤出，全部撤出后才能归档`);
      return;
    }
    const kind = window.prompt('类型（drill=演练 / dispatch=出警）：', 'drill');
    if (kind === null) {
      return;
    }
    const name = window.prompt('事件名称：', '');
    if (name === null) {
      return;
    }
    const location = window.prompt('地点：', '');
    if (location === null) {
      return;
    }
    /* 归档是破坏性操作：确认后人员卡片将被清空，只能在内攻历史中查看 */
    const confirmed = window.confirm(
      `确认归档本次内攻？归档后本站 ${persons.length} 张人员卡片将被清空，仅能在内攻历史中查看。`);
    if (!confirmed) {
      return;
    }
    try {
      await archiveStation(state.stationId, {
        eventKind: kind.trim() || 'drill',
        eventName: name.trim(),
        location: location.trim()
      });
      showToast('本次内攻已归档，可在内攻历史中查看');
      await load();
    } catch (err) {
      showToast(err.message || '归档失败');
    }
  });

  if (!writableStation) {
    quickBtn.style.display = 'none';
    nfcBtn.style.display = 'none';
    nfcBar.style.display = 'none';
  }

  const onOnline = () => {
    state.online = true;
    renderSyncHint().catch(ignoreBackground);
    flushQueue().then(() => {
      renderStats();
      renderCards();
    }).catch(ignoreBackground);
  };
  const onOffline = () => {
    state.online = false;
    renderSyncHint().catch(ignoreBackground);
  };
  window.addEventListener('online', onOnline);
  window.addEventListener('offline', onOffline);

  const timer = window.setInterval(() => {
    if (!state.attack) {
      return;
    }
    if (shouldDeferBackgroundPaint()) {
      return;
    }
    renderStats();
    renderCards();
    peekAttackQueue().then((queue) => {
      const head = queue[0];
      if (head && Number(head.nextAt || 0) <= Date.now()) {
        flushQueue().catch(ignoreBackground);
      }
    }).catch(ignoreBackground);
  }, 1000);
  renderSyncHint().catch(ignoreBackground);

  load();
  if (window.location.hash.indexOf('/attack/quick-add') >= 0 && writableStation) {
    window.setTimeout(() => setQuick(true), 0);
  }

  setPageBackHandler(() => {
    if (quick.classList.contains('show')) {
      setQuick(false);
      return true;
    }
    if (drawer.classList.contains('show')) {
      setDrawer(false);
      return true;
    }
    if (bulkPanel.classList.contains('show')) {
      setBulk(false);
      return true;
    }
    return !confirmLeaveMain();
  });

  return () => {
    setPageBackHandler(null);
    window.clearInterval(timer);
    window.removeEventListener('online', onOnline);
    window.removeEventListener('offline', onOffline);
  };
}
