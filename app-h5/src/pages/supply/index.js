import '../water/water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { fetchNearbyWaters } from '../../api/water.js';
import { fetchStationVehicles, fetchAllVehicles } from '../../api/vehicle.js';
import { fetchSupplyAdvice } from '../../api/assist.js';
import {
  EXPERIENCE_OPTIONS,
  FIRE_INTENSITY,
  HOSE_FACTOR,
  defaultVehicleFlow,
  formatDistance,
  el
} from '../water/shared.js';

/**
 * 取档案里的第一个有效流量：水泵流量 → 水炮流量 → 泡沫炮流量。
 */
function archiveFlow(vehicle) {
  const candidates = [vehicle.pumpFlow, vehicle.waterCannonFlow, vehicle.foamCannonFlow];
  for (let i = 0; i < candidates.length; i += 1) {
    const value = Number(candidates[i]);
    if (Number.isFinite(value) && value > 0) {
      return value;
    }
  }
  return null;
}

/**
 * 载水量文本取首数字（档案里是「18吨」这类文本）。
 */
function firstNumber(text) {
  const match = String(text || '').match(/\d+(\.\d+)?/);
  return match ? Number(match[0]) : 0;
}

/**
 * 单车流量：档案流量优先，三者都没有才按类型估算。
 */
function vehicleFlow(vehicle) {
  const flow = archiveFlow(vehicle);
  if (flow != null) {
    return flow;
  }
  const name = `${vehicle.vehicleType || ''}${vehicle.type || ''}${vehicle.plate || ''}`;
  return defaultVehicleFlow(name);
}

/**
 * 默认参战车辆：载水量大于 0 或流量大于 0 的车，排除宣传车、通信车。
 */
function defaultSelected(vehicle) {
  const text = `${vehicle.vehicleType || ''}${vehicle.type || ''}`;
  if (/宣传|通信/.test(text)) {
    return false;
  }
  return firstNumber(vehicle.waterCap) > 0 || archiveFlow(vehicle) != null || vehicleFlow(vehicle) > 0;
}

export function renderSupplyPage(root, params, hash) {
  root.innerHTML = '';
  const me = getMe() || {};
  const query = new URLSearchParams((hash || '').split('?')[1] || '');
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', '供水计算'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(me);
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  const state = {
    fire: query.get('lng') && query.get('lat')
      ? {
        lng: Number(query.get('lng')),
        lat: Number(query.get('lat')),
        address: query.get('address') || ''
      }
      : null,
    nearby: [],
    vehicles: [],
    selected: new Set(),
    lastLocal: null
  };

  body.appendChild(el('div', 'supply-hint',
    '参战车辆从车辆档案勾选，单车流量优先取档案里的水泵流量，其次水炮、泡沫炮流量，都没有才按类型估算。'
    + 'AI 研判失败仍保留本地计算结果。'));

  const form = el('div', 'water-form');

  // 火灾地点
  const fireRow = el('div', 'wf-row fire-point');
  fireRow.appendChild(el('label', '', '火灾地点'));
  const fireInfo = el('span', 'fire-info', state.fire
    ? `${state.fire.address || '已选点'}（${state.fire.lng.toFixed(6)}, ${state.fire.lat.toFixed(6)}）`
    : '未选择');
  const pickBtn = el('button', '', '地图选点');
  pickBtn.type = 'button';
  pickBtn.addEventListener('click', () => {
    window.location.hash = '#/supply/map';
  });
  fireRow.appendChild(fireInfo);
  fireRow.appendChild(pickBtn);
  form.appendChild(fireRow);

  // 火灾类型
  const typeSel = el('select');
  FIRE_INTENSITY.forEach((item, index) => {
    const opt = el('option', '', item.label);
    opt.value = item.value;
    if (index === 0) {
      opt.selected = true;
    }
    typeSel.appendChild(opt);
  });
  form.appendChild(row('火灾类型', typeSel));

  // 燃烧面积
  const areaInput = numberInput(300, 1);
  form.appendChild(row('燃烧面积 m²', areaInput));

  // 经验系数
  const expSel = el('select');
  EXPERIENCE_OPTIONS.forEach((item) => {
    const opt = el('option', '', item.label);
    opt.value = item.value;
    if (item.value === '1.2') {
      opt.selected = true;
    }
    expSel.appendChild(opt);
  });
  form.appendChild(row('经验场景系数', expSel));

  // 干线
  const hoseLenInput = numberInput(200, 1);
  const hoseDiaSel = el('select');
  [65, 80].forEach((dia) => {
    const opt = el('option', '', `${dia}mm`);
    opt.value = String(dia);
    hoseDiaSel.appendChild(opt);
  });
  hoseDiaSel.value = '80';
  form.appendChild(row('干线长度 m', hoseLenInput));
  form.appendChild(row('干线口径', hoseDiaSel));

  // 参战车辆：从车辆档案勾选
  const vehicleBox = el('div', 'supply-vehicles');
  vehicleBox.appendChild(el('div', 'wf-files-label', '参战车辆（从车辆档案勾选）'));
  form.appendChild(vehicleBox);
  body.appendChild(form);

  const calcBtn = el('button', 'wf-save supply-calc-btn', '计算');
  calcBtn.type = 'button';
  body.appendChild(calcBtn);
  const aiBtn = el('button', 'wf-cancel supply-calc-btn', 'AI 研判');
  aiBtn.type = 'button';
  aiBtn.disabled = true;
  body.appendChild(aiBtn);

  const msg = el('div', 'water-msg');
  body.appendChild(msg);
  const result = el('div', 'supply-result');
  body.appendChild(result);
  page.appendChild(body);
  root.appendChild(page);

  function row(label, input) {
    const wrap = el('div', 'wf-row');
    wrap.appendChild(el('label', '', label));
    wrap.appendChild(input);
    return wrap;
  }

  function numberInput(value, step) {
    const input = document.createElement('input');
    input.type = 'number';
    input.step = String(step);
    input.value = String(value);
    return input;
  }

  function setMsg(text, error) {
    msg.className = error ? 'water-msg error' : 'water-msg';
    msg.textContent = text || '';
  }

  function selectedIntensity() {
    const hit = FIRE_INTENSITY.find((item) => item.value === typeSel.value);
    return hit ? hit.intensity : 0.12;
  }

  function renderVehicles() {
    vehicleBox.innerHTML = '';
    vehicleBox.appendChild(el('div', 'wf-files-label', '参战车辆（从车辆档案勾选）'));
    if (!state.vehicles.length) {
      vehicleBox.appendChild(el('div', 'wf-empty', '没有可参战的车辆档案（已排除报修车）'));
      return;
    }
    state.vehicles.forEach((vehicle) => {
      const line = el('label', 'supply-vehicle');
      const check = document.createElement('input');
      check.type = 'checkbox';
      check.checked = state.selected.has(String(vehicle.id));
      check.addEventListener('change', () => {
        if (check.checked) {
          state.selected.add(String(vehicle.id));
        } else {
          state.selected.delete(String(vehicle.id));
        }
      });
      line.appendChild(check);
      const flow = vehicleFlow(vehicle);
      const archive = archiveFlow(vehicle);
      const source = archive == null ? '类型估算' : '档案流量';
      line.appendChild(el('span', '',
        `${vehicle.plate || vehicle.vehicleType || '未命名车辆'} · ${flow.toFixed(0)} L/s（${source}）`
        + (vehicle.stationName ? ` · ${vehicle.stationName}` : '')));
      vehicleBox.appendChild(line);
    });
  }

  async function loadVehicles() {
    try {
      const stationId = me.role === 'station' && me.stationId ? String(me.stationId) : '';
      const rows = stationId ? await fetchStationVehicles(stationId) : await fetchAllVehicles();
      // 报修车不参与供水计算
      state.vehicles = (rows || []).filter((vehicle) => !/报修/.test(vehicle.status || ''));
      state.selected = new Set();
      state.vehicles.forEach((vehicle) => {
        if (defaultSelected(vehicle)) {
          state.selected.add(String(vehicle.id));
        }
      });
      renderVehicles();
    } catch (err) {
      vehicleBox.innerHTML = '';
      vehicleBox.appendChild(el('div', 'wf-empty', `车辆档案加载失败：${err.message || '网络异常'}`));
    }
  }

  function calc() {
    const area = Number(areaInput.value) || 0;
    if (area <= 0) {
      setMsg('燃烧面积必须大于 0', true);
      return null;
    }
    const experience = Number(expSel.value) || 1.2;
    const required = area * selectedIntensity() * experience;
    const hoseLen = Number(hoseLenInput.value) || 0;
    const hoseDia = Number(hoseDiaSel.value) || 80;
    const hoseFactor = HOSE_FACTOR[hoseDia] != null ? HOSE_FACTOR[hoseDia] : 0.008;
    const hoseLoss = hoseFactor * (Math.max(required, 1) / 10) ** 2 * (hoseLen / 100);
    const vehicles = state.vehicles
      .filter((vehicle) => state.selected.has(String(vehicle.id)))
      .map((vehicle) => ({
        name: vehicle.plate || vehicle.vehicleType || '未命名车辆',
        flow: vehicleFlow(vehicle)
      }));
    const vehicleFlowSum = vehicles.reduce((sum, v) => sum + v.flow, 0);
    // 附近水源只统计好用卡（后端 nearby 已按同一谓词过滤），流量优先用卡片管网流量
    const sourceFlow = state.nearby.reduce((sum, w) => sum + (w.estimateFlow || 0), 0);
    const gap = Math.min(vehicleFlowSum, sourceFlow) - required;
    return {
      required,
      experience,
      hoseLoss,
      vehicles,
      vehicleFlow: vehicleFlowSum,
      sourceFlow,
      gap,
      state: gap >= 0 ? '供水能力基本满足' : '供水能力不足'
    };
  }

  function renderResult(r) {
    result.innerHTML = '';
    const lines = [
      `所需供水流量：${r.required.toFixed(1)} L/s（面积 × 强度 × 经验系数${r.experience.toFixed(1)}）`,
      `参战车辆：${r.vehicles.length} 辆，流量合计 ${r.vehicleFlow.toFixed(0)} L/s`,
      `火点附近水源：${state.nearby.length} 处，流量合计 ${r.sourceFlow.toFixed(0)} L/s`,
      `干线压力损失估算：${r.hoseLoss.toFixed(2)} MPa`,
      `结论：${r.state}（余量 ${r.gap.toFixed(1)} L/s）`,
      r.gap >= 0
        ? '建议：优先就近占用稳定水源，车辆接力供水，保持至少1路备用。'
        : '建议：增加供水车辆或寻找更高流量水源，减少干线长度，必要时分区控火。'
    ];
    lines.forEach((line) => {
      result.appendChild(el('div', 'supply-line', line));
    });
    if (state.nearby.length) {
      result.appendChild(el('div', 'section-title', '参考水源'));
      state.nearby.slice(0, 8).forEach((water) => {
        result.appendChild(el('div', 'supply-source',
          `${water.name} · ${formatDistance(water.distanceM)} · 约${water.estimateFlow} L/s`));
      });
    }
  }

  calcBtn.addEventListener('click', async () => {
    setMsg('');
    if (!state.fire) {
      setMsg('请先选择火灾地点（只统计火点附近水源）', true);
      return;
    }
    try {
      if (!state.nearby.length) {
        const nearby = await fetchNearbyWaters(state.fire.lng, state.fire.lat, 3000);
        state.nearby = (nearby && nearby.waters) || [];
      }
      const r = calc();
      if (r) {
        state.lastLocal = r;
        aiBtn.disabled = false;
        renderResult(r);
      }
    } catch (err) {
      const r = calc();
      if (r) {
        state.lastLocal = r;
        aiBtn.disabled = false;
        renderResult(r);
        setMsg(`附近水源查询失败：${err.message || '网络异常'}，结果未计入水源流量`, true);
      }
    }
  });

  aiBtn.addEventListener('click', async () => {
    if (!state.lastLocal) {
      setMsg('请先完成本地计算', true);
      return;
    }
    const local = state.lastLocal;
    renderResult(local);
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      setMsg('无网：已保留本地计算结果', true);
      return;
    }
    setMsg('AI 研判中…本地结果已保留');
    try {
      const data = await fetchSupplyAdvice({
        fireType: typeSel.value,
        area: Number(areaInput.value) || 0,
        experience: Number(expSel.value) || 1.2,
        requiredFlow: local.required,
        vehicleFlow: local.vehicleFlow,
        sourceFlow: local.sourceFlow,
        gap: local.gap,
        hoseLoss: local.hoseLoss,
        vehicles: local.vehicles,
        sources: state.nearby.slice(0, 8).map((water) => ({
          name: water.name,
          distanceM: water.distanceM,
          estimateFlow: water.estimateFlow
        }))
      });
      renderResult(local);
      if (data && data.advice) {
        result.appendChild(el('div', 'section-title', 'AI 研判'));
        result.appendChild(el('div', 'supply-line', data.advice));
        result.appendChild(el('div', 'supply-hint', data.disclaimer
          || '本结果仅作辅助参考，不替代现场指挥。'));
      }
      setMsg('本地计算优先，AI 为增强说明');
    } catch (err) {
      renderResult(local);
      setMsg(`AI 不可用：${err.message || '网络异常'}，已保留本地计算结果`, true);
    }
  });

  loadVehicles();
}
