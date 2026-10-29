#!/usr/bin/env node
/**
 * out/water.ndjson → 水源检查表 MySQL 补丁（0009_legacy_seed/0001_water_source.sql）。
 *
 * 输入：LEGACY_MIGRATION_OUT/out/water.ndjson（由 extract-bundle-water.mjs 生成）
 * 输出：ccds-bootstrap/scripts/db/patch/rdb/0009_legacy_seed/0001_water_source.sql
 * 只读参考：
 *   0001_init/0002_seed_org_account.sql    ccds_station 站名/name_norm 种子（解析与去重）
 *   0009_legacy_seed/0002_water_fields.sql B1 批次新增的结构化列（生成前必须存在且列齐全）
 *
 * 写入列（19 个）：
 *   station_id, name, type, status, address, lng, lat, extra_json,
 *   setup, pipe_diameter, pressure_mpa, flow_lps, supply_form, add_height_m,
 *   usable, reason_text, reason_code, part_code, is_repair
 *
 * 语义要点：
 *   extra_json 只放人类备注原文（旧版 notes 字段），空为 NULL —— 绝不写 JSON。
 *     Java 侧 WaterViewTranslator 直接把 extra_json 当「备注」返回，写 JSON 会变成前端可见回归。
 *   legacyId / photoRef / locationAccuracy / photoLocation 属机器字段，不入库，保留在 out/water.ndjson，
 *     后续附件登记（Java 侧）继续用 NDJSON。
 *
 * 口径（按客户规则，顺序不可颠倒）：
 *   type      ：含「水鹤」→ crane；否则 type/setup/address/name 任一含「地下」→ underground；其余 ground。
 *               先判水鹤再判地下，避免「地下消火栓」被误判为水鹤；setup 参与判定，避免只凭名称误判。
 *   status    ：usable 含「不好用」或 status 含「报修」或 repair == 「是」→ repair，其余 active。
 *               三条判据与 usable 口径一致：643 条上三者集合完全相同（各 114 条），
 *               以 usable 为主口径，status/repair 作为兜底，防止旧版单字段缺失时状态丢失。
 *   setup     ：含「地下」→ under，否则 above。
 *   supply_form：含「环」→ ring，含「枝」→ branch，其余 NULL。
 *   usable    ：含「不好用」→ bad，否则 good。
 *   is_repair ：repair == 「是」→ 1，== 「否」→ 0，其余 NULL。
 *   reason_code：仅 usable=bad 的行归类，见 classifyReason（顺序即优先级，兜底 damaged）。
 *
 * 幂等：每条记录生成 SET @water_sid + INSERT ... SELECT ... FROM DUAL WHERE NOT EXISTS，
 *       已存在 (station_id, name) 的行不更新、不覆盖；站点缺失时 @water_sid 为 NULL 直接跳过。
 *
 * 运行：node build-water-sql.mjs（Node >= 20，ESM、零第三方依赖）
 */

import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(SCRIPT_DIR, '../../..');
const OUT_ROOT = resolve(process.env.LEGACY_MIGRATION_OUT || join(tmpdir(), 'ccds-migration'));
const WATER_NDJSON = join(OUT_ROOT, 'out', 'water.ndjson');
const STATION_SEED_SQL = join(REPO_ROOT, 'ccds-bootstrap', 'scripts', 'db', 'patch', 'rdb', '0001_init', '0002_seed_org_account.sql');
const SQL_DIR = join(REPO_ROOT, 'ccds-bootstrap', 'scripts', 'db', 'patch', 'rdb', '0009_legacy_seed');
const FIELD_DDL_SQL = join(SQL_DIR, '0002_water_fields.sql');
const SQL_FILE = join(SQL_DIR, '0001_water_source.sql');

/** B1 批次新增列：生成前校验 DDL 里都有，避免补丁与表结构脱节。 */
const REQUIRED_FIELD_COLUMNS = [
  'setup',
  'pipe_diameter',
  'pressure_mpa',
  'flow_lps',
  'supply_form',
  'add_height_m',
  'usable',
  'reason_text',
  'reason_code',
  'part_code',
  'is_repair'
];

const INSERT_COLUMNS = [
  'station_id',
  'name',
  'type',
  'status',
  'address',
  'lng',
  'lat',
  'extra_json',
  'setup',
  'pipe_diameter',
  'pressure_mpa',
  'flow_lps',
  'supply_form',
  'add_height_m',
  'usable',
  'reason_text',
  'reason_code',
  'part_code',
  'is_repair'
].join(', ');

const NAME_MAX = 64;
const ADDRESS_MAX = 128;
const REASON_TEXT_MAX = 255;
const MAX_NAME_SEQ = 999;

/**
 * 九原因码归类：数组顺序即优先级（客户 1.3 口径）。
 * 客户给的顺序：无水 → 漏水 → 压力不足 → 锈蚀 → 埋压 → 圈占 → 挪用 → 塌井 → 兜底毁坏。
 */
const REASON_RULES = [
  { code: 'no_water', keywords: ['无水', '没水', '管网内没', '管道无水', '管网无'] },
  { code: 'leak', keywords: ['漏', '锁不住水'] },
  { code: 'low_pressure', keywords: ['压力'] },
  { code: 'rust', keywords: ['锈', '绣'] },
  { code: 'buried', keywords: ['埋压', '被埋'] },
  { code: 'occupied', keywords: ['圈占', '围挡'] },
  { code: 'diverted', keywords: ['挪用'] },
  { code: 'collapsed_well', keywords: ['塌井', '井塌'] }
];
const REASON_FALLBACK = 'damaged';

/** 读 NDJSON，遇到坏行直接报行号。 */
function readNdjson(path) {
  if (!existsSync(path)) {
    throw new Error(`缺少输入 ${path}，请先运行 extract-bundle-water.mjs`);
  }
  const rows = [];
  const lines = readFileSync(path, 'utf8').split('\n');
  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index].trim();
    if (!line) {
      continue;
    }
    try {
      rows.push(JSON.parse(line));
    } catch (error) {
      throw new Error(`第 ${index + 1} 行不是合法 JSON：${error.message}`);
    }
  }
  return rows;
}

/**
 * 解析 0002_seed_org_account.sql 的 ccds_station 种子，建立「站名/name_norm → id」索引。
 * 与生成的 SQL 里 (SELECT id FROM ccds_station WHERE name = 'X' OR name_norm = 'X' LIMIT 1) 语义一致。
 *
 * @param {string} text 种子 SQL 全文
 * @returns {{byName: Map<string, number>, byNorm: Map<string, number>}}
 */
function parseStationSeed(text) {
  const byName = new Map();
  const byNorm = new Map();
  const rows = text.matchAll(/^\s*\(\s*(\d+)\s*,\s*(?:NULL|\d+)\s*,\s*'((?:[^']|'')*)'\s*,\s*'((?:[^']|'')*)'/gm);
  for (const match of rows) {
    const id = Number(match[1]);
    const name = match[2].replace(/''/g, "'");
    const nameNorm = match[3].replace(/''/g, "'");
    byName.set(name, id);
    if (!byNorm.has(nameNorm)) {
      byNorm.set(nameNorm, id);
    }
  }
  if (byName.size === 0) {
    throw new Error(`未从 ${STATION_SEED_SQL} 解析到任何 ccds_station 种子`);
  }
  return { byName, byNorm };
}

/** 站名 → 站 id；解析不到返回 null（调用方跳过，不猜）。 */
function resolveStationId(seed, stationName) {
  if (seed.byName.has(stationName)) {
    return seed.byName.get(stationName);
  }
  if (seed.byNorm.has(stationName)) {
    return seed.byNorm.get(stationName);
  }
  return null;
}

/** 生成前确认 B1 结构化列已在 0002_water_fields.sql 中定义。 */
function assertFieldColumnsDefined() {
  if (!existsSync(FIELD_DDL_SQL)) {
    throw new Error(`缺少 ${FIELD_DDL_SQL}（B1 结构化字段 DDL），请先创建再生成补丁`);
  }
  const text = readFileSync(FIELD_DDL_SQL, 'utf8');
  const missing = REQUIRED_FIELD_COLUMNS.filter((column) => !new RegExp(`\\b${column}\\b`).test(text));
  if (missing.length > 0) {
    throw new Error(`0002_water_fields.sql 缺少列：${missing.join(', ')}`);
  }
}

/** 类型归一：先水鹤，再地下，其余地上。 */
function normalizeType(row) {
  if (row.type.includes('水鹤')) {
    return 'crane';
  }
  const fields = [row.type, row.setup, row.address, row.name];
  if (fields.some((value) => value.includes('地下'))) {
    return 'underground';
  }
  return 'ground';
}

/** 状态归一：三条判据任一命中即 repair，与 usable 口径一致。 */
function normalizeStatus(row) {
  const badUsable = row.usable.includes('不好用');
  const reported = row.status.includes('报修');
  const repaired = row.repair === '是';
  return badUsable || reported || repaired ? 'repair' : 'active';
}

/** 设置方式：含「地下」为 under，其余 above。 */
function setupCode(row) {
  return row.setup.includes('地下') ? 'under' : 'above';
}

/** 给水形式：含环为 ring，含枝为 branch，其余 NULL。 */
function supplyCode(row) {
  if (row.waterSupply.includes('环')) {
    return 'ring';
  }
  if (row.waterSupply.includes('枝')) {
    return 'branch';
  }
  return null;
}

/** 可使用：含「不好用」为 bad，其余 good。 */
function usableCode(row) {
  return row.usable.includes('不好用') ? 'bad' : 'good';
}

/** 是否报修：是→1，否→0，空→NULL。 */
function isRepairValue(row) {
  if (row.repair === '是') {
    return 1;
  }
  if (row.repair === '否') {
    return 0;
  }
  return null;
}

/**
 * 九原因码归类（纯函数）。仅 usable=bad 的行才调用。
 * 顺序即优先级；原因原文为空时返回 null（无因可归，不硬塞 damaged）。
 *
 * @param {string} text 旧版 reason 原文
 * @returns {string|null} 原因码
 */
function classifyReason(text) {
  const reason = String(text ?? '');
  if (!reason) {
    return null;
  }
  for (const rule of REASON_RULES) {
    if (rule.keywords.some((keyword) => reason.includes(keyword))) {
      return rule.code;
    }
  }
  return REASON_FALLBACK;
}

/** 按字符数截断（VARCHAR 计字符，不计字节）。 */
function truncateChars(value, max) {
  const chars = [...value];
  return chars.length <= max ? value : chars.slice(0, max).join('');
}

/**
 * MySQL 字符串转义：单引号翻倍；反斜杠、换行、回车、制表符、NUL、SUB 按 MySQL 转义序列写成可见转义，
 * 保证一条 INSERT 只占一行（旧版部分 address 里带原始换行）。
 * 注意：若目标库开启 NO_BACKSLASH_ESCAPES，需改为只翻单引号并保留原始控制字符。
 */
function sqlString(value) {
  const text = String(value)
    .replace(/\\/g, '\\\\')
    .replace(/\u0000/g, '\\0')
    .replace(/\u001a/g, '\\Z')
    .replace(/\n/g, '\\n')
    .replace(/\r/g, '\\r')
    .replace(/\t/g, '\\t')
    .replace(/'/g, "''");
  return `'${text}'`;
}

/** 可空字符串：空串写 NULL。 */
function sqlNullable(value) {
  return value ? sqlString(value) : 'NULL';
}

/** 空的数值列统计器。 */
function newNumberStats() {
  return { nulls: 0, rounded: 0, rejected: [] };
}

/**
 * 数值列 → SQL 字面量：空串/非数值/超范围写 NULL，合法值按列精度四舍五入。
 *
 * @param {string} value 原始值
 * @param {number} scale 小数位（0 表示整数列）
 * @param {number[]} range 允许范围 [min, max]
 * @param {string} label 列名（用于告警）
 * @param {object} stats 该列的统计器
 * @returns {string} SQL 字面量
 */
function numberSql(value, scale, range, label, stats) {
  if (!value) {
    stats.nulls += 1;
    return 'NULL';
  }
  const number = Number(value);
  if (!Number.isFinite(number) || number < range[0] || number > range[1]) {
    stats.rejected.push(`${label}=${JSON.stringify(value)}`);
    return 'NULL';
  }
  const decimals = (String(value).split('.')[1] || '').length;
  if (scale === 0 ? !Number.isInteger(number) : decimals > scale) {
    stats.rounded += 1;
  }
  return scale === 0 ? String(Math.round(number)) : number.toFixed(scale);
}

/** 坐标 → SQL 字面量：空串/非数值/超范围写 NULL，合法值四舍五入到 6 位。 */
function coordinateSql(value, limit, stats) {
  if (!value) {
    stats.empty += 1;
    return 'NULL';
  }
  const number = Number(value);
  if (!Number.isFinite(number) || Math.abs(number) > limit) {
    stats.invalid.push(value);
    return 'NULL';
  }
  if ((String(value).split('.')[1] || '').length > 6) {
    stats.rounded += 1;
  }
  return number.toFixed(6);
}

/**
 * 同站同名去重：撞 uk_water_station_name 时依次加 -2/-3…（仍受 64 字符上限约束）。
 * 就地写入 row.finalName。
 *
 * @param {Array<object>} rows 已解析站 id 的记录
 * @returns {Array<object>} 被改名的记录
 */
function assignUniqueNames(rows) {
  const used = new Set();
  const conflicts = [];
  for (const row of rows) {
    let name = truncateChars(row.name, NAME_MAX);
    let seq = 1;
    while (used.has(`${row.stationId}\u0000${name}`)) {
      seq += 1;
      if (seq > MAX_NAME_SEQ) {
        throw new Error(`同站同名过多：${row.station} / ${row.name}`);
      }
      const suffix = `-${seq}`;
      name = `${truncateChars(row.name, NAME_MAX - suffix.length)}${suffix}`;
    }
    if (name !== row.name) {
      conflicts.push({ station: row.station, legacyId: row.legacyId, from: row.name, to: name });
    }
    used.add(`${row.stationId}\u0000${name}`);
    row.finalName = name;
  }
  return conflicts;
}

/** 统计计数并降序输出。 */
function countBy(rows, selector) {
  const map = new Map();
  for (const row of rows) {
    const key = selector(row) ?? '<空>';
    map.set(key, (map.get(key) || 0) + 1);
  }
  return [...map.entries()].sort((left, right) => right[1] - left[1]);
}

/** 生成 SQL 文件头注释。 */
function buildHeader(written, skipped, generatedAt) {
  return [
    '-- 旧版 APK 内置水源检查表 → ccds_water_source（一次性迁移补丁，由脚本生成，勿手改）。',
    '-- 来源：tmp_apk/extracted/assets/water_inspection_manifest.js（window.WATER_INSPECTION_MANIFEST，643 条检查记录）',
    `-- 生成：migration/build-water-sql.mjs，${generatedAt}`,
    `-- 条数：写入 ${written} 条，跳过 ${skipped} 条（站名解析不到或名称为空，见脚本控制台输出）`,
    '--',
    '-- 执行顺序（0009_legacy_seed 内）：0000_landing_tables.sql → 0002_water_fields.sql → 本文件。',
    '--   ccds_station 种子来自 0001_init/0001_org_account.sql + 0002_seed_org_account.sql，必须先有站。',
    '-- 可重复执行：按 (station_id, name) 判重，已存在的行不更新、不覆盖；站点解析不到时该行自动跳过。',
    '--',
    '-- 写入列（19 个）：',
    '--   station_id, name, type, status, address, lng, lat, extra_json,',
    '--   setup, pipe_diameter, pressure_mpa, flow_lps, supply_form, add_height_m,',
    '--   usable, reason_text, reason_code, part_code, is_repair',
    '--',
    '-- 字段口径：',
    '--   type      ：含「水鹤」→ crane；否则 type/setup/address/name 任一含「地下」→ underground；其余 ground。',
    '--   status    ：usable 含「不好用」或 status 含「报修」或 repair=「是」→ repair，其余 active。',
    '--               三条判据与 usable 口径一致（旧版 643 条上三者各 114 条且集合相同），以 usable 为主、另两条兜底。',
    '--   setup     ：含「地下」→ under，否则 above（643 条实测 above 629 / under 14）。',
    '--   supply_form：waterSupply 含「环」→ ring，含「枝」→ branch，其余 NULL（实测 ring 581 / branch 62）。',
    '--   usable    ：含「不好用」→ bad，否则 good（实测 good 529 / bad 114）。',
    '--   is_repair ：repair=「是」→ 1，「否」→ 0，空 → NULL（实测 1:114 / 0:85 / NULL:444）。',
    '--   pipe_diameter：旧版 mm 原文转整数；pressure_mpa 3 位小数、flow_lps 2 位、add_height_m 2 位。',
    '--               空串/非数值/超范围一律 NULL（见脚本控制台逐列统计）。',
    '--   reason_text：旧版 reason 原文（≤255 字符），空 → NULL；不管好用与否都保留原文。',
    '--   reason_code：九原因码，仅 usable=bad 的行归类（好用的 NULL）；part_code：旧数据无部件信息，恒 NULL。',
    '--               九原因码按优先级判定（先命中先得，兜底 damaged）：',
    '--                 no_water(无水/没水/管网内没/管道无水/管网无) → leak(漏/锁不住水) → low_pressure(压力)',
    '--                 → rust(锈/绣) → buried(埋压/被埋) → occupied(圈占/围挡) → diverted(挪用)',
    '--                 → collapsed_well(塌井/井塌) → damaged(兜底：毁坏)',
    '--   extra_json：只放人类备注原文（旧版 notes），空 → NULL；本列被 Java 侧当「备注」直出，禁止写 JSON。',
    '--   legacyId / photoRef / locationAccuracy / photoLocation 属机器字段，不入库，保留在 out/water.ndjson。',
    '--   name   ：超过 64 字符截断；同站同名依次加 -2/-3，避免撞 uk_water_station_name。',
    '--   lng/lat：空串、非数值、超范围一律写 NULL；合法值四舍五入到 6 位小数。',
    '--   address：超过 128 字符截断；空串写 NULL。',
    '--   gmt_create/gmt_modified 由表默认值生成，不在此显式赋值。',
    '--',
    '-- 转义：字符串按 MySQL 默认 sql_mode 处理（单引号翻倍，反斜杠/换行/回车/制表符写成 \\\\ \\n \\r \\t，',
    '--       旧版个别 address 自带换行，转义后每条 INSERT 仍只占一行）。',
    '--       若目标库启用 NO_BACKSLASH_ESCAPES，需重新生成本文件。',
    '--',
    '-- 每条记录形如：SET @water_sid = (SELECT id FROM ccds_station WHERE name = \'<站名>\' OR name_norm = \'<站名>\' LIMIT 1);',
    '--             INSERT INTO ccds_water_source (...) SELECT @water_sid, ... FROM DUAL',
    '--             WHERE @water_sid IS NOT NULL AND NOT EXISTS (SELECT 1 FROM ccds_water_source w WHERE w.station_id = @water_sid AND w.name = \'<名称>\');',
    ''
  ].join('\n');
}

function main() {
  assertFieldColumnsDefined();
  const sourceRows = readNdjson(WATER_NDJSON);
  const seed = parseStationSeed(readFileSync(STATION_SEED_SQL, 'utf8'));

  const resolved = [];
  const skipped = [];
  for (const row of sourceRows) {
    if (!row.name) {
      skipped.push({ row, reason: '名称为空' });
      continue;
    }
    const stationId = resolveStationId(seed, row.station);
    if (stationId === null) {
      skipped.push({ row, reason: '站名解析不到' });
      continue;
    }
    resolved.push({ ...row, stationId });
  }
  const conflicts = assignUniqueNames(resolved);

  const coordStats = { empty: 0, invalid: [], rounded: 0 };
  const numberStats = {
    pipe_diameter: newNumberStats(),
    pressure_mpa: newNumberStats(),
    flow_lps: newNumberStats(),
    add_height_m: newNumberStats()
  };
  const controlChars = [];
  for (const row of resolved) {
    for (const [field, value] of Object.entries(row)) {
      if (typeof value === 'string' && /[\u0000-\u001f]/.test(value)) {
        controlChars.push(`${row.legacyId}.${field}=${JSON.stringify(value)}`);
      }
    }
  }

  const addressTruncated = [];
  const reasonTextTruncated = [];
  const extraJsonWithBrace = [];
  const unclassified = [];
  const reasonCodeRows = [];
  const lines = [];
  const stationTotals = new Map();
  for (const row of resolved) {
    stationTotals.set(row.station, (stationTotals.get(row.station) || 0) + 1);
  }
  let currentStation = null;

  for (const row of resolved) {
    const type = normalizeType(row);
    const status = normalizeStatus(row);
    const usable = usableCode(row);
    const address = truncateChars(row.address, ADDRESS_MAX);
    if (address !== row.address) {
      addressTruncated.push(`${row.station} / ${row.name}`);
    }
    const notes = row.notes;
    if (notes.includes('{')) {
      extraJsonWithBrace.push(`${row.legacyId}=${JSON.stringify(notes)}`);
    }
    const reasonText = truncateChars(row.reason, REASON_TEXT_MAX);
    if (reasonText !== row.reason) {
      reasonTextTruncated.push(`${row.legacyId}`);
    }
    const reasonCode = usable === 'bad' ? classifyReason(row.reason) : null;
    if (usable === 'bad' && reasonCode === REASON_FALLBACK) {
      unclassified.push({ legacyId: row.legacyId, reason: row.reason });
    }
    reasonCodeRows.push(reasonCode);

    const lngSql = coordinateSql(row.lng, 180, coordStats);
    const latSql = coordinateSql(row.lat, 90, coordStats);
    const repairSql = isRepairValue(row);

    if (row.station !== currentStation) {
      if (currentStation !== null) {
        lines.push('');
      }
      currentStation = row.station;
      lines.push(`-- ${row.station}（${stationTotals.get(row.station) || 0} 条，id=${row.stationId}）`);
    }
    lines.push(`SET @water_sid = (SELECT id FROM ccds_station WHERE name = ${sqlString(row.station)} OR name_norm = ${sqlString(row.station)} LIMIT 1);`);
    lines.push(
      `INSERT INTO ccds_water_source (${INSERT_COLUMNS}) `
      + `SELECT @water_sid, ${sqlString(row.finalName)}, ${sqlString(type)}, ${sqlString(status)}, `
      + `${sqlNullable(address)}, ${lngSql}, ${latSql}, ${sqlNullable(notes)}, `
      + `${sqlString(setupCode(row))}, `
      + `${numberSql(row.pipeDiameter, 0, [1, 10000], 'pipe_diameter', numberStats.pipe_diameter)}, `
      + `${numberSql(row.pressure, 3, [0, 99.999], 'pressure_mpa', numberStats.pressure_mpa)}, `
      + `${numberSql(row.flow, 2, [0, 9999.99], 'flow_lps', numberStats.flow_lps)}, `
      + `${sqlNullable(supplyCode(row))}, `
      + `${numberSql(row.height, 2, [-999.99, 999.99], 'add_height_m', numberStats.add_height_m)}, `
      + `${sqlString(usable)}, ${sqlNullable(reasonText)}, ${sqlNullable(reasonCode)}, NULL, `
      + `${repairSql === null ? 'NULL' : repairSql} `
      + 'FROM DUAL WHERE @water_sid IS NOT NULL AND NOT EXISTS '
      + `(SELECT 1 FROM ccds_water_source w WHERE w.station_id = @water_sid AND w.name = ${sqlString(row.finalName)});`
    );
  }

  const generatedAt = new Date().toISOString().replace('T', ' ').slice(0, 19);
  const sqlBody = lines.join('\n');
  const body = `${buildHeader(resolved.length, skipped.length, generatedAt)}\n${sqlBody}\n`;
  mkdirSync(SQL_DIR, { recursive: true });
  writeFileSync(SQL_FILE, body, 'utf8');

  // 自校验：统计只针对语句区，避免把文件头注释里的示例语句算进去。
  const insertCount = (sqlBody.match(/^INSERT INTO ccds_water_source /gm) || []).length;
  const notExistsCount = (sqlBody.match(/NOT EXISTS \(SELECT 1 FROM ccds_water_source /g) || []).length;
  const setCount = (sqlBody.match(/^SET @water_sid = /gm) || []).length;
  if (insertCount !== resolved.length || notExistsCount !== resolved.length || setCount !== resolved.length) {
    throw new Error(`自校验失败：写入 ${resolved.length} 条，SET ${setCount}，INSERT ${insertCount}，NOT EXISTS ${notExistsCount}`);
  }

  console.log('=== 水源检查表 SQL 生成（B1 结构化列）===');
  console.log(`输入：${WATER_NDJSON}`);
  console.log(`输出：${SQL_FILE}`);
  console.log(`写入列（${INSERT_COLUMNS.split(',').length} 个）：${INSERT_COLUMNS}`);
  console.log('\n## 写入/跳过');
  console.log(`  读取：${sourceRows.length} 条`);
  console.log(`  写入：${resolved.length} 条`);
  console.log(`  跳过：${skipped.length} 条`);
  for (const [reason, count] of countBy(skipped, (item) => item.reason)) {
    console.log(`    ${reason}：${count}`);
  }
  for (const item of skipped) {
    console.log(`    ${item.reason}：${item.row.station} / ${item.row.name}（${item.row.legacyId}）`);
  }

  console.log('\n## 类型分布（入库口径）');
  for (const [key, value] of countBy(resolved, (row) => normalizeType(row))) {
    console.log(`  ${String(value).padStart(5)}  ${key}`);
  }
  console.log('\n## 状态分布（入库口径）');
  for (const [key, value] of countBy(resolved, (row) => normalizeStatus(row))) {
    console.log(`  ${String(value).padStart(5)}  ${key}`);
  }
  console.log('\n## setup');
  for (const [key, value] of countBy(resolved, (row) => setupCode(row))) {
    console.log(`  ${String(value).padStart(5)}  ${key}`);
  }
  console.log('\n## supply_form');
  for (const [key, value] of countBy(resolved, (row) => supplyCode(row))) {
    console.log(`  ${String(value).padStart(5)}  ${key}`);
  }
  console.log('\n## usable');
  for (const [key, value] of countBy(resolved, (row) => usableCode(row))) {
    console.log(`  ${String(value).padStart(5)}  ${key}`);
  }
  console.log('\n## is_repair');
  for (const [key, value] of countBy(resolved, (row) => isRepairValue(row))) {
    console.log(`  ${String(value).padStart(5)}  ${key}`);
  }

  console.log('\n## reason_code 分布（仅 usable=bad 归类，其余 NULL）');
  for (const [key, value] of countBy(reasonCodeRows, (code) => code)) {
    console.log(`  ${String(value).padStart(5)}  ${key}`);
  }
  console.log(`  reason_code 非空条数：${reasonCodeRows.filter(Boolean).length}（usable=bad 共 ${resolved.filter((row) => usableCode(row) === 'bad').length} 条）`);
  console.log(`  reason 非空但归类落到 ${REASON_FALLBACK} 兜底：${unclassified.length} 条`);
  for (const item of unclassified) {
    console.log(`    ${item.legacyId}：${item.reason}`);
  }
  console.log(`  好用但 reason 非空（保留原文、不归类）：${resolved.filter((row) => usableCode(row) === 'good' && row.reason).length} 条`);

  console.log('\n## 数值列');
  for (const [column, stats] of Object.entries(numberStats)) {
    console.log(`  ${column}：NULL ${stats.nulls} / 四舍五入 ${stats.rounded} / 拒绝 ${stats.rejected.length}${stats.rejected.length ? ' → ' + stats.rejected.join(', ') : ''}`);
  }

  console.log('\n## 文本列与质量');
  console.log(`  extra_json 含 '{' 的条数（应恒 0，防 JSON 回流）：${extraJsonWithBrace.length}`);
  for (const item of extraJsonWithBrace.slice(0, 10)) {
    console.log(`    ${item}`);
  }
  console.log(`  notes（extra_json 来源）非空条数：${resolved.filter((row) => row.notes).length}`);
  console.log(`  reason_text 超 255 截断：${reasonTextTruncated.length} 条`);
  console.log(`  同站同名撞名：${conflicts.length} 条（已加后缀）`);
  for (const item of conflicts.slice(0, 20)) {
    console.log(`    ${item.station}：${item.from} → ${item.to}（${item.legacyId}）`);
  }
  console.log(`  address 超 128 字符截断：${addressTruncated.length} 条`);
  console.log(`  lng/lat 原始空串写 NULL：${coordStats.empty} 字段`);
  console.log(`  lng/lat 非数值或超范围写 NULL：${coordStats.invalid.length} 字段${coordStats.invalid.length ? ' → ' + coordStats.invalid.map((v) => JSON.stringify(v)).join(', ') : ''}`);
  console.log(`  lng/lat 小数位超 6 位按 6 位四舍五入：${coordStats.rounded} 字段`);
  console.log(`  字符串含控制字符（换行/制表符等，已转义为 \\n \\t，值不变）：${controlChars.length} 处`);
  for (const item of controlChars.slice(0, 20)) {
    console.log(`    ${item}`);
  }

  console.log('\n## 自校验');
  console.log(`  SET @water_sid 条数：${setCount}（期望 ${resolved.length}）`);
  console.log(`  INSERT INTO 条数：${insertCount}（期望 ${resolved.length}）`);
  console.log(`  NOT EXISTS 判重条数：${notExistsCount}（期望 ${resolved.length}）`);
  console.log(`  文件大小：${Buffer.byteLength(body, 'utf8')} 字节`);
  console.log('\n完成。');

  if (extraJsonWithBrace.length > 0) {
    throw new Error('extra_json 出现了 JSON 花括号，拒绝写坏备注语义');
  }
}

try {
  main();
} catch (error) {
  console.error(`生成失败：${error.message}`);
  process.exitCode = 1;
}
