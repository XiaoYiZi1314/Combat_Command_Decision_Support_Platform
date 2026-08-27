import '../water/water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { el } from '../water/shared.js';
import { bridge } from '../../bridge/index.js';

export function renderVersionPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', '版本信息'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(me);
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  body.appendChild(el('div', 'supply-hint', '作战指挥辅助决策平台 · 一期'));
  const name = el('div', 'hq-card');
  name.appendChild(el('div', 'name', '包版本'));
  const meta = el('div', 'hq-meta', '读取中…');
  name.appendChild(meta);
  body.appendChild(name);
  page.appendChild(body);
  root.appendChild(page);

  bridge.getVersion().then((result) => {
    if (!result || !result.ok || !result.data) {
      meta.textContent = 'H5 开发包';
      return;
    }
    const v = result.data.versionName || 'unknown';
    const code = result.data.versionCode != null ? result.data.versionCode : '';
    meta.textContent = code === '' ? v : `${v} (${code})`;
  }).catch(() => {
    meta.textContent = 'H5 开发包';
  });
}
