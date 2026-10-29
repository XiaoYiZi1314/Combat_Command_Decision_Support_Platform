#!/usr/bin/env node
/**
 * 旧版 APK 内置水源数据 → NDJSON（一次性迁移抽取，只读数据源）。
 *
 * 输入（只读，禁止修改）：
 *   tmp_apk/extracted/assets/water_inspection_manifest.js   window.WATER_INSPECTION_MANIFEST=[...]
 *   tmp_apk/extracted/assets/water_sources_manifest.js      window.WATER_SOURCE_MANIFEST=[...]
 *   tmp_apk/extracted/assets/water_inspection_photos.js     window.WATER_INSPECTION_PHOTOS={...}
 *
 * 输出（写 LEGACY_MIGRATION_OUT，默认 %TEMP%\ccds-migration，不落仓库）：
 *   out/water.ndjson              一条检查记录一行，字段见 migration/README.md 3.1
 *   out/water-docs.ndjson         一条水源档案 PDF 一行，字段见 3.2
 *   out/water-photos/<ID>.jpg     内置照片按 ID 解码，见 3.3
 *   out/water-photo-index.ndjson  {id,file,bytes,sha256}
 *
 * 解析方式：这三个文件都是「window.XXX=<纯 JSON>;」赋值，因此先定位赋值标记，
 * 再按括号配平切出 JSON 文本后 JSON.parse；全程不 eval、不执行旧版代码。
 *
 * 运行：node extract-bundle-water.mjs（Node >= 20，ESM、零第三方依赖）
 *      默认导出 photos.js 全量照片；加 --referenced-only 时只导出被 photoRef 引用的照片。
 */

import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(SCRIPT_DIR, '../../..');
const ASSETS_DIR = join(REPO_ROOT, 'tmp_apk', 'extracted', 'assets');
const OUT_ROOT = resolve(process.env.LEGACY_MIGRATION_OUT || join(tmpdir(), 'ccds-migration'));
const OUT_DIR = join(OUT_ROOT, 'out');
const PHOTO_DIR = join(OUT_DIR, 'water-photos');

const WATER_NDJSON = join(OUT_DIR, 'water.ndjson');
const DOCS_NDJSON = join(OUT_DIR, 'water-docs.ndjson');
const PHOTO_INDEX_NDJSON = join(OUT_DIR, 'water-photo-index.ndjson');

/** 照片引用形如 "\"ID_xxx\",1"，取出其中的 ID。 */
const PHOTO_ID_PATTERN = /ID_[A-Za-z0-9]+/;

function sha256Hex(buf) {
  return createHash('sha256').update(buf).digest('hex');
}

/** 读取 assets 下的只读文件；缺失即中止，不做兜底猜测。 */
function readAsset(fileName) {
  const path = join(ASSETS_DIR, fileName);
  if (!existsSync(path)) {
    throw new Error(`数据源缺失：${path}`);
  }
  return readFileSync(path, 'utf8');
}

/**
 * 从「赋值标记 + 纯 JSON」中切出 JSON 并解析，不执行脚本。
 *
 * @param {string} text   脚本全文
 * @param {string} marker 形如 'WATER_INSPECTION_MANIFEST=' 的赋值标记
 * @returns {any} 解析结果
 */
function sliceAssignedJson(text, marker) {
  const at = text.indexOf(marker);
  if (at < 0) {
    throw new Error(`未找到赋值标记：${marker}`);
  }
  let i = at + marker.length;
  while (i < text.length && /\s/.test(text[i])) {
    i += 1;
  }
  if (text[i] !== '[' && text[i] !== '{') {
    throw new Error(`赋值标记后不是 JSON：${marker}`);
  }
  const start = i;
  let depth = 0;
  let inString = false;
  let escaped = false;
  for (; i < text.length; i += 1) {
    const ch = text[i];
    if (escaped) {
      escaped = false;
      continue;
    }
    if (ch === '\\') {
      if (inString) {
        escaped = true;
      }
      continue;
    }
    if (ch === '"') {
      inString = !inString;
      continue;
    }
    if (inString) {
      continue;
    }
    if (ch === '[' || ch === '{') {
      depth += 1;
      continue;
    }
    if (ch === ']' || ch === '}') {
      depth -= 1;
      if (depth === 0) {
        return JSON.parse(text.slice(start, i + 1));
      }
    }
  }
  throw new Error(`JSON 括号未闭合：${marker}`);
}

/** 统一把旧版可能缺失的字段收敛成去空白的字符串。 */
function str(value) {
  return String(value ?? '').trim();
}

/** 旧版 updatedAt 是毫秒时间戳，缺失/非法按 0 保留（契约要求 number）。 */
function millis(value) {
  const n = Number(value);
  return Number.isFinite(n) && n > 0 ? n : 0;
}

/** 检查记录 → README 3.1 契约行（字段顺序即契约顺序）。 */
function toWaterRow(raw) {
  return {
    legacyId: str(raw.id),
    station: str(raw.station),
    name: str(raw.name),
    type: str(raw.type),
    setup: str(raw.setup),
    address: str(raw.address),
    pipeDiameter: str(raw.pipeDiameter),
    pressure: str(raw.pressure),
    flow: str(raw.flow),
    waterSupply: str(raw.waterSupply),
    height: str(raw.height),
    status: str(raw.status),
    usable: str(raw.usable),
    reason: str(raw.reason),
    repair: str(raw.repair),
    notes: str(raw.notes),
    photoRef: str(raw.photoRef),
    lng: str(raw.lng),
    lat: str(raw.lat),
    updatedAt: millis(raw.updatedAt),
    locationAccuracy: str(raw.locationAccuracy),
    photoLocation: str(raw.photoLocation),
    source: str(raw.source)
  };
}

/** 水源档案清单 → README 3.2 契约行；r2Key 取云对象键 docCosKey。 */
function toDocRow(raw) {
  return {
    station: str(raw.station),
    waterName: str(raw.name),
    bizType: 'water_doc',
    r2Key: str(raw.docCosKey),
    originalName: str(raw.fileName),
    contentType: str(raw.docMime) || 'application/pdf',
    sizeBytes: Number(raw.size) || 0
  };
}

/** 写 NDJSON：UTF-8、每行一个对象、末尾换行、不含 BOM。 */
function writeNdjson(filePath, rows) {
  const body = rows.map((row) => JSON.stringify(row)).join('\n');
  writeFileSync(filePath, rows.length ? `${body}\n` : '', 'utf8');
  return rows.length;
}

/**
 * 把 data URI 照片解码落盘，并产出索引。
 *
 * @param {Record<string, string>} photos ID → data URI
 * @returns {{index: Array<object>, failures: Array<object>, totalBytes: number, maxBytes: number, maxId: string}}
 */
function decodePhotos(photos) {
  rmSync(PHOTO_DIR, { recursive: true, force: true });
  mkdirSync(PHOTO_DIR, { recursive: true });
  const index = [];
  const failures = [];
  let totalBytes = 0;
  let maxBytes = 0;
  let maxId = '';
  for (const id of Object.keys(photos).sort()) {
    const uri = String(photos[id] ?? '');
    const comma = uri.indexOf(',');
    const header = comma > 0 ? uri.slice(0, comma) : '';
    if (!/^data:image\/jpeg;base64$/.test(header)) {
      failures.push({ id, reason: `非 JPEG data URI：${header.slice(0, 40)}` });
      continue;
    }
    const bytes = Buffer.from(uri.slice(comma + 1), 'base64');
    if (bytes.length === 0) {
      failures.push({ id, reason: '解码后 0 字节' });
      continue;
    }
    writeFileSync(join(PHOTO_DIR, `${id}.jpg`), bytes);
    index.push({
      id,
      file: `water-photos/${id}.jpg`,
      bytes: bytes.length,
      sha256: sha256Hex(bytes)
    });
    totalBytes += bytes.length;
    if (bytes.length > maxBytes) {
      maxBytes = bytes.length;
      maxId = id;
    }
  }
  return { index, failures, totalBytes, maxBytes, maxId };
}

/** 计数并降序输出。 */
function countBy(rows, selector) {
  const map = new Map();
  for (const row of rows) {
    const key = selector(row) ?? '<空>';
    map.set(key, (map.get(key) || 0) + 1);
  }
  return [...map.entries()].sort((a, b) => b[1] - a[1]);
}

function printCounts(title, entries) {
  console.log(`\n## ${title}`);
  for (const [key, value] of entries) {
    console.log(`  ${String(value).padStart(5)}  ${key}`);
  }
}

/** 收集数据质量告警（不修数据，只提示）。 */
function collectWarnings(records) {
  const badCoord = [];
  const highPrecision = [];
  const noPhotoId = [];
  for (const row of records) {
    for (const [field, value] of [['lng', row.lng], ['lat', row.lat]]) {
      if (!value) {
        continue;
      }
      if (!Number.isFinite(Number(value))) {
        badCoord.push(`${row.legacyId}.${field}=${JSON.stringify(value)}`);
      } else if ((String(value).split('.')[1] || '').length > 6) {
        highPrecision.push(`${row.legacyId}.${field}=${value}`);
      }
    }
    if (row.photoRef && !PHOTO_ID_PATTERN.test(row.photoRef)) {
      noPhotoId.push(`${row.legacyId}（${row.station} / ${row.name}）：${row.photoRef}`);
    }
  }
  return { badCoord, highPrecision, noPhotoId };
}

function main() {
  mkdirSync(OUT_DIR, { recursive: true });

  const records = sliceAssignedJson(readAsset('water_inspection_manifest.js'), 'WATER_INSPECTION_MANIFEST=')
    .map(toWaterRow);
  const docs = sliceAssignedJson(readAsset('water_sources_manifest.js'), 'WATER_SOURCE_MANIFEST=')
    .map(toDocRow);
  const rawPhotos = sliceAssignedJson(readAsset('water_inspection_photos.js'), 'WATER_INSPECTION_PHOTOS=');

  // 默认导出 photos.js 全量（清单内的孤儿照片也保留）；--referenced-only 只导出被 photoRef 引用的。
  const referenced = new Set();
  for (const row of records) {
    const matched = PHOTO_ID_PATTERN.exec(row.photoRef);
    if (matched) {
      referenced.add(matched[0]);
    }
  }
  const onlyReferenced = process.argv.includes('--referenced-only');
  const photos = onlyReferenced
    ? Object.fromEntries(Object.entries(rawPhotos).filter(([id]) => referenced.has(id)))
    : rawPhotos;

  writeNdjson(WATER_NDJSON, records);
  writeNdjson(DOCS_NDJSON, docs);
  const photoResult = decodePhotos(photos);
  writeNdjson(PHOTO_INDEX_NDJSON, photoResult.index);

  const indexIds = new Set(photoResult.index.map((item) => item.id));
  const refWithoutFile = [...referenced].filter((id) => !indexIds.has(id));
  const orphan = photoResult.index.filter((item) => !referenced.has(item.id));
  const warnings = collectWarnings(records);

  console.log('=== 旧版内置水源数据抽取 ===');
  console.log(`数据源：${ASSETS_DIR}`);
  console.log(`输出目录：${OUT_DIR}`);
  console.log(`\n检查表记录：${records.length} 条 → ${WATER_NDJSON}`);
  console.log(`水源档案：${docs.length} 条 → ${DOCS_NDJSON}`);
  console.log(`照片索引：${photoResult.index.length} 条 → ${PHOTO_INDEX_NDJSON}`);

  printCounts('按 station 分组（条数降序）', countBy(records, (row) => row.station));
  printCounts('按 type 分布（旧版原始值）', countBy(records, (row) => row.type));
  printCounts('按 status 分布（旧版原始值）', countBy(records, (row) => row.status));
  printCounts('按 usable 分布（旧版原始值）', countBy(records, (row) => row.usable));
  printCounts('按 setup 分布（旧版原始值）', countBy(records, (row) => row.setup));
  printCounts('水源档案按 contentType 分布', countBy(docs, (row) => row.contentType));
  printCounts('水源档案按 station 分布（条数降序）', countBy(docs, (row) => row.station));

  console.log('\n## 照片');
  console.log(`  photoRef 引用到的唯一 ID：${referenced.size}`);
  console.log(`  解出照片文件：${photoResult.index.length} 张（含未被引用的孤儿照片 ${orphan.length} 张）`);
  console.log(`  引用但缺图：${refWithoutFile.length}${refWithoutFile.length ? ' → ' + refWithoutFile.slice(0, 10).join(', ') : ''}`);
  console.log(`  解码失败：${photoResult.failures.length}`);
  for (const failure of photoResult.failures.slice(0, 10)) {
    console.log(`    ${failure.id}：${failure.reason}`);
  }
  console.log(`  总字节：${photoResult.totalBytes}（约 ${(photoResult.totalBytes / 1048576).toFixed(2)} MB）`);
  console.log(`  最大单张：${photoResult.maxBytes} 字节（${photoResult.maxId}）`);
  console.log(`  0 字节文件：${photoResult.index.filter((item) => item.bytes === 0).length}`);

  console.log('\n## 数据质量告警（只提示，不修改源数据）');
  console.log(`  photoRef 非照片 ID（无照片）：${warnings.noPhotoId.length}`);
  for (const item of warnings.noPhotoId) {
    console.log(`    ${item}`);
  }
  console.log(`  坐标非数值（入库将转 NULL）：${warnings.badCoord.length}`);
  for (const item of warnings.badCoord) {
    console.log(`    ${item}`);
  }
  console.log(`  坐标小数位 > 6（入库按 6 位四舍五入）：${warnings.highPrecision.length}`);
  for (const item of warnings.highPrecision) {
    console.log(`    ${item}`);
  }
  console.log(`  坐标为空：lng ${records.filter((row) => !row.lng).length} 条 / lat ${records.filter((row) => !row.lat).length} 条`);

  if (photoResult.failures.length > 0) {
    process.exitCode = 1;
    console.log('\n存在照片解码失败，退出码置 1。');
  }
  console.log('\n完成。');
}

try {
  main();
} catch (error) {
  console.error(`抽取失败：${error.message}`);
  process.exitCode = 1;
}
