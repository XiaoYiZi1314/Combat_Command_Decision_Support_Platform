/**
 * 从旧版内置 index.html 抽取站级种子数据（花名册 / 编组 / 值班种子 / 值班车辆）。
 *
 * 旧版把种子直接写在脚本里：`var fireStationData={...}`、`var IMPORTED_STATION_DUTY_DATA={...}`、
 * `var IMPORTED_STATION_DUTY_VEHICLE_DATA={...}`。这里用括号配对扫描取值，不 eval 任何代码。
 *
 * 产物：`out/bundle-core.json`
 *   用于两件事：① 云端无数据的站兜底；② 提供编组成员归属（云端只有组名与任务，没有成员）。
 *
 * 运行：node extract-bundle-core.mjs
 */

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { runInNewContext } from 'node:vm';

const LEGACY_ASSETS = process.env.NXGK_LEGACY_ASSETS
  || 'D:\\Project\\Combat_Command_Decision_Support_Platform\\tmp_apk\\extracted\\assets';
const OUT_DIR = process.env.LEGACY_MIGRATION_OUT
  || join(process.env.TEMP || '.', 'ccds-migration');

function extractAssignment(text, variableName) {
  const marker = `${variableName}=`;
  const start = text.indexOf(marker);
  if (start < 0) {
    return null;
  }
  let index = start + marker.length;
  while (index < text.length && /\s/.test(text[index])) {
    index += 1;
  }
  const open = text[index];
  if (open !== '{' && open !== '[') {
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
    if (ch === '{' || ch === '[') {
      depth += 1;
      continue;
    }
    if (ch === '}' || ch === ']') {
      depth -= 1;
      if (depth === 0) {
        return text.slice(index, cursor + 1);
      }
    }
  }
  return null;
}

function parseAssignment(text, variableName) {
  const raw = extractAssignment(text, variableName);
  if (!raw) {
    return null;
  }
  // 旧版把对象写成 JavaScript 字面量（键不带引号），不是严格 JSON。
  // 这里在空沙箱里只求值这个字面量：不注入 require / process / fetch，且限时 3 秒。
  // 输入是客户自带的离线资源，不是运行期外部输入。
  const sandbox = Object.create(null);
  return runInNewContext(`(${raw})`, sandbox, { timeout: 3000 });
}

function main() {
  const htmlPath = join(LEGACY_ASSETS, 'index.html');
  const html = readFileSync(htmlPath, 'utf8');

  const stationData = parseAssignment(html, 'var fireStationData') || {};
  const dutySeed = parseAssignment(html, 'var IMPORTED_STATION_DUTY_DATA') || {};
  const dutyVehicleSeed = parseAssignment(html, 'var IMPORTED_STATION_DUTY_VEHICLE_DATA') || {};
  const dutyVehicleVersion = (/var IMPORTED_DUTY_VEHICLE_VERSION="([^"]*)"/.exec(html) || [])[1] || '';

  const stations = {};
  const names = new Set([
    ...Object.keys(stationData),
    ...Object.keys(dutySeed),
    ...Object.keys(dutyVehicleSeed)
  ]);
  for (const name of names) {
    const base = stationData[name] || {};
    const duty = dutySeed[name] || {};
    const vehicle = dutyVehicleSeed[name] || {};
    stations[name] = {
      category: base.category || '',
      roster: Array.isArray(base.roster) ? base.roster : [],
      groups: Array.isArray(base.groups) ? base.groups : [],
      dutySeed: duty || {},
      dutyVehicles: Array.isArray(vehicle.dutyVehicles) ? vehicle.dutyVehicles : []
    };
  }

  mkdirSync(OUT_DIR, { recursive: true });
  const payload = {
    generatedAt: new Date().toISOString(),
    source: htmlPath,
    dutyVehicleVersion,
    stationCount: Object.keys(stations).length,
    stations
  };
  const outPath = join(OUT_DIR, 'bundle-core.json');
  writeFileSync(outPath, JSON.stringify(payload, null, 1), 'utf8');

  const rosterTotal = Object.values(stations).reduce((sum, s) => sum + s.roster.length, 0);
  const groupTotal = Object.values(stations).reduce((sum, s) => sum + s.groups.length, 0);
  const vehicleTotal = Object.values(stations).reduce((sum, s) => sum + s.dutyVehicles.length, 0);
  const dutyDays = Object.values(stations).reduce((sum, s) => sum + Object.keys(s.dutySeed).length, 0);

  console.log(`bundle-core.json -> ${outPath}`);
  console.log(`站数 ${payload.stationCount}｜内置花名册 ${rosterTotal} 人｜编组 ${groupTotal} 个｜值班种子 ${dutyDays} 站｜值班车辆 ${vehicleTotal} 条`);
  const stationsWithoutGroups = Object.entries(stations).filter(([, s]) => s.groups.length === 0).map(([k]) => k);
  if (stationsWithoutGroups.length) {
    console.log(`无编组种子站：${stationsWithoutGroups.join('、')}`);
  }
  const stationsWithoutRoster = Object.entries(stations).filter(([, s]) => s.roster.length === 0).map(([k]) => k);
  if (stationsWithoutRoster.length) {
    console.log(`无花名册种子站：${stationsWithoutRoster.join('、')}`);
  }
}

main();
