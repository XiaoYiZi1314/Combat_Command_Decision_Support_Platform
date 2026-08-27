const INTERACT_MS = 420;
const SCROLL_HOLD_MS = 700;
const SCROLL_FINISH_MS = 180;
const MOVE_PX = 10;

const info = {
  huaweiHarmony: false,
  brand: '',
  manufacturer: '',
  model: '',
  display: '',
  sdk: 0
};

let interactUntil = 0;
let scrollActive = false;
let scrollLastAt = 0;
let scrollTimer = 0;
let touchStartX = 0;
let touchStartY = 0;
let touchMoved = false;
let guardBound = false;

function parseCompat(raw) {
  if (!raw || typeof raw !== 'object') {
    return {};
  }
  return raw;
}

function detectFromUa(nativeInfo) {
  const ua = String((navigator && navigator.userAgent) || '');
  const merged = Object.assign({}, nativeInfo || {});
  const text = `${ua} ${merged.brand || ''} ${merged.manufacturer || ''} ${merged.model || ''} ${merged.display || ''}`.toLowerCase();
  merged.userAgent = ua;
  merged.huaweiHarmony = Boolean(merged.huaweiHarmony || /huawei|honor|harmony|hmos/.test(text));
  return merged;
}

function applyInfo(next) {
  Object.assign(info, next || {});
  if (!info.huaweiHarmony) {
    return;
  }
  document.documentElement.classList.add('huawei-compat', 'harmony-compat');
}

export function isHuaweiHarmonyRuntime() {
  const html = document.documentElement;
  return Boolean(info.huaweiHarmony)
    || (html && (html.classList.contains('huawei-compat') || html.classList.contains('harmony-compat')));
}

export function markUiInteraction() {
  interactUntil = Date.now() + INTERACT_MS;
}

function finishScroll() {
  if (!isHuaweiHarmonyRuntime()) {
    return;
  }
  if (Date.now() - scrollLastAt < 160) {
    window.clearTimeout(scrollTimer);
    scrollTimer = window.setTimeout(finishScroll, SCROLL_FINISH_MS);
    return;
  }
  scrollActive = false;
  scrollTimer = 0;
}

function markScroll() {
  if (!isHuaweiHarmonyRuntime()) {
    return;
  }
  scrollActive = true;
  scrollLastAt = Date.now();
  window.clearTimeout(scrollTimer);
  scrollTimer = window.setTimeout(finishScroll, SCROLL_FINISH_MS);
}

export function shouldDeferHuaweiTimerRefresh() {
  return Boolean(isHuaweiHarmonyRuntime() && scrollActive && Date.now() - scrollLastAt < SCROLL_HOLD_MS);
}

export function shouldDeferBackgroundPaint() {
  return Date.now() < interactUntil || shouldDeferHuaweiTimerRefresh();
}

function bindGuard() {
  if (guardBound) {
    return;
  }
  guardBound = true;
  const opts = { passive: true, capture: true };
  document.addEventListener('touchstart', (event) => {
    const t = event && event.touches && event.touches[0];
    touchStartX = t ? t.clientX : 0;
    touchStartY = t ? t.clientY : 0;
    touchMoved = false;
    markUiInteraction();
  }, opts);
  document.addEventListener('touchmove', (event) => {
    if (!isHuaweiHarmonyRuntime()) {
      return;
    }
    const t = event && event.touches && event.touches[0];
    if (!t) {
      return;
    }
    const dx = Math.abs(t.clientX - touchStartX);
    const dy = Math.abs(t.clientY - touchStartY);
    if (dx > MOVE_PX || dy > MOVE_PX) {
      touchMoved = true;
      markScroll();
    }
  }, opts);
  document.addEventListener('scroll', markScroll, opts);
  document.addEventListener('wheel', markScroll, opts);
  document.addEventListener('pointerdown', () => {
    markUiInteraction();
  }, opts);
  document.addEventListener('click', () => {
    markUiInteraction();
  }, opts);
  document.addEventListener('touchend', () => {
    if (!isHuaweiHarmonyRuntime() || !touchMoved) {
      return;
    }
    window.clearTimeout(scrollTimer);
    scrollTimer = window.setTimeout(finishScroll, 140);
  }, opts);
  document.addEventListener('touchcancel', () => {
    if (!isHuaweiHarmonyRuntime()) {
      return;
    }
    finishScroll();
  }, opts);
}

export async function initDeviceCompat(bridge) {
  bindGuard();
  let nativeInfo = {};
  if (bridge && typeof bridge.getDeviceCompatInfo === 'function') {
    const result = await bridge.getDeviceCompatInfo();
    if (result && result.ok && result.data) {
      nativeInfo = parseCompat(result.data);
    }
  }
  applyInfo(detectFromUa(nativeInfo));
  return info;
}
