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

export async function fetchRoster(stationId) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/roster`, { headers: authHeader() }));
}

export async function fetchProfile(stationId, profileId) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/profiles/${profileId}`, { headers: authHeader() }));
}

export async function createProfile(stationId, body) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/profiles`, {
    method: 'POST',
    headers: authHeader(),
    body
  }));
}

export async function updateProfile(stationId, profileId, body) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/profiles/${profileId}`, {
    method: 'PUT',
    headers: authHeader(),
    body
  }));
}

export async function deleteProfile(stationId, profileId) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/profiles/${profileId}`, {
    method: 'DELETE',
    headers: authHeader()
  }));
}

export async function saveGroups(stationId, groups) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/groups`, {
    method: 'PUT',
    headers: authHeader(),
    body: { groups }
  }));
}

export async function saveScbaCalibration(stationId, profileId, body) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/profiles/${profileId}/scba-calibrations`, {
    method: 'POST',
    headers: authHeader(),
    body
  }));
}

export async function importRoster(stationId, file) {
  return withRefresh(async () => {
    const form = new FormData();
    form.append('file', file);
    let response;
    try {
      response = await fetch(`/api/v1/stations/${stationId}/profiles/import`, {
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

export async function downloadRosterExcel(stationId, template) {
  return withRefresh(async () => {
    const path = template
      ? `/api/v1/stations/${stationId}/profiles/template`
      : `/api/v1/stations/${stationId}/profiles/export`;
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
    const prefix = template ? '人员档案_战斗编组导入模板_' : '人员档案_战斗编组_';
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
