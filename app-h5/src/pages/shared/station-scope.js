import { getMe } from '../../stores/session.js';
import { el } from '../water/shared.js';

/**
 * 站点选择器共享逻辑：站级锁定本站，指挥级从可见站中选择并持久化到 localStorage。
 * attack-history 与 vehicles 页面共用，仅存储 key 不同。
 */

/**
 * 读取当前生效的站点 ID：站级为本站，指挥级为上次选择（需仍在可见站中）或首个可见站。
 *
 * @param {string} storageKey localStorage 存储键
 * @returns {string} 站点 ID，无可用站时为空串
 */
export function currentStationId(storageKey) {
  const me = getMe() || {};
  if (me.role === 'station' && me.stationId) {
    return String(me.stationId);
  }
  const saved = localStorage.getItem(storageKey);
  if (saved && visibleStations().some((item) => String(item.id) === saved)) {
    return saved;
  }
  const first = visibleStations()[0];
  return first ? String(first.id) : '';
}

/**
 * 持久化指挥级选择的站点 ID。
 *
 * @param {string} storageKey localStorage 存储键
 * @param {string} id 站点 ID
 */
export function setStationId(storageKey, id) {
  localStorage.setItem(storageKey, String(id));
}

function visibleStations() {
  const me = getMe() || {};
  return Array.isArray(me.visibleStations) ? me.visibleStations : [];
}

/**
 * 构建站点切换选择器（仅指挥级渲染；站级返回 null 由页面自行隐藏）。
 *
 * @param {string} storageKey localStorage 存储键
 * @param {Function} onChange 切换站点后的回调
 * @returns {HTMLElement|null} 选择器容器，非指挥级时为 null
 */
export function buildStationSelect(storageKey, onChange) {
  const me = getMe() || {};
  const stations = visibleStations();
  if (me.role === 'station' || !stations.length) {
    return null;
  }
  const wrap = el('div', 'water-station');
  wrap.appendChild(el('label', '', '查看单位'));
  const select = el('select');
  if (me.role === 'hq' || me.role === 'developer') {
    select.appendChild(el('option', '', '全部单位')).value = '';
  }
  stations.forEach((station) => {
    const opt = document.createElement('option');
    opt.value = String(station.id);
    opt.textContent = station.name;
    select.appendChild(opt);
  });
  select.value = currentStationId(storageKey);
  select.addEventListener('change', () => {
    setStationId(storageKey, select.value);
    onChange();
  });
  wrap.appendChild(select);
  return wrap;
}
