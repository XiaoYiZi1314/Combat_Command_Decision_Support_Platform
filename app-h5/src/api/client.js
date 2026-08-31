import { apiBase } from '../bridge/index.js';
import { getAccessToken } from '../stores/session.js';
import { refreshSession } from './auth.js';

const API_PREFIX = '/api/v1';

const CODE_OK = '0';

/**
 * 统一的鉴权请求头。令牌在每次请求时读取，避免闭包持有过期令牌。
 *
 * @returns {Object} 含 Authorization 的头对象，未登录时为空对象
 */
export function authHeader() {
  const token = getAccessToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

/**
 * 带令牌续期重试的请求包装：遇鉴权失败先刷新会话再重发一次，刷新失败则抛原错误。
 *
 * @param {Function} executor 发起请求的函数
 * @returns {Promise<*>} 请求结果
 */
export async function withRefresh(executor) {
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
