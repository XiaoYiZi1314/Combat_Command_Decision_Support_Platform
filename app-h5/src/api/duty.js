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

export async function fetchDutyMonth(stationId, year, month) {
  const query = new URLSearchParams({ year: String(year), month: String(month) });
  return withRefresh(() => apiRequest(`/stations/${stationId}/duty/month?${query}`, {
    headers: authHeader()
  }));
}

export async function saveDutyDay(stationId, date, body) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/duty/${date}`, {
    method: 'PUT',
    headers: authHeader(),
    body
  }));
}

export async function deleteDutyDay(stationId, date) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/duty/${date}`, {
    method: 'DELETE',
    headers: authHeader()
  }));
}
