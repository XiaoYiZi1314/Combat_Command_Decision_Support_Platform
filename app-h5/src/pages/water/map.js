import './water.css';
import { bridge } from '../../bridge/index.js';
import { fetchNearbyWaters, fetchWaters, fetchBaiduMapAk } from '../../api/water.js';
import { currentWaterStationId } from './index.js';
import {
  buildBaiduNavUrl,
  distanceMeters,
  formatDistance,
  el,
  typeLabel
} from './shared.js';
import { loadBaiduMap, wgs84ToBd09 } from '../../maps/baidu.js';

const NEARBY_RADIUS_M = 3000;

let mapInstance = null;
let currentMarkers = [];

export function renderWaterMapPage(root, params, hash) {
  root.innerHTML = '';
  const wantNearby = new URLSearchParams((hash || '').split('?')[1] || '').get('nearby') === '1';
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', wantNearby ? '最近水源' : '水源地图'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    if (mapInstance) {
      mapInstance = null;
    }
    window.location.hash = '#/water';
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  const msg = el('div', 'water-msg');
  
  // 地图容器
  const mapContainer = el('div', 'water-map-container');
  mapContainer.style.width = '100%';
  mapContainer.style.height = '50vh';
  mapContainer.style.marginBottom = '1rem';
  mapContainer.style.borderRadius = '8px';
  mapContainer.style.overflow = 'hidden';
  mapContainer.style.backgroundColor = '#f5f5f5';
  
  const list = el('div', 'water-list');
  const locateBtn = el('button', 'water-locate-btn', wantNearby ? '重新定位查找' : '按当前位置查找附近水源');
  locateBtn.type = 'button';
  locateBtn.addEventListener('click', () => {
    locateAndLoad();
  });
  body.appendChild(locateBtn);
  body.appendChild(msg);
  body.appendChild(mapContainer);
  body.appendChild(list);
  page.appendChild(body);
  root.appendChild(page);

  function setMsg(text, error) {
    msg.className = error ? 'water-msg error' : 'water-msg';
    msg.textContent = text || '';
  }

  async function initMap() {
    try {
      const akResult = await fetchBaiduMapAk();
      const BMap = await loadBaiduMap(akResult.ak);
      
      mapInstance = new BMap.Map(mapContainer);
      mapInstance.enableScrollWheelZoom(true);
      mapInstance.addControl(new BMap.NavigationControl());
      mapInstance.addControl(new BMap.ScaleControl());
      
      return BMap;
    } catch (err) {
      console.warn('Map init failed:', err);
      setMsg('地图加载失败，仅显示列表模式', true);
      mapContainer.style.display = 'none';
      return null;
    }
  }

  function clearMarkers() {
    if (mapInstance && currentMarkers.length > 0) {
      currentMarkers.forEach(marker => mapInstance.removeOverlay(marker));
      currentMarkers = [];
    }
  }

  function createInfoLine(text, emphasized) {
    const line = document.createElement('div');
    line.textContent = text;
    line.style.color = emphasized ? '#999' : '#666';
    line.style.fontSize = '12px';
    line.style.marginBottom = emphasized ? '8px' : '4px';
    return line;
  }

  function createWaterInfoContent(water) {
    const content = document.createElement('div');
    content.style.padding = '8px';

    const name = document.createElement('div');
    name.style.fontWeight = 'bold';
    name.style.marginBottom = '4px';
    name.textContent = water.name || '未命名';
    content.appendChild(name);
    content.appendChild(createInfoLine(typeLabel(water.type), false));

    if (water.stationName) {
      content.appendChild(createInfoLine(`所属: ${water.stationName}`, false));
    }
    if (water.address) {
      content.appendChild(createInfoLine(water.address, false));
    }
    if (water.distanceM != null) {
      content.appendChild(createInfoLine(`距离: ${formatDistance(water.distanceM)}`, false));
    }
    if (water.estimateFlow != null) {
      content.appendChild(createInfoLine(`估算流量: ${water.estimateFlow} L/s`, true));
    }

    const nav = document.createElement('button');
    nav.type = 'button';
    nav.textContent = '百度导航';
    nav.style.padding = '6px 12px';
    nav.style.background = '#1a73e8';
    nav.style.color = 'white';
    nav.style.border = 'none';
    nav.style.borderRadius = '4px';
    nav.style.cursor = 'pointer';
    nav.addEventListener('click', () => {
      bridge.openUrl(buildBaiduNavUrl(water.lat, water.lng, water.name));
    });
    content.appendChild(nav);
    return content;
  }

  function addWaterMarkers(waters, BMap) {
    clearMarkers();
    const bounds = [];

    waters.forEach((water) => {
      if (!water.lng || !water.lat) {
        return;
      }

      const [bdLng, bdLat] = wgs84ToBd09(water.lng, water.lat);
      const point = new BMap.Point(bdLng, bdLat);
      bounds.push(point);

      const marker = new BMap.Marker(point);
      currentMarkers.push(marker);
      mapInstance.addOverlay(marker);

      const infoWindow = new BMap.InfoWindow(createWaterInfoContent(water), { width: 250 });

      marker.addEventListener('click', () => {
        mapInstance.openInfoWindow(infoWindow, point);
      });
    });

    // 设置地图视野
    if (bounds.length === 1) {
      mapInstance.centerAndZoom(bounds[0], 15);
    } else if (bounds.length > 1) {
      mapInstance.setViewport(bounds);
    } else {
      // 无水源，默认居中
      mapInstance.centerAndZoom(new BMap.Point(116.404, 39.915), 12);
    }
  }

  function renderNearby(result, cur, BMap) {
    list.innerHTML = '';
    const waters = (result && result.waters) || [];
    list.appendChild(el('div', 'water-summary',
      `半径${formatDistance(result && result.radiusM)}内 ${waters.length} 处在用水源`));
    
    // 在地图上标注
    if (BMap && mapInstance) {
      addWaterMarkers(waters, BMap);
      // 添加当前位置标记
      if (cur) {
        const [bdLng, bdLat] = wgs84ToBd09(cur.lng, cur.lat);
        const curPoint = new BMap.Point(bdLng, bdLat);
        const curMarker = new BMap.Marker(curPoint, {
          icon: new BMap.Icon('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMTIiIGN5PSIxMiIgcj0iOCIgZmlsbD0iIzRhOTBlMiIgc3Ryb2tlPSJ3aGl0ZSIgc3Ryb2tlLXdpZHRoPSIyIi8+PC9zdmc+',
            new BMap.Size(24, 24))
        });
        mapInstance.addOverlay(curMarker);
        currentMarkers.push(curMarker);
      }
    }
    
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
      const info = `${typeLabel(water.type)} · 估算流量 ${water.estimateFlow || '--'} L/s`;
      card.appendChild(el('div', 'wc-info', water.stationName ? `${water.stationName} · ${info}` : info));
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

  function renderStationWaters(rows, BMap) {
    list.innerHTML = '';
    list.appendChild(el('div', 'water-summary', `共 ${rows.length} 处水源`));
    
    // 在地图上标注
    if (BMap && mapInstance) {
      addWaterMarkers(rows, BMap);
    }
    
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
      const result = await fetchNearbyWaters(cur.lng, cur.lat, NEARBY_RADIUS_M);
      const BMap = await initMap();
      renderNearby(result, cur, BMap);
    } catch (err) {
      setMsg(err.message || '附近水源查询失败', true);
    }
  }

  async function loadDefault() {
    const stationId = currentWaterStationId();
    if (!stationId) {
      list.textContent = '没有可见单位';
      mapContainer.style.display = 'none';
      return;
    }
    try {
      const data = await fetchWaters(stationId);
      const BMap = await initMap();
      renderStationWaters(data.waters || [], BMap);
    } catch (err) {
      list.textContent = err.message || '加载失败';
      mapContainer.style.display = 'none';
    }
  }

  if (wantNearby) {
    locateAndLoad();
  } else {
    loadDefault();
  }
}
