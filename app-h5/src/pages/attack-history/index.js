import '../water/water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { el } from '../water/shared.js';
import {
  deleteArchive,
  downloadArchiveExcel,
  downloadArchiveStatsExcel,
  fetchAllArchives,
  fetchStationArchives,
  updateArchiveInfo
} from '../../api/archive.js';

const STATION_KEY = 'ccds_archive_station';

const KIND_LABELS = {
  drill: '演练',
  dispatch: '出警'
};

function visibleStations() {
  const me = getMe() || {};
  return Array.isArray(me.visibleStations) ? me.visibleStations : [];
}

function currentStationId() {
  const me = getMe() || {};
  if (me.role === 'station' && me.stationId) {
    return String(me.stationId);
  }
  const saved = localStorage.getItem(STATION_KEY);
  if (saved && visibleStations().some((item) => String(item.id) === saved)) {
    return saved;
  }
  const first = visibleStations()[0];
  return first ? String(first.id) : '';
}

function setStationId(id) {
  localStorage.setItem(STATION_KEY, String(id));
}

function kindLabel(kind) {
  return KIND_LABELS[kind] || kind || '演练';
}

export function renderAttackHistoryPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const isCommand = me.role === 'hq' || me.role === 'developer' || me.role === 'brigade';
  const canManage = me.role === 'hq' || me.role === 'developer' || me.role === 'station';

  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', '内攻历史'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(me);
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  const state = {
    kindFilter: '',
    rows: []
  };

  /* 站切换（非站级） */
  const stations = visibleStations();
  if (me.role !== 'station' && stations.length) {
    const wrap = el('div', 'water-station');
    wrap.appendChild(el('label', '', '查看单位'));
    const select = el('select');
    if (me.role === 'hq' || me.role === 'developer') {
      select.appendChild(el('option', '', '全部单位')).value = '';
    }
    stations.forEach((station) => {
      const opt = document.createElement('option');
      opt.value = String(station.id);
      opt.textContent = station.name;
      select.appendChild(opt);
    });
    select.value = currentStationId();
    select.addEventListener('change', () => {
      setStationId(select.value);
      load();
    });
    wrap.appendChild(select);
    body.appendChild(wrap);
  }

  /* 类型筛选 */
  const filters = el('div', 'water-filters');
  const kindSel = el('select');
  kindSel.appendChild(el('option', '', '全部模式'));
  kindSel.appendChild(el('option', '', '演练')).value = 'drill';
  kindSel.appendChild(el('option', '', '出警')).value = 'dispatch';
  kindSel.addEventListener('change', () => {
    state.kindFilter = kindSel.value;
    load();
  });
  filters.appendChild(kindSel);
  body.appendChild(filters);

  /* 导出 */
  const actions = el('div', 'water-actions');
  const exportBtn = el('button', '', '导出记录');
  exportBtn.type = 'button';
  exportBtn.addEventListener('click', async () => {
    try {
      await downloadArchiveExcel(state.kindFilter);
      setMsg('内攻历史记录已导出');
    } catch (err) {
      setMsg(err.message || '导出失败', true);
    }
  });
  actions.appendChild(exportBtn);
  const statsBtn = el('button', '', '导出统计');
  statsBtn.type = 'button';
  statsBtn.addEventListener('click', async () => {
    try {
      await downloadArchiveStatsExcel(state.kindFilter);
      setMsg('内攻历史统计已导出');
    } catch (err) {
      setMsg(err.message || '导出失败', true);
    }
  });
  actions.appendChild(statsBtn);
  body.appendChild(actions);

  const msg = el('div', 'water-msg');
  body.appendChild(msg);

  const summary = el('div', 'water-summary');
  body.appendChild(summary);

  const list = el('div', 'water-list');
  body.appendChild(list);
  page.appendChild(body);
  root.appendChild(page);

  function setMsg(text, error) {
    msg.className = error ? 'water-msg error' : 'water-msg';
    msg.textContent = text || '';
  }

  async function load() {
    const stationId = currentStationId();
    if (!stationId && !isCommand) {
      list.textContent = '没有可见单位';
      return;
    }
    try {
      if (isCommand && !stationId) {
        state.rows = await fetchAllArchives(state.kindFilter);
      } else if (isCommand) {
        state.rows = await fetchAllArchives(state.kindFilter);
      } else {
        state.rows = await fetchStationArchives(stationId, state.kindFilter);
      }
      render();
    } catch (err) {
      list.textContent = err.message || '加载失败';
    }
  }

  function render() {
    list.innerHTML = '';
    const rows = state.rows || [];
    const personCount = rows.reduce((sum, item) => sum + (item.personCount || 0), 0);
    summary.textContent = rows.length
      ? `当前筛选共 ${rows.length} 条记录，累计 ${personCount} 人次。`
      : '当前筛选条件下暂无记录。';
    if (!rows.length) {
      list.appendChild(el('div', 'water-empty', '暂无归档记录'));
      return;
    }
    rows.forEach((archive) => {
      const card = el('div', 'water-card');
      const titleRow = el('div', 'vehicle-title-row');
      const titleBox = el('div', 'vehicle-title-text');
      titleBox.appendChild(el('div', 'vehicle-name',
        `${archive.stationName || '未标记单位'} - ${(archive.finishedAt || '').replace('T', ' ').slice(0, 19)}`));
      titleBox.appendChild(el('div', 'vehicle-request-title',
        `${kindLabel(archive.eventKind)}：${archive.eventName || '未填写名称'}`));
      titleBox.appendChild(el('div', 'vehicle-line danger-text', `地点：${archive.location || '未填写'}`));
      titleRow.appendChild(titleBox);
      card.appendChild(titleRow);
      card.appendChild(el('div', 'vehicle-line',
        `归档时间：${(archive.finishedAt || '').replace('T', ' ').slice(0, 19)}`));
      card.appendChild(el('div', 'vehicle-line',
        `记录人数：${archive.personCount || 0}　内攻小组：${groupCount(archive)} 个`));
      const groups = groupNames(archive);
      if (groups.length) {
        const wrap = el('div', 'archive-group-wrap');
        groups.forEach((name) => {
          wrap.appendChild(el('span', 'archive-group-chip', name));
        });
        card.appendChild(wrap);
      }
      const persons = archive.persons || [];
      if (persons.length) {
        const personList = el('div', 'archive-person-list');
        persons.forEach((person) => {
          personList.appendChild(el('div', 'vehicle-line',
            `${person.name || ''}${person.group ? `（${person.group}）` : ''}　气瓶 ${person.cylType || '-'}L　`
            + `初始 ${person.initPressure || '-'}MPa　终压 ${person.finalPressure || '-'}MPa`));
        });
        card.appendChild(personList);
      }
      if (canManage) {
        const ops = el('div', 'vehicle-ops');
        const edit = el('button', 'vehicle-op-btn', '修改信息');
        edit.type = 'button';
        edit.addEventListener('click', () => {
          editInfo(archive);
        });
        ops.appendChild(edit);
        const del = el('button', 'vehicle-op-btn danger', '删除');
        del.type = 'button';
        del.addEventListener('click', async () => {
          if (!window.confirm(`确认删除 ${archive.stationName || ''} 的这条历史记录？删除后不可恢复。`)) {
            return;
          }
          try {
            await deleteArchive(archive.stationId, archive.id);
            setMsg('历史记录已删除');
            await load();
          } catch (err) {
            setMsg(err.message || '删除失败', true);
          }
        });
        ops.appendChild(del);
        card.appendChild(ops);
      }
      list.appendChild(card);
    });
  }

  function groupCount(archive) {
    return groupNames(archive).length;
  }

  function groupNames(archive) {
    const names = [];
    (archive.persons || []).forEach((person) => {
      const name = String(person.group || '').trim();
      if (name && names.indexOf(name) < 0) {
        names.push(name);
      }
    });
    return names;
  }

  function editInfo(archive) {
    const kind = window.prompt('类型（drill=演练 / dispatch=出警）：', archive.eventKind || 'drill');
    if (kind === null) {
      return;
    }
    const name = window.prompt('事件名称：', archive.eventName || '');
    if (name === null) {
      return;
    }
    const location = window.prompt('地点：', archive.location || '');
    if (location === null) {
      return;
    }
    updateArchiveInfo(archive.stationId, archive.id, {
      eventKind: kind.trim() || 'drill',
      eventName: name.trim(),
      location: location.trim()
    }).then(() => {
      setMsg('归档信息已更新');
      load();
    }).catch((err) => {
      setMsg(err.message || '更新失败', true);
    });
  }

  load();
}
