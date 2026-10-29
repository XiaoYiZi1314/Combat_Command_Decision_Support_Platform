import { apiRequest, authHeader, withRefresh } from './client.js';
import { deleteBizFile, uploadBizFile } from './files.js';

export async function listKeyUnits(stationId, filters) {
  const query = new URLSearchParams();
  if (filters && filters.category) {
    query.set('category', filters.category);
  }
  if (filters && filters.keyword) {
    query.set('keyword', filters.keyword);
  }
  const suffix = query.toString() ? `?${query.toString()}` : '';
  return withRefresh(() => apiRequest(`/stations/${stationId}/key-units${suffix}`, {
    headers: authHeader()
  }));
}

export function getKeyUnit(stationId, id) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/key-units/${id}`, {
    headers: authHeader()
  }));
}

export function createKeyUnit(stationId, body) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/key-units`, {
    method: 'POST',
    headers: authHeader(),
    body
  }));
}

export function updateKeyUnit(stationId, id, body) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/key-units/${id}`, {
    method: 'PUT',
    headers: authHeader(),
    body
  }));
}

export function deleteKeyUnit(stationId, id) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/key-units/${id}`, {
    method: 'DELETE',
    headers: authHeader()
  }));
}

export function deleteKeyUnitFile(fileId) {
  return deleteBizFile(fileId);
}

export function uploadKeyUnitFile(bizType, bizId, file) {
  return uploadBizFile(bizType, bizId, file);
}
