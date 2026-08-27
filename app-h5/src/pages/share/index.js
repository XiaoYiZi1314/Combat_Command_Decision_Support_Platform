import './share.css';
import { createShareToken, revokeShareToken } from '../../api/share.js';
import { bridge } from '../../bridge/index.js';
import { QRCode } from '../../lib/qrcode.js';
import { getMe, homeHashOf } from '../../stores/session.js';

const QR_MIN_VERSION = 1;
const QR_MAX_VERSION = 20;
const QR_SIZE = 260;
const QUIET_ZONE = 4;

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
}

function drawQr(canvas, text) {
  let qr = null;
  for (let version = QR_MIN_VERSION; version <= QR_MAX_VERSION; version += 1) {
    try {
      qr = QRCode.create(version, QRCode.ErrorCorrectLevel.L, text);
      break;
    } catch (err) {
      qr = null;
    }
  }
  if (!qr) throw new Error('共享链接过长，无法生成二维码');
  const count = qr.getModuleCount();
  const cell = Math.max(1, Math.floor(QR_SIZE / (count + QUIET_ZONE * 2)));
  const size = cell * (count + QUIET_ZONE * 2);
  canvas.width = size;
  canvas.height = size;
  const context = canvas.getContext('2d');
  context.fillStyle = '#fff';
  context.fillRect(0, 0, size, size);
  context.fillStyle = '#000';
  for (let row = 0; row < count; row += 1) {
    for (let column = 0; column < count; column += 1) {
      if (qr.isDark(row, column)) {
        context.fillRect((column + QUIET_ZONE) * cell, (row + QUIET_ZONE) * cell, cell, cell);
      }
    }
  }
}

function formatTime(value) {
  if (!value) return '--';
  const normalized = String(value).replace('T', ' ');
  return normalized.slice(0, 19);
}

export function renderSharePage(root) {
  const me = getMe() || {};
  root.innerHTML = '';
  const page = el('div', 'share-page');
  const head = el('div', 'share-head');
  head.appendChild(el('h2', '', '二维码共享实时状态'));
  const back = el('button', 'share-btn', '返回');
  back.type = 'button';
  back.addEventListener('click', () => { window.location.hash = homeHashOf(getMe()); });
  head.appendChild(back);
  page.appendChild(head);
  const body = el('div', 'share-body');
  page.appendChild(body);
  root.appendChild(page);

  if (me.role !== 'station' || !me.stationId) {
    body.appendChild(el('div', 'share-section share-help', '仅站级账号可针对本站当前内攻生成共享二维码。'));
    return;
  }

  const config = el('div', 'share-section');
  config.appendChild(el('div', 'share-title', '启动实时共享'));
  const field = el('div', 'share-field');
  field.appendChild(el('label', '', '共享有效期'));
  const expire = document.createElement('select');
  [[1, '1 小时'], [2, '2 小时（默认）'], [4, '4 小时'], [8, '8 小时'], [24, '24 小时'], [72, '72 小时']].forEach(([value, label]) => {
    const option = el('option', '', label);
    option.value = String(value);
    expire.appendChild(option);
  });
  expire.value = '2';
  field.appendChild(expire);
  config.appendChild(field);
  const generate = el('button', 'share-btn primary share-generate', '启动实时共享二维码');
  generate.type = 'button';
  config.appendChild(generate);
  body.appendChild(config);

  const message = el('div', 'share-msg');
  body.appendChild(message);
  const result = el('div', 'share-section share-result share-hidden');
  const canvas = document.createElement('canvas');
  canvas.className = 'share-canvas';
  result.appendChild(canvas);
  const meta = el('div', 'share-meta');
  const url = el('div', 'share-url');
  result.appendChild(meta);
  result.appendChild(url);
  const actions = el('div', 'share-actions');
  const copy = el('button', 'share-btn', '复制链接');
  const open = el('button', 'share-btn', '打开链接');
  const revoke = el('button', 'share-btn danger', '停止共享');
  [copy, open, revoke].forEach((button) => { button.type = 'button'; actions.appendChild(button); });
  result.appendChild(actions);
  body.appendChild(result);
  body.appendChild(el('div', 'share-section share-help', '微信或其他浏览器扫码后可实时查看本站未撤出人员状态。共享页不包含电话、NFC 号或账号；停止共享或到期后链接立即失效。'));

  let active = null;
  function setMessage(text, error) { message.className = error ? 'share-msg error' : 'share-msg'; message.textContent = text || ''; }
  generate.addEventListener('click', async () => {
    if (active && !window.confirm('重新生成将作废当前共享链接，是否继续？')) return;
    generate.disabled = true;
    setMessage('正在生成共享链接…');
    try {
      if (active) await revokeShareToken(active.tokenId);
      active = await createShareToken(me.stationId, expire.value);
      drawQr(canvas, active.shareUrl);
      meta.textContent = `有效期至 ${formatTime(active.expireAt)}`;
      url.textContent = active.shareUrl;
      result.classList.remove('share-hidden');
      generate.textContent = '更新共享二维码';
      setMessage('共享二维码已生成');
    } catch (err) {
      setMessage(err.message || '生成失败', true);
    } finally {
      generate.disabled = false;
    }
  });
  copy.addEventListener('click', async () => {
    if (!active) return;
    try {
      await navigator.clipboard.writeText(active.shareUrl);
      setMessage('共享链接已复制');
    } catch (err) {
      setMessage('复制失败，请长按链接复制', true);
    }
  });
  open.addEventListener('click', () => { if (active) bridge.openUrl(active.shareUrl); });
  revoke.addEventListener('click', async () => {
    if (!active || !window.confirm('确认停止当前共享？二维码和链接将立即失效。')) return;
    revoke.disabled = true;
    try {
      await revokeShareToken(active.tokenId);
      active = null;
      result.classList.add('share-hidden');
      generate.textContent = '启动实时共享二维码';
      setMessage('共享已停止');
    } catch (err) {
      setMessage(err.message || '停止共享失败', true);
    } finally {
      revoke.disabled = false;
    }
  });
}
