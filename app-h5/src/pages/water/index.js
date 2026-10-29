import './water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { bridge } from '../../bridge/index.js';
import {
  createWater,
  deleteWater,
  downloadWaterExcel,
  downloadWaterSummary,
  fetchCityWaters,
  fetchWater,
  fetchWaterRequests,
  fetchWaterSummary,
  fetchWaters,
  importWaters,
  reviewWaterRequest,
  submitWaterRequest,
  submitWaterVideo,
  updateWater,
  withdrawWaterRequest
} from '../../api/water.js';
import { deleteBizFile, uploadBizFile } from '../../api/files.js';
import { pickLocalFile } from '../../api/file-transfer.js';
import {
  PART_OPTIONS,
  REASON_OPTIONS,
  SETUP_OPTIONS,
  STATUS_OPTIONS,
  SUPPLY_FORM_OPTIONS,
  TYPE_OPTIONS,
  USABLE_OPTIONS,
  buildBaiduNavUrl,
  el,
  isWaterBad,
  optionLabel,
  statusLabel,
  typeLabel,
  waterBadSummary
} from './shared.js';

const STATION_KEY = 'ccds_water_station';
const SCOPE_KEY = 'ccds_water_scope';
const REASON_MAX = 255;
const VIDEO_MAX_MS = 10000;

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

function waterFileName(file) {
  if (file && file.originalName) {
    return file.originalName;
  }
  const type = file && file.contentType ? file.contentType : '文件';
  return `${type} #${file && file.id ? file.id : ''}`;
}

function canDirectWrite(me) {
  return me.role === 'hq' || me.role === 'developer';
}

function canReview(me) {
  return me.role === 'hq' || me.role === 'developer';
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
    rows: [],
    view: 'list',
    requests: [],
    summary: null,
    summaryMonth: ''
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

  // 视图切换：明细 / 汇总 / 审批
  const viewWrap = el('div', 'water-modes');
  const listViewBtn = el('button', 'active', '水源明细');
  const summaryViewBtn = el('button', '', '水源汇总表');
  const requestViewBtn = el('button', '', canReview(me) ? '水源审批' : '我的申请');
  [listViewBtn, summaryViewBtn, requestViewBtn].forEach((btn) => {
    btn.type = 'button';
    viewWrap.appendChild(btn);
  });
  const paintView = () => {
    listViewBtn.className = state.view === 'list' ? 'active' : '';
    summaryViewBtn.className = state.view === 'summary' ? 'active' : '';
    requestViewBtn.className = state.view === 'requests' ? 'active' : '';
  };
  listViewBtn.addEventListener('click', () => {
    state.view = 'list';
    paintView();
    renderList();
  });
  summaryViewBtn.addEventListener('click', () => {
    state.view = 'summary';
    paintView();
    loadSummary();
  });
  requestViewBtn.addEventListener('click', () => {
    state.view = 'requests';
    paintView();
    loadRequests();
  });
  body.appendChild(viewWrap);

  // 导入导出（站级可写时）
  const actions = el('div', 'water-actions');
  const exportBtn = el('button', '', '导出本站');
  const templateBtn = el('button', '', '导出模板');
  const importBtn = el('button', '', '导入表格');
  const summaryExportBtn = el('button', '', '导出汇总表');
  exportBtn.type = 'button';
  templateBtn.type = 'button';
  importBtn.type = 'button';
  summaryExportBtn.type = 'button';
  exportBtn.addEventListener('click', async () => {
    try {
      await downloadWaterExcel(currentWaterStationId(), false);
      setMsg('已导出到系统下载目录');
    } catch (err) {
      setMsg(err.message || '导出失败', true);
    }
  });
  templateBtn.addEventListener('click', async () => {
    try {
      await downloadWaterExcel(currentWaterStationId(), true);
      setMsg('模板已导出到系统下载目录');
    } catch (err) {
      setMsg(err.message || '导出失败', true);
    }
  });
  importBtn.addEventListener('click', async () => {
    const file = await pickLocalFile('.xlsx,.xls,.html,.htm,.csv');
    if (!file) {
      return;
    }
    try {
      const result = await importWaters(currentWaterStationId(), file);
      const details = [];
      if (result.skipExampleCount) details.push(`示例${result.skipExampleCount}条`);
      if (result.skipTypeInvalidCount) details.push(`类型无法识别${result.skipTypeInvalidCount}条`);
      if (result.skipCoordMissingCount) details.push(`缺经纬度${result.skipCoordMissingCount}条`);
      if (result.skipDeletedCount) details.push(`已删除不恢复${result.skipDeletedCount}条`);
      if (result.skipDuplicateCount) details.push(`无变化${result.skipDuplicateCount}条`);
      const skipDetail = details.length > 0 ? `（${details.join('、')}）` : '';
      setMsg(`导入完成：新增${result.addedCount || 0}条，更新${result.updatedCount || 0}条，跳过${result.skippedCount || 0}条${skipDetail}`);
      await load();
    } catch (err) {
      setMsg(err.message || '导入失败', true);
    }
  });
  summaryExportBtn.addEventListener('click', async () => {
    try {
      await downloadWaterSummary(state.summaryMonth);
      setMsg('汇总表已导出到系统下载目录');
    } catch (err) {
      setMsg(err.message || '导出失败', true);
    }
  });
  actions.appendChild(exportBtn);
  actions.appendChild(templateBtn);
  actions.appendChild(summaryExportBtn);
  if (me.role === 'station' || canDirectWrite(me)) {
    actions.appendChild(importBtn);
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

  function writableForFiles() {
    return canDirectWrite(me) || (me.role === 'station' && state.scope === 'own');
  }

  function renderWaterFiles(water, writable, onChanged) {
    const box = el('div', 'wf-files');
    box.appendChild(el('div', 'wf-files-title', '照片与档案材料'));
    appendFileGroup(box, water.photos || [], '照片', 'image/jpeg,image/png', 'water_photo', water.id, writable, onChanged);
    appendFileGroup(box, water.docs || [], '档案 PDF', 'application/pdf', 'water_doc', water.id, writable, onChanged);
    box.appendChild(renderVideoGroup(water, writable));
    box.appendChild(el('div', 'wf-help', '照片支持 jpg/png，档案支持 pdf，单文件最大 20MB。出水视频限 10 秒。'));
    return box;
  }

  function renderVideoGroup(water, writable) {
    const wrap = el('div', 'wf-video');
    wrap.appendChild(el('div', 'wf-files-label', '出水视频（限 10 秒）'));
    if (!writable) {
      return wrap;
    }
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'video/mp4';
    const durationInput = document.createElement('input');
    durationInput.type = 'number';
    durationInput.placeholder = '视频时长（秒）';
    durationInput.step = '0.1';
    const reasonInput = document.createElement('input');
    reasonInput.type = 'text';
    reasonInput.placeholder = '判定不通过原因（可选）';
    reasonInput.maxLength = REASON_MAX;
    const upload = el('button', 'wf-upload', '上传出水视频');
    upload.type = 'button';
    upload.addEventListener('click', async () => {
      const file = input.files && input.files[0];
      if (!file) {
        setMsg('请先选择视频', true);
        return;
      }
      const seconds = Number(durationInput.value);
      if (!Number.isFinite(seconds) || seconds <= 0) {
        setMsg('请填写视频实际时长（秒）', true);
        return;
      }
      if (seconds * 1000 > VIDEO_MAX_MS) {
        setMsg(`出水视频限 10 秒，当前 ${Math.round(seconds)} 秒`, true);
        return;
      }
      upload.disabled = true;
      try {
        const uploaded = await uploadBizFile('water_video', water.id, file);
        await submitWaterVideo(currentWaterStationId(), water.id, {
          fileId: uploaded && uploaded.id ? uploaded.id : null,
          durationMs: Math.round(seconds * 1000),
          checkStatus: 'failed',
          checkReason: reasonInput.value.trim() || '客户端未做画面判定'
        });
        setMsg('出水视频已登记');
        onChanged();
      } catch (err) {
        setMsg(err.message || '上传失败', true);
      } finally {
        upload.disabled = false;
      }
    });
    const row = el('div', 'wf-upload-row');
    row.appendChild(input);
    row.appendChild(durationInput);
    row.appendChild(reasonInput);
    row.appendChild(upload);
    wrap.appendChild(row);
    return wrap;
  }

  function appendFileGroup(box, files, title, accept, bizType, bizId, writable, onChanged) {
    box.appendChild(el('div', 'wf-files-label', title));
    if (!files.length) {
      box.appendChild(el('div', 'wf-empty', `暂无${title}`));
    }
    files.forEach((file) => {
      const row = el('div', 'wf-file');
      const link = document.createElement('a');
      link.href = file.previewUrl || file.downloadUrl || '#';
      link.target = '_blank';
      link.rel = 'noopener noreferrer';
      link.textContent = waterFileName(file);
      row.appendChild(link);
      if (writable) {
        const remove = el('button', '', '删除');
        remove.type = 'button';
        remove.addEventListener('click', async () => {
          try {
            await deleteBizFile(file.id);
            onChanged();
          } catch (err) {
            setMsg(err.message || '删除失败', true);
          }
        });
        row.appendChild(remove);
      }
      box.appendChild(row);
    });
    if (!writable) {
      return;
    }
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    const upload = el('button', 'wf-upload', `上传${title}`);
    upload.type = 'button';
    upload.addEventListener('click', async () => {
      const file = input.files && input.files[0];
      if (!file) {
        setMsg(`请先选择${title}`, true);
        return;
      }
      upload.disabled = true;
      try {
        await uploadBizFile(bizType, bizId, file);
        input.value = '';
        onChanged();
      } catch (err) {
        setMsg(err.message || '上传失败', true);
      } finally {
        upload.disabled = false;
      }
    });
    const row = el('div', 'wf-upload-row');
    row.appendChild(input);
    row.appendChild(upload);
    box.appendChild(row);
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
      if (state.statusFilter !== 'all') {
        // 「报修」只列按唯一谓词算出来的不好用卡；「在用」不混入这些卡
        const bad = isWaterBad(water);
        if (state.statusFilter === 'repair' && !bad) {
          return false;
        }
        if (state.statusFilter === 'active' && bad) {
          return false;
        }
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
    if (state.view === 'summary') {
      return;
    }
    if (state.view === 'requests') {
      return;
    }
    list.innerHTML = '';
    const rows = filteredRows();
    const badCount = rows.filter(isWaterBad).length;
    const summary = el('div', 'water-summary',
      `共 ${rows.length} 处（好用 ${rows.length - badCount}，不好用 ${badCount}）`);
    list.appendChild(summary);
    if (!rows.length) {
      list.appendChild(el('div', 'water-empty', '暂无水源档案'));
    }
    const writable = canDirectWrite(me) || (me.role === 'station' && state.scope === 'own');
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
    const bad = isWaterBad(water);
    const card = el('div', `water-card ${bad ? 'repair' : ''}`.trim());
    const nameRow = el('div', 'wc-name');
    nameRow.appendChild(el('b', '', water.name || '未命名'));
    nameRow.appendChild(el('span', `wc-status ${bad ? 'repair' : 'active'}`,
      bad ? '不好用' : '好用'));
    if (water.pendingApproval) {
      nameRow.appendChild(el('span', 'wc-pending', '待审批'));
    }
    card.appendChild(nameRow);
    const typeText = `${typeLabel(water.type)}${water.stationName ? ` · ${water.stationName}` : ''}`
      + (water.setupLabel ? ` · ${water.setupLabel}` : '');
    card.appendChild(el('div', 'wc-info', typeText));
    if (water.address) {
      card.appendChild(el('div', 'wc-addr', water.address));
    }
    // 不好用的卡直接写出原因、部件与备注，不能只显示一个「报修」
    const badText = waterBadSummary(water);
    if (badText) {
      card.appendChild(el('div', 'wc-bad', badText));
    }
    if (water.waterNo) {
      card.appendChild(el('div', 'wc-no', `编号 ${water.waterNo}`));
    }
    if (water.lng != null && water.lat != null) {
      const nav = el('button', 'wc-nav', '导航');
      nav.type = 'button';
      nav.addEventListener('click', (event) => {
        event.stopPropagation();
        bridge.openUrl(buildBaiduNavUrl(water.lat, water.lng, water.name));
      });
      card.appendChild(nav);
    }
    if (writable) {
      const btns = el('div', 'wc-btns');
      const edit = el('button', 'wc-edit', '编辑');
      edit.type = 'button';
      edit.addEventListener('click', async (event) => {
        event.stopPropagation();
        try {
          const detail = await fetchWater(currentWaterStationId(), water.id);
          state.editing = { isNew: false, water: detail };
          renderEditor();
        } catch (err) {
          setMsg(err.message || '加载失败', true);
        }
      });
      const del = el('button', 'wc-del', '删除');
      del.type = 'button';
      del.addEventListener('click', async (event) => {
        event.stopPropagation();
        if (!window.confirm(`删除 ${water.name}？`)) {
          return;
        }
        try {
          if (canDirectWrite(me)) {
            await deleteWater(currentWaterStationId(), water.id);
          } else {
            // 站级不直改正式卡片，改为提交删除申请
            await submitWaterRequest(currentWaterStationId(), {
              action: 'delete',
              waterId: water.id,
              reason: '申请删除水源',
              clientRequestId: `del-${water.id}-${Date.now()}`
            });
            setMsg('已提交删除申请，待支队审批');
          }
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

  function buildSelect(options, selected) {
    const select = el('select');
    const empty = el('option', '', '未填写');
    empty.value = '';
    select.appendChild(empty);
    options.forEach((item) => {
      const opt = el('option', '', item.label);
      opt.value = item.value;
      select.appendChild(opt);
    });
    select.value = selected || '';
    return select;
  }

  function textInput(placeholder, value, maxLength) {
    const input = document.createElement('input');
    input.type = 'text';
    input.placeholder = placeholder;
    if (maxLength) {
      input.maxLength = maxLength;
    }
    input.value = value == null ? '' : String(value);
    return input;
  }

  function numberInput(placeholder, value, step) {
    const input = document.createElement('input');
    input.type = 'number';
    input.step = step || '0.01';
    input.placeholder = placeholder;
    input.value = value == null ? '' : String(value);
    return input;
  }

  function renderEditor() {
    const editing = state.editing;
    if (!editing) {
      return;
    }
    list.innerHTML = '';
    const water = editing.water || {};
    const form = el('div', 'water-form');
    form.appendChild(el('h3', '', editing.isNew ? '新建水源' : `编辑 ${water.name}`));
    if (!canDirectWrite(me)) {
      form.appendChild(el('div', 'wf-help',
        '消防站提交的改动需支队审批通过后才生效，统计仍按正式卡片计算。'));
    }

    const nameInput = textInput('名称（如：消防水鹤 12）', water.name, 64);
    const typeSel = buildSelect(TYPE_OPTIONS, editing.isNew ? TYPE_OPTIONS[0].value : water.type);
    const setupSel = buildSelect(SETUP_OPTIONS, water.setup);
    const addressInput = textInput('地址', water.address, 128);
    const lngInput = numberInput('经度（WGS84）', water.lng, '0.000001');
    const latInput = numberInput('纬度（WGS84）', water.lat, '0.000001');
    const diameterInput = numberInput('管网直径 mm', water.pipeDiameter, '1');
    const pressureInput = numberInput('管网压力 MPa', water.pressureMpa, '0.001');
    const flowInput = numberInput('管网流量 L/s', water.flowLps, '0.01');
    const supplySel = buildSelect(SUPPLY_FORM_OPTIONS, water.supplyForm);
    const heightInput = numberInput('加水高度 m', water.addHeightM, '0.01');
    const usableSel = buildSelect(USABLE_OPTIONS, water.usable || 'good');
    const repairCheck = document.createElement('input');
    repairCheck.type = 'checkbox';
    repairCheck.checked = water.repair === true;
    const reasonSel = buildSelect(REASON_OPTIONS, water.reasonCode);
    const reasonText = textInput('原因原文（如：阀门损坏导致水压不足）', water.reasonText, REASON_MAX);
    const partSel = buildSelect(PART_OPTIONS, water.partCode);
    const notesInput = document.createElement('textarea');
    notesInput.placeholder = '备注';
    notesInput.rows = 2;
    notesInput.value = water.notes || '';
    const drainViewSel = buildSelect([{ value: 'yes', label: '可查看' }, { value: 'no', label: '不可查看' }],
      water.drainViewable);
    const drainUsableSel = buildSelect(USABLE_OPTIONS, water.drainUsable);
    const drainReasonInput = textInput('无法查看泄水阀原因', water.drainReason, REASON_MAX);
    const drainNotesInput = textInput('泄水阀备注', water.drainNotes, REASON_MAX);
    const warmthSel = buildSelect([{ value: 'yes', label: '有保暖措施' }, { value: 'no', label: '无保暖措施' }],
      water.warmthMeasure === true ? 'yes' : (water.warmthMeasure === false ? 'no' : ''));
    const warmthReasonInput = textInput('无保暖措施原因', water.warmthReason, REASON_MAX);

    const fields = [
      ['名称', nameInput],
      ['类型', typeSel],
      ['设置方式', setupSel],
      ['地址', addressInput],
      ['经度', lngInput],
      ['纬度', latInput],
      ['管网直径 mm', diameterInput],
      ['管网压力 MPa', pressureInput],
      ['管网流量 L/s', flowInput],
      ['给水形式', supplySel],
      ['加水高度 m', heightInput],
      ['可使用', usableSel],
      ['是报修', repairCheck],
      ['不好用原因', reasonSel],
      ['原因原文', reasonText],
      ['部件损坏', partSel],
      ['备注', notesInput],
      ['泄水阀', drainViewSel],
      ['泄水阀好用情况', drainUsableSel],
      ['无法查看泄水阀原因', drainReasonInput],
      ['泄水阀备注', drainNotesInput],
      ['保暖措施', warmthSel],
      ['无保暖措施原因', warmthReasonInput]
    ];
    fields.forEach(([label, input]) => {
      const row = el('div', 'wf-row');
      row.appendChild(el('label', '', label));
      row.appendChild(input);
      form.appendChild(row);
    });

    if (!editing.isNew && water.id) {
      form.appendChild(renderWaterFiles(water, writableForFiles(), () => {
        fetchWater(currentWaterStationId(), water.id).then((detail) => {
          state.editing = { isNew: false, water: detail };
          renderEditor();
        }).catch((err) => {
          setMsg(err.message || '刷新附件失败', true);
        });
      }));
    }

    const save = el('button', 'wf-save', canDirectWrite(me) ? '保存' : '提交审批');
    save.type = 'button';
    save.addEventListener('click', async () => {
      const payload = {
        name: nameInput.value.trim(),
        type: typeSel.value,
        setup: setupSel.value || null,
        address: addressInput.value.trim() || null,
        lng: lngInput.value ? Number(lngInput.value) : null,
        lat: latInput.value ? Number(latInput.value) : null,
        pipeDiameter: diameterInput.value ? Number(diameterInput.value) : null,
        pressureMpa: pressureInput.value ? Number(pressureInput.value) : null,
        flowLps: flowInput.value ? Number(flowInput.value) : null,
        supplyForm: supplySel.value || null,
        addHeightM: heightInput.value ? Number(heightInput.value) : null,
        usable: usableSel.value || 'good',
        isRepair: repairCheck.checked,
        reasonText: reasonText.value.trim() || null,
        notes: notesInput.value.trim() || null,
        drainViewable: drainViewSel.value || null,
        drainUsable: drainUsableSel.value || null,
        drainReason: drainReasonInput.value.trim() || null,
        drainNotes: drainNotesInput.value.trim() || null,
        warmthMeasure: warmthSel.value === '' ? null : warmthSel.value === 'yes',
        warmthReason: warmthReasonInput.value.trim() || null
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
        if (canDirectWrite(me)) {
          if (editing.isNew) {
            await createWater(currentWaterStationId(), payload);
          } else {
            await updateWater(currentWaterStationId(), water.id, payload);
          }
        } else {
          const reason = window.prompt('请填写申请原因', editing.isNew ? '申请新增水源' : '申请修改水源');
          if (reason === null) {
            return;
          }
          await submitWaterRequest(currentWaterStationId(), {
            action: editing.isNew ? 'add' : 'modify',
            waterId: editing.isNew ? null : water.id,
            item: payload,
            reason: reason.trim() || (editing.isNew ? '申请新增水源' : '申请修改水源'),
            clientRequestId: `${editing.isNew ? 'add' : 'mod'}-${water.id || 'new'}-${Date.now()}`
          });
          setMsg('已提交审批申请，待支队通过');
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

  function renderSummary() {
    list.innerHTML = '';
    const stats = state.summary;
    if (!stats) {
      list.appendChild(el('div', 'water-empty', '暂无汇总数据'));
      return;
    }
    list.appendChild(el('div', 'water-summary', stats.title || '水源汇总'));
    list.appendChild(el('div', 'wf-files-title', '水鹤按单位'));
    const craneTable = el('table', 'water-table');
    craneTable.appendChild(tableHead(['单位', '数量', '好用', '好用率', '不好用',
      '无水', '漏水', '压力不足', '锈蚀', '埋压', '圈占', '挪用', '塌井', '毁坏']));
    (stats.craneRows || []).forEach((row) => {
      craneTable.appendChild(tableRow([row.stationName, row.total, row.good, `${row.goodRate}%`, row.bad,
        row.reasons && row.reasons.noWater, row.reasons && row.reasons.leak,
        row.reasons && row.reasons.lowPressure, row.reasons && row.reasons.rust,
        row.reasons && row.reasons.buried, row.reasons && row.reasons.occupied,
        row.reasons && row.reasons.diverted, row.reasons && row.reasons.collapsedWell,
        row.reasons && row.reasons.damaged]));
    });
    list.appendChild(craneTable);

    list.appendChild(el('div', 'wf-files-title', '消火栓按单位'));
    const hydTable = el('table', 'water-table');
    hydTable.appendChild(tableHead(['单位', '小计', '地上', '地下', '好用小计', '地上', '地下',
      '好用率', '不好用小计', '地上', '地下']));
    (stats.hydrantRows || []).forEach((row) => {
      hydTable.appendChild(tableRow([row.stationName, row.total, row.groundTotal, row.undergroundTotal,
        row.good, row.groundGood, row.undergroundGood, `${row.goodRate}%`,
        row.bad, row.groundBad, row.undergroundBad]));
    });
    list.appendChild(hydTable);

    [['消防水鹤', stats.craneTypeRows], ['地上消火栓', stats.groundTypeRows],
      ['地下消火栓', stats.undergroundTypeRows]].forEach(([title, rows]) => {
      list.appendChild(el('div', 'wf-files-title', `${title}类型统计`));
      const table = el('table', 'water-table');
      table.appendChild(tableHead(['单位', '总数', '不好用数', '上传出水视频数', '本月维修数']));
      (rows || []).forEach((row) => {
        table.appendChild(tableRow([row.stationName, row.total, row.bad,
          row.videoCount, row.monthRepairCount]));
      });
      list.appendChild(table);
    });
    list.appendChild(el('div', 'wf-help',
      `应急加水点 ${stats.emergencyPoints || 0} · 消防水塔 ${stats.waterTowers || 0} · 其他水源 ${stats.otherSources || 0}`));
  }

  function tableHead(labels) {
    const thead = document.createElement('thead');
    const tr = document.createElement('tr');
    labels.forEach((label) => {
      const th = document.createElement('th');
      th.textContent = label;
      tr.appendChild(th);
    });
    thead.appendChild(tr);
    return thead;
  }

  function tableRow(values) {
    const tr = document.createElement('tr');
    values.forEach((value) => {
      const td = document.createElement('td');
      td.textContent = value == null ? '' : String(value);
      tr.appendChild(td);
    });
    return tr;
  }

  function renderRequests() {
    list.innerHTML = '';
    if (!state.requests.length) {
      list.appendChild(el('div', 'water-empty', '暂无申请'));
      return;
    }
    state.requests.forEach((item) => {
      const card = el('div', 'water-card');
      const nameRow = el('div', 'wc-name');
      nameRow.appendChild(el('b', '', requestActionLabel(item.action)));
      nameRow.appendChild(el('span', 'wc-status', requestStatusLabel(item.status)));
      card.appendChild(nameRow);
      card.appendChild(el('div', 'wc-info',
        `${item.stationName || ''} · ${item.waterNo || '新增'} · 申请原因：${item.reason || ''}`));
      const beforeText = item.before
        ? `改前：${item.before.name || ''} ${statusLabel(item.before.status || '')}`
        : '改前：无';
      card.appendChild(el('div', 'wc-addr', beforeText));
      if (item.after) {
        card.appendChild(el('div', 'wc-addr',
          `改后：${item.after.name || ''} ${statusLabel(item.after.status || '')}`));
      }
      if (item.reviewReason) {
        card.appendChild(el('div', 'wc-bad', `审批意见：${item.reviewReason}`));
      }
      const btns = el('div', 'wc-btns');
      if (canReview(me) && item.status === 'pending') {
        const pass = el('button', 'wc-edit', '通过');
        pass.type = 'button';
        pass.addEventListener('click', async () => {
          try {
            await reviewWaterRequest(item.stationId, item.id, { approve: true, reviewReason: '同意' });
            setMsg('已通过');
            await loadRequests();
          } catch (err) {
            setMsg(err.message || '审批失败', true);
          }
        });
        const reject = el('button', 'wc-del', '驳回');
        reject.type = 'button';
        reject.addEventListener('click', async () => {
          const reason = window.prompt('请填写驳回原因', '材料不足');
          if (reason === null) {
            return;
          }
          try {
            await reviewWaterRequest(item.stationId, item.id, { approve: false, reviewReason: reason });
            setMsg('已驳回');
            await loadRequests();
          } catch (err) {
            setMsg(err.message || '审批失败', true);
          }
        });
        btns.appendChild(pass);
        btns.appendChild(reject);
      }
      if (!canReview(me) && item.status === 'pending') {
        const cancel = el('button', 'wc-del', '撤回');
        cancel.type = 'button';
        cancel.addEventListener('click', async () => {
          try {
            await withdrawWaterRequest(item.stationId, item.id);
            setMsg('已撤回');
            await loadRequests();
          } catch (err) {
            setMsg(err.message || '撤回失败', true);
          }
        });
        btns.appendChild(cancel);
      }
      if (btns.childNodes.length) {
        card.appendChild(btns);
      }
      list.appendChild(card);
    });
  }

  function requestActionLabel(action) {
    if (action === 'add') {
      return '新增水源';
    }
    if (action === 'delete') {
      return '删除水源';
    }
    return '修改水源';
  }

  function requestStatusLabel(status) {
    if (status === 'approved') {
      return '已通过';
    }
    if (status === 'rejected') {
      return '已驳回';
    }
    return '待审批';
  }

  async function loadSummary() {
    try {
      state.summary = await fetchWaterSummary(state.summaryMonth);
      renderSummary();
    } catch (err) {
      list.textContent = err.message || '汇总加载失败';
    }
  }

  async function loadRequests() {
    try {
      state.requests = await fetchWaterRequests(
        canReview(me) ? null : currentWaterStationId(), null) || [];
      renderRequests();
    } catch (err) {
      list.textContent = err.message || '申请加载失败';
    }
  }

  load();
}
