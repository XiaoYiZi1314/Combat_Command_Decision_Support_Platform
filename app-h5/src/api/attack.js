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
    const refreshed = await refreshSession();
    if (!refreshed) {
      throw err;
    }
    return executor();
  }
}

export async function fetchStationAttack(stationId) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/attack`, { headers: authHeader() }));
}

export async function submitAttackEvent(stationId, body) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/attack/events`, {
    method: 'POST',
    headers: authHeader(),
    body
  }));
}

export async function fetchAttackSnapshots(since) {
  const query = since ? `?since=${encodeURIComponent(since)}` : '';
  return withRefresh(() => apiRequest(`/attack/snapshots${query}`, { headers: authHeader() }));
}
