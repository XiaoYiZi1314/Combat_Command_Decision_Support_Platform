import { apiBase } from '../bridge/index.js';

const API_PREFIX = '/api/v1';

const CODE_OK = '0';

function unwrap(payload) {
  if (!payload || typeof payload !== 'object') {
    throw new Error('系统繁忙，请稍后重试');
  }
  if (payload.code !== CODE_OK) {
    const error = new Error(payload.message || '请求失败');
    error.code = payload.code;
    throw error;
  }
  return payload.data;
}

export async function apiRequest(path, options) {
  const opts = options || {};
  const headers = Object.assign({
    Accept: 'application/json'
  }, opts.headers || {});
  if (opts.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  let response;
  try {
    response = await fetch(`${apiBase()}${API_PREFIX}${path}`, {
      method: opts.method || 'GET',
      headers,
      body: opts.body ? JSON.stringify(opts.body) : undefined
    });
  } catch (err) {
    const error = new Error('网络不可用，请检查连接');
    error.code = 'NETWORK';
    throw error;
  }
  let payload = null;
  try {
    payload = await response.json();
  } catch (err) {
    payload = null;
  }
  if (!payload) {
    const error = new Error(response.ok ? '系统繁忙，请稍后重试' : '请求失败');
    error.code = 'HTTP';
    throw error;
  }
  return unwrap(payload);
}
