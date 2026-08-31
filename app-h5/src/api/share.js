import { apiRequest, authHeader, withRefresh } from './client.js';

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
