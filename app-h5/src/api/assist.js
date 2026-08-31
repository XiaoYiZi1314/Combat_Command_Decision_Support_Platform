import { apiRequest, authHeader, withRefresh } from './client.js';

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

export async function fetchChatAnswer(body) {
  return withRefresh(() => apiRequest('/assist/chat', {
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

export async function generateDocument(body) {
  return withRefresh(() => apiRequest('/assist/document', {
    method: 'POST',
    headers: authHeader(),
    body
  }));
}
