import './water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import {
  createWater,
  deleteWater,
  downloadWaterExcel,
  fetchCityWaters,
  fetchWaters,
  importWaters,
  updateWater
} from '../../api/water.js';
import { TYPE_OPTIONS, STATUS_OPTIONS, buildBaiduNavUrl } from './shared.js';

const STATION_KEY = 'ccds_water_station';
const SCOPE_KEY = 'ccds_water_scope';

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

export function currentWaterStationId() {
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

function setWaterStationId(id) {
  localStorage.setItem(STATION_KEY, String(id));
}

function typeLabel(code) {
  const hit = TYPE_OPTIONS.find((item) => item.value === code);
  return hit ? hit.label : code;
}

function statusLabel(code) {
  const hit = STATUS_OPTIONS.find((item) => item.value === code);
  return hit ? hit.label : code;
}

export function renderWaterPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', '水源档案'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(me);
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  const state = {
    scope: me.role === 'station'
      ? (localStorage.getItem(SCOPE_KEY) === 'all' ? 'all' : 'own')
      : 'own',
    typeFilter: 'all',
    statusFilter: 'all',
    keyword: '',
    editing: null,
    rows: []
  };

  // 站切换（非站级）
  const stations = visibleStations();
  if (me.role !== 'station' && stations.length) {
    const wrap = el('div', 'water-station');
    wrap.appendChild(el('label', '', '查看单位'));
    const select = el('select');
    stations.forEach((station) => {
      const opt = document.createElement('option');
      opt.value = String(station.id);
      opt.textContent = station.name;
      select.appendChild(opt);
    });
    select.value = currentWaterStationId();
    select.addEventListener('change', () => {
      setWaterStationId(select.value);
      load();
    });
    wrap.appendChild(select);
    body.appendChild(wrap);
  }

  // 本单位 / 全市切换（站级）
  if (me.role === 'station') {
    const scopeWrap = el('div', 'water-scope');
    const ownBtn = el('button', '', '本单位水源');
    const allBtn = el('button', '', '全市水源');
    ownBtn.type = 'button';
    allBtn.type = 'button';
    const paintScope = () => {
      ownBtn.className = state.scope === 'own' ? 'active' : '';
      allBtn.className = state.scope === 'all' ? 'active' : '';
    };
    ownBtn.addEventListener('click', () => {
      state.scope = 'own';
      localStorage.setItem(SCOPE_KEY, 'own');
      paintScope();
      load();
    });
    allBtn.addEventListener('click', () => {
      state.scope = 'all';
      localStorage.setItem(SCOPE_KEY, 'all');
      paintScope();
      load();
    });
    paintScope();
    scopeWrap.appendChild(ownBtn);
    scopeWrap.appendChild(allBtn);
    body.appendChild(scopeWrap);
  }

  // 筛选行
  const filters = el('div', 'water-filters');
  const typeSel = el('select');
  typeSel.appendChild(el('option', '', '全部类型')).value = 'all';
  TYPE_OPTIONS.forEach((item) => {
    const opt = el('option', '', item.label);
    opt.value = item.value;
    typeSel.appendChild(opt);
  });
  typeSel.addEventListener('change', () => {
    state.typeFilter = typeSel.value;
    renderList();
  });
  const statusSel = el('select');
  const allStatus = el('option', '', '全部状态');
  allStatus.value = 'all';
  statusSel.appendChild(allStatus);
  STATUS_OPTIONS.forEach((item) => {
    const opt = el('option', '', item.label);
    opt.value = item.value;
    statusSel.appendChild(opt);
  });
  statusSel.addEventListener('change', () => {
    state.statusFilter = statusSel.value;
    renderList();
  });
  const search = document.createElement('input');
  search.type = 'search';
  search.placeholder = '名称 / 地址关键词';
  search.addEventListener('input', () => {
    state.keyword = search.value.trim();
    renderList();
  });
  filters.appendChild(typeSel);
  filters.appendChild(statusSel);
  filters.appendChild(search);
  body.appendChild(filters);

  // 模式切换：卡片 / 地图
  const modeWrap = el('div', 'water-modes');
  const cardBtn = el('button', 'active', '卡片');
  const mapBtn = el('button', '', '地图');
  const nearbyBtn = el('button', 'water-nearby-btn', '最近水源');
  cardBtn.type = 'button';
  mapBtn.type = 'button';
  nearbyBtn.type = 'button';
  cardBtn.addEventListener('click', () => {
    modeWrap.dataset.mode = 'cards';
    cardBtn.className = 'active';
    mapBtn.className = '';
    renderList();
  });
  mapBtn.addEventListener('click', () => {
    window.location.hash = '#/water/map';
  });
  nearbyBtn.addEventListener('click', () => {
    window.location.hash = '#/water/map?nearby=1';
  });
  modeWrap.appendChild(cardBtn);
  modeWrap.appendChild(mapBtn);
  modeWrap.appendChild(nearbyBtn);
  modeWrap.dataset.mode = 'cards';
  body.appendChild(modeWrap);

  // 导入导出（站级可写时）
  const actions = el('div', 'water-actions');
  const exportBtn = el('button', '', '导出本站');
  const templateBtn = el('button', '', '导出模板');
  const importBtn = el('button', '', '导入表格');
  const fileInput = el('input', 'hidden-file');
  fileInput.type = 'file';
  fileInput.accept = '.xlsx,.xls,.html,.htm,.csv';
  exportBtn.type = 'button';
  templateBtn.type = 'button';
  importBtn.type = 'button';
  exportBtn.addEventListener('click', async () => {
    try {
      await downloadWaterExcel(currentWaterStationId(), false);
      setMsg('本站水源已导出');
    } catch (err) {
      setMsg(err.message || '导出失败', true);
    }
  });
  templateBtn.addEventListener('click', async () => {
    try {
      await downloadWaterExcel(currentWaterStationId(), true);
      setMsg('模板已导出');
    } catch (err) {
      setMsg(err.message || '导出失败', true);
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
      const result = await importWaters(currentWaterStationId(), file);
      setMsg(`导入完成：新增${result.addedCount || 0}条，跳过${result.skippedCount || 0}条`);
      await load();
    } catch (err) {
      setMsg(err.message || '导入失败', true);
    }
  });
  actions.appendChild(exportBtn);
  actions.appendChild(templateBtn);
  if (me.role === 'station') {
    actions.appendChild(importBtn);
    actions.appendChild(fileInput);
  }
  body.appendChild(actions);

  const msg = el('div', 'water-msg');
  body.appendChild(msg);
  const list = el('div', 'water-list');
  body.appendChild(list);
  page.appendChild(body);
  root.appendChild(page);

  function setMsg(text, error) {
    msg.className = error ? 'water-msg error' : 'water-msg';
    msg.textContent = text || '';
  }

  async function load() {
    const stationId = currentWaterStationId();
    if (!stationId) {
      list.textContent = '没有可见单位';
      return;
    }
    try {
      if (state.scope === 'all') {
        state.rows = await fetchCityWaters();
      } else {
        const result = await fetchWaters(stationId);
        state.rows = result.waters || [];
      }
      renderList();
    } catch (err) {
      list.textContent = err.message || '加载失败';
    }
  }

  function filteredRows() {
    return state.rows.filter((water) => {
      if (state.typeFilter !== 'all' && water.type !== state.typeFilter) {
        return false;
      }
      if (state.statusFilter !== 'all' && water.status !== state.statusFilter) {
        return false;
      }
      if (state.keyword) {
        const text = `${water.name || ''} ${water.address || ''}`;
        if (!text.includes(state.keyword)) {
          return false;
        }
      }
      return true;
    });
  }

  function renderList() {
    list.innerHTML = '';
    const rows = filteredRows();
    const summary = el('div', 'water-summary',
      `共 ${rows.length} 处（在用 ${rows.filter((w) => w.status === 'active').length}，报修 ${rows.filter((w) => w.status === 'repair').length}）`);
    list.appendChild(summary);
    if (!rows.length) {
      list.appendChild(el('div', 'water-empty', '暂无水源档案'));
    }
    const writable = me.role === 'station' && state.scope === 'own';
    rows.forEach((water) => {
      list.appendChild(renderCard(water, writable));
    });
    if (writable) {
      const create = el('button', 'water-new', '+ 新建水源');
      create.type = 'button';
      create.addEventListener('click', () => {
        state.editing = { isNew: true };
        renderEditor();
      });
      list.appendChild(create);
    }
  }

  function renderCard(water, writable) {
    const card = el('div', `water-card ${water.status === 'repair' ? 'repair' : ''}`.trim());
    const nameRow = el('div', 'wc-name');
    nameRow.appendChild(el('b', '', water.name || '未命名'));
    nameRow.appendChild(el('span', `wc-status ${water.status}`, statusLabel(water.status)));
    card.appendChild(nameRow);
    card.appendChild(el('div', 'wc-info', `${typeLabel(water.type)}${water.stationName ? ` · ${water.stationName}` : ''}`));
    if (water.address) {
      card.appendChild(el('div', 'wc-addr', water.address));
    }
    if (water.lng != null && water.lat != null) {
      const nav = el('button', 'wc-nav', '导航');
      nav.type = 'button';
      nav.addEventListener('click', (event) => {
        event.stopPropagation();
        window.open(buildBaiduNavUrl(water.lat, water.lng, water.name), '_blank', 'noopener');
      });
      card.appendChild(nav);
    }
    if (writable) {
      const btns = el('div', 'wc-btns');
      const edit = el('button', 'wc-edit', '编辑');
      edit.type = 'button';
      edit.addEventListener('click', (event) => {
        event.stopPropagation();
        state.editing = { isNew: false, water };
        renderEditor();
      });
      const del = el('button', 'wc-del', '删除');
      del.type = 'button';
      del.addEventListener('click', async (event) => {
        event.stopPropagation();
        if (!window.confirm(`删除 ${water.name}？`)) {
          return;
        }
        try {
          await deleteWater(currentWaterStationId(), water.id);
          await load();
        } catch (err) {
          setMsg(err.message || '删除失败', true);
        }
      });
      btns.appendChild(edit);
      btns.appendChild(del);
      card.appendChild(btns);
    }
    return card;
  }

  function renderEditor() {
    const editing = state.editing;
    if (!editing) {
      return;
    }
    list.innerHTML = '';
    const form = el('div', 'water-form');
    form.appendChild(el('h3', '', editing.isNew ? '新建水源' : `编辑 ${editing.water.name}`));

    const nameInput = document.createElement('input');
    nameInput.type = 'text';
    nameInput.placeholder = '名称（如：消防水鹤 12）';
    nameInput.value = editing.isNew ? '' : (editing.water.name || '');
    const typeSel = el('select');
    TYPE_OPTIONS.forEach((item) => {
      const opt = el('option', '', item.label);
      opt.value = item.value;
      typeSel.appendChild(opt);
    });
    typeSel.value = editing.isNew ? TYPE_OPTIONS[0].value : editing.water.type;
    const statusSel = el('select');
    STATUS_OPTIONS.forEach((item) => {
      const opt = el('option', '', item.label);
      opt.value = item.value;
      statusSel.appendChild(opt);
    });
    statusSel.value = editing.isNew ? 'active' : editing.water.status;
    const addressInput = document.createElement('input');
    addressInput.type = 'text';
    addressInput.placeholder = '地址';
    addressInput.value = editing.isNew ? '' : (editing.water.address || '');
    const lngInput = document.createElement('input');
    lngInput.type = 'number';
    lngInput.step = '0.000001';
    lngInput.placeholder = '经度（WGS84）';
    lngInput.value = editing.isNew ? '' : (editing.water.lng == null ? '' : String(editing.water.lng));
    const latInput = document.createElement('input');
    latInput.type = 'number';
    latInput.step = '0.000001';
    latInput.placeholder = '纬度（WGS84）';
    latInput.value = editing.isNew ? '' : (editing.water.lat == null ? '' : String(editing.water.lat));
    const notesInput = document.createElement('textarea');
    notesInput.placeholder = '备注';
    notesInput.rows = 2;
    notesInput.value = editing.isNew ? '' : (editing.water.notes || '');

    const fields = [
      ['名称', nameInput],
      ['类型', typeSel],
      ['状态', statusSel],
      ['地址', addressInput],
      ['经度', lngInput],
      ['纬度', latInput],
      ['备注', notesInput]
    ];
    fields.forEach(([label, input]) => {
      const row = el('div', 'wf-row');
      row.appendChild(el('label', '', label));
      row.appendChild(input);
      form.appendChild(row);
    });

    const save = el('button', 'wf-save', '保存');
    save.type = 'button';
    save.addEventListener('click', async () => {
      const payload = {
        name: nameInput.value.trim(),
        type: typeSel.value,
        status: statusSel.value,
        address: addressInput.value.trim() || null,
        lng: lngInput.value ? Number(lngInput.value) : null,
        lat: latInput.value ? Number(latInput.value) : null,
        notes: notesInput.value.trim() || null
      };
      if (!payload.name) {
        setMsg('名称不能为空', true);
        return;
      }
      if (payload.lng == null || payload.lat == null) {
        setMsg('经纬度必填（可先在地图页取点）', true);
        return;
      }
      try {
        if (editing.isNew) {
          await createWater(currentWaterStationId(), payload);
        } else {
          await updateWater(currentWaterStationId(), editing.water.id, payload);
        }
        state.editing = null;
        await load();
      } catch (err) {
        setMsg(err.message || '保存失败', true);
      }
    });
    const cancel = el('button', 'wf-cancel', '取消');
    cancel.type = 'button';
    cancel.addEventListener('click', () => {
      state.editing = null;
      renderList();
    });
    form.appendChild(save);
    form.appendChild(cancel);
    list.appendChild(form);
  }

  load();
}
