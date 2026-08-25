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

export async function fetchWaters(stationId) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/waters`, { headers: authHeader() }));
}

export async function createWater(stationId, body) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/waters`, {
    method: 'POST',
    headers: authHeader(),
    body
  }));
}

export async function updateWater(stationId, waterId, body) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/waters/${waterId}`, {
    method: 'PUT',
    headers: authHeader(),
    body
  }));
}

export async function deleteWater(stationId, waterId) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/waters/${waterId}`, {
    method: 'DELETE',
    headers: authHeader()
  }));
}

export async function fetchCityWaters() {
  return withRefresh(() => apiRequest('/waters/city', { headers: authHeader() }));
}

export async function fetchNearbyWaters(lng, lat, radiusM) {
  const params = new URLSearchParams({ lng: String(lng), lat: String(lat) });
  if (radiusM) {
    params.set('radiusM', String(radiusM));
  }
  return withRefresh(() => apiRequest(`/waters/nearby?${params.toString()}`, { headers: authHeader() }));
}

export async function fetchBaiduMapAk() {
  return withRefresh(() => apiRequest('/maps/baidu-token', { headers: authHeader() }));
}

export async function importWaters(stationId, file) {
  return withRefresh(async () => {
    const form = new FormData();
    form.append('file', file);
    let response;
    try {
      response = await fetch(`/api/v1/stations/${stationId}/waters/import`, {
        method: 'POST',
        headers: authHeader(),
        body: form
      });
    } catch (err) {
      const error = new Error('网络不可用，请检查连接');
      error.code = 'NETWORK';
      throw error;
    }
    const payload = await response.json();
    if (!payload || payload.code !== '0') {
      const error = new Error((payload && payload.message) || '导入失败');
      error.code = payload && payload.code;
      throw error;
    }
    return payload.data;
  });
}

export async function downloadWaterExcel(stationId, template) {
  return withRefresh(async () => {
    const path = template
      ? `/api/v1/stations/${stationId}/waters/template`
      : `/api/v1/stations/${stationId}/waters/export`;
    let response;
    try {
      response = await fetch(path, { headers: authHeader() });
    } catch (err) {
      const error = new Error('网络不可用，请检查连接');
      error.code = 'NETWORK';
      throw error;
    }
    if (!response.ok) {
      let message = '导出失败';
      let code = 'HTTP';
      try {
        const payload = await response.json();
        message = payload.message || message;
        code = payload.code || code;
      } catch (parseErr) {
        if (parseErr && parseErr.message) {
          message = parseErr.message;
        }
      }
      const error = new Error(message);
      error.code = code;
      throw error;
    }
    const blob = await response.blob();
    const stamp = new Date().toISOString().slice(0, 10);
    const prefix = template ? '水源检查记录导入模板_' : '水源检查记录_';
    const name = `${prefix}${stamp}.xlsx`;
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = name;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  });
}
