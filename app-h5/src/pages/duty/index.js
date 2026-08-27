import './duty.css';
import { deleteDutyDay, fetchDutyMonth, saveDutyDay } from '../../api/duty.js';
import { fetchRoster } from '../../api/roster.js';
import { getMe, homeHashOf } from '../../stores/session.js';

const WEEK_LABELS = ['一', '二', '三', '四', '五', '六', '日'];
const NOTE_MAX_LENGTH = 500;
const SNAPSHOT_VERSION = 1;

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  if (text != null) {
    node.textContent = text;
  }
  return node;
}

function pad(value) {
  return String(value).padStart(2, '0');
}

function dateKey(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function parseDate(key) {
  const parts = String(key || '').split('-').map(Number);
  return new Date(parts[0], parts[1] - 1, parts[2]);
}

function addDays(date, days) {
  const next = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  next.setDate(next.getDate() + days);
  return next;
}

function visibleStations() {
  const me = getMe() || {};
  return Array.isArray(me.visibleStations) ? me.visibleStations : [];
}

function initialStationId() {
  const me = getMe() || {};
  if (me.role === 'station' && me.stationId) {
    return String(me.stationId);
  }
  const first = visibleStations()[0];
  return first ? String(first.id) : '';
}

function safeSnapshotGroups(raw) {
  if (!raw) {
    return [];
  }
  try {
    const snapshot = JSON.parse(raw);
    return Array.isArray(snapshot.groups) ? snapshot.groups : [];
  } catch (err) {
    return [];
  }
}

function snapshotOf(groups) {
  const safeGroups = (groups || []).map((group) => ({
    name: group.name || '未命名编组',
    members: (group.members || []).map((member) => ({
      profileId: member.profileId,
      name: member.name || '',
      roleInGroup: member.roleInGroup || '组员'
    }))
  }));
  return JSON.stringify({ version: SNAPSHOT_VERSION, groups: safeGroups });
}

export function renderDutyPage(root) {
  const me = getMe() || {};
  const writable = me.role === 'station';
  const now = new Date();
  const state = {
    stationId: initialStationId(),
    month: new Date(now.getFullYear(), now.getMonth(), 1),
    selectedDate: dateKey(now),
    rows: new Map(),
    roster: { profiles: [], groups: [] },
    loadVersion: 0
  };

  root.innerHTML = '';
  const page = el('div', 'duty-page');
  const head = el('div', 'duty-head');
  head.appendChild(el('h2', '', '值班表'));
  const back = el('button', 'duty-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(getMe());
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'duty-body');
  page.appendChild(body);
  root.appendChild(page);

  if (!state.stationId) {
    body.appendChild(el('div', 'duty-section duty-empty', '当前账号没有可查看的消防站'));
    return;
  }

  if (!writable) {
    const stationWrap = el('div', 'duty-station');
    stationWrap.appendChild(el('label', '', '查看单位'));
    const stationSelect = document.createElement('select');
    visibleStations().forEach((station) => {
      const option = document.createElement('option');
      option.value = String(station.id);
      option.textContent = station.name;
      stationSelect.appendChild(option);
    });
    stationSelect.value = state.stationId;
    stationSelect.addEventListener('change', () => {
      state.stationId = stationSelect.value;
      loadMonth();
    });
    stationWrap.appendChild(stationSelect);
    body.appendChild(stationWrap);
  }

  const calendarSection = el('div', 'duty-section');
  const monthHead = el('div', 'duty-month-head');
  const prev = el('button', 'duty-btn', '‹');
  const monthTitle = el('div', 'duty-month-title');
  const next = el('button', 'duty-btn', '›');
  prev.type = 'button';
  next.type = 'button';
  monthHead.appendChild(prev);
  monthHead.appendChild(monthTitle);
  monthHead.appendChild(next);
  calendarSection.appendChild(monthHead);
  const week = el('div', 'duty-week');
  WEEK_LABELS.forEach((label) => week.appendChild(el('span', '', label)));
  calendarSection.appendChild(week);
  const calendar = el('div', 'duty-calendar');
  calendarSection.appendChild(calendar);
  body.appendChild(calendarSection);

  const detail = el('div', 'duty-section');
  const shortcuts = el('div', 'duty-shortcuts');
  const todayButton = el('button', 'duty-btn', '今日');
  const tomorrowButton = el('button', 'duty-btn', '明日');
  todayButton.type = 'button';
  tomorrowButton.type = 'button';
  shortcuts.appendChild(todayButton);
  shortcuts.appendChild(tomorrowButton);
  detail.appendChild(shortcuts);
  const message = el('div', 'duty-msg');
  detail.appendChild(message);
  const detailContent = el('div');
  detail.appendChild(detailContent);
  body.appendChild(detail);

  function setMessage(text, error) {
    message.className = error ? 'duty-msg error' : 'duty-msg';
    message.textContent = text || '';
  }

  function selectDate(date) {
    state.selectedDate = dateKey(date);
    const targetMonth = new Date(date.getFullYear(), date.getMonth(), 1);
    if (targetMonth.getTime() !== state.month.getTime()) {
      state.month = targetMonth;
      loadMonth();
      return;
    }
    renderCalendar();
    renderDetail();
  }

  prev.addEventListener('click', () => {
    state.month = new Date(state.month.getFullYear(), state.month.getMonth() - 1, 1);
    state.selectedDate = dateKey(state.month);
    loadMonth();
  });
  next.addEventListener('click', () => {
    state.month = new Date(state.month.getFullYear(), state.month.getMonth() + 1, 1);
    state.selectedDate = dateKey(state.month);
    loadMonth();
  });
  todayButton.addEventListener('click', () => selectDate(new Date()));
  tomorrowButton.addEventListener('click', () => selectDate(addDays(new Date(), 1)));

  function renderCalendar() {
    calendar.innerHTML = '';
    monthTitle.textContent = `${state.month.getFullYear()}年${state.month.getMonth() + 1}月`;
    const first = new Date(state.month.getFullYear(), state.month.getMonth(), 1);
    const leading = (first.getDay() + 6) % 7;
    for (let i = 0; i < leading; i += 1) {
      calendar.appendChild(el('div', 'duty-day-empty'));
    }
    const total = new Date(state.month.getFullYear(), state.month.getMonth() + 1, 0).getDate();
    const todayKey = dateKey(new Date());
    for (let day = 1; day <= total; day += 1) {
      const date = new Date(state.month.getFullYear(), state.month.getMonth(), day);
      const key = dateKey(date);
      const button = el('button', 'duty-day', String(day));
      button.type = 'button';
      if (state.rows.has(key)) {
        button.classList.add('has-duty');
      }
      if (key === todayKey) {
        button.classList.add('today');
      }
      if (key === state.selectedDate) {
        button.classList.add('selected');
      }
      button.addEventListener('click', () => selectDate(date));
      calendar.appendChild(button);
    }
  }

  function fillOfficerSelect(select, selectedId) {
    const empty = document.createElement('option');
    empty.value = '';
    empty.textContent = '未指定';
    select.appendChild(empty);
    (state.roster.profiles || []).forEach((profile) => {
      const option = document.createElement('option');
      option.value = String(profile.id);
      option.textContent = profile.title ? `${profile.name} · ${profile.title}` : profile.name;
      select.appendChild(option);
    });
    select.value = selectedId == null ? '' : String(selectedId);
  }

  function appendGroups(container, groups) {
    const box = el('div', 'duty-groups');
    box.appendChild(el('h3', '', '当日战斗编组快照'));
    if (!groups.length) {
      box.appendChild(el('div', 'duty-empty', '暂无战斗编组'));
    }
    groups.forEach((group) => {
      const names = (group.members || []).map((member) => {
        const role = member.roleInGroup ? `（${member.roleInGroup}）` : '';
        return `${member.name || '未命名'}${role}`;
      });
      box.appendChild(el('div', 'duty-group', `${group.name || '未命名编组'}：${names.join('、') || '暂无成员'}`));
    });
    container.appendChild(box);
  }

  function officerCard(title, value, deputy) {
    const card = el('div', deputy ? 'duty-officer deputy' : 'duty-officer');
    card.appendChild(el('div', 'duty-officer-title', title));
    card.appendChild(el('div', 'duty-read-name', value || '未指定'));
    return card;
  }

  function renderReadonly(row) {
    const officers = el('div', 'duty-officers');
    officers.appendChild(officerCard('主官', row && row.mainOfficerName, false));
    officers.appendChild(officerCard('副职', row && row.deputyOfficerName, true));
    detailContent.appendChild(officers);
    const groups = row ? safeSnapshotGroups(row.groupSnapshotJson) : [];
    appendGroups(detailContent, groups);
    detailContent.appendChild(el('div', 'duty-field', '备注'));
    detailContent.appendChild(el('div', 'duty-note-view', row && row.note ? row.note : '暂无备注'));
    if (!row) {
      detailContent.appendChild(el('div', 'duty-empty', '该日暂无值班安排'));
    }
  }

  function renderEditor(row) {
    const officers = el('div', 'duty-officers');
    const mainCard = el('div', 'duty-officer');
    mainCard.appendChild(el('div', 'duty-officer-title', '主官'));
    const mainSelect = document.createElement('select');
    fillOfficerSelect(mainSelect, row && row.mainOfficerId);
    mainCard.appendChild(mainSelect);
    const deputyCard = el('div', 'duty-officer deputy');
    deputyCard.appendChild(el('div', 'duty-officer-title', '副职'));
    const deputySelect = document.createElement('select');
    fillOfficerSelect(deputySelect, row && row.deputyOfficerId);
    deputyCard.appendChild(deputySelect);
    officers.appendChild(mainCard);
    officers.appendChild(deputyCard);
    detailContent.appendChild(officers);

    appendGroups(detailContent, state.roster.groups || []);
    const noteField = el('div', 'duty-field');
    noteField.appendChild(el('label', '', '备注'));
    const note = document.createElement('input');
    note.type = 'text';
    note.maxLength = NOTE_MAX_LENGTH;
    note.placeholder = '可填写带班、备勤或特殊任务';
    note.value = row && row.note ? row.note : '';
    noteField.appendChild(note);
    detailContent.appendChild(noteField);

    const actions = el('div', 'duty-actions');
    const save = el('button', 'duty-btn primary', '保存并同步');
    save.type = 'button';
    save.addEventListener('click', async () => {
      save.disabled = true;
      setMessage('正在保存…');
      try {
        const saved = await saveDutyDay(state.stationId, state.selectedDate, {
          dutyDate: state.selectedDate,
          mainOfficerId: mainSelect.value ? Number(mainSelect.value) : null,
          deputyOfficerId: deputySelect.value ? Number(deputySelect.value) : null,
          note: note.value.trim(),
          groupSnapshotJson: snapshotOf(state.roster.groups)
        });
        state.rows.set(state.selectedDate, saved);
        setMessage('值班表已保存并同步');
        renderCalendar();
        renderDetail();
      } catch (err) {
        setMessage(err.message || '保存失败', true);
      } finally {
        save.disabled = false;
      }
    });
    actions.appendChild(save);
    if (row) {
      const remove = el('button', 'duty-btn danger', '清空当日');
      remove.type = 'button';
      remove.addEventListener('click', async () => {
        if (!window.confirm(`确认清空 ${state.selectedDate} 的值班安排？`)) {
          return;
        }
        remove.disabled = true;
        try {
          await deleteDutyDay(state.stationId, state.selectedDate);
          state.rows.delete(state.selectedDate);
          setMessage('当日值班安排已清空');
          renderCalendar();
          renderDetail();
        } catch (err) {
          setMessage(err.message || '清空失败', true);
        } finally {
          remove.disabled = false;
        }
      });
      actions.appendChild(remove);
    }
    detailContent.appendChild(actions);
  }

  function renderDetail() {
    detailContent.innerHTML = '';
    const selected = parseDate(state.selectedDate);
    detailContent.appendChild(el('div', 'duty-date-title',
      `${selected.getMonth() + 1}月${selected.getDate()}日值班情况`));
    const row = state.rows.get(state.selectedDate) || null;
    if (writable) {
      renderEditor(row);
    } else {
      renderReadonly(row);
    }
  }

  async function loadMonth() {
    const version = state.loadVersion + 1;
    state.loadVersion = version;
    setMessage('正在加载值班表…');
    try {
      const [rows, roster] = await Promise.all([
        fetchDutyMonth(state.stationId, state.month.getFullYear(), state.month.getMonth() + 1),
        fetchRoster(state.stationId)
      ]);
      if (version !== state.loadVersion) {
        return;
      }
      state.rows = new Map((rows || []).map((row) => [row.dutyDate, row]));
      state.roster = roster || { profiles: [], groups: [] };
      setMessage('');
      renderCalendar();
      renderDetail();
    } catch (err) {
      if (version !== state.loadVersion) {
        return;
      }
      state.rows = new Map();
      state.roster = { profiles: [], groups: [] };
      setMessage(err.message || '值班表加载失败', true);
      renderCalendar();
      renderDetail();
    }
  }

  loadMonth();
}
