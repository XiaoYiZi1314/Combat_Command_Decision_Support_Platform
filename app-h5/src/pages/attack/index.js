import './attack.css';
import { getMe } from '../../stores/session.js';
import { fetchRoster } from '../../api/roster.js';
import { fetchStationAttack, submitAttackEvent } from '../../api/attack.js';
import { bridge } from '../../bridge/index.js';
import {
  attackQueueLength,
  enqueueAttackEvent,
  getAttackStationId,
  peekAttackQueue,
  readAttackCache,
  rewriteQueuedPersonId,
  saveAttackCache,
  setAttackStationId,
  shiftAttackQueue
} from '../../stores/attack.js';
import {
  SCBA,
  fmtElapsed,
  fmtMinSec,
  newEventId,
  remainSecOf,
  resolveStatus,
  statusLabel,
  worstStatus
} from '../../lib/scba.js';

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

function visibleStations() {
  const me = getMe() || {};
  return Array.isArray(me.visibleStations) ? me.visibleStations : [];
}

function headSub(me, attack) {
  const stationName = (attack && attack.stationName) || me.stationName;
  const brigadeName = (attack && attack.brigadeName) || me.brigadeName;
  if (brigadeName && stationName && brigadeName !== stationName) {
    return `${brigadeName} / ${stationName}`;
  }
  return brigadeName || stationName || '作战指挥辅助决策平台';
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

function countsOf(persons) {
  const counts = { in: 0, warn: 0, danger: 0, out: 0, pending: 0 };
  persons.forEach((p) => {
    const key = p.liveStatus;
    if (counts[key] != null) {
      counts[key] += 1;
    }
  });
  return counts;
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
    selectedIds: {},
    groupTab: '全部',
    focusId: '',
    nfcReady: bridge.hasNfc(),
    syncing: false
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
  quickBtn.type = 'button';
  syncBtn.type = 'button';
  moreBtn.type = 'button';
  actions.appendChild(quickBtn);
  actions.appendChild(syncBtn);
  actions.appendChild(moreBtn);
  page.appendChild(actions);

  const list = el('div', 'list main-pager');
  const peoplePage = el('div', 'main-page main-people-page');
  const groupPage = el('div', 'main-page main-group-page');
  list.appendChild(peoplePage);
  list.appendChild(groupPage);
  page.appendChild(list);

  const toast = el('div', 'attack-toast');
  page.appendChild(toast);

  const nfcBar = el('div', 'nfc-bar');
  const nfcBtn = el('button', '', state.nfcReady ? '请进行NFC扫描' : '本机无 NFC，请用快速录入');
  nfcBtn.type = 'button';
  nfcBar.appendChild(nfcBtn);
  page.appendChild(nfcBar);

  const overlay = el('div', 'drawer-overlay');
  const drawer = el('div', 'drawer');
  drawer.appendChild(buildDrawer());
  page.appendChild(overlay);
  page.appendChild(drawer);

  const quick = el('div', 'quick-panel');
  page.appendChild(quick);
  root.appendChild(page);

  function showToast(message) {
    toast.textContent = message;
    toast.classList.add('show');
    window.setTimeout(() => toast.classList.remove('show'), 2200);
  }

  function currentPersons() {
    const now = Date.now();
    const listData = ((state.attack && state.attack.persons) || []).map((p) => livePerson(p, now));
    return listData;
  }

  function renderStats() {
    const counts = countsOf(currentPersons());
    stats.innerHTML = '';
    [
      ['in', '安全', 'var(--blue)', counts.in],
      ['warn', '预警', 'var(--amber)', counts.warn],
      ['danger', '危险', 'var(--red)', counts.danger],
      ['out', '已撤出', 'var(--dim)', counts.out]
    ].forEach((item) => {
      const box = el('div', state.filter === item[0] ? 'stat active' : 'stat');
      const num = el('div', 'num', String(item[3]));
      num.style.color = item[2];
      box.appendChild(num);
      box.appendChild(el('div', 'label', item[1]));
      box.addEventListener('click', () => {
        state.filter = state.filter === item[0] ? '' : item[0];
        renderCards();
        renderStats();
      });
      stats.appendChild(box);
    });
  }

  function renderCards() {
    peoplePage.innerHTML = '';
    const persons = currentPersons();
    const filtered = state.filter ? persons.filter((p) => p.liveStatus === state.filter) : persons;
    if (!filtered.length) {
      const empty = el('div', 'empty');
      empty.appendChild(el('div', 'station-badge', ((state.attack && state.attack.stationName) || '本站').slice(0, 1)));
      empty.appendChild(el('div', 'station', (state.attack && state.attack.stationName) || '本站'));
      empty.appendChild(el('p', '', '暂无内攻人员'));
      peoplePage.appendChild(empty);
      renderGroups(persons);
      return;
    }
    filtered.forEach((person) => peoplePage.appendChild(renderCard(person)));
    renderGroups(persons);
  }

  function renderGroups(persons) {
    groupPage.innerHTML = '';
    const map = {};
    persons.forEach((p) => {
      const name = p.groupName || SCBA.ungrouped;
      if (!map[name]) {
        map[name] = { name, items: [] };
      }
      map[name].items.push(p);
    });
    const names = Object.keys(map);
    if (!names.length) {
      groupPage.appendChild(el('div', 'empty', '暂无战斗编组'));
      return;
    }
    names.forEach((name) => {
      const row = map[name];
      const card = el('div', 'group-overview-card');
      const worst = worstStatus(row.items.map((item) => item.liveStatus));
      card.appendChild(el('div', 'gc-name', `${name} · ${statusLabel(worst)}`));
      const counts = countsOf(row.items);
      card.appendChild(el('div', 'gc-counts', `安全 ${counts.in}  预警 ${counts.warn}  危险 ${counts.danger}  预录入 ${counts.pending}`));
      groupPage.appendChild(card);
    });
  }

  function renderCard(person) {
    const card = el('div', `card ${person.liveStatus}`);
    card.dataset.personId = String(person.id);
    if (String(state.focusId) === String(person.id)) {
      card.classList.add('focus');
    }
    const top = el('div', 'top');
    const name = el('div', 'name', person.displayName || '');
    name.appendChild(el('span', 'cyl-tag', `${person.cylType || '6.8'}L`));
    if (person.groupName) {
      name.appendChild(el('span', 'group-tag', person.groupName));
    }
    if (person.temp) {
      name.appendChild(el('span', 'temp-tag', '临时'));
    }
    if (person.calibrated) {
      name.appendChild(el('span', 'curve-tag', '标定'));
    }
    top.appendChild(name);
    top.appendChild(el('div', `status s-${person.liveStatus}`, statusLabel(person.liveStatus)));
    card.appendChild(top);

    const metrics = el('div', 'metrics');
    const pressure = Number(person.currentPressure || 0);
    const remain = person.liveRemain;
    const pCls = pressure <= SCBA.dangerPressure ? 'c-danger' : (pressure <= SCBA.warnPressure ? 'c-warn' : 'c-ok');
    const rCls = remain != null && remain <= SCBA.dangerTimeSec ? 'c-danger' : (remain != null && remain <= SCBA.warnTimeSec ? 'c-warn' : 'c-ok');
    metrics.appendChild(metric('当前压力', pressure.toFixed(1), 'MPa', pCls));
    metrics.appendChild(metric('剩余时间', person.liveStatus === 'pending' || person.liveStatus === 'out' ? '--' : fmtMinSec(remain), '', rCls));
    metrics.appendChild(metric('已作业', person.liveStatus === 'pending' ? '--' : fmtElapsed(person.liveElapsed), '', 'c-ok'));
    card.appendChild(metrics);

    const bar = el('div', 'bar');
    const fill = el('div', 'bar-fill');
    const init = Number(person.initPressure || 0);
    fill.style.width = init > 0 ? `${Math.min(100, pressure / init * 100)}%` : '0%';
    fill.style.background = pressure <= SCBA.dangerPressure ? 'var(--red)' : (pressure <= SCBA.warnPressure ? 'var(--amber)' : 'var(--blue)');
    bar.appendChild(fill);
    card.appendChild(bar);

    if (writableStation) {
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
          personId: person.id
        }, '已删除'));
        actionsRow.appendChild(enter);
        actionsRow.appendChild(del);
      } else if (person.liveStatus !== 'out') {
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
          personId: person.id
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
    }
    return card;
  }

  function metric(label, value, unit, cls) {
    const box = el('div', 'metric');
    const val = el('div', `val ${cls}`, unit ? `${value} ${unit}` : value);
    box.appendChild(val);
    box.appendChild(el('div', 'lbl', label));
    return box;
  }

  function pressureControl(id, value) {
    const wrap = el('div', 'pressure-control');
    const headRow = el('div', 'pressure-head');
    headRow.appendChild(el('span', '', '压力'));
    const label = el('b', '', `${Number(value).toFixed(1)} MPa`);
    headRow.appendChild(label);
    const input = document.createElement('input');
    input.type = 'range';
    input.className = 'pressure-range';
    input.min = String(SCBA.pressureMin);
    input.max = String(SCBA.pressureMax);
    input.step = String(SCBA.pressureStep);
    input.value = String(value);
    input.addEventListener('input', () => {
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
    const pressure = pressureControl('enter', person.currentPressure || SCBA.defaultPressure);
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
    const pressure = pressureControl('upd', person.currentPressure || SCBA.defaultPressure);
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

  function buildDrawer() {
    const wrap = document.createDocumentFragment();
    const headRow = el('div', 'drawer-head');
    headRow.appendChild(el('h2', '', '更多功能'));
    const close = el('button', 'drawer-close', '✕');
    close.type = 'button';
    close.addEventListener('click', () => setDrawer(false));
    headRow.appendChild(close);
    wrap.appendChild(headRow);
    const body = el('div', 'drawer-body');
    [
      ['人员档案', '#/roster'],
      ['天气与方位', '#/weather'],
      ['空呼时间计算器', '#/scba'],
      ['重点单位', '#/key-units'],
      ['水源档案', '#/water'],
      ['二维码共享', '#/share'],
      ['危化品查询', '#/hazmat'],
      ['值班表', '#/duty'],
      ['供水计算', '#/supply'],
      ['AI助手', '#/ai'],
      ['版本信息', '#/version']
    ].forEach((item) => {
      const btn = el('button', 'drawer-item', item[0]);
      btn.type = 'button';
      btn.addEventListener('click', () => {
        setDrawer(false);
        window.location.hash = item[1];
      });
      body.appendChild(btn);
    });
    wrap.appendChild(body);
    return wrap;
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
        workLevel: body.workLevel || 'moderate',
        scene: body.scene || 'flat',
        status: 'pending',
        remainSec: null,
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
      next.status = 'in';
      next.enteredAt = new Date().toISOString();
      next.currentPressure = body.pressure;
      next.initPressure = body.pressure;
      next.cylType = body.cylType || next.cylType;
    } else if (body.type === 'remeasure') {
      next.currentPressure = body.pressure;
      next.gmtModified = new Date().toISOString();
    } else if (body.type === 'withdraw') {
      next.status = 'out';
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

  async function submit(body, okText) {
    if (!state.stationId) {
      showToast('没有可见单位');
      return null;
    }
    const local = applyLocalEvent(body);
    enqueueAttackEvent(state.stationId, body);
    state.openEnterId = '';
    state.openUpdateId = '';
    renderStats();
    renderCards();
    try {
      await flushQueue();
    } catch (err) {
      if (err.code === 'ATTACK_PERSON_DUPLICATE') {
        showToast(err.message || '该人员已有未撤出卡片');
        await load();
        const existing = body.profileId ? findActiveByProfile(body.profileId) : findActiveByName(body.displayName);
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
    const current = body.profileId ? findActiveByProfile(body.profileId) : local;
    if (current && body.type === 'pre_add') {
      focusPerson(current.id);
    }
    if (okText) {
      showToast(okText);
    }
    return current;
  }

  function applyAttack(data) {
    state.attack = data;
    saveAttackCache(state.stationId, data);
    if (data && data.stationName) {
      title.textContent = data.stationName;
      sub.textContent = headSub(me, data);
    }
  }

  async function flushQueue() {
    while (peekAttackQueue().length) {
      const item = peekAttackQueue()[0];
      try {
        const result = await submitAttackEvent(item.stationId, item.body);
        shiftAttackQueue();
        if (item.body && item.body.type === 'pre_add' && result.person && item.body.eventId) {
          rewriteQueuedPersonId(`local_${item.body.eventId}`, result.person.id);
        }
        if (String(item.stationId) === String(state.stationId)) {
          applyAttack(result.attack);
        }
      } catch (err) {
        if (err.code === 'NETWORK') {
          return;
        }
        if (err.code === 'ATTACK_PERSON_DUPLICATE' || err.code === 'ATTACK_EVENT_INVALID'
            || err.code === 'ATTACK_STATUS_INVALID') {
          shiftAttackQueue();
          throw err;
        }
        return;
      }
    }
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
      applyAttack(data);
      if (writableStation) {
        state.roster = await fetchRoster(state.stationId);
      }
      await flushQueue();
      renderStats();
      renderCards();
    } catch (err) {
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
    syncBtn.textContent = '同步中…';
    await flushQueue();
    await load();
    const n = attackQueueLength();
    syncBtn.textContent = '云同步';
    showToast(n ? `仍有 ${n} 条待传` : '已同步');
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
      showToast('本机无 NFC，请用快速录入');
      return;
    }
    const tag = String(result.data.tag || '').replace(/[\s:：\-_]/g, '').toUpperCase();
    const roster = state.roster || {};
    const matched = ((roster.profiles) || []).find((prof) => {
      const nfc = String(prof.nfcTag || '').replace(/[\s:：\-_]/g, '').toUpperCase();
      return nfc && nfc === tag;
    });
    if (!matched) {
      showToast('未找到对应花名册');
      setQuick(true);
      return;
    }
    const existing = currentPersons().find((p) => String(p.profileId) === String(matched.id) && p.liveStatus !== 'out');
    if (existing && existing.liveStatus === 'pending') {
      state.openEnterId = existing.id;
      state.openUpdateId = '';
      renderCards();
      showToast(`${matched.name} 已预录入，请确认入场`);
      return;
    }
    if (existing) {
      state.openUpdateId = existing.id;
      state.openEnterId = '';
      renderCards();
      showToast(`${matched.name} 已在场，请复测`);
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

  if (!writableStation) {
    quickBtn.style.display = 'none';
    nfcBar.style.display = 'none';
  }

  const timer = window.setInterval(() => {
    if (!state.attack) {
      return;
    }
    renderStats();
    renderCards();
  }, 1000);

  load();
  if (window.location.hash.indexOf('/attack/quick-add') >= 0 && writableStation) {
    window.setTimeout(() => setQuick(true), 0);
  }
  return () => window.clearInterval(timer);
}
