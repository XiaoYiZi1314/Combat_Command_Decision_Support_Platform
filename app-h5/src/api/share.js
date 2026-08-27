import { apiRequest } from './client.js';
import { getAccessToken } from '../stores/session.js';
import { refreshSession } from './auth.js';

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
    if (!(await refreshSession())) {
      throw err;
    }
    return executor();
  }
}

export function createShareToken(stationId, expireHours) {
  return withRefresh(() => apiRequest('/share', {
    method: 'POST',
    headers: authHeader(),
    body: { stationId: Number(stationId), expireHours: Number(expireHours) }
  }));
}

export function revokeShareToken(tokenId) {
  return withRefresh(() => apiRequest(`/share/${tokenId}`, {
    method: 'DELETE',
    headers: authHeader()
  }));
}
