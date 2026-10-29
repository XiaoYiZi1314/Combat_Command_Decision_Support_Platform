export const TYPE_OPTIONS = [
  { value: 'crane', label: '消防水鹤' },
  { value: 'ground', label: '地上消火栓' },
  { value: 'underground', label: '地下消火栓' }
];

export const STATUS_OPTIONS = [
  { value: 'active', label: '在用' },
  { value: 'repair', label: '报修' }
];

/**
 * 设置方式（1.1 编辑表字段）。
 */
export const SETUP_OPTIONS = [
  { value: 'above', label: '地上' },
  { value: 'under', label: '地下' }
];

/**
 * 给水形式（1.1 编辑表字段）。
 */
export const SUPPLY_FORM_OPTIONS = [
  { value: 'ring', label: '环状' },
  { value: 'branch', label: '枝状' }
];

/**
 * 可使用（1.1 编辑表字段）。不好用判定由「可使用 + 是否报修」派生状态，前端不再单独传状态。
 */
export const USABLE_OPTIONS = [
  { value: 'good', label: '好用' },
  { value: 'bad', label: '不好用' }
];

/**
 * 九项不好用原因，一条卡只记一个主原因（1.3 口径）。
 */
export const REASON_OPTIONS = [
  { value: 'no_water', label: '无水' },
  { value: 'leak', label: '漏水' },
  { value: 'low_pressure', label: '压力不足' },
  { value: 'rust', label: '锈蚀' },
  { value: 'buried', label: '埋压' },
  { value: 'occupied', label: '圈占' },
  { value: 'diverted', label: '挪用' },
  { value: 'collapsed_well', label: '塌井' },
  { value: 'damaged', label: '毁坏' }
];

/**
 * 八项部件损坏，勾一项主部件（1.3 口径）。
 */
export const PART_OPTIONS = [
  { value: 'pipe', label: '管网' },
  { value: 'cover', label: '井盖' },
  { value: 'blind_cover', label: '出水口闷盖' },
  { value: 'valve', label: '闸阀' },
  { value: 'drain_valve', label: '放水阀' },
  { value: 'inlet_valve', label: '上水阀' },
  { value: 'outlet', label: '出水口' },
  { value: 'sign', label: '标志牌' }
];

/**
 * 火灾类型供水强度 L/(s·m²)，沿现网。
 */
export const FIRE_INTENSITY = [
  { value: 'residential', label: '住宅/一般建筑', intensity: 0.10 },
  { value: 'commercial', label: '商市场/人员密集', intensity: 0.14 },
  { value: 'warehouse', label: '仓库/大跨度', intensity: 0.16 },
  { value: 'highrise', label: '高层', intensity: 0.12 },
  { value: 'chemical', label: '危化品/油品', intensity: 0.18 }
];

/**
 * 经验场景系数，文案沿现网。
 */
export const EXPERIENCE_OPTIONS = [
  { value: '1.1', label: '1.1（火势较小）' },
  { value: '1.2', label: '1.2（一般，默认）' },
  { value: '1.3', label: '1.3（火势较大）' },
  { value: '1.4', label: '1.4（猛烈燃烧）' }
];

/**
 * 干线水头损失系数：65mm 与 80mm 水带，沿现网经验公式。
 */
export const HOSE_FACTOR = { 65: 0.018, 80: 0.008 };

/**
 * Haversine 直线距离（米）。
 */
export function distanceMeters(lng1, lat1, lng2, lat2) {
  const rad = Math.PI / 180;
  const dLat = (lat2 - lat1) * rad;
  const dLng = (lng2 - lng1) * rad;
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLng / 2) ** 2;
  return Math.round(2 * 6371008.8 * Math.asin(Math.min(1, Math.sqrt(a))));
}

export function formatDistance(m) {
  const value = Number(m);
  if (!Number.isFinite(value)) {
    return '--';
  }
  return value >= 1000 ? `${(value / 1000).toFixed(value >= 10000 ? 1 : 2)}公里` : `${Math.round(value)}米`;
}

/**
 * 百度导航 URL。坐标按 WGS84 传给 coord_type。
 */
export function buildBaiduNavUrl(lat, lng, name) {
  return `https://api.map.baidu.com/direction?destination=latlng:${encodeURIComponent(`${lat},${lng}`)}`
    + `|name:${encodeURIComponent(name || '消防水源')}&mode=driving&coord_type=wgs84&output=html`;
}

/**
 * 按车辆类型估算泵量 L/s。仅在车辆档案缺少水泵/水炮/泡沫炮流量时兜底使用。
 */
export function defaultVehicleFlow(vehicleName) {
  const text = String(vehicleName || '');
  if (/举高|喷射|高喷|水罐|泡沫/.test(text)) {
    return 60;
  }
  if (/水|泡沫|泵/.test(text)) {
    return 40;
  }
  return 0;
}

/**
 * 创建 DOM 元素工具函数。
 */
export function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  if (text) {
    node.textContent = text;
  }
  return node;
}

/**
 * 根据类型码获取中文标签。
 */
export function typeLabel(code) {
  const hit = TYPE_OPTIONS.find((item) => item.value === code);
  return hit ? hit.label : code;
}

/**
 * 根据状态码获取中文标签。
 */
export function statusLabel(code) {
  const hit = STATUS_OPTIONS.find((item) => item.value === code);
  return hit ? hit.label : code;
}

/**
 * 按选项表取标签。
 */
export function optionLabel(options, code) {
  const hit = (options || []).find((item) => item.value === code);
  return hit ? hit.label : '';
}

/**
 * 不好用判定：与后端 WaterStatusResolver.isBad 同一口径。
 * 状态=报修，或可使用=不好用，或是否报修=是。
 */
export function isWaterBad(water) {
  if (!water) {
    return false;
  }
  return water.bad === true
    || water.status === 'repair'
    || water.usable === 'bad'
    || water.repair === true;
}

/**
 * 水源卡片原因摘要：不好用的卡直接写原因、部件与备注，不能只写一个「报修」。
 */
export function waterBadSummary(water) {
  if (!isWaterBad(water)) {
    return '';
  }
  const parts = [];
  const reason = water.reasonLabel || optionLabel(REASON_OPTIONS, water.reasonCode) || water.reasonText;
  if (reason) {
    parts.push(`原因：${reason}`);
  }
  const part = water.partLabel || optionLabel(PART_OPTIONS, water.partCode);
  if (part) {
    parts.push(`部件：${part}`);
  }
  if (water.notes) {
    parts.push(`备注：${water.notes}`);
  }
  return parts.join(' · ');
}
