import '../water/water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { fetchNearbyWaters } from '../../api/water.js';
import {
  EXPERIENCE_OPTIONS,
  FIRE_INTENSITY,
  HOSE_FACTOR,
  defaultVehicleFlow,
  distanceMeters,
  formatDistance,
  el
} from '../water/shared.js';

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
    nearby: []
  };

  body.appendChild(el('div', 'supply-hint',
    '一期无车辆档案：车辆供水能力按类型取保守默认泵量（举高/水罐/泡沫类 60 L/s，其他水类 40 L/s）。'));

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

  // 参战车辆（自由输入车辆类型名，逐行一条，空行忽略）
  const vehiclesInput = document.createElement('textarea');
  vehiclesInput.rows = 3;
  vehiclesInput.placeholder = '每行一辆，如：\n18吨水罐消防车\n抢险救援消防车';
  form.appendChild(row('参战车辆类型（每行一辆）', vehiclesInput));

  body.appendChild(form);

  const calcBtn = el('button', 'wf-save supply-calc-btn', '计算');
  calcBtn.type = 'button';
  body.appendChild(calcBtn);

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
    const vehicles = vehiclesInput.value.split('\n')
      .map((line) => line.trim())
      .filter(Boolean)
      .map((name) => ({ name, flow: defaultVehicleFlow(name) }));
    const vehicleFlow = vehicles.reduce((sum, v) => sum + v.flow, 0);
    const sourceFlow = state.nearby.reduce((sum, w) => sum + (w.estimateFlow || 0), 0);
    const gap = Math.min(vehicleFlow, sourceFlow) - required;
    return {
      required,
      experience,
      hoseLoss,
      vehicles,
      vehicleFlow,
      sourceFlow,
      gap,
      state: gap >= 0 ? '供水能力基本满足' : '供水能力不足'
    };
  }

  function renderResult(r) {
    result.innerHTML = '';
    const lines = [
      `所需供水流量：${r.required.toFixed(1)} L/s（面积 × 强度 × 经验系数${r.experience.toFixed(1)}）`,
      `参战车辆：${r.vehicles.length} 辆，默认泵量合计 ${r.vehicleFlow.toFixed(0)} L/s`,
      `火点附近水源：${state.nearby.length} 处，估算流量合计 ${r.sourceFlow.toFixed(0)} L/s`,
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
        renderResult(r);
      }
    } catch (err) {
      const r = calc();
      if (r) {
        renderResult(r);
        setMsg(`附近水源查询失败：${err.message || '网络异常'}，结果未计入水源流量`, true);
      }
    }
  });
}
