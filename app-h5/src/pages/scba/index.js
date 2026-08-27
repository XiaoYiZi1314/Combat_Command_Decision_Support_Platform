import './scba.css';
import { fetchProfile, fetchRoster, saveScbaCalibration } from '../../api/roster.js';
import { SCBA, fmtMinSec, remainSecOf } from '../../lib/scba.js';
import { getMe, homeHashOf } from '../../stores/session.js';

const ALARM_PRESSURE = 5;

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

function initialStationId() {
  const me = getMe() || {};
  if (me.role === 'station' && me.stationId) return String(me.stationId);
  const first = stations()[0];
  return first ? String(first.id) : '';
}

function metric(label, className) {
  const box = el('div', className ? `scba-metric ${className}` : 'scba-metric');
  box.appendChild(el('label', '', label));
  const value = el('b', '', '--');
  box.appendChild(value);
  return { box, value };
}

function personalK(pressure, cylType, fullTimeSec) {
  const measuredMin = fullTimeSec / SCBA.secondsPerMinute;
  const formulaMin = Number(pressure) * SCBA.pressureToVolume
    * SCBA.cylVolume[String(cylType)] / SCBA.rmv.heavy;
  return measuredMin > 0 ? formulaMin / measuredMin : null;
}

export function renderScbaPage(root) {
  const me = getMe() || {};
  const writable = me.role === 'station';
  const state = { stationId: initialStationId(), roster: null, profile: null, result: null };
  root.innerHTML = '';
  const page = el('div', 'scba-page');
  const head = el('div', 'scba-head');
  head.appendChild(el('h2', '', '空呼时间计算器'));
  const back = el('button', 'scba-btn', '返回');
  back.type = 'button';
  back.addEventListener('click', () => { window.location.hash = homeHashOf(getMe()); });
  head.appendChild(back);
  page.appendChild(head);
  const body = el('div', 'scba-body');
  page.appendChild(body);
  root.appendChild(page);
  if (!state.stationId) { body.appendChild(el('div', 'scba-section scba-help', '当前账号没有可查看的消防站')); return; }

  const msg = el('div', 'scba-msg');
  body.appendChild(msg);
  const form = el('div', 'scba-section');
  form.appendChild(el('div', 'scba-title', '个人高强度实测标定'));
  if (!writable) {
    const stationField = field('选择单位', 'select');
    stations().forEach((station) => { const option = el('option', '', station.name); option.value = String(station.id); stationField.input.appendChild(option); });
    stationField.input.value = state.stationId;
    stationField.input.addEventListener('change', () => { state.stationId = stationField.input.value; loadRoster(); });
    form.appendChild(stationField.row);
  }
  const personField = field('人员姓名', 'select');
  form.appendChild(personField.row);
  const help = el('div', 'scba-help');
  const details = document.createElement('details');
  const summary = el('summary', '', '测试方法说明（点击展开）');
  details.appendChild(summary);
  details.appendChild(el('div', '', '由本人佩戴实际气瓶和全套装备，在接近实战的持续高强度状态下，从实际初始压力计时至完全用尽。记录总用时后计算个人 K 值。严禁为了测试故意耗尽现场备用气源。'));
  help.appendChild(details);
  form.appendChild(help);
  const pressureField = field('气瓶初始压力（MPa）', 'number');
  pressureField.input.min = '0.1'; pressureField.input.max = '30'; pressureField.input.step = '0.1'; pressureField.input.value = '25';
  const cylField = field('气瓶规格', 'select');
  [['6.8', '6.8L'], ['9', '9L']].forEach(([value, label]) => { const option = el('option', '', label); option.value = value; cylField.input.appendChild(option); });
  const topGrid = el('div', 'scba-grid'); topGrid.appendChild(pressureField.row); topGrid.appendChild(cylField.row); form.appendChild(topGrid);
  const minuteField = field('高强度使用时间（分钟）', 'number'); minuteField.input.min = '0'; minuteField.input.step = '1';
  const secondField = field('补充秒数（0–59）', 'number'); secondField.input.min = '0'; secondField.input.max = '59'; secondField.input.step = '1'; secondField.input.value = '0';
  const timeGrid = el('div', 'scba-grid'); timeGrid.appendChild(minuteField.row); timeGrid.appendChild(secondField.row); form.appendChild(timeGrid);
  const calculate = el('button', 'scba-btn primary', '计算校准时间'); calculate.type = 'button'; form.appendChild(calculate);
  body.appendChild(form);

  const result = el('div', 'scba-section');
  result.appendChild(el('div', 'scba-title', '估算结果'));
  const metrics = el('div', 'scba-result');
  const full = metric('高强度完全用尽', ''); const alarm = metric('到5MPa报警', 'warn'); const evac = metric('80%强制撤离参考', 'danger');
  [full, alarm, evac].forEach((item) => metrics.appendChild(item.box)); result.appendChild(metrics);
  const estimates = el('div', 'scba-estimates'); result.appendChild(estimates);
  const actions = el('div', 'scba-actions');
  const save = el('button', 'scba-btn primary', '保存到个人档案'); save.type = 'button'; if (writable) actions.appendChild(save); result.appendChild(actions);
  body.appendChild(result);
  const historySection = el('div', 'scba-section'); historySection.appendChild(el('div', 'scba-title', '历史标定')); const history = el('div', 'scba-history'); historySection.appendChild(history); body.appendChild(historySection);
  body.appendChild(el('div', 'scba-section scba-help', '估算结果仅供作战指挥辅助参考，不替代报警哨、现场安全员判断和撤离口令。'));

  function field(label, type) {
    const row = el('div', 'scba-field'); row.appendChild(el('label', '', label));
    const input = document.createElement(type === 'select' ? 'select' : 'input'); if (type !== 'select') input.type = type; row.appendChild(input); return { row, input };
  }
  function setMessage(text, error) { msg.className = error ? 'scba-msg error' : 'scba-msg'; msg.textContent = text || ''; }
  function drawHistory() {
    history.innerHTML = '';
    const rows = state.profile && state.profile.calibrations ? state.profile.calibrations : [];
    if (!rows.length) { history.appendChild(el('div', 'scba-help', '暂无标定记录')); return; }
    rows.forEach((item) => history.appendChild(el('div', 'scba-history-item', `${item.measuredAt ? String(item.measuredAt).replace('T', ' ').slice(0, 19) : '--'} · ${item.cylType}L · ${item.pressure}MPa · ${fmtMinSec(item.fullTimeSec)} · ${item.source || '标定'}`)));
  }
  async function selectProfile() {
    if (!personField.input.value) { state.profile = null; drawHistory(); return; }
    try {
      state.profile = await fetchProfile(state.stationId, personField.input.value);
      cylField.input.value = String(state.profile.cylType || '6.8');
      if (state.profile.morningPressure) pressureField.input.value = String(state.profile.morningPressure);
      drawHistory();
    } catch (err) { setMessage(err.message || '档案加载失败', true); }
  }
  async function loadRoster() {
    try {
      state.roster = await fetchRoster(state.stationId);
      personField.input.innerHTML = '';
      const empty = el('option', '', '-- 选择人员 --'); empty.value = ''; personField.input.appendChild(empty);
      (state.roster.profiles || []).forEach((profile) => { const option = el('option', '', profile.name); option.value = String(profile.id); personField.input.appendChild(option); });
      state.profile = null; drawHistory();
    } catch (err) { setMessage(err.message || '花名册加载失败', true); }
  }
  personField.input.addEventListener('change', selectProfile);
  calculate.addEventListener('click', () => {
    const pressure = Number(pressureField.input.value); const minutes = Number(minuteField.input.value || 0); const seconds = Number(secondField.input.value || 0); const total = Math.round(minutes * 60 + seconds);
    if (!(pressure > 0 && pressure <= 30) || total < 60 || total > 14400 || seconds < 0 || seconds > 59) { setMessage('请输入有效压力和1分钟至4小时的实测时间', true); return; }
    const k = personalK(pressure, cylField.input.value, total);
    if (!Number.isFinite(k) || k <= 0) {
      state.result = null;
      setMessage('标定参数无法计算，请检查气瓶规格和实测时间', true);
      return;
    }
    const alarmSec = Math.round(total * Math.max(0, pressure - ALARM_PRESSURE) / pressure);
    const evacSec = Math.round(alarmSec * SCBA.evacRatio);
    full.value.textContent = fmtMinSec(total); alarm.value.textContent = fmtMinSec(alarmSec); evac.value.textContent = fmtMinSec(evacSec);
    estimates.innerHTML = '';
    estimates.appendChild(el('div', 'scba-estimate', `个人 K 值：${k.toFixed(3)}`));
    estimates.appendChild(el('div', 'scba-estimate', `中强度 / 平地预计：${fmtMinSec(remainSecOf(pressure, cylField.input.value, 'moderate', 'flat', k))}`));
    estimates.appendChild(el('div', 'scba-estimate', `低强度 / 平地预计：${fmtMinSec(remainSecOf(pressure, cylField.input.value, 'light', 'flat', k))}`));
    state.result = { pressure, fullTimeSec: total, cylType: cylField.input.value, source: '空呼时间计算器' };
    setMessage('计算完成');
  });
  save.addEventListener('click', async () => {
    if (!state.profile || !state.result) { setMessage('请选择人员并先完成计算', true); return; }
    save.disabled = true;
    try { state.profile = await saveScbaCalibration(state.stationId, state.profile.id, state.result); drawHistory(); setMessage('标定已保存到个人档案'); }
    catch (err) { setMessage(err.message || '标定保存失败', true); }
    finally { save.disabled = false; }
  });
  loadRoster();
}
