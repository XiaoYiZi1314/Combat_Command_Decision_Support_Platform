import './roster.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import {
  deleteProfile,
  downloadRosterExcel,
  fetchRoster,
  importRoster,
  saveGroups
} from '../../api/roster.js';

const STATION_KEY = 'ccds_roster_station';

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

export function currentRosterStationId() {
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

function setRosterStationId(id) {
  localStorage.setItem(STATION_KEY, String(id));
}

function memberLabel(member) {
  return member.roleInGroup ? `${member.name} / ${member.roleInGroup}` : member.name;
}

export function renderRosterPage(root) {
  root.innerHTML = '';
  const page = el('div', 'roster-page');
  const head = el('div', 'roster-head');
  head.appendChild(el('h2', '', '人员档案'));
  const back = el('button', 'roster-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(getMe());
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'roster-body');
  const stations = visibleStations();
  const me = getMe() || {};
  if (me.role !== 'station' && stations.length) {
    const wrap = el('div', 'roster-station');
    wrap.appendChild(el('label', '', '查看单位'));
    const select = el('select');
    stations.forEach((station) => {
      const opt = document.createElement('option');
      opt.value = String(station.id);
      opt.textContent = station.name;
      select.appendChild(opt);
    });
    select.value = currentRosterStationId();
    select.addEventListener('change', () => {
      setRosterStationId(select.value);
      load();
    });
    wrap.appendChild(select);
    body.appendChild(wrap);
  }

  const actions = el('div', 'roster-actions');
  const exportBtn = el('button', '', '导出Excel模板');
  const importBtn = el('button', '', '导入表格');
  const fileInput = el('input', 'hidden-file');
  fileInput.type = 'file';
  fileInput.accept = '.xlsx,.xls,.html,.htm,.csv';
  actions.appendChild(exportBtn);
  if (me.role === 'station') {
    actions.appendChild(importBtn);
    actions.appendChild(fileInput);
  }
  body.appendChild(actions);
  const msg = el('div', 'roster-msg');
  body.appendChild(msg);
  const list = el('div');
  body.appendChild(list);
  page.appendChild(body);
  root.appendChild(page);

  exportBtn.addEventListener('click', async () => {
    try {
      await downloadRosterExcel(currentRosterStationId(), true);
      setMsg(msg, 'Excel模板已导出');
    } catch (err) {
      setMsg(msg, err.message || '导出失败', true);
    }
  });
  importBtn.addEventListener('click', () => fileInput.click());
  fileInput.addEventListener('change', async () => {
    const file = fileInput.files && fileInput.files[0];
    fileInput.value = '';
    if (!file) {
      return;
    }
    try {
      const result = await importRoster(currentRosterStationId(), file);
      const preserved = result.commandGroupPreserved ? '，已保留原指挥组' : '';
      setMsg(msg, `导入完成：新增${result.addedCount}人，更新${result.updatedCount}人，跳过${result.skippedCount}人，新增编组${result.addedGroupCount}个${preserved}`);
      await load();
    } catch (err) {
      setMsg(msg, err.message || '导入失败', true);
    }
  });

  async function load() {
    const stationId = currentRosterStationId();
    if (!stationId) {
      list.textContent = '没有可见单位';
      return;
    }
    try {
      const data = await fetchRoster(stationId);
      renderList(list, data, load, msg);
    } catch (err) {
      list.textContent = err.message || '加载失败';
    }
  }

  load();
}

function setMsg(node, text, error) {
  node.className = error ? 'roster-msg error' : 'roster-msg';
  node.textContent = text || '';
}

function renderList(root, data, reload, msg) {
  root.innerHTML = '';
  const profiles = data.profiles || [];
  const groups = data.groups || [];
  const writable = Boolean(data.writable);
  if (!profiles.length) {
    root.appendChild(el('div', 'pc-points', '该单位暂无人员档案'));
  }
  profiles.forEach((prof) => {
    const card = el('div', 'profile-card');
    const name = el('div', 'pc-name', prof.name || '');
    const info = el('span', 'pc-info', `${prof.title || ''} / ${prof.cylType || '6.8'}L${prof.phoneMasked ? ` / 电话 ${prof.phoneMasked}` : ''}${prof.nfcTag ? ` / NFC ${prof.nfcTag}` : ''}`);
    name.appendChild(info);
    card.appendChild(name);
    card.appendChild(el('div', 'pc-points', `${prof.calibrationCount || 0}条空呼校准`));
    if (writable) {
      const btns = el('div', 'pc-btns');
      const edit = el('button', 'pc-edit', '编辑');
      edit.type = 'button';
      edit.addEventListener('click', (event) => {
        event.stopPropagation();
        window.location.hash = `#/roster/${prof.id}`;
      });
      const del = el('button', 'pc-del', '删除');
      del.type = 'button';
      del.addEventListener('click', async (event) => {
        event.stopPropagation();
        if (!window.confirm(`删除 ${prof.name} 的档案？`)) {
          return;
        }
        try {
          await deleteProfile(data.stationId, prof.id);
          await reload();
        } catch (err) {
          setMsg(msg, err.message || '删除失败', true);
        }
      });
      btns.appendChild(edit);
      btns.appendChild(del);
      card.appendChild(btns);
    }
    card.addEventListener('click', () => {
      window.location.hash = `#/roster/${prof.id}`;
    });
    root.appendChild(card);
  });
  if (writable) {
    const create = el('button', 'roster-new', '+ 新建档案');
    create.type = 'button';
    create.addEventListener('click', () => {
      window.location.hash = '#/roster/new';
    });
    root.appendChild(create);
  }
  root.appendChild(el('div', 'section-title', '战斗编组'));
  groups.forEach((group) => {
    root.appendChild(renderGroupCard(group, profiles, data, writable, reload, msg));
  });
  if (writable) {
    const addWrap = el('div', 'group-add');
    const input = document.createElement('input');
    input.placeholder = '新编组名称';
    const btn = el('button', 'pc-add', '新建编组');
    btn.type = 'button';
    btn.addEventListener('click', async () => {
      const name = (input.value || '').trim();
      if (!name) {
        return;
      }
      const next = (data.groups || []).map(toGroupPayload);
      next.push({ name, sortNo: next.length + 1, members: [] });
      try {
        await saveGroups(data.stationId, next);
        await reload();
      } catch (err) {
        setMsg(msg, err.message || '保存编组失败', true);
      }
    });
    addWrap.appendChild(input);
    addWrap.appendChild(btn);
    root.appendChild(addWrap);
  }
}

function renderGroupCard(group, profiles, data, writable, reload, msg) {
  const card = el('div', 'group-card');
  const head = el('div', 'group-head');
  head.appendChild(el('b', '', group.name || '未命名编组'));
  head.appendChild(el('span', '', `${(group.members || []).length}人`));
  card.appendChild(head);
  const chips = el('div', 'group-members');
  (group.members || []).forEach((member) => {
    const chip = el('span', 'gc-member', memberLabel(member));
    if (writable) {
      const rm = document.createElement('button');
      rm.type = 'button';
      rm.textContent = '×';
      rm.addEventListener('click', async () => {
        const next = (data.groups || []).map((item) => {
          const payload = toGroupPayload(item);
          if (item.id === group.id) {
            payload.members = payload.members.filter((row) => row.profileId !== member.profileId);
          }
          return payload;
        });
        try {
          await saveGroups(data.stationId, next);
          await reload();
        } catch (err) {
          setMsg(msg, err.message || '保存编组失败', true);
        }
      });
      chip.appendChild(rm);
    }
    chips.appendChild(chip);
  });
  card.appendChild(chips);
  if (writable) {
    const addWrap = el('div', 'group-add');
    const select = document.createElement('select');
    const used = new Set((group.members || []).map((item) => String(item.profileId)));
    profiles.forEach((prof) => {
      if (used.has(String(prof.id))) {
        return;
      }
      const opt = document.createElement('option');
      opt.value = String(prof.id);
      opt.textContent = prof.name;
      select.appendChild(opt);
    });
    const btn = el('button', 'pc-add', '加入');
    btn.type = 'button';
    btn.addEventListener('click', async () => {
      if (!select.value) {
        return;
      }
      const next = (data.groups || []).map((item) => {
        const payload = toGroupPayload(item);
        if (item.id === group.id) {
          payload.members.push({ profileId: Number(select.value), roleInGroup: '' });
        }
        return payload;
      });
      try {
        await saveGroups(data.stationId, next);
        await reload();
      } catch (err) {
        setMsg(msg, err.message || '保存编组失败', true);
      }
    });
    addWrap.appendChild(select);
    addWrap.appendChild(btn);
    card.appendChild(addWrap);
  }
  return card;
}

function toGroupPayload(group) {
  return {
    id: group.id,
    name: group.name,
    sortNo: group.sortNo,
    members: (group.members || []).map((member) => ({
      profileId: member.profileId,
      roleInGroup: member.roleInGroup || ''
    }))
  };
}
