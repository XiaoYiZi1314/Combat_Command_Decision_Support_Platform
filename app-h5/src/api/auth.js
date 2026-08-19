import { apiRequest } from './client.js';
import { getAccessToken, getRefreshToken, saveSession, clearSession } from '../stores/session.js';

function authHeader() {
  const token = getAccessToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function withRefresh(executor) {
  try {
    return await executor();
  } catch (err) {
    if (err.code !== 'AUTH_UNAUTHORIZED' && err.code !== 'AUTH_REFRESH_INVALID') {
      throw err;
    }
    const refreshed = await refreshSession();
    if (!refreshed) {
      throw err;
    }
    return executor();
  }
}

export async function login(username, password) {
  const data = await apiRequest('/auth/login', {
    method: 'POST',
    body: { username, password, deviceHint: 'h5' }
  });
  saveSession(data);
  return data;
}

export async function changePassword(oldPassword, newPassword) {
  const data = await apiRequest('/auth/password', {
    method: 'POST',
    headers: authHeader(),
    body: { oldPassword, newPassword }
  });
  saveSession(data);
  return data;
}

export async function fetchMe() {
  return withRefresh(() => apiRequest('/me', { headers: authHeader() }));
}

export async function fetchOrgStations() {
  return withRefresh(() => apiRequest('/org/stations', { headers: authHeader() }));
}

export async function logout() {
  try {
    await apiRequest('/auth/logout', {
      method: 'POST',
      headers: authHeader()
    });
  } catch (err) {
    console.warn('logout request failed', err.code || err.name);
  }
  clearSession();
}

export async function refreshSession() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    clearSession();
    return null;
  }
  try {
    const data = await apiRequest('/auth/refresh', {
      method: 'POST',
      body: { refreshToken }
    });
    saveSession(data);
    return data;
  } catch (err) {
    clearSession();
    return null;
  }
}
