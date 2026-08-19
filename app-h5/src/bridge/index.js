const UNSUPPORTED = 'UNSUPPORTED';
const MOCK_TAG = 'MOCK-NFC-0001';

function resolveNativeBridge() {
  if (window.CcdsNativeBridge && typeof window.CcdsNativeBridge.invoke === 'function') {
    return window.CcdsNativeBridge;
  }
  return null;
}

function wrap(ok, data, errorCode) {
  return { ok, data, errorCode };
}

async function invoke(method, payload) {
  const native = resolveNativeBridge();
  if (native) {
    return native.invoke(method, payload || {});
  }
  return mockInvoke(method, payload);
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
    default:
      return wrap(false, null, UNSUPPORTED);
  }
}

export const bridge = {
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
  }
};
