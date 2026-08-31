import { apiRequest, authHeader, withRefresh } from './client.js';

export async function fetchStationArchives(stationId, eventKind) {
  const query = new URLSearchParams();
  if (eventKind) {
    query.set('eventKind', eventKind);
  }
  const suffix = query.toString() ? `?${query}` : '';
  return withRefresh(() => apiRequest(`/stations/${stationId}/attack-archives${suffix}`, {
    headers: authHeader()
  }));
}

export async function fetchAllArchives(eventKind) {
  const query = new URLSearchParams();
  if (eventKind) {
    query.set('eventKind', eventKind);
  }
  const suffix = query.toString() ? `?${query}` : '';
  return withRefresh(() => apiRequest(`/attack-archives${suffix}`, {
    headers: authHeader()
  }));
}

export async function archiveStation(stationId, body) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/attack-archives`, {
    method: 'POST',
    headers: authHeader(),
    body
  }));
}

export async function updateArchiveInfo(stationId, archiveId, body) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/attack-archives/${archiveId}`, {
    method: 'PUT',
    headers: authHeader(),
    body
  }));
}

export async function deleteArchive(stationId, archiveId) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/attack-archives/${archiveId}`, {
    method: 'DELETE',
    headers: authHeader()
  }));
}

async function downloadExcel(path, eventKind, filePrefix) {
  const query = new URLSearchParams();
  if (eventKind) {
    query.set('eventKind', eventKind);
  }
  const suffix = query.toString() ? `?${query}` : '';
  let response;
  try {
    response = await fetch(`api/v1${path}${suffix}`, { headers: authHeader() });
  } catch (err) {
    const error = new Error('网络不可用，请检查连接');
    error.code = 'NETWORK';
    throw error;
  }
  if (!response.ok) {
    let message = '导出失败';
    try {
      const payload = await response.json();
      message = payload.message || message;
    } catch (parseErr) {
      /* 保留默认提示 */
    }
    throw new Error(message);
  }
  const blob = await response.blob();
  const stamp = new Date().toISOString().slice(0, 10);
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${filePrefix}${stamp}.xlsx`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export async function downloadArchiveExcel(eventKind) {
  await downloadExcel('/attack-archives/export', eventKind, '内攻历史记录_');
}

export async function downloadArchiveStatsExcel(eventKind) {
  await downloadExcel('/attack-archives/export-stats', eventKind, '内攻历史统计_');
}
