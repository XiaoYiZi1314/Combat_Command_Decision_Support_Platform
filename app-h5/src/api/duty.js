import { apiRequest, authHeader, withRefresh } from './client.js';

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
