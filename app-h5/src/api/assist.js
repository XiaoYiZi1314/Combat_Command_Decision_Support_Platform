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

export async function searchHazmat(query) {
  const params = new URLSearchParams({ q: query });
  return withRefresh(() => apiRequest(`/hazmat/search?${params.toString()}`, { headers: authHeader() }));
}

export async function visionHazmat(body) {
  return withRefresh(() => apiRequest('/assist/hazmat-vision', {
    method: 'POST',
    headers: authHeader(),
    body
  }));
}

export async function fetchAttackAdvice(body) {
  return withRefresh(() => apiRequest('/assist/attack-advice', {
    method: 'POST',
    headers: authHeader(),
    body
  }));
}

export async function fetchWeather(lng, lat) {
  const params = new URLSearchParams({ lng: String(lng), lat: String(lat) });
  return withRefresh(() => apiRequest(`/weather?${params.toString()}`, { headers: authHeader() }));
}

export async function fetchSupplyAdvice(body) {
  return withRefresh(() => apiRequest('/assist/supply-calc', {
    method: 'POST',
    headers: authHeader(),
    body
  }));
}
