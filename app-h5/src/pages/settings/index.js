import '../hq/hq.css';
import { getMe } from '../../stores/session.js';
import { attackQueueLength } from '../../stores/attack.js';
import { syncStatusText } from '../../lib/sync.js';
import { logout } from '../../api/auth.js';

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  if (text) {
    node.textContent = text;
  }
  return node;
}

export function renderSettingsPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'hq-page');
  const head = el('div', 'hq-head');
  const text = el('div', 'hq-head-text');
  text.appendChild(el('h1', '', '参数设置'));
  text.appendChild(el('div', 'sub', me.stationName || me.brigadeName || '作战指挥辅助决策平台'));
  head.appendChild(text);
  page.appendChild(head);
  const back = el('button', 'hq-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = me.role === 'station' ? '#/attack' : '#/hq';
  });
  page.appendChild(back);
  const list = el('div', 'hq-list');
  const syncCard = el('div', 'hq-card');
  const online = typeof navigator === 'undefined' ? true : navigator.onLine;
  syncCard.appendChild(el('div', 'name', '同步状态'));
  const meta = el('div', 'hq-meta', '读取中…');
  syncCard.appendChild(meta);
  attackQueueLength().then((n) => {
    meta.textContent = syncStatusText(online, n);
  }).catch(() => {
    meta.textContent = syncStatusText(online, 0);
  });
  list.appendChild(syncCard);
  const out = el('button', 'hq-back', '退出登录');
  out.type = 'button';
  out.addEventListener('click', async () => {
    await logout();
    window.location.hash = '#/login';
  });
  list.appendChild(out);
  page.appendChild(list);
  root.appendChild(page);
}
