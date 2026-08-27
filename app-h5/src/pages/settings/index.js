import './settings.css';
import { getMe } from '../../stores/session.js';
import { attackQueueLength } from '../../stores/attack.js';
import { getScbaSettings, resetScbaSettings, saveScbaSettings } from '../../stores/settings.js';
import { syncStatusText } from '../../lib/sync.js';
import { logout } from '../../api/auth.js';

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
}

function numberField(label, value, step, min, max) {
  const row = el('div', 'settings-field');
  row.appendChild(el('label', '', label));
  const input = document.createElement('input');
  input.type = 'number'; input.value = String(value); input.step = String(step); input.min = String(min); input.max = String(max);
  row.appendChild(input);
  return { row, input };
}

export function renderSettingsPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  let settings = getScbaSettings();
  const page = el('div', 'settings-page');
  const head = el('div', 'settings-head');
  head.appendChild(el('h2', '', '参数设置'));
  const back = el('button', 'settings-btn', '返回'); back.type = 'button'; back.addEventListener('click', () => { window.location.hash = me.role === 'station' ? '#/attack' : '#/hq'; }); head.appendChild(back); page.appendChild(head);
  const body = el('div', 'settings-body'); page.appendChild(body); root.appendChild(page);
  const msg = el('div', 'settings-msg'); body.appendChild(msg);
  const thresholds = el('div', 'settings-section'); thresholds.appendChild(el('div', 'settings-title', '空呼预警参数'));
  const warnPressure = numberField('压力预警（MPa）', settings.warnPressure, .1, .1, 30);
  const dangerPressure = numberField('压力危险（MPa）', settings.dangerPressure, .1, 0, 29.9);
  const warnTime = numberField('时间预警（分钟）', settings.warnTimeMin, 1, 1, 120);
  const dangerTime = numberField('时间危险（分钟）', settings.dangerTimeMin, 1, 0, 119);
  [warnPressure, dangerPressure, warnTime, dangerTime].forEach((field) => thresholds.appendChild(field.row));
  const work = el('div', 'settings-field'); work.appendChild(el('label', '', '默认工作强度')); const workSelect = document.createElement('select'); [['light','低'],['moderate','中'],['heavy','高']].forEach(([value,label]) => { const option = el('option','',label); option.value=value; workSelect.appendChild(option); }); workSelect.value=settings.defaultWorkLevel; work.appendChild(workSelect); thresholds.appendChild(work);
  const voice = el('label', 'settings-switch'); const voiceInput = document.createElement('input'); voiceInput.type='checkbox'; voiceInput.checked=settings.voiceEnabled; voice.appendChild(voiceInput); voice.appendChild(el('span','','启用语音预警提醒')); thresholds.appendChild(voice);
  const actions = el('div', 'settings-actions'); const save = el('button','settings-btn primary','保存本机设置'); const reset = el('button','settings-btn','恢复默认'); save.type='button'; reset.type='button'; actions.appendChild(save); actions.appendChild(reset); thresholds.appendChild(actions);
  thresholds.appendChild(el('div','settings-help','本阶段设置保存在当前设备，并立即影响 H5 人员卡片阈值颜色与状态判断；后端共享页和指挥投影仍使用一期默认阈值。'));
  body.appendChild(thresholds);
  const sync = el('div','settings-section'); sync.appendChild(el('div','settings-title','同步状态')); const meta=el('div','settings-help','读取中…'); sync.appendChild(meta); const online=typeof navigator==='undefined'?true:navigator.onLine; attackQueueLength().then((n)=>{meta.textContent=syncStatusText(online,n);}).catch(()=>{meta.textContent=syncStatusText(online,0);}); body.appendChild(sync);
  const out=el('button','settings-btn danger settings-logout','退出登录'); out.type='button'; out.addEventListener('click',async()=>{if(!window.confirm('确认退出当前账号？'))return; await logout(); window.location.hash='#/login';}); body.appendChild(out);
  function setMessage(text,error){msg.className=error?'settings-msg error':'settings-msg';msg.textContent=text||'';}
  save.addEventListener('click',()=>{const next={warnPressure:Number(warnPressure.input.value),dangerPressure:Number(dangerPressure.input.value),warnTimeMin:Number(warnTime.input.value),dangerTimeMin:Number(dangerTime.input.value),defaultWorkLevel:workSelect.value,voiceEnabled:voiceInput.checked};if(!(next.dangerPressure<next.warnPressure)){setMessage('压力危险阈值必须低于预警阈值',true);return;}if(!(next.dangerTimeMin<next.warnTimeMin)){setMessage('时间危险阈值必须低于预警阈值',true);return;}settings=saveScbaSettings(next);setMessage('本机参数已保存');});
  reset.addEventListener('click',()=>{settings=resetScbaSettings();warnPressure.input.value=settings.warnPressure;dangerPressure.input.value=settings.dangerPressure;warnTime.input.value=settings.warnTimeMin;dangerTime.input.value=settings.dangerTimeMin;workSelect.value=settings.defaultWorkLevel;voiceInput.checked=settings.voiceEnabled;setMessage('已恢复一期默认参数');});
}
