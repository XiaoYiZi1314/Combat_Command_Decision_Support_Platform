import '../water/water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { fetchAttackAdvice, fetchWeather } from '../../api/assist.js';
import { fetchStationAttack } from '../../api/attack.js';
import { el } from '../water/shared.js';
import { bridge } from '../../bridge/index.js';

const DISCLAIMER = '本结果仅作辅助参考，不替代现场指挥。';
const ACTIVE = new Set(['pending', 'in', 'warn', 'danger']);

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

function formatWeather(data) {
  if (!data) {
    return '';
  }
  const parts = [];
  if (data.temperature != null) {
    parts.push(`${data.temperature}℃`);
  }
  if (data.description) {
    parts.push(data.description);
  }
  if (data.windDirection) {
    parts.push(`${data.windDirection}${data.windSpeed != null ? ` ${data.windSpeed}m/s` : ''}`);
  }
  if (data.humidity != null) {
    parts.push(`湿${data.humidity}%`);
  }
  return parts.join('，').slice(0, 200);
}

export function renderAiPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', 'AI 助手'));
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

  const weatherInput = document.createElement('input');
  weatherInput.maxLength = 200;
  weatherInput.placeholder = '自动带入，可改';
  form.appendChild(row('天气摘要', weatherInput));

  const sceneInput = document.createElement('textarea');
  sceneInput.rows = 3;
  sceneInput.maxLength = 200;
  sceneInput.placeholder = '火点/现场描述，可空';
  form.appendChild(row('现场描述', sceneInput));

  const personBox = el('div', 'water-form');
  personBox.appendChild(el('div', 'wc-info', '未撤出人员（自动带入，可改后再研判）'));
  form.appendChild(personBox);
  const go = el('button', 'wf-save', '辅助研判');
  go.type = 'button';
  form.appendChild(go);
  body.appendChild(form);

  const msg = el('div', 'water-msg');
  body.appendChild(msg);
  const result = el('div', 'supply-result');
  body.appendChild(result);
  page.appendChild(body);
  root.appendChild(page);

  let personDrafts = [];

  function row(label, input) {
    const wrap = el('div', 'wf-row');
    wrap.appendChild(el('label', '', label));
    wrap.appendChild(input);
    return wrap;
  }

  function setMsg(text, error) {
    msg.className = error ? 'water-msg error' : 'water-msg';
    msg.textContent = text || '';
  }

  function online() {
    return typeof navigator === 'undefined' ? true : navigator.onLine;
  }

  function renderPersons() {
    while (personBox.childNodes.length > 1) {
      personBox.removeChild(personBox.lastChild);
    }
    if (!personDrafts.length) {
      personBox.appendChild(el('div', 'water-empty', '当前无未撤出人员，仍可按天气与现场描述研判。'));
      return;
    }
    personDrafts.forEach((person, index) => {
      const card = el('div', 'water-card');
      const nameInput = document.createElement('input');
      nameInput.maxLength = 10;
      nameInput.value = person.displayName || '';
      nameInput.addEventListener('input', () => {
        personDrafts[index].displayName = nameInput.value.trim();
      });
      card.appendChild(row('姓名', nameInput));
      const statusInput = document.createElement('input');
      statusInput.maxLength = 16;
      statusInput.value = person.status || '';
      statusInput.addEventListener('input', () => {
        personDrafts[index].status = statusInput.value.trim();
      });
      card.appendChild(row('状态', statusInput));
      const pressureInput = document.createElement('input');
      pressureInput.type = 'number';
      pressureInput.step = '0.1';
      pressureInput.value = person.pressure == null ? '' : String(person.pressure);
      pressureInput.addEventListener('input', () => {
        const value = Number(pressureInput.value);
        personDrafts[index].pressure = Number.isFinite(value) ? value : undefined;
      });
      card.appendChild(row('压力 MPa', pressureInput));
      const remainInput = document.createElement('input');
      remainInput.type = 'number';
      remainInput.value = person.remainSec == null ? '' : String(person.remainSec);
      remainInput.addEventListener('input', () => {
        const value = Number(remainInput.value);
        personDrafts[index].remainSec = Number.isFinite(value) ? value : undefined;
      });
      card.appendChild(row('剩余秒', remainInput));
      const groupInput = document.createElement('input');
      groupInput.maxLength = 32;
      groupInput.value = person.groupName || '';
      groupInput.addEventListener('input', () => {
        personDrafts[index].groupName = groupInput.value.trim();
      });
      card.appendChild(row('编组', groupInput));
      personBox.appendChild(card);
    });
  }

  async function loadWeather() {
    try {
      const located = await bridge.locate();
      if (!located || !located.ok || !located.data) {
        return;
      }
      const lng = Number(located.data.lng);
      const lat = Number(located.data.lat);
      if (!Number.isFinite(lng) || !Number.isFinite(lat) || (!lng && !lat)) {
        return;
      }
      const data = await fetchWeather(lng, lat);
      const summary = formatWeather(data);
      if (summary && !weatherInput.value.trim()) {
        weatherInput.value = summary;
      }
    } catch (err) {
      if (!weatherInput.value.trim()) {
        weatherInput.placeholder = '天气摘要自动带入失败，可手填';
      }
    }
  }

  async function loadPreview() {
    const stationId = stationSel.value;
    if (!stationId) {
      personDrafts = [];
      renderPersons();
      return;
    }
    try {
      const data = await fetchStationAttack(stationId);
      personDrafts = ((data && data.persons) || [])
        .filter((item) => ACTIVE.has(item.status))
        .slice(0, 40)
        .map((item) => ({
          displayName: item.displayName || '',
          status: item.status || '',
          pressure: item.currentPressure,
          remainSec: item.remainSec,
          groupName: item.groupName || ''
        }));
      renderPersons();
    } catch (err) {
      personDrafts = [];
      renderPersons();
      setMsg(err.message || '人员摘要加载失败', true);
    }
  }

  go.addEventListener('click', async () => {
    if (!online()) {
      setMsg('无网：已禁用 AI', true);
      return;
    }
    const stationId = Number(stationSel.value);
    if (!stationId) {
      setMsg('请选择单位', true);
      return;
    }
    setMsg('研判中…');
    result.innerHTML = '';
    try {
      const data = await fetchAttackAdvice({
        stationId,
        weatherSummary: weatherInput.value.trim() || undefined,
        sceneDesc: sceneInput.value.trim() || undefined,
        persons: personDrafts
      });
      if (data.rotationAdvice) {
        result.appendChild(el('div', 'supply-line', `轮换：${data.rotationAdvice}`));
      }
      if (data.withdrawAdvice) {
        result.appendChild(el('div', 'supply-line', `撤离：${data.withdrawAdvice}`));
      }
      if (data.riskHint) {
        result.appendChild(el('div', 'supply-line', `风险：${data.riskHint}`));
      }
      result.appendChild(el('div', 'supply-hint', data.disclaimer || DISCLAIMER));
      setMsg('');
    } catch (err) {
      setMsg(err.message || '研判失败', true);
    }
  });

  stationSel.addEventListener('change', loadPreview);
  loadPreview();
  loadWeather();
}
