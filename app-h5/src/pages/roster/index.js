import './roster.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import {
  deleteProfile,
  downloadAllProfilesExcel,
  downloadRosterExcel,
  fetchProfileTombstones,
  fetchProfileTransfers,
  fetchRoster,
  importRoster,
  restoreProfileTombstone,
  reviewProfileTransfer,
  saveGroups,
  submitProfileTransfer
} from '../../api/roster.js';
import { pickLocalFile } from '../../api/file-transfer.js';

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

const GROUP_ROLES = ['组员', '组长', '站指挥员'];

function fillRoleSelect(select, selected) {
  GROUP_ROLES.forEach((role) => {
    const opt = document.createElement('option');
    opt.value = role;
    opt.textContent = role;
    select.appendChild(opt);
  });
  select.value = selected && GROUP_ROLES.includes(selected) ? selected : '组员';
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
  const exportBtn = el('button', '', '导出本站');
  const templateBtn = el('button', '', '导出模板');
  const importBtn = el('button', '', '导入表格');
  const transferBtn = el('button', '', '人员调动');
  const tombstoneBtn = el('button', '', '已删除名单');
  actions.appendChild(exportBtn);
  actions.appendChild(templateBtn);
  if (me.role === 'station') {
    actions.appendChild(importBtn);
    actions.appendChild(transferBtn);
  }
  if (me.role === 'hq' || me.role === 'developer') {
    const exportAllBtn = el('button', '', '导出所有单位');
    exportAllBtn.type = 'button';
    exportAllBtn.addEventListener('click', async () => {
      try {
        await downloadAllProfilesExcel();
        setMsg(msg, '已导出到系统下载目录');
      } catch (err) {
        setMsg(msg, err.message || '导出失败', true);
      }
    });
    actions.appendChild(exportAllBtn);
  }
  actions.appendChild(tombstoneBtn);
  body.appendChild(actions);
  const msg = el('div', 'roster-msg');
  body.appendChild(msg);
  const list = el('div');
  body.appendChild(list);
  page.appendChild(body);
  root.appendChild(page);

  transferBtn.type = 'button';
  transferBtn.addEventListener('click', () => {
    renderTransferForm(list, msg, load);
  });
  tombstoneBtn.type = 'button';
  tombstoneBtn.addEventListener('click', () => {
    renderTombstones(list, msg, currentRosterStationId());
  });

  exportBtn.addEventListener('click', async () => {
    try {
      await downloadRosterExcel(currentRosterStationId(), false);
      setMsg(msg, '已导出到系统下载目录');
    } catch (err) {
      setMsg(msg, err.message || '导出失败', true);
    }
  });
  templateBtn.addEventListener('click', async () => {
    try {
      await downloadRosterExcel(currentRosterStationId(), true);
      setMsg(msg, '模板已导出到系统下载目录');
    } catch (err) {
      setMsg(msg, err.message || '导出失败', true);
    }
  });
  importBtn.addEventListener('click', async () => {
    const file = await pickLocalFile('.xlsx,.xls,.html,.htm,.csv');
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
    // 4.5：本站、所属大队、支队都显示完整电话，不再只给打码值
    const phoneText = prof.phone || prof.phoneMasked;
    const info = el('span', 'pc-info', `${prof.title || ''} / ${prof.cylType || '6.8'}L`
      + `${phoneText ? ` / 电话 ${phoneText}` : ''}`
      + `${prof.archiveNo ? ` / 档案号 ${prof.archiveNo}` : ''}`
      + `${prof.nfcTag ? ` / NFC ${prof.nfcTag}` : ''}`);
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
    const chip = el('span', 'gc-member');
    chip.appendChild(document.createTextNode(member.name || ''));
    if (writable) {
      const roleSelect = document.createElement('select');
      roleSelect.className = 'gc-role';
      fillRoleSelect(roleSelect, member.roleInGroup);
      roleSelect.addEventListener('change', async () => {
        const next = (data.groups || []).map((item) => {
          const payload = toGroupPayload(item);
          if (item.id === group.id) {
            payload.members = payload.members.map((row) => {
              if (row.profileId === member.profileId) {
                return { ...row, roleInGroup: roleSelect.value };
              }
              return row;
            });
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
      chip.appendChild(roleSelect);
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
    } else if (member.roleInGroup) {
      chip.appendChild(document.createTextNode(` / ${member.roleInGroup}`));
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
    const roleSelect = document.createElement('select');
    fillRoleSelect(roleSelect, '组员');
    const btn = el('button', 'pc-add', '加入');
    btn.type = 'button';
    btn.addEventListener('click', async () => {
      if (!select.value) {
        return;
      }
      const next = (data.groups || []).map((item) => {
        const payload = toGroupPayload(item);
        if (item.id === group.id) {
          payload.members.push({
            profileId: Number(select.value),
            roleInGroup: roleSelect.value || '组员'
          });
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
    addWrap.appendChild(roleSelect);
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

/**
 * 人员调动表单：选人员、调出站、调入站，提交后由支队审批。
 */
function renderTransferForm(list, msg, reload) {
  const me = getMe() || {};
  const stations = visibleStations();
  list.innerHTML = '';
  const form = el('div', 'roster-form');
  form.appendChild(el('h3', '', '人员调动'));
  form.appendChild(el('div', 'roster-help',
    '提交后由支队审批，通过后才落档。调入站会分配新的档案号，不会沿用调出站的档案号。'));

  const profileSelect = document.createElement('select');
  const toSelect = document.createElement('select');
  stations.forEach((station) => {
    const opt = document.createElement('option');
    opt.value = String(station.id);
    opt.textContent = station.name;
    toSelect.appendChild(opt);
  });
  if (me.stationId) {
    toSelect.value = String(me.stationId);
  }

  const reasonInput = document.createElement('input');
  reasonInput.type = 'text';
  reasonInput.placeholder = '调动原因';
  reasonInput.maxLength = 255;

  const row = (label, input) => {
    const wrap = el('div', 'wf-row');
    wrap.appendChild(el('label', '', label));
    wrap.appendChild(input);
    return wrap;
  };

  const submit = el('button', 'wf-save', '提交调动申请');
  submit.type = 'button';
  const cancel = el('button', 'wf-cancel', '取消');
  cancel.type = 'button';
  cancel.addEventListener('click', () => reload());

  form.appendChild(row('调出人员', profileSelect));
  form.appendChild(row('调入站', toSelect));
  form.appendChild(row('调动原因', reasonInput));
  form.appendChild(submit);
  form.appendChild(cancel);
  list.appendChild(form);

  fetchRoster(currentRosterStationId()).then((data) => {
    (data.profiles || []).forEach((profile) => {
      const opt = document.createElement('option');
      opt.value = String(profile.id);
      opt.textContent = profile.archiveNo
        ? `${profile.name}（${profile.archiveNo}）` : profile.name;
      profileSelect.appendChild(opt);
    });
  }).catch((err) => {
    setMsg(msg, err.message || '花名册加载失败', true);
  });

  submit.addEventListener('click', async () => {
    if (!profileSelect.value || !toSelect.value || !reasonInput.value.trim()) {
      setMsg(msg, '请选择人员、调入站并填写原因', true);
      return;
    }
    try {
      await submitProfileTransfer({
        fromStationId: Number(currentRosterStationId()),
        toStationId: Number(toSelect.value),
        profileId: Number(profileSelect.value),
        reason: reasonInput.value.trim(),
        clientRequestId: `xfer-${profileSelect.value}-${Date.now()}`
      });
      setMsg(msg, '已提交调动申请，待支队审批');
      reload();
    } catch (err) {
      setMsg(msg, err.message || '提交失败', true);
    }
  });
}

/**
 * 已删除名单：删除记录长期有效，只有用户显式恢复才解开。
 */
function renderTombstones(list, msg, stationId) {
  list.innerHTML = '';
  const head = el('div', 'section-title', '已删除名单');
  list.appendChild(head);
  fetchProfileTombstones(stationId).then((rows) => {
    if (!rows || !rows.length) {
      list.appendChild(el('div', 'pc-points', '没有已删除记录'));
      return;
    }
    rows.forEach((row) => {
      const card = el('div', 'profile-card');
      const name = el('div', 'pc-name', row.name || '');
      name.appendChild(el('span', 'pc-info',
        `${row.archiveNo || ''}${row.nfcTag ? ` / NFC ${row.nfcTag}` : ''}`));
      card.appendChild(name);
      const flags = [];
      if (row.nameBlocked) {
        flags.push('姓名拦截中');
      }
      if (row.nfcBlocked) {
        flags.push('NFC 拦截中');
      }
      if (row.archiveBlocked) {
        flags.push('档案号拦截中');
      }
      card.appendChild(el('div', 'pc-points', flags.join(' · ') || '已全部解除'));
      const btns = el('div', 'pc-btns');
      if (row.nameBlocked) {
        const restoreName = el('button', 'pc-edit', '恢复姓名');
        restoreName.type = 'button';
        restoreName.addEventListener('click', async () => {
          try {
            await restoreProfileTombstone(stationId, { scope: 'name', name: row.name });
            setMsg(msg, '已恢复姓名记录');
            renderTombstones(list, msg, stationId);
          } catch (err) {
            setMsg(msg, err.message || '恢复失败', true);
          }
        });
        btns.appendChild(restoreName);
      }
      if (row.nfcBlocked && row.nfcTag) {
        const restoreNfc = el('button', 'pc-del', '解除 NFC');
        restoreNfc.type = 'button';
        restoreNfc.addEventListener('click', async () => {
          if (!window.confirm(`确认解除 NFC ${row.nfcTag} 的删除记录？`)) {
            return;
          }
          try {
            await restoreProfileTombstone(stationId, {
              scope: 'nfc', name: row.name, nfcTag: row.nfcTag
            });
            setMsg(msg, '已解除 NFC 记录');
            renderTombstones(list, msg, stationId);
          } catch (err) {
            setMsg(msg, err.message || '解除失败', true);
          }
        });
        btns.appendChild(restoreNfc);
      }
      if (btns.childNodes.length) {
        card.appendChild(btns);
      }
      list.appendChild(card);
    });
  }).catch((err) => {
    list.textContent = err.message || '加载失败';
  });
}

/**
 * 支队审批调动申请。
 */
export function renderTransferApprovals(root) {
  root.innerHTML = '';
  const page = el('div', 'roster-page');
  const head = el('div', 'roster-head');
  head.appendChild(el('h2', '', '人员调动审批'));
  const back = el('button', 'roster-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(getMe());
  });
  head.appendChild(back);
  page.appendChild(head);
  const body = el('div', 'roster-body');
  const msg = el('div', 'roster-msg');
  body.appendChild(msg);
  const list = el('div');
  body.appendChild(list);
  page.appendChild(body);
  root.appendChild(page);

  function paint() {
    fetchProfileTransfers(null, null).then((rows) => {
      list.innerHTML = '';
      if (!rows || !rows.length) {
        list.appendChild(el('div', 'pc-points', '暂无调动申请'));
        return;
      }
      rows.forEach((item) => {
        const card = el('div', 'profile-card');
        card.appendChild(el('div', 'pc-name',
          `${item.profileName || ''}：${item.fromStationName || ''} → ${item.toStationName || ''}`));
        card.appendChild(el('div', 'pc-points',
          `${transferStatusLabel(item.status)} · ${item.sourceArchiveNo || '无档案号'}`
          + (item.newArchiveNo ? ` → ${item.newArchiveNo}` : '')));
        if (item.reason) {
          card.appendChild(el('div', 'pc-points', `原因：${item.reason}`));
        }
        if (item.status === 'pending') {
          const btns = el('div', 'pc-btns');
          const pass = el('button', 'pc-edit', '通过');
          pass.type = 'button';
          pass.addEventListener('click', async () => {
            try {
              await reviewProfileTransfer(item.id, { approve: true, reviewReason: '同意' });
              setMsg(msg, '已通过');
              paint();
            } catch (err) {
              setMsg(msg, err.message || '审批失败', true);
            }
          });
          const reject = el('button', 'pc-del', '驳回');
          reject.type = 'button';
          reject.addEventListener('click', async () => {
            const reason = window.prompt('驳回原因', '材料不足');
            if (reason === null) {
              return;
            }
            try {
              await reviewProfileTransfer(item.id, { approve: false, reviewReason: reason });
              setMsg(msg, '已驳回');
              paint();
            } catch (err) {
              setMsg(msg, err.message || '审批失败', true);
            }
          });
          btns.appendChild(pass);
          btns.appendChild(reject);
          card.appendChild(btns);
        }
        list.appendChild(card);
      });
    }).catch((err) => {
      list.textContent = err.message || '加载失败';
    });
  }

  paint();
}

function transferStatusLabel(status) {
  if (status === 'approved') {
    return '已通过';
  }
  if (status === 'rejected') {
    return '已驳回';
  }
  return '待审批';
}
