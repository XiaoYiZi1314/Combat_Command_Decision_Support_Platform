const UNSUPPORTED = 'UNSUPPORTED';
const BAD_RESPONSE = 'BAD_RESPONSE';
const MOCK_TAG = 'MOCK-NFC-0001';

const caps = {
  nfc: false,
  locate: false,
  heading: false,
  speak: false,
  openUrl: false
};

function wrap(ok, data, errorCode) {
  return { ok, data, errorCode: errorCode || '' };
}

function parseJson(raw) {
  if (raw && typeof raw === 'object') {
    return raw;
  }
  if (typeof raw !== 'string' || !raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch (err) {
    return null;
  }
}

function applyCaps(next) {
  const src = next && typeof next === 'object' ? next : {};
  caps.nfc = Boolean(src.nfc);
  caps.locate = Boolean(src.locate);
  caps.heading = Boolean(src.heading);
  caps.speak = Boolean(src.speak);
  caps.openUrl = Boolean(src.openUrl);
  return caps;
}

function resolveNativeBridge() {
  if (window.CcdsNativeBridge && typeof window.CcdsNativeBridge.invoke === 'function') {
    return window.CcdsNativeBridge;
  }
  return null;
}

async function invoke(method, payload) {
  const native = resolveNativeBridge();
  if (!native) {
    return mockInvoke(method, payload);
  }
  try {
    const body = JSON.stringify(payload || {});
    const result = native.invoke(method, body);
    const resolved = result && typeof result.then === 'function' ? await result : result;
    const parsed = parseJson(resolved);
    if (!parsed || typeof parsed.ok !== 'boolean') {
      return wrap(false, null, BAD_RESPONSE);
    }
    return parsed;
  } catch (err) {
    return wrap(false, null, UNSUPPORTED);
  }
}

async function mockInvoke(method, payload) {
  switch (method) {
    case 'nfcRead':
      return wrap(true, { tag: MOCK_TAG, source: 'mock' }, '');
    case 'heading':
      return wrap(true, { degrees: 0, source: 'mock' }, '');
    case 'locate':
      return wrap(true, { lng: 0, lat: 0, source: 'mock' }, '');
    case 'speak':
      return wrap(true, { spoken: Boolean(payload && payload.text), source: 'mock' }, '');
    case 'openUrl':
      if (payload && payload.url) {
        window.open(payload.url, '_blank', 'noopener');
        return wrap(true, { opened: true, source: 'mock' }, '');
      }
      return wrap(false, null, 'INVALID_URL');
    case 'getVersion':
      return wrap(true, { versionName: 'h5-dev', versionCode: 0, source: 'mock' }, '');
    case 'getCapabilities':
      return wrap(true, {
        nfc: false,
        locate: false,
        heading: false,
        speak: false,
        openUrl: false,
        source: 'mock'
      }, '');
    case 'getDeviceCompatInfo':
      return wrap(true, { huaweiHarmony: false, source: 'mock' }, '');
    default:
      return wrap(false, null, UNSUPPORTED);
  }
}

function readNativeCaps() {
  const native = resolveNativeBridge();
  if (!native) {
    return applyCaps({});
  }
  if (typeof native.capabilities === 'function') {
    const parsed = parseJson(native.capabilities());
    if (parsed) {
      return applyCaps(parsed);
    }
  }
  return caps;
}

export function apiBase() {
  const injected = window.CCDS_API_BASE;
  if (typeof injected === 'string' && injected) {
    return injected.replace(/\/$/, '');
  }
  return '';
}

export async function probeBridge() {
  const native = resolveNativeBridge();
  if (!native) {
    applyCaps({});
    return caps;
  }
  readNativeCaps();
  const result = await invoke('getCapabilities');
  if (result.ok && result.data) {
    applyCaps(result.data);
  }
  return caps;
}

export const bridge = {
  hasNfc() {
    if (!resolveNativeBridge()) {
      return false;
    }
    readNativeCaps();
    return Boolean(caps.nfc);
  },
  capabilities() {
    readNativeCaps();
    return Object.assign({}, caps);
  },
  nfcRead() {
    return invoke('nfcRead');
  },
  heading() {
    return invoke('heading');
  },
  locate() {
    return invoke('locate');
  },
  speak(text) {
    return invoke('speak', { text });
  },
  openUrl(url) {
    return invoke('openUrl', { url });
  },
  getVersion() {
    return invoke('getVersion');
  },
  getDeviceCompatInfo() {
    return invoke('getDeviceCompatInfo');
  }
};
