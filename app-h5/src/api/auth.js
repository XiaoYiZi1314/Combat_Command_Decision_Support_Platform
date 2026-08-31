import { apiRequest, authHeader, withRefresh } from './client.js';

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

export async function issueWebSocketTicket() {
  return withRefresh(() => apiRequest('/auth/ws-ticket', {
    method: 'POST',
    headers: authHeader()
  }));
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
