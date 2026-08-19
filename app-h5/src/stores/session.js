const ACCESS_KEY = 'ccds_access_token';
const REFRESH_KEY = 'ccds_refresh_token';
const ME_KEY = 'ccds_me';
const LEGAL_KEY = 'ccds_legal_notice_agreed';
const ORG_KEY = 'ccds_org_tree';

function readJson(key) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  } catch (err) {
    console.warn('session cache parse failed', key, err.name);
    return null;
  }
}

function writeJson(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch (err) {
    console.warn('session cache write failed', key, err.name);
  }
}

function writeText(key, value) {
  try {
    localStorage.setItem(key, value);
  } catch (err) {
    console.warn('session cache write failed', key, err.name);
  }
}

function removeKey(key) {
  try {
    localStorage.removeItem(key);
  } catch (err) {
    console.warn('session cache remove failed', key, err.name);
  }
}

let memory = {
  accessToken: localStorage.getItem(ACCESS_KEY) || '',
  refreshToken: localStorage.getItem(REFRESH_KEY) || '',
  me: readJson(ME_KEY),
  orgTree: readJson(ORG_KEY)
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

export function getOrgTree() {
  return memory.orgTree;
}

export function saveOrgTree(orgTree) {
  memory.orgTree = orgTree || null;
  if (memory.orgTree) {
    writeJson(ORG_KEY, memory.orgTree);
    return;
  }
  removeKey(ORG_KEY);
}

export function saveSession(login) {
  memory.accessToken = login.accessToken || '';
  memory.refreshToken = login.refreshToken || '';
  memory.me = login.me || null;
  writeText(ACCESS_KEY, memory.accessToken);
  writeText(REFRESH_KEY, memory.refreshToken);
  if (memory.me) {
    writeJson(ME_KEY, memory.me);
  }
}

export function clearSession() {
  memory = { accessToken: '', refreshToken: '', me: null, orgTree: null };
  removeKey(ACCESS_KEY);
  removeKey(REFRESH_KEY);
  removeKey(ME_KEY);
  removeKey(ORG_KEY);
}

export function hasSession() {
  return Boolean(memory.accessToken);
}

export function isLegalAgreed() {
  try {
    return localStorage.getItem(LEGAL_KEY) === '1';
  } catch (err) {
    console.warn('legal flag read failed', err.name);
    return false;
  }
}

export function agreeLegal() {
  writeText(LEGAL_KEY, '1');
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
