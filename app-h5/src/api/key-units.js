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
  return withRefresh(() => apiRequest(`/files/${fileId}`, {
    method: 'DELETE',
    headers: authHeader()
  }));
}

export async function uploadKeyUnitFile(bizType, bizId, file) {
  const presign = await withRefresh(() => apiRequest('/files/presign', {
    method: 'POST',
    headers: authHeader(),
    body: {
      bizType,
      bizId,
      fileName: file.name,
      contentType: file.type || 'application/octet-stream',
      sizeBytes: file.size
    }
  }));
  let response;
  try {
    response = await fetch(presign.uploadUrl, {
      method: 'PUT',
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
      body: file
    });
    if (!response.ok) {
      const error = new Error('文件上传失败，请稍后重试');
      error.code = 'FILE_UPLOAD_FAILED';
      throw error;
    }
    return await withRefresh(() => apiRequest(`/files/${presign.fileId}/confirm`, {
      method: 'POST',
      headers: authHeader(),
      body: { confirmTicket: presign.confirmTicket }
    }));
  } catch (err) {
    try {
      await deleteKeyUnitFile(presign.fileId);
    } catch (cleanupErr) {
      console.warn('file upload cleanup failed', cleanupErr.code || cleanupErr.name);
    }
    if (err.code === 'FILE_UPLOAD_FAILED') {
      throw err;
    }
    if (err.code && err.code !== 'NETWORK') {
      throw err;
    }
    const error = new Error('文件上传失败，请检查网络');
    error.code = 'NETWORK';
    throw error;
  }
}
