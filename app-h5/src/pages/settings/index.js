import './settings.css';
import { getMe } from '../../stores/session.js';
import { attackQueueLength } from '../../stores/attack.js';
import { getScbaSettings, resetScbaSettings, saveScbaSettings } from '../../stores/settings.js';
import { syncStatusText } from '../../lib/sync.js';
import { logout } from '../../api/auth.js';
import { fetchScbaSettings, saveScbaSettings as saveScbaSettingsRemote } from '../../api/attack.js';
import { buildOutdoorToggle, isOutdoor, setOutdoor } from '../../lib/theme.js';
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
  const stationId = me.stationId ? String(me.stationId) : '';
  // 3.3：站级账号保存后阈值随实时状态下发；指挥端只读
  const canWriteServer = me.role === 'station' && stationId;
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
  const actions = el('div', 'settings-actions'); const save = el('button','settings-btn primary', canWriteServer ? '保存并下发' : '保存本机设置'); const reset = el('button','settings-btn','恢复默认'); save.type='button'; reset.type='button'; actions.appendChild(save); actions.appendChild(reset); thresholds.appendChild(actions);
  thresholds.appendChild(el('div','settings-help', canWriteServer
    ? '保存后阈值随本站实时状态下发，二维码共享页与支队单站卡片都用这份阈值算颜色。'
    : '当前账号为只读角色，阈值以消防站保存的为准。'));
  body.appendChild(thresholds);

  // 2.3：设置里也可以开关户外模式
  const theme = el('div', 'settings-section');
  theme.appendChild(el('div', 'settings-title', '显示'));
  const themeRow = el('label', 'settings-switch');
  const themeInput = document.createElement('input');
  themeInput.type = 'checkbox';
  themeInput.checked = isOutdoor();
  themeInput.addEventListener('change', () => setOutdoor(themeInput.checked));
  themeRow.appendChild(themeInput);
  themeRow.appendChild(el('span', '', '户外模式（主界面白底黑字）'));
  theme.appendChild(themeRow);
  theme.appendChild(el('div', 'settings-help', '只影响内攻主界面与支队首页；值班表、水源、车辆等页面保持深色。'));
  body.appendChild(theme);

  // 5.1：改密入口（保留后端改密能力作管理员重置兜底）
  const account = el('div', 'settings-section');
  account.appendChild(el('div', 'settings-title', '账号'));
  const pwdRow = el('div', 'settings-field');
  pwdRow.appendChild(el('label', '', '修改密码'));
  const pwdBtn = el('button', 'settings-btn', '修改');
  pwdBtn.type = 'button';
  pwdBtn.addEventListener('click', () => {
    window.location.hash = '#/login?change=1';
  });
  pwdRow.appendChild(pwdBtn);
  account.appendChild(pwdRow);
  account.appendChild(el('div', 'settings-help', '新密码不少于 8 位。'));
  body.appendChild(account);

  const sync = el('div','settings-section'); sync.appendChild(el('div','settings-title','同步状态')); const meta=el('div','settings-help','读取中…'); sync.appendChild(meta); const online=typeof navigator==='undefined'?true:navigator.onLine; attackQueueLength().then((n)=>{meta.textContent=syncStatusText(online,n);}).catch(()=>{meta.textContent=syncStatusText(online,0);}); body.appendChild(sync);
  const out=el('button','settings-btn danger settings-logout','退出登录'); out.type='button'; out.addEventListener('click',async()=>{if(!window.confirm('确认退出当前账号？'))return; await logout(); window.location.hash='#/login';}); body.appendChild(out);
  function setMessage(text,error){msg.className=error?'settings-msg error':'settings-msg';msg.textContent=text||'';}

  // 站级账号进页面先拉服务端阈值，以服务端为准
  if (canWriteServer) {
    fetchScbaSettings(stationId).then((remote) => {
      if (!remote) {
        return;
      }
      warnPressure.input.value = remote.warnPressure;
      dangerPressure.input.value = remote.dangerPressure;
      warnTime.input.value = remote.warnTimeMin;
      dangerTime.input.value = remote.dangerTimeMin;
      if (remote.defaultWorkLevel) {
        workSelect.value = remote.defaultWorkLevel;
      }
      voiceInput.checked = remote.voiceEnabled !== false;
    }).catch(() => {
      setMessage('服务端阈值读取失败，显示本机值', true);
    });
  }

  save.addEventListener('click', async () => {
    const next = {
      warnPressure: Number(warnPressure.input.value),
      dangerPressure: Number(dangerPressure.input.value),
      warnTimeMin: Number(warnTime.input.value),
      dangerTimeMin: Number(dangerTime.input.value),
      defaultWorkLevel: workSelect.value,
      voiceEnabled: voiceInput.checked
    };
    if (!(next.dangerPressure < next.warnPressure)) {
      setMessage('压力危险阈值必须低于预警阈值', true);
      return;
    }
    if (!(next.dangerTimeMin < next.warnTimeMin)) {
      setMessage('时间危险阈值必须低于预警阈值', true);
      return;
    }
    settings = saveScbaSettings(next);
    if (!canWriteServer) {
      setMessage('本机参数已保存');
      return;
    }
    save.disabled = true;
    try {
      await saveScbaSettingsRemote(stationId, {
        warnPressure: next.warnPressure,
        dangerPressure: next.dangerPressure,
        warnTimeMin: next.warnTimeMin,
        dangerTimeMin: next.dangerTimeMin,
        defaultWorkLevel: next.defaultWorkLevel,
        voiceEnabled: next.voiceEnabled
      });
      setMessage('阈值已保存并下发到共享页与指挥端');
    } catch (err) {
      setMessage(err.message || '保存失败', true);
    } finally {
      save.disabled = false;
    }
  });
  reset.addEventListener('click', () => {
    settings = resetScbaSettings();
    warnPressure.input.value = settings.warnPressure;
    dangerPressure.input.value = settings.dangerPressure;
    warnTime.input.value = settings.warnTimeMin;
    dangerTime.input.value = settings.dangerTimeMin;
    workSelect.value = settings.defaultWorkLevel;
    voiceInput.checked = settings.voiceEnabled;
    setMessage('已恢复默认参数');
  });
}
