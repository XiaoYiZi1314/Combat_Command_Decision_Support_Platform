/**
 * 旧系统 R2（S3 兼容）只读客户端。
 *
 * 用途：把客户旧版 APP 存放在 Cloudflare R2 上的历史数据读出，供一次性迁移使用。
 * 凭证不落仓库：优先读环境变量 R2_AK / R2_SK，否则从旧版 APK 的 cloud_config.js 解析
 * （该文件本身不入库，见 .gitignore 的 tmp_apk/）。本客户端只做 ListObjectsV2 与 GetObject。
 *
 * 运行环境：Node.js >= 20（需要全局 fetch、node:crypto）。
 */

import { createHash, createHmac } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { basename } from 'node:path';

const ENDPOINT = 'https://51f5e121cef646ebec251bfb154a5f83.r2.cloudflarestorage.com';
const BUCKET = 'nxgk-files';
const REGION = 'auto';
const SERVICE = 's3';

const DEFAULT_LEGACY_CONFIG = 'C:\\Users\\lz200\\Desktop\\消防系统\\消防系统\\nxgk_6241_source\\assets\\cloud_config.js';

function sha256Hex(buf) {
  return createHash('sha256').update(buf).digest('hex');
}

function hmac(key, data) {
  return createHmac('sha256', key).update(data).digest();
}

function signingKey(secret, datestamp) {
  let key = hmac(`AWS4${secret}`, datestamp);
  key = hmac(key, REGION);
  key = hmac(key, SERVICE);
  return hmac(key, 'aws4_request');
}

function encode(value) {
  return encodeURIComponent(String(value)).replace(/[!'()*]/g, (ch) => `%${ch.charCodeAt(0).toString(16).toUpperCase()}`);
}

function amzNow() {
  const now = new Date();
  const iso = now.toISOString().replace(/[:-]|\.\d{3}/g, '');
  return { amzDate: iso, datestamp: iso.slice(0, 8) };
}

function canonicalQuery(params) {
  return Object.keys(params)
    .sort()
    .map((key) => `${encode(key)}=${encode(params[key])}`)
    .join('&');
}

async function signedRequest(method, path, params, payload = Buffer.alloc(0), extraHeaders = {}) {
  const { ak, sk } = readCreds();
  const { amzDate, datestamp } = amzNow();
  const host = new URL(ENDPOINT).host;
  const query = canonicalQuery(params);
  const payloadHash = sha256Hex(payload);
  const headers = {
    host,
    'x-amz-content-sha256': payloadHash,
    'x-amz-date': amzDate,
    ...Object.fromEntries(Object.entries(extraHeaders).map(([k, v]) => [k.toLowerCase(), v]))
  };
  const signedHeaders = Object.keys(headers).sort();
  const canonicalHeaders = signedHeaders.map((k) => `${k}:${headers[k]}\n`).join('');
  const canonicalRequest = [
    method,
    path,
    query,
    canonicalHeaders,
    signedHeaders.join(';'),
    payloadHash
  ].join('\n');
  const scope = `${datestamp}/${REGION}/${SERVICE}/aws4_request`;
  const stringToSign = [
    'AWS4-HMAC-SHA256',
    amzDate,
    scope,
    sha256Hex(Buffer.from(canonicalRequest, 'utf8'))
  ].join('\n');
  const signature = createHmac('sha256', signingKey(sk, datestamp)).update(stringToSign).digest('hex');
  const url = `${ENDPOINT}${path}${query ? `?${query}` : ''}`;
  const requestHeaders = {};
  for (const key of signedHeaders) {
    if (key !== 'host') {
      requestHeaders[key] = headers[key];
    }
  }
  requestHeaders.authorization = `AWS4-HMAC-SHA256 Credential=${ak}/${scope}, SignedHeaders=${signedHeaders.join(';')}, Signature=${signature}`;
  const response = await fetch(url, { method, headers: requestHeaders, body: method === 'GET' ? undefined : payload });
  if (!response.ok) {
    const text = await response.text().catch(() => '');
    throw new Error(`R2 ${method} ${path} -> ${response.status} ${text.slice(0, 400)}`);
  }
  return Buffer.from(await response.arrayBuffer());
}

function readCreds() {
  const ak = process.env.R2_AK;
  const sk = process.env.R2_SK;
  if (ak && sk) {
    return { ak, sk };
  }
  const configPath = process.env.NXGK_LEGACY_CLOUD_CONFIG || DEFAULT_LEGACY_CONFIG;
  const text = readFileSync(configPath, 'utf8');
  const akMatch = /r2AccessKeyId\s*:\s*'([^']+)'/.exec(text);
  const skMatch = /r2SecretAccessKey\s*:\s*'([^']+)'/.exec(text);
  if (!akMatch || !skMatch) {
    throw new Error(`cloud_config 中未找到 R2 凭证：${basename(configPath)}`);
  }
  return { ak: akMatch[1], sk: skMatch[1] };
}

/**
 * 列出全部对象（自动翻页）。
 *
 * @param {string} prefix 对象键前缀
 * @returns {Promise<Array<{key:string,size:number,lastModified:string}>>}
 */
export async function listAll(prefix = '') {
  const out = [];
  let token;
  for (let page = 0; page < 1000; page += 1) {
    const params = { 'list-type': '2', 'max-keys': '1000' };
    if (prefix) {
      params.prefix = prefix;
    }
    if (token) {
      params['continuation-token'] = token;
    }
    const xml = (await signedRequest('GET', `/${BUCKET}`, params)).toString('utf8');
    for (const match of xml.matchAll(/<Contents>([\s\S]*?)<\/Contents>/g)) {
      const body = match[1];
      out.push({
        key: decodeXml((/<Key>([\s\S]*?)<\/Key>/.exec(body) || [])[1] || ''),
        size: Number((/<Size>(\d+)<\/Size>/.exec(body) || [])[1] || 0),
        lastModified: (/(<LastModified>([\s\S]*?)<\/LastModified>)/.exec(body) || [])[2] || ''
      });
    }
    if (!/<IsTruncated>true<\/IsTruncated>/.test(xml)) {
      break;
    }
    token = decodeXml((/<NextContinuationToken>([\s\S]*?)<\/NextContinuationToken>/.exec(xml) || [])[1] || '');
    if (!token) {
      break;
    }
  }
  return out;
}

/**
 * 读取单个对象内容。
 *
 * @param {string} key 对象键
 * @returns {Promise<Buffer>}
 */
export async function getObject(key) {
  const path = `/${BUCKET}/${key.split('/').map(encode).join('/')}`;
  return signedRequest('GET', path, {});
}

/**
 * 读取单个对象的文本内容（UTF-8）。
 *
 * @param {string} key 对象键
 * @returns {Promise<string>}
 */
export async function getText(key) {
  return (await getObject(key)).toString('utf8');
}

function decodeXml(text) {
  return text
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'");
}

export const R2_CONSTANT = { ENDPOINT, BUCKET, DEFAULT_LEGACY_CONFIG };
