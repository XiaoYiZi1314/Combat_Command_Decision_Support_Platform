import '../water/water.css';
import { bridge } from '../../bridge/index.js';
import { el } from '../water/shared.js';
import { fetchBaiduMapAk } from '../../api/water.js';
import { loadBaiduMap, bd09ToWgs84Coord, wgs84ToBd09 } from '../../maps/baidu.js';

let mapInstance = null;
let selectedMarker = null;
let selectedCoords = null;

export function renderSupplyMapPage(root) {
  root.innerHTML = '';
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', '选火灾地点'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    if (mapInstance) {
      mapInstance = null;
    }
    window.location.hash = '#/supply';
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  const msg = el('div', 'water-msg');
  body.appendChild(msg);
  
  // 地图容器
  const mapContainer = el('div', 'supply-map-container');
  mapContainer.style.width = '100%';
  mapContainer.style.height = '60vh';
  mapContainer.style.marginBottom = '1rem';
  mapContainer.style.borderRadius = '8px';
  mapContainer.style.overflow = 'hidden';
  mapContainer.style.backgroundColor = '#f5f5f5';
  body.appendChild(mapContainer);

  const form = el('div', 'water-form');
  const lngInput = document.createElement('input');
  lngInput.type = 'number';
  lngInput.step = '0.000001';
  lngInput.placeholder = '经度（WGS84）';
  const latInput = document.createElement('input');
  latInput.type = 'number';
  latInput.step = '0.000001';
  latInput.placeholder = '纬度（WGS84）';
  const addressInput = document.createElement('input');
  addressInput.type = 'text';
  addressInput.placeholder = '地点描述（可选）';

  const rows = [
    ['经度', lngInput],
    ['纬度', latInput],
    ['地点描述', addressInput]
  ];
  rows.forEach(([label, input]) => {
    const row = el('div', 'wf-row');
    row.appendChild(el('label', '', label));
    row.appendChild(input);
    form.appendChild(row);
  });

  const locateBtn = el('button', '', '取当前位置');
  locateBtn.type = 'button';
  locateBtn.addEventListener('click', async () => {
    msg.className = 'water-msg';
    msg.textContent = '正在定位…';
    const located = await bridge.locate();
    if (!located || !located.ok || !located.data || !(located.data.lng || located.data.lat)) {
      msg.className = 'water-msg error';
      msg.textContent = '无法获取当前位置，请开启定位权限';
      return;
    }
    const lng = Number(located.data.lng);
    const lat = Number(located.data.lat);
    lngInput.value = String(lng);
    latInput.value = String(lat);
    selectedCoords = { lng, lat };
    msg.className = 'water-msg';
    msg.textContent = '已填入当前位置';
    
    // 在地图上标注
    if (mapInstance) {
      updateMapMarker(lng, lat);
    }
  });
  form.appendChild(locateBtn);

  const confirmBtn = el('button', 'wf-save', '确认火点');
  confirmBtn.type = 'button';
  confirmBtn.addEventListener('click', () => {
    const lng = Number(lngInput.value);
    const lat = Number(latInput.value);
    if (!Number.isFinite(lng) || !Number.isFinite(lat) || !lng || !lat) {
      msg.className = 'water-msg error';
      msg.textContent = '请填写经纬度或取当前位置';
      return;
    }
    const address = encodeURIComponent(addressInput.value.trim() || '地图点选位置');
    window.location.hash = `#/supply?lng=${lng}&lat=${lat}&address=${address}`;
  });
  form.appendChild(confirmBtn);
  body.appendChild(form);
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
      
      // 默认居中（如果有定位则用定位）
      const located = await bridge.locate();
      let centerPoint;
      if (located && located.ok && located.data && located.data.lng && located.data.lat) {
        const [bdLng, bdLat] = wgs84ToBd09(
          Number(located.data.lng), 
          Number(located.data.lat)
        );
        centerPoint = new BMap.Point(bdLng, bdLat);
        mapInstance.centerAndZoom(centerPoint, 15);
      } else {
        // 默认北京
        centerPoint = new BMap.Point(116.404, 39.915);
        mapInstance.centerAndZoom(centerPoint, 12);
      }
      
      // 点击地图选点
      mapInstance.addEventListener('click', (e) => {
        const bdLng = e.point.lng;
        const bdLat = e.point.lat;
        
        // 转换回 WGS84
        const [wgsLng, wgsLat] = bd09ToWgs84Coord(bdLng, bdLat);
        
        lngInput.value = wgsLng.toFixed(6);
        latInput.value = wgsLat.toFixed(6);
        selectedCoords = { lng: wgsLng, lat: wgsLat };
        
        // 更新地图标记
        if (selectedMarker) {
          mapInstance.removeOverlay(selectedMarker);
        }
        selectedMarker = new BMap.Marker(e.point);
        mapInstance.addOverlay(selectedMarker);
        
        setMsg('已选择火点位置，可调整或确认');
      });
      
      setMsg('点击地图选择火灾位置，或手动输入经纬度');
      return BMap;
    } catch (err) {
      console.warn('Map init failed:', err);
      setMsg('地图加载失败，请手动输入经纬度', true);
      mapContainer.style.display = 'none';
      return null;
    }
  }

  function updateMapMarker(wgsLng, wgsLat) {
    if (!mapInstance) return;
    
    const [bdLng, bdLat] = wgs84ToBd09(wgsLng, wgsLat);
    const point = new BMap.Point(bdLng, bdLat);
    
    if (selectedMarker) {
      mapInstance.removeOverlay(selectedMarker);
    }
    selectedMarker = new BMap.Marker(point);
    mapInstance.addOverlay(selectedMarker);
    mapInstance.centerAndZoom(point, 15);
  }

  // 初始化地图
  initMap();
}
