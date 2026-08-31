import '../water/water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { el } from '../water/shared.js';
import {
  VEHICLE_TYPES,
  deleteStationVehicle,
  deleteVehicleRequest,
  fetchAllVehicleRequests,
  fetchAllVehicles,
  fetchStationVehicles,
  fetchVehicleRequests,
  reviewVehicleRequest,
  saveStationVehicle,
  submitVehicleRequest,
  vehicleTypeLabel
} from '../../api/vehicle.js';

const STATION_KEY = 'ccds_vehicle_station';

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

const STATUS_OPTIONS = [
  { value: '执勤', label: '执勤' },
  { value: '报修', label: '报修' }
];

const ACTION_LABELS = {
  add: '新增',
  modify: '编辑',
  delete: '删除'
};

const REQUEST_STATUS_LABELS = {
  pending: '待审批',
  approved: '已通过',
  rejected: '已驳回'
};

function summaryOf(vehicle) {
  const parts = [];
  if (vehicle.waterCap) { parts.push(`水 ${vehicle.waterCap}t`); }
  if (vehicle.foamCap) { parts.push(`泡沫 ${vehicle.foamCap}t`); }
  if (vehicle.powderCap) { parts.push(`干粉 ${vehicle.powderCap}t`); }
  if (vehicle.workHeight) { parts.push(`举高 ${vehicle.workHeight}m`); }
  if (vehicle.color) { parts.push(vehicle.color); }
  return parts.join('　');
}

export function renderVehiclesPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const isCommand = me.role === 'hq' || me.role === 'developer' || me.role === 'brigade';
  const directWrite = me.role === 'hq' || me.role === 'developer';

  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', '车辆档案'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(me);
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  const state = {
    typeFilter: '',
    statusFilter: '',
    keyword: '',
    view: 'list',
    editing: null,
    rows: [],
    requests: []
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

  /* 模式切换：列表 / 审批 */
  const modeWrap = el('div', 'water-modes');
  const listBtn = el('button', 'active', isCommand ? (directWrite ? '车辆列表' : '车辆列表') : '车辆列表');
  const approvalBtn = el('button', '', directWrite ? '车辆审批' : '申请记录');
  listBtn.type = 'button';
  approvalBtn.type = 'button';
  listBtn.addEventListener('click', () => {
    state.view = 'list';
    listBtn.className = 'active';
    approvalBtn.className = '';
    form.style.display = 'none';
    list.style.display = '';
    renderRequests();
    renderList();
  });
  approvalBtn.addEventListener('click', () => {
    state.view = 'approvals';
    approvalBtn.className = 'active';
    listBtn.className = '';
    form.style.display = 'none';
    list.style.display = 'none';
    renderRequests();
  });
  modeWrap.appendChild(listBtn);
  modeWrap.appendChild(approvalBtn);
  body.appendChild(modeWrap);

  /* 筛选行 */
  const filters = el('div', 'water-filters');
  const statusSel = el('select');
  statusSel.appendChild(el('option', '', '全部状态'));
  STATUS_OPTIONS.forEach((item) => {
    const opt = el('option', '', item.label);
    opt.value = item.value;
    statusSel.appendChild(opt);
  });
  statusSel.addEventListener('change', () => {
    state.statusFilter = statusSel.value;
    load();
  });
  const typeSel = el('select');
  typeSel.appendChild(el('option', '', '全部车辆类型'));
  VEHICLE_TYPES.forEach((item) => {
    const opt = el('option', '', item.name);
    opt.value = item.id;
    typeSel.appendChild(opt);
  });
  typeSel.addEventListener('change', () => {
    state.typeFilter = typeSel.value;
    load();
  });
  const search = document.createElement('input');
  search.type = 'search';
  search.placeholder = '搜索单位、号牌、型号、厂家';
  search.addEventListener('input', () => {
    state.keyword = search.value.trim();
    load();
  });
  filters.appendChild(statusSel);
  filters.appendChild(typeSel);
  filters.appendChild(search);
  body.appendChild(filters);

  /* 新增按钮 */
  const actions = el('div', 'water-actions');
  const addBtn = el('button', '', directWrite ? '新增车辆' : '申请新增车辆');
  addBtn.type = 'button';
  addBtn.addEventListener('click', () => {
    state.editing = null;
    openForm(null);
  });
  actions.appendChild(addBtn);
  body.appendChild(actions);

  const msg = el('div', 'water-msg');
  body.appendChild(msg);

  /* 审批/申请记录区 */
  const requestBox = el('div', 'water-list');
  body.appendChild(requestBox);

  /* 车辆列表 */
  const list = el('div', 'water-list');
  body.appendChild(list);

  /* 编辑表单 */
  const form = el('div', 'water-form');
  form.style.display = 'none';
  body.appendChild(form);

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
      const params = {
        type: state.typeFilter || '',
        status: state.statusFilter || '',
        keyword: state.keyword || ''
      };
      if (isCommand) {
        state.rows = await fetchAllVehicles(Object.assign({}, params, {
          stationId: stationId || ''
        }));
      } else {
        state.rows = await fetchStationVehicles(stationId, params);
      }
      renderList();
      await loadRequests();
    } catch (err) {
      list.textContent = err.message || '加载失败';
    }
  }

  async function loadRequests() {
    try {
      if (isCommand) {
        state.requests = await fetchAllVehicleRequests();
      } else {
        state.requests = await fetchVehicleRequests(currentStationId());
      }
      renderRequests();
    } catch (err) {
      state.requests = [];
    }
  }

  function renderList() {
    if (state.view !== 'list') {
      return;
    }
    list.innerHTML = '';
    if (!(state.rows || []).length) {
      list.appendChild(el('div', 'water-empty', '暂无车辆记录'));
      return;
    }
    (state.rows || []).forEach((vehicle) => {
      const card = el('div', 'water-card' + (vehicle.status === '报修' ? ' repair' : ''));
      const titleRow = el('div', 'vehicle-title-row');
      const titleBox = el('div', 'vehicle-title-text');
      titleBox.appendChild(el('div', 'vehicle-name', vehicle.vehicleType || vehicleTypeLabel(vehicle.type) || '车辆'));
      titleBox.appendChild(el('div', 'vehicle-plate', vehicle.plate || vehicle.oldPlate || '未登记号牌'));
      titleRow.appendChild(titleBox);
      titleRow.appendChild(el('span', 'vehicle-status ' + (vehicle.status === '报修' ? 'repair' : ''), vehicle.status || '执勤'));
      card.appendChild(titleRow);
      if (vehicle.stationName) {
        card.appendChild(el('div', 'vehicle-line', `单位：${vehicle.stationName}`));
      }
      if (vehicle.model || vehicle.maker) {
        card.appendChild(el('div', 'vehicle-line', `型号：${vehicle.model || '-'}　厂家：${vehicle.maker || '-'}`));
      }
      const summary = summaryOf(vehicle);
      if (summary) {
        card.appendChild(el('div', 'vehicle-line', summary));
      }
      if (vehicle.vin) {
        card.appendChild(el('div', 'vehicle-vin', `VIN：${vehicle.vin}`));
      }
      const canOperate = directWrite || (me.role === 'station'
        && String(me.stationId) === String(vehicle.stationId));
      if (canOperate) {
        const ops = el('div', 'vehicle-ops');
        const edit = el('button', 'vehicle-op-btn', directWrite ? '编辑' : '申请编辑');
        edit.type = 'button';
        edit.addEventListener('click', () => {
          state.editing = vehicle;
          openForm(vehicle);
        });
        ops.appendChild(edit);
        card.appendChild(ops);
      }
      list.appendChild(card);
    });
  }

  function renderRequests() {
    requestBox.innerHTML = '';
    if (state.view !== 'approvals' && !(state.requests || []).some((r) => r.status === 'pending' && directWrite)) {
      return;
    }
    const visible = state.view === 'approvals'
      ? (state.requests || []).slice().reverse()
      : (state.requests || []).filter((r) => r.status === 'pending');
    if (!visible.length) {
      if (state.view === 'approvals') {
        requestBox.appendChild(el('div', 'water-empty', directWrite ? '暂无待审批申请' : '暂无申请记录'));
      }
      return;
    }
    const box = el('div', 'water-card');
    box.appendChild(el('div', 'vehicle-name', directWrite ? '车辆变更审批' : '车辆申请记录'));
    visible.forEach((request) => {
      const item = request.item || {};
      const row = el('div', 'vehicle-request-item');
      const info = el('div', 'vehicle-request-info');
      const head = el('div', '', `${item.plate || item.oldPlate || item.vehicleType || '未命名车辆'}　${item.vehicleType || vehicleTypeLabel(item.type) || ''}`);
      head.className = 'vehicle-request-title';
      info.appendChild(head);
      info.appendChild(el('div', 'vehicle-line',
        `单位：${request.stationName || ''}　申请：${ACTION_LABELS[request.action] || request.action}　状态：${REQUEST_STATUS_LABELS[request.status] || request.status}`));
      if (request.action === 'modify' && request.before) {
        info.appendChild(el('div', 'vehicle-line', `修改前：${request.before.plate || ''} ${request.before.model || ''}`));
      }
      row.appendChild(info);
      const ops = el('div', 'vehicle-request-ops');
      if (directWrite && request.status === 'pending') {
        const ok = el('button', 'vehicle-op-btn primary', '审批通过');
        ok.type = 'button';
        ok.addEventListener('click', async () => {
          if (!window.confirm(`审批通过 ${request.stationName || ''} 的${ACTION_LABELS[request.action] || ''}申请？`)) {
            return;
          }
          try {
            await reviewVehicleRequest(request.stationId, request.id, true);
            setMsg('车辆申请已审批通过');
            await load();
          } catch (err) {
            setMsg(err.message || '审批失败', true);
          }
        });
        ops.appendChild(ok);
        const reject = el('button', 'vehicle-op-btn danger', '驳回');
        reject.type = 'button';
        reject.addEventListener('click', async () => {
          if (!window.confirm(`驳回 ${request.stationName || ''} 的${ACTION_LABELS[request.action] || ''}申请？`)) {
            return;
          }
          try {
            await reviewVehicleRequest(request.stationId, request.id, false);
            setMsg('已驳回车辆申请');
            await load();
          } catch (err) {
            setMsg(err.message || '驳回失败', true);
          }
        });
        ops.appendChild(reject);
      }
      const del = el('button', 'vehicle-op-btn danger', '删除记录');
      del.type = 'button';
      del.addEventListener('click', async () => {
        if (!window.confirm('确认删除该申请记录？')) {
          return;
        }
        try {
          await deleteVehicleRequest(request.stationId, request.id);
          setMsg('申请记录已删除');
          await loadRequests();
        } catch (err) {
          setMsg(err.message || '删除失败', true);
        }
      });
      ops.appendChild(del);
      row.appendChild(ops);
      box.appendChild(row);
    });
    requestBox.appendChild(box);
  }

  function formField(labelText, value, attrs) {
    const row = el('div', 'wf-row');
    row.appendChild(el('label', '', labelText));
    const input = document.createElement(attrs && attrs.tag === 'textarea' ? 'textarea' : 'input');
    if (attrs && attrs.tag === 'select') {
      return formSelect(labelText, value, attrs.options);
    }
    Object.keys(attrs || {}).forEach((key) => {
      if (key !== 'tag' && key !== 'options') {
        input[key] = attrs[key];
      }
    });
    input.value = value || '';
    row.appendChild(input);
    row._input = input;
    return row;
  }

  function formSelect(labelText, value, options) {
    const row = el('div', 'wf-row');
    row.appendChild(el('label', '', labelText));
    const select = document.createElement('select');
    options.forEach((opt) => {
      const option = el('option', '', opt.label);
      option.value = opt.value;
      select.appendChild(option);
    });
    select.value = value || '';
    row.appendChild(select);
    row._input = select;
    return row;
  }

  function openForm(vehicle) {
    form.innerHTML = '';
    form.style.display = '';
    list.style.display = 'none';
    const title = el('h3', '',
      directWrite
        ? (state.editing ? '编辑车辆档案' : '新增车辆')
        : (state.editing ? '申请编辑车辆' : '申请新增车辆'));
    form.appendChild(title);

    const typeRow = formSelect('车辆类型', (vehicle && vehicle.type) || 'water_foam',
      VEHICLE_TYPES.map((item) => ({ value: item.id, label: item.name })));
    const vehicleTypeRow = formField('具体类型', (vehicle && vehicle.vehicleType) || '', { placeholder: '如 水罐消防车' });
    const plateRow = formField('转改后号牌/地方号牌', (vehicle && vehicle.plate) || '', { placeholder: '如 黑X5415应急' });
    const oldPlateRow = formField('原号牌', (vehicle && vehicle.oldPlate) || '');
    const statusRow = formSelect('状态', (vehicle && vehicle.status) || '执勤', STATUS_OPTIONS);
    const modelRow = formField('厂牌型号', (vehicle && vehicle.model) || '');
    const makerRow = formField('生产厂家', (vehicle && vehicle.maker) || '');
    const waterRow = formField('载水量(t)', (vehicle && vehicle.waterCap) || '');
    const foamRow = formField('载泡沫量(t)', (vehicle && vehicle.foamCap) || '');
    const powderRow = formField('载干粉量(t)', (vehicle && vehicle.powderCap) || '');
    const heightRow = formField('举高高度(m)', (vehicle && vehicle.workHeight) || '');
    const engineRow = formField('发动机号', (vehicle && vehicle.engineNo) || '');
    const vinRow = formField('车辆识别代号', (vehicle && vehicle.vin) || '');
    const colorRow = formField('车体颜色', (vehicle && vehicle.color) || '');
    const madeRow = formField('出厂日期', (vehicle && vehicle.madeDate) || '', { placeholder: '如 2009年11月16日' });
    const equipRow = formField('装备时间', (vehicle && vehicle.equipDate) || '', { placeholder: '如 2010-04-08' });
    const notesRow = formField('备注', (vehicle && vehicle.notes) || '', { tag: 'textarea', rows: 2 });

    [typeRow, vehicleTypeRow, plateRow, oldPlateRow, statusRow, modelRow, makerRow,
      waterRow, foamRow, powderRow, heightRow, engineRow, vinRow, colorRow, madeRow,
      equipRow, notesRow].forEach((row) => form.appendChild(row));

    const btnRow = el('div', 'vehicle-form-btns');
    const save = el('button', 'wf-save', directWrite ? (state.editing ? '保存' : '保存') : (state.editing ? '申请编辑' : '申请新增'));
    save.type = 'button';
    save.addEventListener('click', async () => {
      const payload = {
        id: state.editing ? state.editing.id : null,
        type: typeRow._input.value,
        vehicleType: vehicleTypeRow._input.value.trim(),
        plate: plateRow._input.value.trim(),
        oldPlate: oldPlateRow._input.value.trim(),
        status: statusRow._input.value,
        model: modelRow._input.value.trim(),
        maker: makerRow._input.value.trim(),
        waterCap: waterRow._input.value.trim(),
        foamCap: foamRow._input.value.trim(),
        powderCap: powderRow._input.value.trim(),
        workHeight: heightRow._input.value.trim(),
        engineNo: engineRow._input.value.trim(),
        vin: vinRow._input.value.trim(),
        color: colorRow._input.value.trim(),
        madeDate: madeRow._input.value.trim(),
        equipDate: equipRow._input.value.trim(),
        notes: notesRow._input.value.trim()
      };
      const stationId = state.editing
        ? state.editing.stationId
        : (me.role === 'station' ? me.stationId : (currentStationId() || me.stationId));
      if (!stationId) {
        setMsg('请先选择所属单位', true);
        return;
      }
      try {
        if (directWrite) {
          await saveStationVehicle(stationId, payload);
          setMsg('车辆档案已保存');
        } else {
          await submitVehicleRequest(stationId, {
            action: state.editing ? 'modify' : 'add',
            item: payload,
            before: state.editing ? {
              id: state.editing.id,
              plate: state.editing.plate,
              model: state.editing.model
            } : null
          });
          setMsg('变更申请已提交，等待支队审批');
        }
        form.style.display = 'none';
        list.style.display = '';
        await load();
      } catch (err) {
        setMsg(err.message || '保存失败', true);
      }
    });
    const cancel = el('button', 'wf-cancel', '取消');
    cancel.type = 'button';
    cancel.addEventListener('click', () => {
      form.style.display = 'none';
      list.style.display = '';
    });
    btnRow.appendChild(save);
    btnRow.appendChild(cancel);
    form.appendChild(btnRow);

    if (state.editing) {
      const delRow = el('div', 'vehicle-form-btns');
      const del = el('button', 'vehicle-op-btn danger', directWrite ? '删除车辆' : '申请删除');
      del.type = 'button';
      del.addEventListener('click', async () => {
        if (!window.confirm(directWrite ? '确认删除该车辆档案？' : '确认提交删除申请？')) {
          return;
        }
        try {
          if (directWrite) {
            await deleteStationVehicle(state.editing.stationId, state.editing.id);
            setMsg('车辆已删除');
          } else {
            await submitVehicleRequest(state.editing.stationId, {
              action: 'delete',
              item: {
                id: state.editing.id,
                plate: state.editing.plate,
                vehicleType: state.editing.vehicleType
              },
              before: state.editing
            });
            setMsg('删除申请已提交，等待支队审批');
          }
          form.style.display = 'none';
          list.style.display = '';
          await load();
        } catch (err) {
          setMsg(err.message || '删除失败', true);
        }
      });
      delRow.appendChild(del);
      form.appendChild(delRow);
    }
  }

  load();
}
