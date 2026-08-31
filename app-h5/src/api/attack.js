import { apiRequest, authHeader, withRefresh } from './client.js';

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
