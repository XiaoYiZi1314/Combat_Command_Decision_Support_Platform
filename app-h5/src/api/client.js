import { apiBase } from '../bridge/index.js';
import { getAccessToken } from '../stores/session.js';
import { refreshSession } from './auth.js';

const API_PREFIX = '/api/v1';

const CODE_OK = '0';

/**
 * 构造后端接口完整地址：注入了 API 基地址（Android 壳）时拼接基地址，
 * 浏览器开发环境返回相对路径。裸 fetch 场景（Excel 导入导出）必须使用本函数。
 *
 * @param {string} path 以 / 开头的接口路径
 * @returns {string} 完整请求地址
 */
export function apiUrl(path) {
  return `${apiBase()}${API_PREFIX}${path}`;
}

/** 默认请求超时（毫秒）：普通后端接口超过此时长视为连接悬挂 */
const DEFAULT_REQUEST_TIMEOUT_MS = 15000;

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
  const requestTimeoutMs = Number.isFinite(opts.timeoutMs) && opts.timeoutMs > 0
    ? opts.timeoutMs
    : DEFAULT_REQUEST_TIMEOUT_MS;
  const controller = typeof AbortController === 'function' ? new AbortController() : null;
  const timer = controller
    ? setTimeout(() => controller.abort(), requestTimeoutMs)
    : null;
  try {
    response = await fetch(`${apiBase()}${API_PREFIX}${path}`, {
      method: opts.method || 'GET',
      headers,
      body: opts.body ? JSON.stringify(opts.body) : undefined,
      signal: controller ? controller.signal : undefined
    });
  } catch (err) {
    const aborted = err && (err.name === 'AbortError' || err.code === 20);
    const error = new Error(aborted
      ? `请求超时（${requestTimeoutMs / 1000} 秒无响应），请检查网络后重试`
      : '网络不可用，请检查连接');
    error.code = aborted ? 'TIMEOUT' : 'NETWORK';
    throw error;
  } finally {
    if (timer) {
      clearTimeout(timer);
    }
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
