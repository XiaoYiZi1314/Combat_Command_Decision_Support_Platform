import './water.css';
import { bridge } from '../../bridge/index.js';
import { fetchNearbyWaters, fetchWaters } from '../../api/water.js';
import { currentWaterStationId } from './index.js';
import {
  TYPE_OPTIONS,
  buildBaiduNavUrl,
  distanceMeters,
  formatDistance
} from './shared.js';

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

function typeLabel(code) {
  const hit = TYPE_OPTIONS.find((item) => item.value === code);
  return hit ? hit.label : code;
}

export function renderWaterMapPage(root, params, hash) {
  root.innerHTML = '';
  const wantNearby = new URLSearchParams((hash || '').split('?')[1] || '').get('nearby') === '1';
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', wantNearby ? '最近水源' : '水源地图'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = '#/water';
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  const msg = el('div', 'water-msg');
  const list = el('div', 'water-list');
  const locateBtn = el('button', 'water-locate-btn', wantNearby ? '重新定位查找' : '按当前位置查找附近水源');
  locateBtn.type = 'button';
  locateBtn.addEventListener('click', () => {
    locateAndLoad();
  });
  body.appendChild(locateBtn);
  body.appendChild(msg);
  body.appendChild(list);
  page.appendChild(body);
  root.appendChild(page);

  function setMsg(text, error) {
    msg.className = error ? 'water-msg error' : 'water-msg';
    msg.textContent = text || '';
  }

  function renderNearby(result, cur) {
    list.innerHTML = '';
    const waters = (result && result.waters) || [];
    list.appendChild(el('div', 'water-summary',
      `半径${formatDistance(result && result.radiusM)}内 ${waters.length} 处在用水源`));
    if (!waters.length) {
      list.appendChild(el('div', 'water-empty', '附近没有在用水源'));
      return;
    }
    waters.forEach((water) => {
      const card = el('div', 'water-card');
      const nameRow = el('div', 'wc-name');
      nameRow.appendChild(el('b', '', water.name || '未命名'));
      nameRow.appendChild(el('span', 'wc-dist', formatDistance(water.distanceM)));
      card.appendChild(nameRow);
      card.appendChild(el('div', 'wc-info', `${typeLabel(water.type)} · 估算流量 ${water.estimateFlow || '--'} L/s`));
      if (water.address) {
        card.appendChild(el('div', 'wc-addr', water.address));
      }
      const nav = el('button', 'wc-nav', '百度导航');
      nav.type = 'button';
      nav.addEventListener('click', () => {
        bridge.openUrl(buildBaiduNavUrl(water.lat, water.lng, water.name));
      });
      card.appendChild(nav);
      list.appendChild(card);
    });
    if (cur) {
      setMsg(`已按当前位置检索（${cur.lng.toFixed(5)}, ${cur.lat.toFixed(5)}）`);
    }
  }

  function renderStationWaters(rows) {
    list.innerHTML = '';
    list.appendChild(el('div', 'water-summary', `共 ${rows.length} 处水源`));
    if (!rows.length) {
      list.appendChild(el('div', 'water-empty', '该单位暂无水源档案'));
      return;
    }
    rows.forEach((water) => {
      const card = el('div', `water-card ${water.status === 'repair' ? 'repair' : ''}`.trim());
      const nameRow = el('div', 'wc-name');
      nameRow.appendChild(el('b', '', water.name || '未命名'));
      nameRow.appendChild(el('span', `wc-status ${water.status}`,
        water.status === 'repair' ? '报修' : '在用'));
      card.appendChild(nameRow);
      card.appendChild(el('div', 'wc-info', typeLabel(water.type)));
      if (water.address) {
        card.appendChild(el('div', 'wc-addr', water.address));
      }
      if (water.lng != null && water.lat != null) {
        const nav = el('button', 'wc-nav', '导航');
        nav.type = 'button';
        nav.addEventListener('click', () => {
          bridge.openUrl(buildBaiduNavUrl(water.lat, water.lng, water.name));
        });
        card.appendChild(nav);
      }
      list.appendChild(card);
    });
  }

  async function locateAndLoad() {
    setMsg('正在定位…');
    const located = await bridge.locate();
    if (!located || !located.ok || !located.data || !(located.data.lng || located.data.lat)) {
      setMsg('无法获取当前位置，请开启定位权限', true);
      return;
    }
    const cur = { lng: Number(located.data.lng), lat: Number(located.data.lat) };
    try {
      const result = await fetchNearbyWaters(cur.lng, cur.lat, 3000);
      renderNearby(result, cur);
    } catch (err) {
      setMsg(err.message || '附近水源查询失败', true);
    }
  }

  async function loadDefault() {
    const stationId = currentWaterStationId();
    if (!stationId) {
      list.textContent = '没有可见单位';
      return;
    }
    try {
      const data = await fetchWaters(stationId);
      renderStationWaters(data.waters || []);
    } catch (err) {
      list.textContent = err.message || '加载失败';
    }
  }

  if (wantNearby) {
    locateAndLoad();
  } else {
    loadDefault();
  }
}
