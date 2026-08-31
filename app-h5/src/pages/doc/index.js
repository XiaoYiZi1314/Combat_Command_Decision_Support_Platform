import '../water/water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { el } from '../water/shared.js';
import { generateDocument } from '../../api/assist.js';
import { fetchStationAttack } from '../../api/attack.js';
import { fetchStationVehicles, vehicleTypeLabel } from '../../api/vehicle.js';
import { fetchWeather } from '../../api/assist.js';
import { bridge } from '../../bridge/index.js';

const DISCLAIMER = '本文书由辅助系统生成，仅供内部参考，正式行文需人工核签。';

const TEMPLATE_OPTIONS = [
  { value: 'fire_report', label: '火灾扑救报告' },
  { value: 'rescue_report', label: '抢险救援报告' },
  { value: 'drill_report', label: '演练记录' },
  { value: 'inspect_report', label: '内攻安全检查记录' }
];

function visibleStations() {
  const me = getMe() || {};
  return Array.isArray(me.visibleStations) ? me.visibleStations : [];
}

function defaultStationId(me) {
  if (me.role === 'station' && me.stationId) {
    return String(me.stationId);
  }
  const first = visibleStations()[0];
  return first ? String(first.id) : '';
}

export function renderDocPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', '文书生成'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(me);
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  body.appendChild(el('div', 'supply-hint', DISCLAIMER));

  const form = el('div', 'water-form');
  form.appendChild(el('h3', '', '文书类型'));
  const templateSel = el('select');
  TEMPLATE_OPTIONS.forEach((item) => {
    const opt = el('option', '', item.label);
    opt.value = item.value;
    templateSel.appendChild(opt);
  });
  form.appendChild(row('类型', templateSel));

  const eventName = document.createElement('input');
  eventName.maxLength = 100;
  eventName.placeholder = '如 ××路仓库火灾扑救';
  form.appendChild(row('事件名称', eventName));

  const locationInput = document.createElement('input');
  locationInput.maxLength = 200;
  locationInput.placeholder = '可空';
  form.appendChild(row('地点', locationInput));

  const stationSel = el('select');
  const stations = visibleStations();
  if (me.role === 'station' && me.stationId) {
    const opt = el('option', '', me.stationName || '本站');
    opt.value = String(me.stationId);
    stationSel.appendChild(opt);
    stationSel.disabled = true;
  } else {
    stations.forEach((station) => {
      const opt = el('option', '', station.name || `站 ${station.id}`);
      opt.value = String(station.id);
      stationSel.appendChild(opt);
    });
  }
  stationSel.value = defaultStationId(me);
  form.appendChild(row('单位', stationSel));
  stationSel.addEventListener('change', () => {
    loadContext();
  });

  const weatherInput = document.createElement('input');
  weatherInput.maxLength = 200;
  weatherInput.placeholder = '自动带入，可改';
  form.appendChild(row('天气摘要', weatherInput));

  const contextInput = document.createElement('textarea');
  contextInput.rows = 6;
  contextInput.maxLength = 2000;
  contextInput.placeholder = '内攻人员与出动车辆将自动带入，可补充修改';
  form.appendChild(row('现场摘要', contextInput));

  const go = el('button', 'wf-save', 'AI生成文书');
  go.type = 'button';
  form.appendChild(go);
  body.appendChild(form);

  const msg = el('div', 'water-msg');
  body.appendChild(msg);

  const output = el('div', 'doc-output');
  output.style.display = 'none';
  body.appendChild(output);

  const copyBtn = el('button', 'wf-cancel doc-copy-btn', '复制文书');
  copyBtn.type = 'button';
  copyBtn.style.display = 'none';
  body.appendChild(copyBtn);

  page.appendChild(body);
  root.appendChild(page);

  function setMsg(text, error) {
    msg.className = error ? 'water-msg error' : 'water-msg';
    msg.textContent = text || '';
  }

  function row(label, input) {
    const wrap = el('div', 'wf-row');
    wrap.appendChild(el('label', '', label));
    wrap.appendChild(input);
    return wrap;
  }

  async function loadContext() {
    const stationId = stationSel.value;
    if (!stationId) {
      return;
    }
    /* 内攻人员 */
    let personText = '';
    try {
      const attack = await fetchStationAttack(stationId);
      const persons = (attack && attack.persons) || [];
      const lines = persons.map((person) => {
        const parts = [person.displayName || ''];
        if (person.cylType) { parts.push(`气瓶${person.cylType}L`); }
        if (person.initPressure != null) { parts.push(`初始压力${person.initPressure}MPa`); }
        if (person.currentPressure != null) { parts.push(`当前压力${person.currentPressure}MPa`); }
        if (person.status) { parts.push(`状态${person.status}`); }
        return parts.join('|');
      });
      if (lines.length) {
        personText = `内攻人员：\n${lines.join('\n')}`;
      }
    } catch (err) {
      /* 内攻数据不可用时仅省略该段 */
    }
    /* 出动车辆 */
    let vehicleText = '';
    try {
      const vehicles = await fetchStationVehicles(stationId, {});
      const lines = (vehicles || []).map((vehicle) => {
        const name = vehicle.vehicleType || vehicleTypeLabel(vehicle.type) || '车辆';
        return `${name} 牌照:${vehicle.plate || '无'}`;
      });
      if (lines.length) {
        vehicleText = `出动车辆：\n${lines.slice(0, 20).join('\n')}`;
      }
    } catch (err) {
      /* 车辆数据不可用时仅省略该段 */
    }
    const parts = [];
    if (personText) { parts.push(personText); }
    if (vehicleText) { parts.push(vehicleText); }
    contextInput.value = parts.join('\n');

    /* 天气：优先定位，失败则留空 */
    try {
      const located = await bridge.locate();
      if (located && located.ok && located.data && located.data.lng != null && located.data.lat != null) {
        const weather = await fetchWeather(located.data.lng, located.data.lat);
        if (weather) {
          const weatherParts = [];
          if (weather.temperature != null) { weatherParts.push(`温度${weather.temperature}℃`); }
          if (weather.humidity != null) { weatherParts.push(`湿度${weather.humidity}%`); }
          if (weather.windSpeed != null) { weatherParts.push(`风速${weather.windSpeed}m/s`); }
          if (weather.windDirection) { weatherParts.push(`风向${weather.windDirection}`); }
          weatherInput.value = weatherParts.join('，');
        }
      }
    } catch (err) {
      /* 定位或天气失败时留空 */
    }
  }

  go.addEventListener('click', async () => {
    go.disabled = true;
    setMsg('AI 正在生成文书…');
    output.style.display = 'none';
    copyBtn.style.display = 'none';
    const contextParts = [];
    const stationId = stationSel.value;
    const stationName = me.role === 'station' ? me.stationName : stationText(stationSel);
    if (stationName) {
      contextParts.push(`当前消防站：${stationName}`);
    }
    if (weatherInput.value.trim()) {
      contextParts.push(`天气：${weatherInput.value.trim()}`);
    }
    if (contextInput.value.trim()) {
      contextParts.push(contextInput.value.trim());
    }
    try {
      const result = await generateDocument({
        template: templateSel.value,
        eventName: eventName.value.trim(),
        location: locationInput.value.trim(),
        date: '',
        context: contextParts.join('\n')
      });
      output.textContent = result.content || '';
      output.style.display = '';
      copyBtn.style.display = '';
      setMsg('文书已生成');
    } catch (err) {
      setMsg(err.message || '生成失败', true);
    } finally {
      go.disabled = false;
    }
  });

  copyBtn.addEventListener('click', async () => {
    const text = output.textContent;
    if (!text) {
      return;
    }
    try {
      await navigator.clipboard.writeText(text);
      setMsg('文书已复制');
    } catch (err) {
      setMsg('复制失败，请长按正文手动复制', true);
    }
  });

  function stationText(select) {
    const opt = select.options[select.selectedIndex];
    return opt ? opt.textContent : '';
  }

  loadContext();
}
