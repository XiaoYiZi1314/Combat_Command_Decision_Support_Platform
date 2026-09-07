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
      return wrap(true, { lng: 123.95, lat: 47.35, source: 'mock' }, '');
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
    case 'saveFile':
      return wrap(true, { path: 'browser', source: 'mock' }, '');
    case 'pickFile':
      return wrap(false, null, UNSUPPORTED);
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
  },
  /**
   * 保存文件到设备下载目录（Android 壳）；浏览器环境返回 unsupported，
   * 调用方需自行降级到 <a download>。
   *
   * @param {string} fileName 目标文件名
   * @param {Blob} blob 文件内容
   * @returns {Promise<{ok:boolean,data:Object,errorCode:string}>}
   */
  async saveFile(fileName, blob) {
    const base64 = await blobToBase64(blob);
    return invoke('saveFile', { fileName, base64 });
  },
  /**
   * 拉起系统文件选择器（Android 壳）；浏览器环境返回 unsupported，
   * 调用方需自行降级到 <input type="file">。
   *
   * @param {string} mime 可选的 MIME 过滤
   * @returns {Promise<{ok:boolean,data:Object,errorCode:string}>}
   */
  pickFile(mime) {
    return invoke('pickFile', { mime });
  }
};

/**
 * Blob 转 base64（无换行）。
 *
 * @param {Blob} blob
 * @returns {Promise<string>}
 */
function blobToBase64(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const text = String(reader.result || '');
      const comma = text.indexOf(',');
      resolve(comma >= 0 ? text.slice(comma + 1) : text);
    };
    reader.onerror = () => reject(new Error('读取文件内容失败'));
    reader.readAsDataURL(blob);
  });
}
