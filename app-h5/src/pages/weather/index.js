import '../water/water.css';
import './weather.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { fetchWeather } from '../../api/assist.js';
import { el } from '../water/shared.js';
import { bridge } from '../../bridge/index.js';

const DIRS = ['北', '东北', '东', '东南', '南', '西南', '西', '西北'];
const HINT = '用于判断烟气方向，不替代气象台。';

function dirOf(deg) {
  const n = Number(deg);
  if (!Number.isFinite(n)) {
    return '--';
  }
  return DIRS[Math.round(((n % 360) + 360) % 360 / 45) % 8];
}

function windScale(speed) {
  const v = Number(speed);
  if (!Number.isFinite(v)) {
    return '--';
  }
  if (v < 0.3) {
    return '0 级';
  }
  if (v < 1.6) {
    return '1 级';
  }
  if (v < 3.4) {
    return '2 级';
  }
  if (v < 5.5) {
    return '3 级';
  }
  if (v < 8.0) {
    return '4 级';
  }
  if (v < 10.8) {
    return '5 级';
  }
  if (v < 13.9) {
    return '6 级';
  }
  if (v < 17.2) {
    return '7 级';
  }
  return '8 级以上';
}

function fmtClock(value) {
  if (!value) {
    return '--';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    const text = String(value);
    const idx = text.indexOf('T');
    return idx > 0 ? text.slice(idx + 1, idx + 6) : text;
  }
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${hh}:${mm}`;
}

function drawCompass(canvas, heading) {
  if (!canvas) {
    return;
  }
  const ctx = canvas.getContext('2d');
  const w = canvas.width;
  const h = canvas.height;
  const cx = w / 2;
  const cy = h / 2;
  const r = Math.min(cx, cy) - 8;
  ctx.clearRect(0, 0, w, h);
  ctx.save();
  ctx.translate(cx, cy);
  ctx.rotate(-heading * Math.PI / 180);
  ctx.beginPath();
  ctx.arc(0, 0, r, 0, 2 * Math.PI);
  ctx.fillStyle = 'rgba(22,27,34,.9)';
  ctx.fill();
  ctx.strokeStyle = 'rgba(74,158,255,.4)';
  ctx.lineWidth = 2;
  ctx.stroke();
  for (let i = 0; i < 360; i += 10) {
    const rad = i * Math.PI / 180;
    const len = i % 30 === 0 ? 12 : 6;
    ctx.beginPath();
    ctx.moveTo(Math.sin(rad) * (r - len), -Math.cos(rad) * (r - len));
    ctx.lineTo(Math.sin(rad) * (r - 2), -Math.cos(rad) * (r - 2));
    ctx.strokeStyle = i === 0 ? '#f85149' : 'rgba(255,255,255,.3)';
    ctx.lineWidth = i % 90 === 0 ? 2 : 1;
    ctx.stroke();
  }
  const labels = [
    { t: 'N', a: 0, c: '#f85149' },
    { t: 'E', a: 90, c: '#fff' },
    { t: 'S', a: 180, c: '#fff' },
    { t: 'W', a: 270, c: '#fff' }
  ];
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.font = 'bold 16px sans-serif';
  labels.forEach((item) => {
    const rad = item.a * Math.PI / 180;
    ctx.fillStyle = item.c;
    ctx.fillText(item.t, Math.sin(rad) * (r - 24), -Math.cos(rad) * (r - 24));
  });
  ctx.beginPath();
  ctx.moveTo(0, -(r - 36));
  ctx.lineTo(-6, 0);
  ctx.lineTo(0, 10);
  ctx.lineTo(6, 0);
  ctx.closePath();
  ctx.fillStyle = '#f85149';
  ctx.fill();
  ctx.restore();
  ctx.beginPath();
  ctx.arc(cx, cy, 4, 0, 2 * Math.PI);
  ctx.fillStyle = '#d4af37';
  ctx.fill();
}

export function renderWeatherPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'water-page weather-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', '天气与方位'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(me);
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  body.appendChild(el('div', 'supply-hint', HINT));

  const compassWrap = el('div', 'weather-compass');
  const canvas = document.createElement('canvas');
  canvas.width = 200;
  canvas.height = 200;
  canvas.className = 'weather-canvas';
  const headingText = el('div', 'weather-heading', '--°');
  const dirText = el('div', 'weather-dir', '等待启动');
  const startBtn = el('button', 'water-back', '启动罗盘');
  startBtn.type = 'button';
  compassWrap.appendChild(canvas);
  compassWrap.appendChild(headingText);
  compassWrap.appendChild(dirText);
  compassWrap.appendChild(startBtn);
  body.appendChild(compassWrap);

  const wind = el('div', 'weather-grid');
  const windSpeed = el('div', 'weather-cell');
  const windScaleEl = el('div', 'weather-cell');
  const windDir = el('div', 'weather-cell');
  windSpeed.appendChild(el('span', '', '风速'));
  const windSpeedVal = el('b', '', '--');
  windSpeed.appendChild(windSpeedVal);
  windScaleEl.appendChild(el('span', '', '风力等级'));
  const windScaleVal = el('b', '', '--');
  windScaleEl.appendChild(windScaleVal);
  windDir.appendChild(el('span', '', '风向'));
  const windDirVal = el('b', '', '--');
  windDir.appendChild(windDirVal);
  wind.appendChild(windSpeed);
  wind.appendChild(windScaleEl);
  wind.appendChild(windDir);
  body.appendChild(wind);

  const loc = el('div', 'weather-loc', '自动定位');
  const temp = el('div', 'weather-temp', '--°C');
  const desc = el('div', 'weather-desc', '暂无天气数据');
  body.appendChild(loc);
  body.appendChild(temp);
  body.appendChild(desc);

  const detail = el('div', 'weather-grid');
  function cell(label) {
    const box = el('div', 'weather-cell');
    box.appendChild(el('span', '', label));
    const val = el('b', '', '--');
    box.appendChild(val);
    detail.appendChild(box);
    return val;
  }
  const humid = cell('湿度');
  const pressure = cell('气压');
  const sunrise = cell('日出');
  const sunset = cell('日落');
  body.appendChild(detail);

  const hours = el('div', 'weather-hours');
  body.appendChild(hours);

  const refresh = el('button', 'water-back', '刷新天气');
  refresh.type = 'button';
  body.appendChild(refresh);
  page.appendChild(body);
  root.appendChild(page);

  let headingTimer = 0;
  let closed = false;
  drawCompass(canvas, 0);

  async function tickHeading() {
    if (closed) {
      return;
    }
    const result = await bridge.heading();
    if (!result || !result.ok || !result.data || result.data.degrees == null) {
      dirText.textContent = '无传感器，请用手选方位';
      return;
    }
    const deg = Math.round((((Number(result.data.degrees) % 360) + 360) % 360));
    headingText.textContent = `${deg}°`;
    dirText.textContent = `朝向${dirOf(deg)}`;
    drawCompass(canvas, deg);
  }

  function startCompass() {
    window.clearInterval(headingTimer);
    dirText.textContent = '读取中…';
    tickHeading();
    headingTimer = window.setInterval(tickHeading, 800);
  }

  async function loadWeather() {
    desc.textContent = '定位中…';
    const located = await bridge.locate();
    if (!located || !located.ok || !located.data) {
      desc.textContent = '无法定位，天气不可用';
      return;
    }
    const lng = Number(located.data.lng);
    const lat = Number(located.data.lat);
    if (!Number.isFinite(lng) || !Number.isFinite(lat) || (!lng && !lat)) {
      desc.textContent = '无法定位，天气不可用';
      return;
    }
    loc.textContent = '已定位（坐标已脱敏展示）';
    desc.textContent = '加载中…';
    try {
      const data = await fetchWeather(lng, lat);
      temp.textContent = data && data.temperature != null ? `${data.temperature}°C` : '--°C';
      desc.textContent = (data && data.description) || '暂无天气数据';
      windSpeedVal.textContent = data && data.windSpeed != null ? `${data.windSpeed} m/s` : '--';
      windScaleVal.textContent = windScale(data && data.windSpeed);
      windDirVal.textContent = data && data.windDirection ? data.windDirection : dirOf(data && data.windDeg);
      humid.textContent = data && data.humidity != null ? `${data.humidity}%` : '--';
      pressure.textContent = data && data.pressure != null ? `${data.pressure} hPa` : '--';
      sunrise.textContent = fmtClock(data && data.sunrise);
      sunset.textContent = fmtClock(data && data.sunset);
      hours.innerHTML = '';
      ((data && data.hourlyForecast) || []).slice(0, 12).forEach((item) => {
        const card = el('div', 'weather-hour');
        card.appendChild(el('div', '', fmtClock(item.time)));
        card.appendChild(el('b', '', item.temperature != null ? `${item.temperature}°` : '--'));
        card.appendChild(el('div', '', item.description || ''));
        hours.appendChild(card);
      });
    } catch (err) {
      desc.textContent = err.message || '天气服务暂不可用';
    }
  }

  startBtn.addEventListener('click', startCompass);
  refresh.addEventListener('click', loadWeather);
  loadWeather();

  return () => {
    closed = true;
    window.clearInterval(headingTimer);
  };
}
