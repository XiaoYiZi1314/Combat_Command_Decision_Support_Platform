import './key-units.css';
import {
  createKeyUnit,
  deleteKeyUnit,
  deleteKeyUnitFile,
  getKeyUnit,
  listKeyUnits,
  updateKeyUnit,
  uploadKeyUnitFile
} from '../../api/key-units.js';
import { getMe, homeHashOf } from '../../stores/session.js';

const CATEGORIES = ['人员密集场所', '高层建筑', '地下建筑', '危险化学品', '文物古建', '大跨度厂房', '其他'];
const PLAN_BIZ_TYPE = 'keyunit_plan';
const FLOOR_BIZ_TYPE = 'keyunit_floor';

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
}

function stations() {
  const me = getMe() || {};
  return Array.isArray(me.visibleStations) ? me.visibleStations : [];
}

function stationId() {
  const me = getMe() || {};
  if (me.role === 'station' && me.stationId) return String(me.stationId);
  const first = stations()[0];
  return first ? String(first.id) : '';
}

function fileName(file) {
  const type = file && file.contentType ? file.contentType : '文件';
  return `${type} #${file && file.id ? file.id : ''}`;
}

function fileLink(file, writable, onDelete) {
  const row = el('div', 'ku-file');
  const link = document.createElement('a');
  link.href = file.previewUrl || file.downloadUrl || '#';
  link.target = '_blank';
  link.rel = 'noopener noreferrer';
  link.textContent = fileName(file);
  row.appendChild(link);
  if (writable) {
    const remove = el('button', '', '删除');
    remove.type = 'button';
    remove.addEventListener('click', onDelete);
    row.appendChild(remove);
  }
  return row;
}

function header(title, backHash) {
  const head = el('div', 'key-units-head');
  head.appendChild(el('h2', '', title));
  const back = el('button', 'ku-btn', '返回');
  back.type = 'button';
  back.addEventListener('click', () => { window.location.hash = backHash || homeHashOf(getMe()); });
  head.appendChild(back);
  return head;
}

function stationSelector(current, onChange) {
  const select = document.createElement('select');
  stations().forEach((station) => {
    const option = document.createElement('option');
    option.value = String(station.id);
    option.textContent = station.name;
    select.appendChild(option);
  });
  select.value = current;
  select.addEventListener('change', () => onChange(select.value));
  return select;
}

export function renderKeyUnitsPage(root) {
  const me = getMe() || {};
  const writable = me.role === 'station';
  const state = { stationId: stationId(), keyword: '', category: '' };
  root.innerHTML = '';
  const page = el('div', 'key-units-page');
  page.appendChild(header('重点单位', null));
  const body = el('div', 'ku-body');
  page.appendChild(body);
  root.appendChild(page);
  if (!state.stationId) {
    body.appendChild(el('div', 'ku-section ku-empty', '当前账号没有可查看的消防站'));
    return;
  }

  if (!writable) {
    const stationBox = el('div', 'ku-section');
    stationBox.appendChild(el('div', 'ku-field', '查看单位'));
    stationBox.appendChild(stationSelector(state.stationId, (id) => { state.stationId = id; load(); }));
    body.appendChild(stationBox);
  }

  const toolbar = el('div', 'ku-toolbar');
  const keyword = document.createElement('input');
  keyword.type = 'search';
  keyword.placeholder = '搜索单位名称';
  const search = el('button', 'ku-btn primary', '搜索');
  search.type = 'button';
  const filter = el('div', 'ku-filter');
  const category = document.createElement('select');
  const all = el('option', '', '全部类别');
  all.value = '';
  category.appendChild(all);
  CATEGORIES.forEach((item) => { const option = el('option', '', item); option.value = item; category.appendChild(option); });
  const add = el('button', 'ku-btn primary', '+ 添加重点单位');
  add.type = 'button';
  filter.appendChild(category);
  if (writable) filter.appendChild(add);
  toolbar.appendChild(keyword);
  toolbar.appendChild(search);
  toolbar.appendChild(filter);
  body.appendChild(toolbar);
  const msg = el('div', 'ku-msg');
  const list = el('div', 'ku-list');
  body.appendChild(msg);
  body.appendChild(list);

  search.addEventListener('click', () => { state.keyword = keyword.value.trim(); state.category = category.value; load(); });
  keyword.addEventListener('keydown', (event) => { if (event.key === 'Enter') search.click(); });
  category.addEventListener('change', () => { state.category = category.value; load(); });
  add.addEventListener('click', () => { window.location.hash = '#/key-units/new'; });

  async function load() {
    list.textContent = '正在加载…';
    try {
      const data = await listKeyUnits(state.stationId, state);
      list.innerHTML = '';
      if (!data || !data.length) { list.appendChild(el('div', 'ku-empty', '暂无重点单位')); return; }
      data.forEach((unit) => list.appendChild(renderCard(unit)));
    } catch (err) { list.textContent = err.message || '加载失败'; }
  }

  function renderCard(unit) {
    const card = el('div', 'ku-card');
    const cardHead = el('div', 'ku-card-head');
    cardHead.appendChild(el('strong', '', unit.name || '未命名单位'));
    cardHead.appendChild(el('span', 'ku-category', unit.category || '未分类'));
    card.appendChild(cardHead);
    const meta = [unit.address, unit.contact, unit.phoneMasked ? `电话 ${unit.phoneMasked}` : ''].filter(Boolean).join(' · ');
    card.appendChild(el('div', 'ku-meta', meta || '暂无基础资料'));
    if (unit.notes) card.appendChild(el('div', 'ku-plan-preview', unit.notes));
    const actions = el('div', 'ku-card-actions');
    const view = el('button', 'ku-btn', '查看预案');
    view.type = 'button';
    view.addEventListener('click', () => { window.location.hash = `#/key-units/${unit.id}`; });
    actions.appendChild(view);
    if (writable) {
      const edit = el('button', 'ku-btn', '编辑');
      edit.type = 'button';
      edit.addEventListener('click', () => { window.location.hash = `#/key-units/${unit.id}`; });
      const remove = el('button', 'ku-btn danger', '删除');
      remove.type = 'button';
      remove.addEventListener('click', async () => {
        if (!window.confirm(`确认删除重点单位“${unit.name}”？`)) return;
        try { await deleteKeyUnit(state.stationId, unit.id); setMessage('重点单位已删除'); await load(); }
        catch (err) { setMessage(err.message || '删除失败', true); }
      });
      actions.appendChild(edit);
      actions.appendChild(remove);
    }
    card.appendChild(actions);
    return card;
  }

  function setMessage(text, error) { msg.className = error ? 'ku-msg error' : 'ku-msg'; msg.textContent = text || ''; }
  load();
}

export function renderKeyUnitEditPage(root, params) {
  const me = getMe() || {};
  const writable = me.role === 'station';
  const creating = !params.id || params.id === 'new';
  const state = { stationId: stationId(), id: creating ? null : params.id, unit: null, planFiles: [], floorPlans: [] };
  root.innerHTML = '';
  const page = el('div', 'key-units-page');
  page.appendChild(header(creating ? '添加重点单位' : '预案详情', '#/key-units'));
  const body = el('div', 'ku-body');
  page.appendChild(body);
  root.appendChild(page);
  if (!state.stationId) { body.appendChild(el('div', 'ku-section ku-empty', '当前账号没有可查看的消防站')); return; }
  const form = el('div', 'ku-section');
  const title = el('div', 'ku-form-title', creating ? '单位资料' : '单位资料与预案');
  form.appendChild(title);
  const fields = el('div', 'ku-fields');
  const controls = {};
  [['name', '单位名称', 'text'], ['address', '详细地址', 'text'], ['contact', '联系人', 'text'], ['phone', '联系电话', 'tel'], ['notes', '备注', 'text']].forEach(([key, label, type]) => {
    const field = el('div', 'ku-field'); field.appendChild(el('label', '', label));
    const input = document.createElement('input'); input.type = type; input.maxLength = key === 'notes' ? 1000 : 500; input.disabled = !writable; controls[key] = input; field.appendChild(input); fields.appendChild(field);
  });
  const categoryField = el('div', 'ku-field'); categoryField.appendChild(el('label', '', '类别'));
  const category = document.createElement('select'); CATEGORIES.forEach((item) => { const option = el('option', '', item); option.value = item; category.appendChild(option); }); category.disabled = !writable; controls.category = category; categoryField.appendChild(category); fields.appendChild(categoryField);
  const planField = el('div', 'ku-field'); planField.appendChild(el('label', '', '预案正文'));
  const planText = document.createElement('textarea'); planText.disabled = !writable; controls.planText = planText; planField.appendChild(planText); fields.appendChild(planField);
  form.appendChild(fields);
  const fileSection = el('div', 'ku-section');
  fileSection.appendChild(el('div', 'ku-form-title', '附件资料'));
  const planList = el('div', 'ku-files');
  fileSection.appendChild(el('div', 'ku-field', '预案文件'));
  fileSection.appendChild(planList);
  const fileInput = document.createElement('input');
  fileInput.type = 'file';
  fileInput.accept = '.doc,.docx,.pdf,.txt,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document';
  const upload = el('button', 'ku-btn primary', '上传预案');
  upload.type = 'button';
  const uploadBox = el('div', 'ku-upload');
  uploadBox.appendChild(fileInput);
  if (writable) uploadBox.appendChild(upload);
  fileSection.appendChild(uploadBox);
  const floorList = el('div', 'ku-files');
  fileSection.appendChild(el('div', 'ku-field', '平面图'));
  fileSection.appendChild(floorList);
  const floorInput = document.createElement('input');
  floorInput.type = 'file';
  floorInput.accept = 'image/jpeg,image/png,application/pdf';
  const floorUpload = el('button', 'ku-btn primary', '上传平面图');
  floorUpload.type = 'button';
  const floorBox = el('div', 'ku-upload');
  floorBox.appendChild(floorInput);
  if (writable) floorBox.appendChild(floorUpload);
  fileSection.appendChild(floorBox);
  fileSection.appendChild(el('div', 'ku-help', '预案支持 doc/docx/pdf/txt，平面图支持 jpg/png/pdf，单文件最大 20MB；上传后使用短时预览地址。正文抽取失败时可直接粘贴补充。'));
  const msg = el('div', 'ku-msg'); body.appendChild(msg); body.appendChild(form); body.appendChild(fileSection);
  const actions = el('div', 'ku-section ku-card-actions');
  if (writable) { const save = el('button', 'ku-btn primary', '保存'); save.type = 'button'; actions.appendChild(save); save.addEventListener('click', () => saveUnit(save)); }
  body.appendChild(actions);

  function setMessage(text, error) { msg.className = error ? 'ku-msg error' : 'ku-msg'; msg.textContent = text || ''; }
  function renderFileList(container, files, emptyText) {
    container.innerHTML = '';
    if (!files.length) {
      container.appendChild(el('div', 'ku-empty', emptyText));
      return;
    }
    files.forEach((file) => container.appendChild(fileLink(file, writable, async () => {
      if (!window.confirm('确认删除该文件？')) return;
      try {
        await deleteKeyUnitFile(file.id);
        state.planFiles = state.planFiles.filter((item) => item.id !== file.id);
        state.floorPlans = state.floorPlans.filter((item) => item.id !== file.id);
        renderFiles();
      } catch (err) {
        setMessage(err.message || '文件删除失败', true);
      }
    })));
  }
  function renderFiles() {
    renderFileList(planList, state.planFiles, '尚未上传预案文件');
    renderFileList(floorList, state.floorPlans, '尚未上传平面图');
  }
  function fill(unit) { state.unit = unit; state.planFiles = unit.planFiles || []; state.floorPlans = unit.floorPlans || []; Object.keys(controls).forEach((key) => { controls[key].value = unit[key] || ''; }); renderFiles(); }
  async function saveUnit(button) { if (!controls.name.value.trim()) { setMessage('单位名称不能为空', true); return; } button.disabled = true; try { const bodyData = { name: controls.name.value.trim(), address: controls.address.value.trim(), category: controls.category.value, contact: controls.contact.value.trim(), phone: controls.phone.value.trim(), notes: controls.notes.value.trim(), planText: controls.planText.value }; const saved = creating ? await createKeyUnit(state.stationId, bodyData) : await updateKeyUnit(state.stationId, state.id, bodyData); state.id = saved.id; state.planFiles = saved.planFiles || state.planFiles; setMessage('保存成功'); renderFiles(); if (creating) { window.history.replaceState({}, '', `#/key-units/${saved.id}`); } } catch (err) { setMessage(err.message || '保存失败', true); } finally { button.disabled = false; } }
  async function uploadFile(input, button, bizType, emptyText) {
    const file = input.files && input.files[0];
    if (!file) { setMessage(`请先选择${emptyText}`, true); return; }
    if (!state.id) { setMessage('请先保存单位资料，再上传文件', true); return; }
    button.disabled = true;
    try {
      await uploadKeyUnitFile(bizType, Number(state.id), file);
      fill(await getKeyUnit(state.stationId, state.id));
      input.value = '';
      setMessage(`${emptyText}上传成功`);
    } catch (err) {
      setMessage(err.message || '上传失败', true);
    } finally {
      button.disabled = false;
    }
  }
  upload.addEventListener('click', () => uploadFile(fileInput, upload, PLAN_BIZ_TYPE, '预案文件'));
  floorUpload.addEventListener('click', () => uploadFile(floorInput, floorUpload, FLOOR_BIZ_TYPE, '平面图'));
  async function load() { if (creating) { category.value = CATEGORIES[0]; renderFiles(); return; } try { fill(await getKeyUnit(state.stationId, state.id)); } catch (err) { setMessage(err.message || '加载失败', true); } }
  load();
}
