import '../water/water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { fetchAttackAdvice } from '../../api/assist.js';
import { fetchStationAttack } from '../../api/attack.js';
import { el } from '../water/shared.js';

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

function statusLabel(code) {
  if (code === 'pending') {
    return '预录入';
  }
  if (code === 'in') {
    return '安全';
  }
  if (code === 'warn') {
    return '预警';
  }
  if (code === 'danger') {
    return '危险';
  }
  return code || '-';
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
  weatherInput.placeholder = '温、风、湿等摘要，可空';
  form.appendChild(row('天气摘要', weatherInput));

  const sceneInput = document.createElement('textarea');
  sceneInput.rows = 3;
  sceneInput.maxLength = 200;
  sceneInput.placeholder = '火点/现场描述，可空';
  form.appendChild(row('现场描述', sceneInput));

  const preview = el('div', 'wc-info', '将自动带入本站未撤出人员（姓名、状态、压力、剩余时间、编组）');
  form.appendChild(preview);
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

  async function loadPreview() {
    const stationId = stationSel.value;
    if (!stationId) {
      preview.textContent = '请选择单位';
      return;
    }
    try {
      const data = await fetchStationAttack(stationId);
      const persons = ((data && data.persons) || []).filter((item) => ACTIVE.has(item.status));
      if (!persons.length) {
        preview.textContent = '当前无未撤出人员，仍可按天气与现场描述研判。';
        return;
      }
      preview.textContent = persons.slice(0, 8).map((item) => {
        const remain = item.remainSec == null ? '-' : `${item.remainSec}秒`;
        return `${item.displayName} ${statusLabel(item.status)} ${item.currentPressure ?? '-'}MPa ${remain}`;
      }).join('；');
    } catch (err) {
      preview.textContent = err.message || '人员摘要加载失败';
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
        sceneDesc: sceneInput.value.trim() || undefined
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
}
