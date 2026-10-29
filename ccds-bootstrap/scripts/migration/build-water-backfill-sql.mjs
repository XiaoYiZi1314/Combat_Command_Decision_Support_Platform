#!/usr/bin/env node
/**
 * 为「已存在但缺 B1 结构化字段」的旧水源行生成回填补丁。
 *
 * 背景：0001_water_source.sql 按 (station_id, name) 判重，只 INSERT 不 UPDATE。
 *       当目标库事先已存在同名水源行（现场录入数据）时，这些行会被判重跳过，
 *       导致 setup/usable/reason_code 等 B1 列仍为 NULL，p11 的 1.1/1.2/1.3 口径无法落地。
 *       本脚本为这些行生成 UPDATE 补丁，只写 B1 结构化列，不覆盖现场已填的
 *       type/status/address/lng/lat/extra_json。
 *
 * 输入：LEGACY_MIGRATION_OUT/out/water.ndjson（由 extract-bundle-water.mjs 生成）
 *       LEGACY_MIGRATION_OUT/legacy-water-existing.tsv（可选，`<站名>\t<名称>` 每行一条，
 *       列出目标库中已存在的水源；缺省时脚本对全部 643 条都生成 UPDATE，
 *       UPDATE 的 WHERE 条件带 setup IS NULL，天然幂等且不会影响已回填行）
 * 输出：ccds-bootstrap/scripts/db/patch/rdb/0009_legacy_seed/0003_water_backfill.sql
 *
 * 幂等：UPDATE ... WHERE station_id = @water_sid AND name = '...' AND setup IS NULL，
 *       重复执行第二次影响 0 行。
 *
 * 运行：node build-water-backfill-sql.mjs（Node >= 20，ESM、零第三方依赖）
 */

import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(SCRIPT_DIR, '../../..');
const OUT_ROOT = resolve(process.env.LEGACY_MIGRATION_OUT || join(tmpdir(), 'ccds-migration'));
const WATER_NDJSON = join(OUT_ROOT, 'out', 'water.ndjson');
const EXISTING_TSV = join(OUT_ROOT, 'legacy-water-existing.tsv');
const SQL_DIR = join(REPO_ROOT, 'ccds-bootstrap', 'scripts', 'db', 'patch', 'rdb', '0009_legacy_seed');
const SQL_FILE = join(SQL_DIR, '0003_water_backfill.sql');

const NAME_MAX = 64;
const ADDRESS_MAX = 128;
const REASON_TEXT_MAX = 255;

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

const BACKFILL_COLUMNS = [
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

function usableCode(row) {
  return row.usable.includes('不好用') ? 'bad' : 'good';
}

function setupCode(row) {
  return row.setup.includes('地下') ? 'under' : 'above';
}

function supplyCode(row) {
  if (row.waterSupply.includes('环')) {
    return 'ring';
  }
  if (row.waterSupply.includes('枝')) {
    return 'branch';
  }
  return null;
}

function isRepairValue(row) {
  if (row.repair === '是') {
    return 1;
  }
  if (row.repair === '否') {
    return 0;
  }
  return null;
}

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

function truncateChars(value, max) {
  const chars = [...value];
  return chars.length <= max ? value : chars.slice(0, max).join('');
}

function sqlString(value) {
  const text = String(value)
    .replace(/\\/g, '\\\\')
    .replace(/\x00/g, '\\0')
    .replace(/\x1a/g, '\\Z')
    .replace(/\n/g, '\\n')
    .replace(/\r/g, '\\r')
    .replace(/\t/g, '\\t')
    .replace(/'/g, "''");
  return `'${text}'`;
}

function sqlNullable(value) {
  return value ? sqlString(value) : 'NULL';
}

function numberSql(value, scale, range) {
  if (!value) {
    return 'NULL';
  }
  const number = Number(value);
  if (!Number.isFinite(number) || number < range[0] || number > range[1]) {
    return 'NULL';
  }
  return scale === 0 ? String(Math.round(number)) : number.toFixed(scale);
}

function readExistingKeys() {
  if (!existsSync(EXISTING_TSV)) {
    return null;
  }
  const keys = new Set();
  for (const line of readFileSync(EXISTING_TSV, 'utf8').split('\n')) {
    const trimmed = line.trim();
    if (!trimmed) {
      continue;
    }
    const parts = trimmed.split('\t');
    if (parts.length < 2) {
      continue;
    }
    keys.add(`${parts[0].trim()}::${parts[1].trim()}`);
  }
  return keys;
}

function buildHeader(count, scoped, generatedAt) {
  return [
    '-- 旧水源行 B1 结构化字段回填补丁（由 build-water-backfill-sql.mjs 生成，勿手改）。',
    '-- 背景：0001_water_source.sql 按 (station_id, name) 判重只 INSERT 不 UPDATE，',
    '--       目标库中已存在同名水源行时会被判重跳过，B1 列（setup/usable/reason_code 等）仍为 NULL。',
    '--       本补丁只 UPDATE 这 11 个 B1 列，不覆盖现场已填的 type/status/address/lng/lat/extra_json。',
    `-- 生成：migration/build-water-backfill-sql.mjs，${generatedAt}`,
    `-- 条数：${count} 条${scoped ? '（按 legacy-water-existing.tsv 限定到目标库已存在的水源）' : '（未限定，全量生成，靠 WHERE setup IS NULL 幂等）'}`,
    '--',
    '-- 执行顺序：0000_landing_tables.sql → 0002_water_fields.sql → 0001_water_source.sql → 本文件。',
    '-- 幂等：UPDATE 的 WHERE 带 setup IS NULL，重复执行第二次影响 0 行。',
    ''
  ].join('\n');
}

function main() {
  const sourceRows = readNdjson(WATER_NDJSON);
  const existingKeys = readExistingKeys();
  const generatedAt = new Date().toISOString().replace('T', ' ').slice(0, 19);
  const lines = [];
  let written = 0;
  let skipped = 0;

  for (const row of sourceRows) {
    if (!row.name) {
      skipped += 1;
      continue;
    }
    const name = truncateChars(row.name, NAME_MAX);
    if (existingKeys && !existingKeys.has(`${row.station.trim()}::${name}`)) {
      continue;
    }
    const usable = usableCode(row);
    const reasonText = truncateChars(row.reason || '', REASON_TEXT_MAX);
    const reasonCode = usable === 'bad' ? classifyReason(row.reason) : null;
    const repair = isRepairValue(row);
    const assignments = [
      `setup = ${sqlString(setupCode(row))}`,
      `pipe_diameter = ${numberSql(row.pipeDiameter, 0, [1, 10000])}`,
      `pressure_mpa = ${numberSql(row.pressure, 3, [0, 99.999])}`,
      `flow_lps = ${numberSql(row.flow, 2, [0, 9999.99])}`,
      `supply_form = ${sqlNullable(supplyCode(row))}`,
      `add_height_m = ${numberSql(row.height, 2, [-999.99, 999.99])}`,
      `usable = ${sqlString(usable)}`,
      `reason_text = ${sqlNullable(reasonText)}`,
      `reason_code = ${sqlNullable(reasonCode)}`,
      'part_code = NULL',
      `is_repair = ${repair === null ? 'NULL' : String(repair)}`
    ];
    lines.push(`SET @water_sid = (SELECT id FROM ccds_station WHERE name = ${sqlString(row.station)} OR name_norm = ${sqlString(row.station)} LIMIT 1);`);
    lines.push(
      `UPDATE ccds_water_source SET ${assignments.join(', ')} `
      + `WHERE station_id = @water_sid AND name = ${sqlString(name)} AND setup IS NULL;`
    );
    written += 1;
  }

  const body = `${buildHeader(written, Boolean(existingKeys), generatedAt)}${lines.join('\n')}\n`;
  mkdirSync(SQL_DIR, { recursive: true });
  writeFileSync(SQL_FILE, body, 'utf8');

  const updateCount = (body.match(/^UPDATE ccds_water_source SET /gm) || []).length;
  if (updateCount !== written) {
    throw new Error(`自校验失败：生成 ${written} 条 UPDATE，实际 ${updateCount} 条`);
  }

  console.log('=== 水源 B1 字段回填补丁 ===');
  console.log(`输入：${WATER_NDJSON}`);
  console.log(`输出：${SQL_FILE}`);
  console.log(`读取：${sourceRows.length} 条`);
  console.log(`生成 UPDATE：${written} 条`);
  console.log(`跳过（名称为空）：${skipped} 条`);
  console.log(`限定范围：${existingKeys ? 'legacy-water-existing.tsv' : '未限定（全量生成）'}`);
  console.log(`回填列：${BACKFILL_COLUMNS.join(', ')}`);
  console.log(`文件大小：${Buffer.byteLength(body, 'utf8')} 字节`);
  console.log('\n完成。');
}

try {
  main();
} catch (error) {
  console.error(`生成失败：${error.message}`);
  process.exitCode = 1;
}
