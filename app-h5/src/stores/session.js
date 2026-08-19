const ACCESS_KEY = 'ccds_access_token';
const REFRESH_KEY = 'ccds_refresh_token';
const ME_KEY = 'ccds_me';
const LEGAL_KEY = 'ccds_legal_notice_agreed';

function readJson(key) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  } catch (err) {
    return null;
  }
}

function writeJson(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch (err) {
    /* 本机存储满时忽略，会话仍在内存 */
  }
}

let memory = {
  accessToken: localStorage.getItem(ACCESS_KEY) || '',
  refreshToken: localStorage.getItem(REFRESH_KEY) || '',
  me: readJson(ME_KEY)
};

export function getAccessToken() {
  return memory.accessToken || '';
}

export function getRefreshToken() {
  return memory.refreshToken || '';
}

export function getMe() {
  return memory.me;
}

export function saveSession(login) {
  memory.accessToken = login.accessToken || '';
  memory.refreshToken = login.refreshToken || '';
  memory.me = login.me || null;
  try {
    localStorage.setItem(ACCESS_KEY, memory.accessToken);
    localStorage.setItem(REFRESH_KEY, memory.refreshToken);
  } catch (err) {
    /* 忽略 */
  }
  if (memory.me) {
    writeJson(ME_KEY, memory.me);
  }
}

export function clearSession() {
  memory = { accessToken: '', refreshToken: '', me: null };
  try {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(ME_KEY);
  } catch (err) {
    /* 忽略 */
  }
}

export function hasSession() {
  return Boolean(memory.accessToken);
}

export function isLegalAgreed() {
  try {
    return localStorage.getItem(LEGAL_KEY) === '1';
  } catch (err) {
    return false;
  }
}

export function agreeLegal() {
  try {
    localStorage.setItem(LEGAL_KEY, '1');
  } catch (err) {
    /* 忽略 */
  }
}

export function homeHashOf(me) {
  if (me && me.homeHash) {
    return me.homeHash;
  }
  if (me && (me.role === 'hq' || me.role === 'brigade' || me.role === 'developer')) {
    return '#/hq';
  }
  return '#/attack';
}
