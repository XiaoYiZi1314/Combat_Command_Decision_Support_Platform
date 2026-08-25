import '../water/water.css';
import { bridge } from '../../bridge/index.js';

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

export function renderSupplyMapPage(root) {
  root.innerHTML = '';
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', '选火灾地点'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = '#/supply';
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  const msg = el('div', 'water-msg');
  body.appendChild(msg);

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
    lngInput.value = String(located.data.lng);
    latInput.value = String(located.data.lat);
    msg.className = 'water-msg';
    msg.textContent = '已填入当前位置';
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
}
