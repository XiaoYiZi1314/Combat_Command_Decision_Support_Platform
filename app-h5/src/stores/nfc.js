/**
 * NFC 联动记录本地存储：记录扫描的标签并维护 标签→人员编号 别名映射。
 * 与客户 APK 的 batchNFCPanel + nfcTagAliases 语义一致。
 */

const RECORDS_KEY = 'ccds_nfc_records';
const ALIAS_KEY = 'ccds_nfc_alias';

function normalizeTag(tag) {
  return String(tag || '').replace(/[\s:：\-_]/g, '').toUpperCase();
}

/**
 * 人员档案编号统一成三位数字，避免 1 与 001 被当成两套号。
 */
export function canonicalCode(tag) {
  const value = normalizeTag(tag);
  if (!value) {
    return '';
  }
  if (/^\d+$/.test(value) && value.length <= 4) {
    return value.replace(/^0+(?!$)/, '').padStart(3, '0');
  }
  return value;
}

function readJson(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) {
      return fallback;
    }
    const parsed = JSON.parse(raw);
    return parsed == null ? fallback : parsed;
  } catch (err) {
    return fallback;
  }
}

function writeJson(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch (err) {
    /* 存储满时静默失败，读取侧仍可用旧值 */
  }
}

/**
 * 读取扫描记录列表。
 *
 * @returns {Array<{tag: string, alias: string, name: string, time: number}>}
 */
export function readRecords() {
  const list = readJson(RECORDS_KEY, []);
  return Array.isArray(list) ? list : [];
}

/**
 * 追加一条扫描记录（去重：同标签保留最新）。
 *
 * @param {string} tag 原始标签
 * @param {string} alias 绑定的人员编号
 * @param {string} name 匹配到的人员姓名
 * @returns {Array} 更新后的记录列表
 */
export function addRecord(tag, alias, name) {
  const normalized = normalizeTag(tag);
  if (!normalized) {
    return readRecords();
  }
  const list = readRecords().filter((item) => item.tag !== normalized);
  list.push({
    tag: normalized,
    alias: canonicalCode(alias),
    name: name || '',
    time: Date.now()
  });
  writeJson(RECORDS_KEY, list);
  return list;
}

/**
 * 删除一条扫描记录。
 *
 * @param {string} tag 原始标签
 * @returns {Array} 更新后的记录列表
 */
export function removeRecord(tag) {
  const normalized = normalizeTag(tag);
  const list = readRecords().filter((item) => item.tag !== normalized);
  writeJson(RECORDS_KEY, list);
  return list;
}

/**
 * 读取 标签→人员编号 别名映射。
 *
 * @returns {Object<string, string>}
 */
export function readAliases() {
  const map = readJson(ALIAS_KEY, {});
  return map && typeof map === 'object' ? map : {};
}

/**
 * 写别名映射。
 *
 * @param {Object<string, string>} map 别名映射
 */
export function writeAliases(map) {
  writeJson(ALIAS_KEY, map || {});
}

/**
 * 设置单条别名。
 *
 * @param {string} tag 原始标签
 * @param {string} alias 人员编号
 */
export function setAlias(tag, alias) {
  const map = readAliases();
  const normalized = normalizeTag(tag);
  if (!normalized || !alias) {
    return;
  }
  map[normalized] = canonicalCode(alias);
  writeAliases(map);
}

/**
 * 匹配人员编号：档案编号等于标签本身或其别名。
 *
 * @param {string} profileNfc 人员档案里的 NFC 编号
 * @param {string} tag 扫描到的原始标签
 * @returns {boolean} 是否匹配
 */
export function tagMatchesProfile(profileNfc, tag) {
  const profileCode = canonicalCode(profileNfc);
  if (!profileCode) {
    return false;
  }
  const normalized = normalizeTag(tag);
  if (profileCode === canonicalCode(normalized)) {
    return true;
  }
  const map = readAliases();
  return canonicalCode(map[normalized]) === profileCode;
}

/**
 * 依据标签反查绑定的编号。
 *
 * @param {string} tag 原始标签
 * @returns {string} 绑定的编号，未绑定为空串
 */
export function aliasOf(tag) {
  const map = readAliases();
  return map[normalizeTag(tag)] || '';
}
