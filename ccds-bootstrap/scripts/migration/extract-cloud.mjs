#!/usr/bin/env node
/**
 * 云端 ETL：把旧系统同步在 Cloudflare R2 上的站级业务数据抽成 NDJSON。
 *
 * 产出（写入 LEGACY_MIGRATION_OUT，默认 %TEMP%\ccds-migration）：
 *   out/roster.ndjson          一个消防站一行，契约见 README 3.4
 *   out/cloud-station-map.json 每站计数，供人工核对
 *   out/cloud-failures.ndjson  单站/单成员失败明细（不中断整批）
 *
 * 数据源（只读，桶 nxgk-files）：
 *   nxgk/cloud/nxgk_inner_attack_cloud_v1/station_<base64url(站名)>/
 *     persons/index.json      明文
 *     persons/<pid>/profile.json  明文（只取 index.people 列到的 pid）
 *     duty_v2.json | duty.json      gzip-base64 包装
 *     scba_samples.json             gzip-base64 包装
 *     transfers.json                gzip-base64 包装
 *     snapshot_v2.json              gzip-base64 包装
 *
 * 不迁移：snapshot.json / runtime.json（README 第 5 节：进行中实时态不迁移）。
 *
 * 敏感数据：phone 只在进程内存与 NDJSON 之间流转，不打印到日志/控制台，
 * 由 Java 侧 FieldCipherUtil 加密落库（README 第 2 节）。
 *
 * 用法：
 *   node extract-cloud.mjs
 *   node extract-cloud.mjs --only=红旗路消防站,光明路消防站
 *   node extract-cloud.mjs --limit=3
 *   node extract-cloud.mjs --cross-check        # 额外读 runtime.json 交叉核对阈值字段
 *   node extract-cloud.mjs --use-inventory-cache # 复用上次 listAll 清单（仅调试提速）
 *
 * 环境变量：
 *   LEGACY_MIGRATION_OUT        产物目录（默认 %TEMP%\ccds-migration）
 *   R2_AK / R2_SK               R2 凭证（缺省时 r2.mjs 回退读旧版 cloud_config.js）
 *   CLOUD_ETL_CONCURRENCY       并发 GetObject 数（默认 6）
 */

import { gunzipSync } from 'node:zlib';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

import { listAll, getObject } from './r2.mjs';

/** 云端站级数据根前缀。 */
const CLOUD_PREFIX = 'nxgk/cloud/nxgk_inner_attack_cloud_v1/';

/** 产物目录。 */
const OUT_DIR = process.env.LEGACY_MIGRATION_OUT || join(tmpdir(), 'ccds-migration');

/** GetObject 并发数。 */
const CONCURRENCY = Math.max(1, Number(process.env.CLOUD_ETL_CONCURRENCY || 10) || 10);

/**
 * 非站点的历史脏键（旧版 APP 的默认/兜底桶），不迁入花名册。
 * 例：station_ZGVmYXVsdA 解出来是 "default"，带 index/snapshot 但没有真实人员。
 */
const EXCLUDED_STATION_NAMES = new Set(['default']);

/** 参与抽取的站级文件名（snapshot/runtime 不迁移，runtime 仅交叉核对用）。 */
const STATION_FILES = ['duty_v2.json', 'duty.json', 'scba_samples.json', 'transfers.json', 'snapshot_v2.json'];

/** 站名无 index.json 时用于探测站名的文件优先级。 */
const NAME_PROBE_ORDER = ['snapshot_v2.json', 'duty_v2.json', 'duty.json', 'scba_samples.json', 'transfers.json'];

const failures = [];
let fetchedProfileObjects = 0;
let historicalProfileObjects = 0;

const log = (...args) => console.log(...args);
const warn = (...args) => console.warn('[WARN]', ...args);

// ---------------------------------------------------------------------------
// 参数
// ---------------------------------------------------------------------------

/**
 * 解析命令行参数。
 *
 * @param {string[]} argv 进程参数（已去掉 node 与脚本名）
 * @returns {{only:string[]|null, limit:number, crossCheck:boolean, useInventoryCache:boolean}}
 */
function parseArgs(argv) {
  const out = { only: null, limit: 0, crossCheck: false, useInventoryCache: false };
  for (const arg of argv) {
    if (arg.startsWith('--only=')) {
      out.only = arg.slice('--only='.length).split(',').map((s) => s.trim()).filter(Boolean);
    } else if (arg.startsWith('--limit=')) {
      out.limit = Math.max(0, Number(arg.slice('--limit='.length)) || 0);
    } else if (arg === '--cross-check') {
      out.crossCheck = true;
    } else if (arg === '--use-inventory-cache') {
      out.useInventoryCache = true;
    } else {
      warn(`忽略无法识别的参数：${arg}`);
    }
  }
  return out;
}

// ---------------------------------------------------------------------------
// 通用小工具
// ---------------------------------------------------------------------------

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 站点键 → 站名。stationKey = 'station_' + base64url(站名)。
 *
 * @param {string} stationKey 站点键
 * @returns {string} 站名（解码失败返回空串）
 */
function decodeStationKey(stationKey) {
  if (!stationKey || !stationKey.startsWith('station_')) {
    return '';
  }
  const b64 = stationKey.slice('station_'.length).replace(/-/g, '+').replace(/_/g, '/');
  try {
    return Buffer.from(b64, 'base64').toString('utf8');
  } catch {
    return '';
  }
}

/**
 * 站名 → 站点键（仅 bundle 兜底时生成）。
 *
 * @param {string} name 站名
 * @returns {string} 站点键
 */
function encodeStationKey(name) {
  return `station_${Buffer.from(String(name), 'utf8').toString('base64url')}`;
}

/**
 * 把字符串统一成字符串（null/undefined → ''）。
 *
 * @param {unknown} value 原值
 * @returns {string} 字符串
 */
function str(value) {
  if (value === null || value === undefined) {
    return '';
  }
  return String(value);
}

/**
 * 把值归一成数字，无法解析返回 null。
 *
 * @param {unknown} value 原值
 * @returns {number|null} 数字
 */
function numOrNull(value) {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

/**
 * 布尔归一：true/false 原样，1/0、'true'/'false'、'yes'/'no' 归一。
 *
 * @param {unknown} value 原值
 * @returns {boolean|null} 布尔值
 */
function normBool(value) {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  if (typeof value === 'boolean') {
    return value;
  }
  if (typeof value === 'number') {
    return value !== 0;
  }
  const text = String(value).trim().toLowerCase();
  if (['true', '1', 'yes', 'on'].includes(text)) {
    return true;
  }
  if (['false', '0', 'no', 'off'].includes(text)) {
    return false;
  }
  return Boolean(value);
}

/**
 * 解析 R2 对象内容：兼容明文 JSON 与 {v,enc:'gzip-base64',data} 包装。
 *
 * @param {Buffer} buf 对象字节
 * @param {string} key 对象键（仅用于报错）
 * @returns {object} 解析后的对象
 */
function parsePayload(buf, key) {
  let obj;
  try {
    obj = JSON.parse(buf.toString('utf8'));
  } catch (e) {
    throw new Error(`JSON 解析失败（${key}）：${e.message}`);
  }
  if (obj && obj.enc === 'gzip-base64' && typeof obj.data === 'string') {
    try {
      return JSON.parse(gunzipSync(Buffer.from(obj.data, 'base64')).toString('utf8'));
    } catch (e) {
      throw new Error(`gzip-base64 解包失败（${key}）：${e.message}`);
    }
  }
  return obj;
}

/**
 * 读取并解析一个 R2 对象，带 3 次重试（403/404 不重试）。
 *
 * @param {string} key 对象键
 * @returns {Promise<object>} 解析后的对象
 */
async function fetchJson(key) {
  let lastErr;
  for (let attempt = 0; attempt < 3; attempt += 1) {
    try {
      return parsePayload(await getObject(key), key);
    } catch (e) {
      lastErr = e;
      const msg = String(e?.message || e);
      if (/\b(403|404)\b/.test(msg) || /NoSuchKey/.test(msg)) {
        break;
      }
      await sleep(300 * (attempt + 1));
    }
  }
  throw lastErr;
}

/**
 * 并发受限的 map（保持输入顺序）。
 *
 * @param {Array} items 输入
 * @param {number} limit 并发数
 * @param {(item:any, index:number)=>Promise<any>} fn 映射函数
 * @returns {Promise<Array>} 结果
 */
async function mapLimit(items, limit, fn) {
  const results = new Array(items.length);
  let cursor = 0;
  const workers = new Array(Math.min(limit, items.length || 1)).fill(0).map(async () => {
    for (;;) {
      const index = cursor;
      cursor += 1;
      if (index >= items.length) {
        return;
      }
      results[index] = await fn(items[index], index);
    }
  });
  await Promise.all(workers);
  return results;
}

/**
 * 记录一条失败（不抛出，不中断整批）。
 *
 * @param {string} station 站名
 * @param {string} stage 阶段
 * @param {unknown} error 错误
 */
function recordFailure(station, stage, error) {
  const message = error instanceof Error ? error.message : str(error);
  failures.push({ station, stage, error: message });
}

// ---------------------------------------------------------------------------
// 站级清单
// ---------------------------------------------------------------------------

/**
 * 一次 listAll 后按站归并清单，只保留契约需要的键，不保留 7.6 万个对象全量。
 *
 * @param {{useInventoryCache:boolean}} options 选项
 * @returns {Promise<{candidates:Array, objectCount:number}>}
 */
async function indexCloud(options) {
  const cachePath = join(OUT_DIR, 'cloud-inventory-cache.json');
  let objects;
  if (options.useInventoryCache && existsSync(cachePath)) {
    const cached = JSON.parse(readFileSync(cachePath, 'utf8'));
    objects = cached.objects || [];
    log(`listAll 命中本地清单缓存（${objects.length} 个对象，生成于 ${cached.createdAt}）：${cachePath}`);
  } else {
    const listStartedAt = Date.now();
    objects = await listAll(CLOUD_PREFIX);
    const listMs = Date.now() - listStartedAt;
    log(`listAll(${CLOUD_PREFIX}) → ${objects.length} 个对象，耗时 ${(listMs / 1000).toFixed(1)} s`);
    // 仅显式要求缓存时才落盘清单，默认运行只产出契约要求的 3 个文件。
    if (options.useInventoryCache || process.env.CLOUD_ETL_WRITE_CACHE === '1') {
      try {
        writeFileSync(cachePath, JSON.stringify({ createdAt: new Date().toISOString(), objects }), 'utf8');
      } catch (e) {
        warn(`写清单缓存失败（不影响本次抽取）：${e.message}`);
      }
    }
  }

  const stations = new Map();
  for (const obj of objects) {
    const rel = obj.key.slice(CLOUD_PREFIX.length);
    const slash = rel.indexOf('/');
    if (slash <= 0) {
      continue;
    }
    const stationKey = rel.slice(0, slash);
    if (!stationKey.startsWith('station_')) {
      continue;
    }
    const rest = rel.slice(slash + 1);
    let station = stations.get(stationKey);
    if (!station) {
      station = { stationKey, files: {}, indexKey: null, profileCount: 0, objectCount: 0 };
      stations.set(stationKey, station);
    }
    station.objectCount += 1;
    if (rest === 'persons/index.json') {
      station.indexKey = obj.key;
    } else if (STATION_FILES.includes(rest)) {
      station.files[rest] = obj.key;
    } else if (rest === 'runtime.json') {
      station.files['runtime.json'] = obj.key;
    } else if (rest.startsWith('persons/') && rest.endsWith('/profile.json')) {
      station.profileCount += 1;
    }
  }

  historicalProfileObjects = [...stations.values()].reduce((sum, s) => sum + s.profileCount, 0);
  const candidates = [...stations.values()].filter(
    (s) => s.indexKey || STATION_FILES.some((name) => s.files[name])
  );
  log(`站级候选 ${candidates.length} 个（另有 ${historicalProfileObjects} 个历史 profile 对象，只取在册 pid）`);
  return { candidates, objectCount: objects.length };
}

/**
 * 确定候选站的真实站名：优先 index.json 的 station 字段，否则探测一个站级文件的 station 字段。
 *
 * @param {object} candidate 候选站
 * @returns {Promise<{name:string, payloads:object}>}
 */
async function resolveStationName(candidate) {
  const payloads = {};
  if (candidate.indexKey) {
    const index = await fetchJson(candidate.indexKey);
    payloads['persons/index.json'] = index;
    return { name: str(index?.station) || decodeStationKey(candidate.stationKey), payloads };
  }
  for (const name of NAME_PROBE_ORDER) {
    const key = candidate.files[name];
    if (!key) {
      continue;
    }
    try {
      const payload = await fetchJson(key);
      payloads[name] = payload;
      return { name: str(payload?.station) || decodeStationKey(candidate.stationKey), payloads };
    } catch (e) {
      recordFailure(decodeStationKey(candidate.stationKey), `probe:${name}`, e);
    }
  }
  return { name: decodeStationKey(candidate.stationKey), payloads };
}

// ---------------------------------------------------------------------------
// 契约字段构造
// ---------------------------------------------------------------------------

/**
 * 合并 index.people 条目与 profile.json 的 profile。
 * people[] 是「在册权威」（legacyId/姓名/cylType 以它为准），profile.json 只补 people 缺的字段。
 *
 * @param {object} peopleEntry index.people 条目
 * @param {object} fileProfile persons/<pid>/profile.json 里的 profile
 * @returns {object} 合并后的档案
 */
function mergePerson(peopleEntry, fileProfile) {
  const merged = { ...(fileProfile && typeof fileProfile === 'object' ? fileProfile : {}) };
  for (const [key, value] of Object.entries(peopleEntry || {})) {
    if (value !== undefined && value !== null && value !== '') {
      merged[key] = value;
    }
  }
  return merged;
}

/**
 * 合并后的档案 → 契约 profiles[] 条目。
 *
 * @param {object} person 合并后的档案
 * @returns {object} 契约条目
 */
function toProfileEntry(person) {
  const src = person || {};
  return {
    legacyId: src.id ?? null,
    name: str(src.name),
    role: str(src.role),
    rank: str(src.rank),
    // 电话原样保留（可为空串），不打码、不删除；仅在内存与 NDJSON 中流转。
    phone: str(src.phone),
    cylType: str(src.cylType),
    height: numOrNull(src.height),
    weight: numOrNull(src.weight),
    age: numOrNull(src.age),
    nfcTag: str(src.nfcTag),
    morningPressure: numOrNull(src.morningPressure)
  };
}

/**
 * 归一 bundle 里 groups[].members 的两种写法：['姓名'] 或 [{name}]。
 *
 * @param {Array} members 成员数组
 * @returns {string[]} 姓名数组
 */
function normalizeMemberNames(members) {
  if (!Array.isArray(members)) {
    return [];
  }
  const names = [];
  for (const member of members) {
    if (typeof member === 'string') {
      names.push(member);
    } else if (member && typeof member === 'object') {
      names.push(str(member.name ?? member.memberName));
    }
  }
  return names.filter(Boolean);
}

/**
 * 从 bundle-core.json 里取某站的 fireStationData 种子。
 *
 * @param {object|null} bundle bundle-core.json 内容
 * @param {string} station 站名
 * @returns {object|null} 站级种子
 */
function bundleStationOf(bundle, station) {
  if (!bundle || typeof bundle !== 'object') {
    return null;
  }
  const containers = [bundle.fireStationData, bundle.stations, bundle];
  for (const container of containers) {
    if (container && typeof container === 'object' && container[station] && typeof container[station] === 'object') {
      return container[station];
    }
  }
  return null;
}

/**
 * 构造 groups[]：组名与顺序取「当前编制」= snapshot_v2.data.settings.localGroupTasks 的键。
 *
 * 为什么不并云端 data.groups / 内置种子的组名：
 *   - localGroupTasks 已是旧版界面的当前编制（每站 0~16 个键，全域 80 个），
 *     再并云端 groups（全域 136 个）会把「历史同义组」重新写进 NDJSON，
 *     编组数虚高到 230，同一个人还会被塞进多个组（跨组重复）。
 *   - 内置种子组名是另一套命名（灭火1组/救援1组），与 localGroupTasks 零同名，
 *     并进来只会产生平行重复编组。
 *   - 组名只此一处来源；云端/内置只用于「同名组」的成员归属。
 *
 * 成员归属优先级：内置种子同名组 → 云端同名组（带 roles）。
 * 同一 legacyId 在同一站最多进一个组，重复的记 failures `member_dup_group`。
 *
 * 无 localGroupTasks 的站（实测 4 站：海河路特勤站/海山路消防站/金鼎消防站/战勤保障分队）
 * 默认不迁编组并记 failures `group_source_missing`；设环境变量 CLOUD_ETL_GROUPS_FALLBACK=1
 * 时改用云端 data.groups 组名兜底（全域会变成 136 组，与 Lead 的 80 组口径不一致，按需开启）。
 *
 * @param {object} input 构造入参
 * @returns {{groups:Array, memberSource:string, groupSources:object}} 契约 groups[] 与来源统计
 */
function buildGroups(input) {
  const { station, settings, cloudData, bundleStation, profileByName } = input;
  const localGroupTasks =
    settings && typeof settings.localGroupTasks === 'object' ? settings.localGroupTasks : null;
  const cloudGroups = Array.isArray(cloudData?.groups) ? cloudData.groups : [];
  const bundleGroups = Array.isArray(bundleStation?.groups) ? bundleStation.groups : [];

  const localKeys = localGroupTasks ? Object.keys(localGroupTasks) : [];
  const cloudGroupNames = cloudGroups.map((g) => str(g?.name)).filter(Boolean);
  const bundleGroupNames = bundleGroups.map((g) => str(g?.name)).filter(Boolean);
  const useFallback = localKeys.length === 0 && process.env.CLOUD_ETL_GROUPS_FALLBACK === '1';

  const orderedNames = [];
  const seen = new Set();
  const appendNames = (names) => {
    for (const name of names) {
      if (name && !seen.has(name)) {
        seen.add(name);
        orderedNames.push(name);
      }
    }
  };
  appendNames(localKeys);
  if (useFallback) {
    appendNames(cloudGroupNames);
    appendNames(bundleGroupNames);
  }

  if (orderedNames.length === 0) {
    recordFailure(
      station,
      'group_source_missing',
      `localGroupTasks 为空（云端 groups ${cloudGroupNames.length} 组、内置种子 ${bundleGroupNames.length} 组未迁入；` +
        '设 CLOUD_ETL_GROUPS_FALLBACK=1 可用云端组名兜底）'
    );
  }

  const groups = [];
  const groupSources = { bundle: 0, cloud: 0, none: 0 };
  const claimedMembers = new Map();
  let duplicateMemberCount = 0;

  orderedNames.forEach((name, index) => {
    const cloudGroup = cloudGroups.find((g) => str(g?.name) === name);
    const bundleGroup = bundleGroups.find((g) => str(g?.name) === name);
    const members = [];

    const pushMember = (memberName, roleInGroup) => {
      const profile = profileByName.get(memberName);
      if (!profile) {
        recordFailure(station, 'member_unresolved', `${name}/${memberName}`);
        return;
      }
      const legacyId = profile.legacyId;
      if (claimedMembers.has(legacyId)) {
        duplicateMemberCount += 1;
        recordFailure(station, 'member_dup_group', `${name}/${memberName}（已属 ${claimedMembers.get(legacyId)}）`);
        return;
      }
      claimedMembers.set(legacyId, name);
      members.push({ legacyId, roleInGroup: str(roleInGroup) });
    };

    // 1) 内置种子的同名组优先（bundle 无 roles）。
    const bundleNames = bundleGroup ? normalizeMemberNames(bundleGroup.members || bundleGroup.memberNames) : [];
    // 2) 云端同名组（带 roles）。
    const cloudMembers = Array.isArray(cloudGroup?.members) ? cloudGroup.members : [];

    if (bundleNames.length > 0) {
      groupSources.bundle += 1;
      for (const memberName of bundleNames) {
        pushMember(memberName, bundleGroup?.roles?.[memberName]);
      }
    } else if (cloudMembers.length > 0) {
      groupSources.cloud += 1;
      for (const memberName of cloudMembers) {
        pushMember(str(memberName), cloudGroup?.roles?.[memberName]);
      }
    } else {
      groupSources.none += 1;
    }

    groups.push({ name, sortNo: index + 1, members });
  });

  const used = Object.entries(groupSources).filter(([, count]) => count > 0).map(([key]) => key);
  const localNameSet = new Set(localKeys);
  return {
    groups,
    memberSource: used.length === 0 ? 'none' : used.join('+'),
    groupSources,
    localGroupNames: localKeys.length,
    appendedGroups: orderedNames.filter((name) => !localNameSet.has(name)).length,
    duplicateMemberCount,
    // 内置种子组名与云端两套命名的重合度（用于向 Lead 证明 bundle 组名不可用）。
    bundleNamesMatchedLocal: bundleGroupNames.filter((name) => localNameSet.has(name)).length,
    bundleNamesMatchedCloud: bundleGroupNames.filter((name) => cloudGroupNames.includes(name)).length
  };
}

/**
 * 构造 groupTasks[]：与 groups[] 一一对应，task/location 取 localGroupTasks 里的同名字段。
 *
 * @param {object|null} settings snapshot_v2.data.settings
 * @param {Array} groups 已构造的 groups[]
 * @returns {Array} 契约 groupTasks[]
 */
function buildGroupTasks(settings, groups) {
  const localGroupTasks =
    settings && typeof settings.localGroupTasks === 'object' ? settings.localGroupTasks : null;
  return groups.map((group) => {
    const task = localGroupTasks ? localGroupTasks[group.name] : null;
    return {
      name: group.name,
      task: str(task?.task),
      location: str(task?.location)
    };
  });
}

/**
 * 构造 calibrations[]：scba_samples.profiles[].scbaCalibrations[]。
 * fullTime/rawTime 单位是「分钟」（如 20.2666 → 20 分 16 秒 = 1216 秒）。
 *
 * @param {object} scba scba_samples.json
 * @param {Map<string,object>} profileById 在册档案（pid → 档案）
 * @returns {Array} 契约 calibrations[]
 */
function buildCalibrations(scba, profileById, station) {
  const rows = [];
  const scbaProfiles = Array.isArray(scba?.profiles) ? scba.profiles : [];
  for (const sampled of scbaProfiles) {
    const calibrations = Array.isArray(sampled?.scbaCalibrations) ? sampled.scbaCalibrations : [];
    for (const calibration of calibrations) {
      const minutes = numOrNull(calibration?.fullTime) ?? numOrNull(calibration?.rawTime);
      const legacyId = calibration?.profileId ?? sampled?.id ?? null;
      if (minutes === null) {
        recordFailure(station, 'calibration_incomplete', `${legacyId} 缺少 fullTime/rawTime`);
        continue;
      }
      const owner = legacyId ? profileById.get(legacyId) : null;
      rows.push({
        legacyId,
        pressure: numOrNull(calibration?.pressure),
        fullTimeSec: Math.round(minutes * 60),
        cylType: str(calibration?.cylType) || str(owner?.cylType),
        source: str(calibration?.source),
        measuredAt: numOrNull(calibration?.time)
      });
    }
  }
  return rows;
}

/**
 * 构造 duty[]：duty_v2 优先，缺失时回退 duty.json 的 data.dutySchedule.entries。
 *
 * @param {object|null} dutyPayload 值班载荷
 * @returns {Array} 契约 duty[]
 */
function buildDuty(dutyPayload) {
  const entries = dutyPayload?.data?.dutySchedule?.entries;
  if (!entries || typeof entries !== 'object') {
    return [];
  }
  return Object.keys(entries)
    .map((key) => {
      const entry = entries[key] || {};
      return {
        date: str(entry.date) || key,
        mainOfficerName: str(entry.mainOfficer),
        deputyOfficerName: str(entry.deputyOfficer),
        note: str(entry.note)
      };
    })
    .sort((a, b) => (a.date < b.date ? -1 : 1));
}

/**
 * 构造 transfers[]：transfers.json 的 data.profileTransferRequests（键名已现场探测）。
 *
 * @param {object|null} transfersPayload 调档载荷
 * @returns {Array} 契约 transfers[]
 */
function buildTransfers(transfersPayload) {
  const data = transfersPayload?.data;
  const requests = Array.isArray(data?.profileTransferRequests)
    ? data.profileTransferRequests
    : Array.isArray(data?.requests)
      ? data.requests
      : Array.isArray(data?.list)
        ? data.list
        : [];
  return requests.map((request) => ({
    fromStation: str(request.fromStation),
    toStation: str(request.toStation),
    sourceProfileId: request.sourceProfileId ?? null,
    name: str(request.profileName),
    nfcTag: str(request.nfcTag),
    status: str(request.status),
    createdAt: numOrNull(request.createdAt),
    updatedAt: numOrNull(request.updatedAt),
    reviewedAt: numOrNull(request.reviewedAt),
    reviewer: str(request.reviewer),
    // request.profileId 是调动后新档案号；request.profile 里的 phone 不外带（电话只走 profiles[]）。
    newProfileId: request.profileId ?? null
  }));
}

/**
 * 构造 settings：snapshot_v2.data.settings → 契约字段。
 *
 * @param {object|null} settings 云端 settings
 * @returns {object|null} 契约 settings
 */
function buildSettings(settings) {
  if (!settings || typeof settings !== 'object') {
    return null;
  }
  return {
    warnPressure: numOrNull(settings.warnPressure),
    dangerPressure: numOrNull(settings.dangerPressure),
    warnTimeMin: numOrNull(settings.warnTime),
    dangerTimeMin: numOrNull(settings.dangerTime),
    workLevel: str(settings.workLevel),
    scene: str(settings.scene),
    voiceEnabled: normBool(settings.voiceAlert)
  };
}

// ---------------------------------------------------------------------------
// 单站抽取
// ---------------------------------------------------------------------------

/**
 * 抽取单个云端站。
 *
 * @param {object} station 归并后的站（含 name/stationKey/files/payloads）
 * @param {object} context 运行上下文
 * @returns {Promise<{line:object, stats:object}>} 契约行与计数
 */
async function extractCloudStation(station, context) {
  const { stationKey, name, files } = station;
  const base = `${CLOUD_PREFIX}${stationKey}/`;
  const payloads = { ...(station.payloads || {}) };

  const index = payloads['persons/index.json'] || null;
  const alive = Array.isArray(index?.people) ? index.people : [];
  const byPid = new Map();
  for (const person of alive) {
    if (person && person.id !== undefined && person.id !== null) {
      byPid.set(String(person.id), person);
    }
  }

  // 1) 只取 index.people 列到的 pid 的 profile.json（明文），其余历史 profile 全部跳过。
  //    实测有相当一部分在册 pid 没有 profile.json（404，旧版只在采样后才写该文件），
  //    这种情况用 index.people 的同构条目兜底，不算失败。
  const pids = [...byPid.keys()];
  let profileFileMissing = 0;
  const profileResults = await mapLimit(pids, CONCURRENCY, async (pid) => {
    const key = `${base}persons/${pid}/profile.json`;
    try {
      const payload = await fetchJson(key);
      fetchedProfileObjects += 1;
      return payload?.profile ? payload.profile : payload;
    } catch (e) {
      const message = String(e?.message || e);
      if (/404|NoSuchKey/.test(message)) {
        profileFileMissing += 1;
        return null;
      }
      profileFileMissing += 1;
      recordFailure(name, 'profile', `${pid}: ${message}`);
      return null;
    }
  });
  const profileById = new Map();
  const profileByName = new Map();
  const profiles = [];
  let legacyIdMissing = 0;
  let cylTypeMissing = 0;
  pids.forEach((pid, i) => {
    // people[] 是在册权威；profile.json 缺失时直接用 people 同构条目，保证花名册不丢人。
    const entry = toProfileEntry(mergePerson(byPid.get(pid), profileResults[i]));
    if (entry.legacyId === null || entry.legacyId === '') {
      legacyIdMissing += 1;
      recordFailure(name, 'legacyId_missing', `${pid} ${entry.name}`);
    }
    if (!entry.cylType) {
      cylTypeMissing += 1;
      recordFailure(name, 'cylType_missing', `${entry.legacyId} ${entry.name}`);
    }
    profiles.push(entry);
    profileById.set(pid, entry);
    if (entry.name && !profileByName.has(entry.name)) {
      profileByName.set(entry.name, entry);
    }
  });

  // 2) 站级聚合文件：缺一个不影响其它字段。
  async function load(fileName, stage) {
    if (payloads[fileName]) {
      return payloads[fileName];
    }
    const key = files[fileName];
    if (!key) {
      return null;
    }
    try {
      const payload = await fetchJson(key);
      payloads[fileName] = payload;
      return payload;
    } catch (e) {
      recordFailure(station.name, stage, e);
      return null;
    }
  }

  const dutyV2 = await load('duty_v2.json', 'duty_v2');
  const dutyV1 = dutyV2 ? null : await load('duty.json', 'duty');
  const scba = await load('scba_samples.json', 'scba_samples');
  const transfers = await load('transfers.json', 'transfers');
  const snapshotV2 = await load('snapshot_v2.json', 'snapshot_v2');

  const settings = snapshotV2?.data?.settings || null;
  const cloudData = snapshotV2?.data || null;
  const bundleStation = bundleStationOf(context.bundle, station.name);
  const { groups, memberSource, groupSources, localGroupNames, appendedGroups, duplicateMemberCount, bundleNamesMatchedLocal, bundleNamesMatchedCloud } = buildGroups({
    station: name,
    settings,
    cloudData,
    bundleStation,
    profileByName
  });

  const scenarioPoints = Array.isArray(scba?.profiles)
    ? scba.profiles.reduce((sum, sampled) => sum + (Array.isArray(sampled?.scbaScenarioPoints) ? sampled.scbaScenarioPoints.length : 0), 0)
    : 0;

  const line = {
    station: name,
    stationKey,
    source: 'cloud',
    indexUpdatedAt: numOrNull(index?.updatedAt),
    profiles,
    deletedKeys: Array.isArray(index?.deletedProfileKeys) ? index.deletedProfileKeys.slice() : [],
    groups,
    calibrations: buildCalibrations(scba, profileById, name),
    duty: buildDuty(dutyV2 || dutyV1),
    transfers: buildTransfers(transfers),
    settings: buildSettings(settings),
    operationMode: str(
      snapshotV2?.operationMode ?? snapshotV2?.data?.operationMode ?? snapshotV2?.data?.settings?.operationMode
    ),
    groupTasks: buildGroupTasks(settings, groups)
  };

  return {
    line,
    stats: {
      stationKey,
      peopleCount: alive.length,
      profilesAlive: profiles.length,
      profilesSkipped: alive.length - profiles.length,
      profileFileMissing,
      legacyIdMissing,
      cylTypeMissing,
      groups: line.groups.length,
      groupsWithMembers: line.groups.filter((group) => group.members.length > 0).length,
      groupMembers: line.groups.reduce((sum, group) => sum + group.members.length, 0),
      groupMemberSource: memberSource,
      groupSources,
      localGroupNames,
      appendedGroups,
      duplicateMemberCount,
      groupTasks: line.groupTasks.length,
      bundleNamesMatchedLocal,
      bundleNamesMatchedCloud,
      dutyDays: line.duty.length,
      calibrations: line.calibrations.length,
      scbaSampleCount: numOrNull(scba?.sampleCount),
      scbaScenarioPoints: scenarioPoints,
      transfers: line.transfers.length,
      deletedKeys: line.deletedKeys.length,
      settings: line.settings !== null,
      operationMode: line.operationMode,
      indexUpdatedAt: line.indexUpdatedAt,
      files: {
        index: Boolean(station.indexKey),
        dutyV2: Boolean(files['duty_v2.json']),
        duty: Boolean(files['duty.json']),
        scbaSamples: Boolean(files['scba_samples.json']),
        transfers: Boolean(files['transfers.json']),
        snapshotV2: Boolean(files['snapshot_v2.json'])
      }
    }
  };
}

/**
 * 兜底：云端无数据的站，用 out/bundle-core.json 的 fireStationData 种子构造一行。
 * bundle 无 legacyId，profiles[].legacyId 输出 null（Java 侧按姓名/NFC upsert）。
 *
 * @param {string} name 站名
 * @param {object} bundleStation fireStationData[站]
 * @returns {{line:object, stats:object}} 契约行与计数
 */
function extractBundleStation(name, bundleStation) {
  const roster = Array.isArray(bundleStation?.roster) ? bundleStation.roster : [];
  const profiles = roster.map((person) => ({
    legacyId: person?.id ?? person?.legacyId ?? null,
    name: str(person?.name),
    role: str(person?.title ?? person?.role),
    rank: str(person?.rank),
    phone: str(person?.phone),
    cylType: str(person?.cylType),
    height: numOrNull(person?.height),
    weight: numOrNull(person?.weight),
    age: numOrNull(person?.age),
    nfcTag: str(person?.nfcTag),
    morningPressure: numOrNull(person?.morningPressure)
  }));
  const profileByName = new Map();
  for (const profile of profiles) {
    if (profile.name && !profileByName.has(profile.name)) {
      profileByName.set(profile.name, profile);
    }
  }
  const bundleGroups = Array.isArray(bundleStation?.groups) ? bundleStation.groups : [];
  const groups = bundleGroups
    .filter((group) => str(group?.name))
    .map((group, index) => {
      const members = [];
      for (const memberName of normalizeMemberNames(group.members || group.memberNames)) {
        const profile = profileByName.get(memberName);
        if (!profile) {
          recordFailure(name, 'member_unresolved', `${str(group.name)}/${memberName}`);
          continue;
        }
        members.push({ legacyId: profile.legacyId, roleInGroup: str(group?.roles?.[memberName]) });
      }
      return { name: str(group.name), sortNo: index + 1, members };
    });

  const line = {
    station: name,
    stationKey: encodeStationKey(name),
    source: 'bundle',
    indexUpdatedAt: null,
    profiles,
    deletedKeys: [],
    groups,
    calibrations: [],
    duty: [],
    transfers: [],
    settings: null,
    operationMode: '',
    groupTasks: groups.map((group) => ({ name: group.name, task: '', location: '' }))
  };
  return {
    line,
    stats: {
      stationKey: line.stationKey,
      profilesAlive: profiles.length,
      profilesSkipped: 0,
      groups: groups.length,
      groupMembers: groups.reduce((sum, group) => sum + group.members.length, 0),
      groupMemberSource: 'bundle',
      dutyDays: 0,
      calibrations: 0,
      scbaSampleCount: null,
      transfers: 0,
      deletedKeys: 0,
      settings: false,
      operationMode: '',
      indexUpdatedAt: null,
      source: 'bundle'
    }
  };
}

// ---------------------------------------------------------------------------
// 主流程
// ---------------------------------------------------------------------------

/**
 * 主流程。
 *
 * @returns {Promise<void>}
 */
async function main() {
  const startedAt = Date.now();
  const args = parseArgs(process.argv.slice(2));
  mkdirSync(OUT_DIR, { recursive: true });

  log('=== 云端 ETL：R2 → out/roster.ndjson（README 3.4 契约）===');
  log(`产物目录：${OUT_DIR}`);
  log(`过滤条件：only=${args.only ? args.only.join(',') : '(全部站)'} limit=${args.limit || 0} 并发=${CONCURRENCY}`);

  // 1) 一次 listAll，按站归并（只保留契约需要的键）。
  const { candidates } = await indexCloud(args);

  // 2) 解析站名（index.station 优先），并缓存已读 payload 避免重复 GetObject。
  const resolved = await mapLimit(candidates, CONCURRENCY, async (candidate) => {
    try {
      const { name, payloads } = await resolveStationName(candidate);
      return { ...candidate, name, payloads };
    } catch (e) {
      const fallbackName = decodeStationKey(candidate.stationKey);
      recordFailure(fallbackName, 'station_name', e);
      return { ...candidate, name: fallbackName, payloads: {} };
    }
  });

  // 2.5) 排除非站点脏键（如 station_ZGVmYXVsdA → "default"）。
  const usable = resolved.filter((station) => {
    const normalized = String(station.name || '').trim().toLowerCase();
    if (EXCLUDED_STATION_NAMES.has(normalized)) {
      log(`跳过非站点键：${station.stationKey}（解出站名「${station.name}」，不是真实消防站）`);
      return false;
    }
    return true;
  });

  // 3) 同名站去重（存在同一站名多个 stationKey 的历史脏键）：保留证据最全的那个。
  const byName = new Map();
  for (const station of usable) {
    if (!station.name) {
      recordFailure(station.stationKey, 'station_name', '站名为空，已跳过');
      continue;
    }
    const score = (station.indexKey ? 1000 : 0) + Object.keys(station.files).length * 10 + station.objectCount;
    const existing = byName.get(station.name);
    if (!existing) {
      byName.set(station.name, { ...station, score });
      continue;
    }
    const winner = existing.score >= score ? existing : { ...station, score };
    const loser = existing.score >= score ? station : existing;
    byName.set(station.name, winner);
    recordFailure(
      station.name,
      'station_key_duplicate',
      `保留 ${winner.stationKey}（对象 ${winner.objectCount} 个），丢弃 ${loser.stationKey}（对象 ${loser.objectCount} 个）`
    );
  }

  // 4) 兜底种子。
  const bundlePath = join(OUT_DIR, 'bundle-core.json');
  let bundle = null;
  if (existsSync(bundlePath)) {
    try {
      bundle = JSON.parse(readFileSync(bundlePath, 'utf8'));
      log(`兜底种子：${bundlePath}（仅用于云端无数据的站）`);
    } catch (e) {
      warn(`bundle-core.json 解析失败，兜底能力禁用：${e.message}`);
    }
  } else {
    warn(`未找到 ${bundlePath}（Lead 尚未生成）。仅影响「云端无数据」的站，本次按跳过处理。`);
  }

  // 5) 抽取计划 = 云端站 ∪ 仅 bundle 站；再按 --only / --limit 过滤。
  //    先建全集再过滤，避免 --only 抽测时把兜底站也混进产物。
  const cloudStations = [...byName.values()].sort((a, b) => String(a.name).localeCompare(String(b.name), 'zh'));
  const cloudNames = new Set(cloudStations.map((station) => station.name));
  const bundleOnlyNames = bundle ? collectBundleStationNames(bundle).filter((name) => !cloudNames.has(name)) : [];
  let plan = [
    ...cloudStations.map((station) => ({ kind: 'cloud', name: station.name, stationKey: station.stationKey, station })),
    ...bundleOnlyNames.map((name) => ({ kind: 'bundle', name, stationKey: encodeStationKey(name), station: null }))
  ].sort((a, b) => String(a.name).localeCompare(String(b.name), 'zh'));

  if (args.only) {
    const wanted = new Set(args.only);
    plan = plan.filter((item) => wanted.has(item.name) || wanted.has(item.stationKey));
    const missing = args.only.filter(
      (name) => !plan.some((item) => item.name === name || item.stationKey === name)
    );
    for (const name of missing) {
      warn(`--only 指定的站未在云端/bundle 中找到：${name}`);
    }
  }
  if (args.limit > 0) {
    plan = plan.slice(0, args.limit);
  }
  log(
    `待抽取站数：${plan.length}（云端 ${plan.filter((item) => item.kind === 'cloud').length}，` +
      `bundle 兜底 ${plan.filter((item) => item.kind === 'bundle').length}）`
  );

  const context = { bundle };

  // 6) 逐站抽取（单站失败只记 failures，不中断整批）。
  const rosterLines = [];
  const stationMap = {};
  for (const item of plan) {
    try {
      const { line, stats } =
        item.kind === 'bundle'
          ? extractBundleStation(item.name, bundleStationOf(bundle, item.name))
          : await extractCloudStation(item.station, context);
      if (args.crossCheck && item.kind === 'cloud') {
        stats.crossCheck = await crossCheckThresholds(item.station, line.settings);
      }
      rosterLines.push(line);
      stationMap[line.station] = stats;
    } catch (e) {
      recordFailure(item.name, item.kind === 'bundle' ? 'bundle' : 'station', e);
      warn(`站失败：${item.name} → ${e.message}`);
    }
  }

  // 7) 落盘。
  const rosterPath = join(OUT_DIR, 'roster.ndjson');
  writeFileSync(rosterPath, rosterLines.map((line) => JSON.stringify(line)).join('\n') + (rosterLines.length ? '\n' : ''), 'utf8');
  const mapPath = join(OUT_DIR, 'cloud-station-map.json');
  writeFileSync(mapPath, `${JSON.stringify(stationMap, null, 2)}\n`, 'utf8');
  const failuresPath = join(OUT_DIR, 'cloud-failures.ndjson');
  writeFileSync(failuresPath, failures.map((row) => JSON.stringify(row)).join('\n') + (failures.length ? '\n' : ''), 'utf8');

  // 8) 汇总。
  const elapsedMs = Date.now() - startedAt;
  log('');
  log(`roster.ndjson        ${rosterLines.length} 行 → ${rosterPath}`);
  log(`cloud-station-map    ${Object.keys(stationMap).length} 站 → ${mapPath}`);
  log(`cloud-failures       ${failures.length} 条 → ${failuresPath}`);
  log(`跳过历史 profile     ${historicalProfileObjects - fetchedProfileObjects} 个（只取在册 pid 的 ${fetchedProfileObjects} 个）`);
  log(`耗时                 ${(elapsedMs / 1000).toFixed(1)} s`);
  printStationTable(rosterLines, stationMap);
  printSummary(rosterLines, stationMap);
  printAnomalies(stationMap);
  log('');
  log('=== 完成 ===');
}

/**
 * 交叉核对：snapshot_v2 阈值 与 runtime.json 阈值是否一致（只读，不迁移）。
 *
 * @param {object} station 站
 * @param {object|null} settings 已抽取的 settings
 * @returns {Promise<object|null>} 差异描述
 */
async function crossCheckThresholds(station, settings) {
  const key = station.files['runtime.json'];
  if (!key || !settings) {
    return null;
  }
  try {
    const runtime = await fetchJson(key);
    const live = runtime?.data?.settings || {};
    const diffs = [];
    const pairs = [
      ['warnPressure', 'warnPressure'],
      ['dangerPressure', 'dangerPressure'],
      ['warnTimeMin', 'warnTime'],
      ['dangerTimeMin', 'dangerTime']
    ];
    for (const [left, right] of pairs) {
      if (settings[left] !== numOrNull(live[right])) {
        diffs.push(`${left}: snapshot_v2=${settings[left]} runtime=${numOrNull(live[right])}`);
      }
    }
    if (diffs.length > 0) {
      recordFailure(station.name, 'cross_check', diffs.join('; '));
      return { ok: false, diffs };
    }
    return { ok: true, diffs: [] };
  } catch (e) {
    recordFailure(station.name, 'cross_check', e);
    return null;
  }
}

/**
 * 收集 bundle-core.json 里的站名。
 *
 * @param {object} bundle bundle 内容
 * @returns {string[]} 站名
 */
function collectBundleStationNames(bundle) {
  for (const container of [bundle?.fireStationData, bundle?.stations, bundle]) {
    if (container && typeof container === 'object') {
      const names = Object.keys(container).filter((name) => {
        const value = container[name];
        return value && typeof value === 'object' && (Array.isArray(value.roster) || Array.isArray(value.groups));
      });
      if (names.length > 0) {
        return names;
      }
    }
  }
  return [];
}

/**
 * 打印每站对账表。
 *
 * @param {Array} rosterLines roster 行
 * @param {object} stationMap 计数
 */
function printStationTable(rosterLines, stationMap) {
  const header = [
    '站名', 'source', 'people', 'profiles', 'legacyId空', 'cylType空',
    'groups', 'members', 'duty', 'cal', 'transfers', 'settings', 'mode'
  ];
  const rows = rosterLines.map((line) => {
    const stats = stationMap[line.station] || {};
    return [
      line.station,
      line.source,
      String(stats.peopleCount ?? line.profiles.length),
      String(line.profiles.length),
      String(stats.legacyIdMissing ?? '?'),
      String(stats.cylTypeMissing ?? '?'),
      String(line.groups.length),
      String(line.groups.reduce((sum, group) => sum + group.members.length, 0)),
      String(line.duty.length),
      String(line.calibrations.length),
      String(line.transfers.length),
      line.settings ? 'Y' : '-',
      line.operationMode || '-'
    ];
  });
  log('');
  log('逐站对账：');
  printAligned([header, ...rows]);
}

/**
 * 打印汇总（总数、根因线索、失败分布）。
 *
 * @param {Array} rosterLines roster 行
 * @param {object} stationMap 计数
 */
function printSummary(rosterLines, stationMap) {
  const stats = Object.values(stationMap);
  const total = (pick) => stats.reduce((sum, item) => sum + (Number(pick(item)) || 0), 0);
  const bySource = rosterLines.reduce((acc, line) => {
    acc[line.source] = (acc[line.source] || 0) + 1;
    return acc;
  }, {});
  const groupsBySource = { bundle: 0, cloud: 0, none: 0 };
  for (const item of stats) {
    for (const [key, count] of Object.entries(item.groupSources || {})) {
      groupsBySource[key] = (groupsBySource[key] || 0) + count;
    }
  }

  log('');
  log('汇总：');
  log(`  站数 ${rosterLines.length}（${Object.entries(bySource).map(([k, v]) => `${k}=${v}`).join(' ')}）`);
  log(`  people 合计 ${total((s) => s.peopleCount)}，profiles 合计 ${total((s) => s.profilesAlive)}`);
  log(`  legacyId 为空 ${total((s) => s.legacyIdMissing)} 人，cylType 为空 ${total((s) => s.cylTypeMissing)} 人`);
  log(`  duty 合计 ${total((s) => s.dutyDays)} 天，calibrations 合计 ${total((s) => s.calibrations)} 条`);
  log(`  transfers 合计 ${total((s) => s.transfers)} 条，settings 非空 ${stats.filter((s) => s.settings).length} 站，mode 非空 ${stats.filter((s) => s.operationMode).length} 站`);
  log(`  groups 合计 ${total((s) => s.groups)} 个，成员合计 ${total((s) => s.groupMembers)} 人（跨组重复被拦 ${total((s) => s.duplicateMemberCount)} 人次）`);
  log(`  groupTasks 合计 ${total((s) => s.groupTasks)} 条（与 groups 一一对应）`);
  log(`  组内成员来源：bundle 命中 ${groupsBySource.bundle} 组，cloud 命中 ${groupsBySource.cloud} 组，无成员组 ${groupsBySource.none} 个`);
  log(`  组名来源：localGroupTasks 声明 ${total((s) => s.localGroupNames)} 个 + 补云端组名 ${total((s) => s.appendedGroups)} 个`);
  log(
    `  内置种子组名与云端同名命中：localGroupTasks ${total((s) => s.bundleNamesMatchedLocal)} 个 / 云端 groups ` +
      `${total((s) => s.bundleNamesMatchedCloud)} 个（另一套命名，不作组名来源，只作同名组成员兜底）`
  );

  const calSum = total((s) => s.calibrations);
  const scenarioSum = total((s) => s.scbaScenarioPoints);
  const matched = stats.filter((s) => s.scbaSampleCount === s.calibrations + s.scbaScenarioPoints).length;
  log(
    `  空呼样本口径：calibrations ${calSum} + 场景采样点 ${scenarioSum} = sampleCount 合计 ${total((s) => s.scbaSampleCount)}；` +
      `逐站 sampleCount === calibrations + scenarioPoints 吻合 ${matched}/${stats.length} 站`
  );

  const stageCount = failures.reduce((acc, row) => {
    acc[row.stage] = (acc[row.stage] || 0) + 1;
    return acc;
  }, {});
  const stageText = Object.entries(stageCount).map(([k, v]) => `${k}=${v}`).join(' ') || '无';
  log(`  failures：${failures.length} 条（${stageText}）`);
}

/**
 * 打印异常站（profiles=0 或 duty=0）与组内成员缺失。
 *
 * @param {object} stationMap 计数
 */
function printAnomalies(stationMap) {
  const anomalies = Object.entries(stationMap).filter(
    ([, stats]) => stats.profilesAlive === 0 || stats.dutyDays === 0
  );
  log('');
  if (anomalies.length === 0) {
    log('异常站（profiles=0 或 duty=0）：无');
    return;
  }
  log('异常站（profiles=0 或 duty=0）：');
  for (const [name, stats] of anomalies) {
    log(`  - ${name}：profiles=${stats.profilesAlive} duty=${stats.dutyDays} groups=${stats.groups} cal=${stats.calibrations}`);
  }
}

/**
 * 按显示宽度对齐打印表格（中文按 2 列宽）。
 *
 * @param {string[][]} rows 行
 */
function printAligned(rows) {
  const width = (text) => [...String(text)].reduce((sum, ch) => sum + (ch.charCodeAt(0) > 0x2e80 ? 2 : 1), 0);
  const columns = Math.max(...rows.map((row) => row.length));
  const widths = new Array(columns).fill(0);
  for (const row of rows) {
    for (let i = 0; i < row.length; i += 1) {
      widths[i] = Math.max(widths[i], width(row[i]));
    }
  }
  for (const row of rows) {
    const line = row
      .map((cell, i) => {
        const padding = widths[i] - width(cell);
        return `${cell}${' '.repeat(Math.max(0, padding))}`;
      })
      .join('  ');
    log(`  ${line.trimEnd()}`);
  }
}

main().catch((e) => {
  console.error('[FATAL]', e);
  process.exitCode = 1;
});
