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

export const VEHICLE_TYPES = [
  { id: 'water_foam', name: '水罐泡沫车' },
  { id: 'aerial', name: '登高平台车' },
  { id: 'high_spray', name: '高喷车' },
  { id: 'rescue', name: '抢险救援车' },
  { id: 'troop_transport', name: '运兵车' },
  { id: 'air_supply', name: '供气车' },
  { id: 'long_supply', name: '远程供水车' },
  { id: 'dry_powder', name: '干粉车' },
  { id: 'smoke_exhaust', name: '排烟车' },
  { id: 'command', name: '通信指挥车' },
  { id: 'publicity', name: '宣传车' },
  { id: 'other', name: '其他车辆' }
];

export function vehicleTypeLabel(code) {
  const hit = VEHICLE_TYPES.find((item) => item.id === code);
  return hit ? hit.label || hit.name : code;
}

export async function fetchStationVehicles(stationId, params) {
  const query = new URLSearchParams();
  Object.keys(params || {}).forEach((key) => {
    if (params[key]) {
      query.set(key, params[key]);
    }
  });
  const suffix = query.toString() ? `?${query}` : '';
  return withRefresh(() => apiRequest(`/stations/${stationId}/vehicles${suffix}`, {
    headers: authHeader()
  }));
}

export async function fetchAllVehicles(params) {
  const query = new URLSearchParams();
  Object.keys(params || {}).forEach((key) => {
    if (params[key]) {
      query.set(key, params[key]);
    }
  });
  const suffix = query.toString() ? `?${query}` : '';
  return withRefresh(() => apiRequest(`/vehicles${suffix}`, {
    headers: authHeader()
  }));
}

export async function saveStationVehicle(stationId, body) {
  if (body.id) {
    return withRefresh(() => apiRequest(`/stations/${stationId}/vehicles/${body.id}`, {
      method: 'PUT',
      headers: authHeader(),
      body
    }));
  }
  return withRefresh(() => apiRequest(`/stations/${stationId}/vehicles`, {
    method: 'POST',
    headers: authHeader(),
    body
  }));
}

export async function deleteStationVehicle(stationId, vehicleId) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/vehicles/${vehicleId}`, {
    method: 'DELETE',
    headers: authHeader()
  }));
}

export async function submitVehicleRequest(stationId, body) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/vehicle-requests`, {
    method: 'POST',
    headers: authHeader(),
    body
  }));
}

export async function fetchVehicleRequests(stationId) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/vehicle-requests`, {
    headers: authHeader()
  }));
}

export async function fetchAllVehicleRequests() {
  return withRefresh(() => apiRequest('/vehicle-requests', {
    headers: authHeader()
  }));
}

export async function reviewVehicleRequest(stationId, requestId, approve) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/vehicle-requests/${requestId}`, {
    method: 'POST',
    headers: authHeader(),
    body: { approve }
  }));
}

export async function deleteVehicleRequest(stationId, requestId) {
  return withRefresh(() => apiRequest(`/stations/${stationId}/vehicle-requests/${requestId}`, {
    method: 'DELETE',
    headers: authHeader()
  }));
}
