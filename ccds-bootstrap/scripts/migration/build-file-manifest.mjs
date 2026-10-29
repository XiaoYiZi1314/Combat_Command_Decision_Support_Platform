/**
 * 生成旧系统附件迁移清单。
 *
 * 输入：
 *   ① R2 对象清单（nxgk/<站>/keyunit、water-photo、water-drain-photo、water-insulation-photo、
 *      water-video、nxgk/water_sources_pdf/**）
 *   ② R2 上的 `nxgk/preplans/migrated/manifest.json`（重点单位预案：原始键 → 单位名）
 *   ③ 内置 `water_sources_manifest.js`（水源档案 PDF：站名 + 水源名）
 *   ④ 内置 `water_inspection_manifest.js` / `out/water.ndjson`（水源检查记录 id，用于 drain/ins 照片归位）
 *
 * 输出：
 *   out/legacy-assets.ndjson         全量附件盘点（含可迁移性与映射状态），供 1.5/1.6 统计与排期
 *   out/legacy-file-manifest.json    可被 LegacyObjectImportService 直接消费的清单（只含已落盘文件）
 *   out/legacy-assets-summary.json   按站/按类型的计数与体积
 *
 * 用法：
 *   node build-file-manifest.mjs                        # 只盘点，不下载
 *   node build-file-manifest.mjs --download=water_doc   # 下载水源档案 PDF 并生成可消费清单
 *   node build-file-manifest.mjs --download=all         # 全部下载（约 5 GB，慎用）
 *   node build-file-manifest.mjs --refresh              # 重新列 R2（默认复用 inventory.json）
 */

import { readFileSync, writeFileSync, mkdirSync, existsSync, createWriteStream } from 'node:fs';
import { join, dirname, basename } from 'node:path';
import { createHash } from 'node:crypto';
import { runInNewContext } from 'node:vm';
import { listAll, getObject } from './r2.mjs';

const OUT_DIR = process.env.LEGACY_MIGRATION_OUT || join(process.env.TEMP || '.', 'ccds-migration');
const LEGACY_ASSETS = process.env.NXGK_LEGACY_ASSETS
  || 'D:\\Project\\Combat_Command_Decision_Support_Platform\\tmp_apk\\extracted\\assets';
const CLOUD_PREFIX = 'nxgk/cloud/nxgk_inner_attack_cloud_v1/';
const PREPLAN_MANIFEST = 'nxgk/preplans/migrated/manifest.json';

const DOWNLOAD_ALIASES = {
  water_doc: 'waterDoc',
  waterdoc: 'waterDoc',
  keyunit: 'keyunit',
  water_photo: 'waterPhoto',
  waterphoto: 'waterPhoto',
  drain: 'drainPhoto',
  ins: 'insPhoto',
  video: 'waterVideo'
};

function normalizeKind(token) {
  const raw = String(token).trim();
  if (!raw || raw === 'all') {
    return raw;
  }
  return DOWNLOAD_ALIASES[raw.toLowerCase()] || raw;
}

const KINDS = {
  keyunit: { dir: 'keyunit', bizType: 'keyunit_plan', label: '重点单位预案' },
  waterDoc: { dir: 'water_sources_pdf', bizType: 'water_doc', label: '水源档案 PDF' },
  drainPhoto: { suffix: 'water-drain-photo', bizType: 'water_photo', label: '泄水阀照片' },
  insPhoto: { suffix: 'water-insulation-photo', bizType: 'water_photo', label: '保暖措施照片' },
  waterPhoto: { suffix: 'water-photo', bizType: 'water_photo', label: '水源照片' },
  waterVideo: { suffix: 'water-video', bizType: 'water_video', label: '出水视频' }
};

function argValue(name, fallback) {
  const hit = process.argv.find((arg) => arg.startsWith(`--${name}=`));
  return hit ? hit.slice(name.length + 3) : fallback;
}

function hasFlag(name) {
  return process.argv.includes(`--${name}`);
}

function parseJsAssignment(text, variableName) {
  const start = text.indexOf(`${variableName}=`);
  if (start < 0) {
    return null;
  }
  let index = text.indexOf('=', start) + 1;
  while (index < text.length && /\s/.test(text[index])) {
    index += 1;
  }
  const open = text[index];
  if (open !== '[' && open !== '{') {
    return null;
  }
  let depth = 0;
  let quote = '';
  let escaped = false;
  for (let cursor = index; cursor < text.length; cursor += 1) {
    const ch = text[cursor];
    if (quote) {
      if (escaped) {
        escaped = false;
      } else if (ch === '\\') {
        escaped = true;
      } else if (ch === quote) {
        quote = '';
      }
      continue;
    }
    if (ch === '"' || ch === "'") {
      quote = ch;
      continue;
    }
    if (ch === '[' || ch === '{') {
      depth += 1;
      continue;
    }
    if (ch === ']' || ch === '}') {
      depth -= 1;
      if (depth === 0) {
        return runInNewContext(`(${text.slice(index, cursor + 1)})`, Object.create(null), { timeout: 5000 });
      }
    }
  }
  return null;
}

function readJson(path, fallback) {
  if (!existsSync(path)) {
    return fallback;
  }
  return JSON.parse(readFileSync(path, 'utf8'));
}

async function loadInventory(refresh) {
  const cachePath = join(OUT_DIR, 'inventory.json');
  if (!refresh) {
    const cached = readJson(cachePath, null);
    if (cached && Array.isArray(cached.objects)) {
      return cached.objects;
    }
  }
  const objects = await listAll('nxgk');
  mkdirSync(OUT_DIR, { recursive: true });
  writeFileSync(cachePath, JSON.stringify({ bucket: 'nxgk-files', count: objects.length, objects }, null, 0), 'utf8');
  return objects;
}

/** `station_<base64url>` → 站名 */
function decodeStationKey(key) {
  const encoded = key.slice('station_'.length);
  try {
    return Buffer.from(encoded.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8');
  } catch {
    return '';
  }
}

function safeFilePart(text) {
  return String(text || '').replace(/[\\/:*?"<>|\r\n]/g, '_').slice(0, 120);
}

async function downloadTo(key, targetPath) {
  mkdirSync(dirname(targetPath), { recursive: true });
  if (existsSync(targetPath)) {
    const bytes = readFileSync(targetPath);
    return { bytes: bytes.length, sha256: createHash('sha256').update(bytes).digest('hex') };
  }
  const buffer = await getObject(key);
  await new Promise((resolve, reject) => {
    const stream = createWriteStream(targetPath);
    stream.on('error', reject);
    stream.on('finish', resolve);
    stream.end(buffer);
  });
  return { bytes: buffer.length, sha256: createHash('sha256').update(buffer).digest('hex') };
}

async function main() {
  mkdirSync(OUT_DIR, { recursive: true });
  const objects = await loadInventory(hasFlag('refresh'));
  const downloadKinds = String(argValue('download', '') || '').split(',').map((v) => normalizeKind(v)).filter(Boolean);
  const downloadAll = downloadKinds.includes('all');
  const downloadLimit = Number(argValue('limit', '0')) || 0;
  const wantDownload = (kind) => downloadAll || downloadKinds.includes(kind);

  // ① 重点单位预案映射（原始 keyunit 键 → 单位名）
  const preplanKeyToUnit = new Map();
  try {
    const text = (await getObject(PREPLAN_MANIFEST)).toString('utf8');
    const manifest = JSON.parse(text);
    for (const [originalKey, item] of Object.entries(manifest.items || {})) {
      preplanKeyToUnit.set(originalKey, { station: item.station, unitName: item.name, migratedKey: item.key });
    }
  } catch (err) {
    console.warn(`读取预案件清单失败，预案单位名将退化为文件名：${err.message}`);
  }

  // ② 水源档案 PDF 映射（站名 + 水源名 → 对象键）
  const waterDocByKey = new Map();
  try {
    const list = parseJsAssignment(readFileSync(join(LEGACY_ASSETS, 'water_sources_manifest.js'), 'utf8'), 'window.WATER_SOURCE_MANIFEST') || [];
    for (const item of list) {
      if (item && item.docCosKey) {
        waterDocByKey.set(item.docCosKey, { station: item.station, waterName: item.name, fileSize: item.size });
      }
    }
  } catch (err) {
    console.warn(`读取水源档案清单失败：${err.message}`);
  }

  // ③ 水源检查记录 id（drain / insulation 照片按 municipal_<id> 归位）
  const waterIds = new Set();
  const waterNdjson = [join(OUT_DIR, 'water.ndjson'), join(OUT_DIR, 'out', 'water.ndjson')].find((p) => existsSync(p));
  if (waterNdjson) {
    for (const line of readFileSync(waterNdjson, 'utf8').split('\n')) {
      if (!line.trim()) {
        continue;
      }
      const row = JSON.parse(line);
      if (row.legacyId) {
        waterIds.add(row.legacyId);
      }
    }
  } else {
    const list = parseJsAssignment(readFileSync(join(LEGACY_ASSETS, 'water_inspection_manifest.js'), 'utf8'), 'window.WATER_INSPECTION_MANIFEST') || [];
    for (const item of list) {
      if (item && item.id) {
        waterIds.add(item.id);
      }
    }
  }

  const assets = [];
  const summary = {};
  for (const object of objects) {
    const key = object.key;
    if (key.startsWith(CLOUD_PREFIX)) {
      continue;
    }
    let kind = null;
    let station = '';
    if (key.includes('/keyunit/')) {
      kind = 'keyunit';
      station = key.split('/')[1] || '';
    } else if (key.startsWith(KINDS.waterDoc.dir + '/') || key.startsWith('nxgk/' + KINDS.waterDoc.dir + '/')) {
      kind = 'waterDoc';
    } else {
      for (const candidate of ['drainPhoto', 'insPhoto', 'waterPhoto', 'waterVideo']) {
        if (key.includes(`/${KINDS[candidate].suffix}/`)) {
          kind = candidate;
          station = key.split('/')[1] || '';
          break;
        }
      }
    }
    if (!kind) {
      continue;
    }

    const fileName = basename(key);
    const asset = {
      kind,
      bizType: KINDS[kind].bizType,
      station,
      r2Key: key,
      originalName: fileName,
      contentType: guessContentType(fileName),
      sizeBytes: object.size,
      mapped: false,
      mapping: null,
      downloadable: true,
      note: ''
    };

    if (kind === 'keyunit') {
      const mapped = preplanKeyToUnit.get(key) || preplanKeyToUnit.get(`nxgk-files/${key}`);
      if (mapped) {
        asset.mapped = true;
        asset.station = mapped.station || station;
        asset.mapping = { unitName: mapped.unitName };
        asset.originalName = `${mapped.unitName}.pdf`;
      } else {
        asset.mapping = { unitName: stripKeyunitPrefix(fileName) };
        asset.mapped = true;
        asset.note = '按文件名解析单位名';
      }
    } else if (kind === 'waterDoc') {
      const mapped = waterDocByKey.get(key);
      if (mapped) {
        asset.mapped = true;
        asset.station = mapped.station || '';
        asset.mapping = { waterName: mapped.waterName };
      } else {
        asset.station = key.split('/')[2] ? key.split('/')[2].replace('水源档案', '') : '';
        asset.note = '清单未收录，按目录名归站';
      }
    } else if (kind === 'drainPhoto' || kind === 'insPhoto') {
      const legacyId = /^((?:municipal|ws)_[0-9A-Za-z]+)/.exec(fileName);
      if (legacyId && waterIds.has(legacyId[1])) {
        asset.mapped = true;
        asset.mapping = { waterLegacyId: legacyId[1] };
      } else {
        asset.note = '无法从文件名解析水源编号';
        asset.downloadable = false;
      }
    } else if (kind === 'waterPhoto' || kind === 'waterVideo') {
      asset.mapped = false;
      asset.note = '旧版未在文件名中写水源编号，无法归位到具体水源；仅用于统计与人工核对';
    }

    summary[kind] = summary[kind] || { count: 0, bytes: 0, mapped: 0, stations: {} };
    summary[kind].count += 1;
    summary[kind].bytes += object.size;
    summary[kind].mapped += asset.mapped ? 1 : 0;
    summary[kind].stations[asset.station || '(未归站)'] = (summary[kind].stations[asset.station || '(未归站)'] || 0) + 1;
    assets.push(asset);
  }

  writeFileSync(join(OUT_DIR, 'legacy-assets.ndjson'), assets.map((a) => JSON.stringify(a)).join('\n') + '\n', 'utf8');
  writeFileSync(join(OUT_DIR, 'legacy-assets-summary.json'), JSON.stringify({ generatedAt: new Date().toISOString(), summary }, null, 1), 'utf8');

  // ④ 可选下载 → 生成可被 LegacyObjectImportService 消费的清单
  const waterById = new Map();
  const waterNdjsonForName = [join(OUT_DIR, 'water.ndjson'), join(OUT_DIR, 'out', 'water.ndjson')].find((p) => existsSync(p));
  if (waterNdjsonForName) {
    for (const line of readFileSync(waterNdjsonForName, 'utf8').split('\n')) {
      if (!line.trim()) {
        continue;
      }
      const row = JSON.parse(line);
      if (row.legacyId) {
        waterById.set(row.legacyId, { station: row.station, name: row.name });
      }
    }
  }
  const items = [];
  for (const asset of assets) {
    if (!asset.downloadable || !wantDownload(asset.kind)) {
      continue;
    }
    if (downloadLimit > 0 && items.length >= downloadLimit) {
      break;
    }
    const targetPath = join(OUT_DIR, 'downloads', asset.kind, safeFilePart(asset.station || 'unknown'), safeFilePart(asset.originalName));
    try {
      const stat = await downloadTo(asset.r2Key, targetPath);
      const legacyWater = asset.mapping && asset.mapping.waterLegacyId ? waterById.get(asset.mapping.waterLegacyId) : null;
      const bizName = asset.mapping && (asset.mapping.unitName || asset.mapping.waterName)
        || (legacyWater ? legacyWater.name : null);
      if (!bizName) {
        continue;
      }
      if (legacyWater && legacyWater.station) {
        asset.station = legacyWater.station;
      }
      items.push({
        bizType: asset.bizType,
        stationName: asset.station,
        bizName,
        originalName: asset.originalName,
        contentType: asset.contentType,
        sizeBytes: stat.bytes,
        cosKey: `legacy/${safeFilePart(asset.station)}/${asset.kind}/${asset.originalName}`,
        localPath: targetPath,
        categoryHint: null,
        sha256: stat.sha256
      });
    } catch (err) {
      console.warn(`下载失败 ${asset.r2Key}：${err.message}`);
    }
  }
  const manifestPath = join(OUT_DIR, 'legacy-file-manifest.json');
  const bundlePhotoItems = buildBundlePhotoItems(waterIds);
  const allItems = items.concat(bundlePhotoItems);
  writeFileSync(manifestPath, JSON.stringify({ items: allItems }, null, 1), 'utf8');

  console.log('=== 附件盘点 ===');
  for (const [kind, stat] of Object.entries(summary)) {
    console.log(`${KINDS[kind].label.padEnd(12)} ${String(stat.count).padStart(5)} 个  ${(stat.bytes / 1048576).toFixed(1).padStart(9)} MB  可归位 ${stat.mapped}`);
  }
  console.log(`\n清单：${manifestPath}（共 ${allItems.length} 项，其中内置水源照片 ${bundlePhotoItems.length} 项、R2 下载 ${items.length} 项）`);
  console.log(`盘点：${join(OUT_DIR, 'legacy-assets.ndjson')}`);
  if (!downloadKinds.length) {
    console.log('未下载任何文件。需要生成可消费清单请加 --download=water_doc 或 --download=all');
  }
}

/**
 * 内置照片迁移清单：water_inspection_photos.js 解出的 696 张 jpg 里，被 photoRef 引用到的那些
 * 按「站名 + 水源名」挂到对应水源（bizType=water_photo），本地文件已存在，无需下载。
 */
function buildBundlePhotoItems(waterIds) {
  const indexFile = join(OUT_DIR, 'out', 'water-photo-index.ndjson');
  const waterFile = join(OUT_DIR, 'out', 'water.ndjson');
  if (!existsSync(indexFile) || !existsSync(waterFile)) {
    return [];
  }
  const photoById = new Map();
  for (const line of readFileSync(indexFile, 'utf8').split('\n')) {
    if (!line.trim()) {
      continue;
    }
    const row = JSON.parse(line);
    photoById.set(row.id, row.file);
  }
  const items = [];
  for (const line of readFileSync(waterFile, 'utf8').split('\n')) {
    if (!line.trim()) {
      continue;
    }
    const water = JSON.parse(line);
    if (!waterIds.has(water.legacyId)) {
      continue;
    }
    const refs = String(water.photoRef || '').match(/ID_[0-9A-Fa-f]+/g) || [];
    let seq = 0;
    for (const id of refs) {
      const relative = photoById.get(id);
      if (!relative) {
        continue;
      }
      const localPath = join(OUT_DIR, 'out', relative);
      if (!existsSync(localPath)) {
        continue;
      }
      seq += 1;
      const bytes = readFileSync(localPath);
      items.push({
        bizType: 'water_photo',
        stationName: water.station,
        bizName: water.name,
        originalName: `${safeFilePart(water.name)}-${seq}.jpg`,
        contentType: 'image/jpeg',
        sizeBytes: bytes.length,
        cosKey: `legacy/${safeFilePart(water.station)}/waterPhoto/${safeFilePart(water.name)}-${seq}.jpg`,
        localPath,
        categoryHint: null,
        sha256: createHash('sha256').update(bytes).digest('hex')
      });
    }
  }
  return items;
}

function stripKeyunitPrefix(fileName) {  const withoutExt = fileName.replace(/\.[^.]+$/, '');
  const parts = withoutExt.split('-');
  const trimmed = parts.length >= 3 ? parts.slice(2).join('-') : withoutExt;
  return trimmed.replace(/^《|》$/g, '').replace(/[（）()]/g, '');
}

function guessContentType(fileName) {
  const ext = (fileName.split('.').pop() || '').toLowerCase();
  if (ext === 'pdf') {
    return 'application/pdf';
  }
  if (ext === 'jpg' || ext === 'jpeg') {
    return 'image/jpeg';
  }
  if (ext === 'png') {
    return 'image/png';
  }
  if (ext === 'mp4') {
    return 'video/mp4';
  }
  if (ext === 'docx') {
    return 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
  }
  if (ext === 'doc') {
    return 'application/msword';
  }
  return 'application/octet-stream';
}

main().catch((err) => {
  console.error('生成失败：', err);
  process.exitCode = 1;
});
