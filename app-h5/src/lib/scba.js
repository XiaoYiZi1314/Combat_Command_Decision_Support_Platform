import { getScbaSettings } from '../stores/settings.js';

export const SCBA = {
  warnPressure: 10,
  dangerPressure: 4,
  warnTimeSec: 12 * 60,
  dangerTimeSec: 5 * 60,
  cylVolume: { '6.8': 6.8, 9: 9 },
  rmv: { light: 41, moderate: 55, heavy: 68 },
  sceneFactor: { flat: 1, highrise: 1.19, dark: 1.09, large: 1.1, search: 1.25 },
  defaultPressure: 25,
  pressureMin: 0,
  pressureMax: 30,
  pressureStep: 0.1,
  evacRatio: 0.8,
  secondsPerMinute: 60,
  pressureToVolume: 10,
  ungrouped: '未分组'
};

export function cylVolumeOf(cylType) {
  const key = String(cylType || '6.8').replace(/L$/i, '');
  return SCBA.cylVolume[key] || 6.8;
}

export function rmvOf(workLevel) {
  return SCBA.rmv[workLevel] || SCBA.rmv.moderate;
}

export function sceneFactorOf(scene) {
  return SCBA.sceneFactor[scene] || 1;
}

export function remainSecOf(pressure, cylType, workLevel, scene, personalK) {
  const p = Number(pressure);
  if (!(p > 0)) {
    return 0;
  }
  let rmv = rmvOf(workLevel);
  const k = Number(personalK);
  if (k > 0) {
    rmv *= k;
  }
  const denom = rmv * sceneFactorOf(scene);
  if (!(denom > 0)) {
    return 0;
  }
  const minutes = (p * SCBA.pressureToVolume * cylVolumeOf(cylType)) / denom;
  return Math.max(0, Math.round(minutes * SCBA.secondsPerMinute));
}

export function resolveStatus(status, pressure, remainSec) {
  if (status === 'out') {
    return 'out';
  }
  if (status === 'pending' || !status) {
    return 'pending';
  }
  const settings = getScbaSettings();
  const p = Number(pressure) || 0;
  const rem = remainSec == null ? 0 : Number(remainSec);
  if (p <= settings.dangerPressure
      || rem <= settings.dangerTimeMin * SCBA.secondsPerMinute) {
    return 'danger';
  }
  if (p <= settings.warnPressure
      || rem <= settings.warnTimeMin * SCBA.secondsPerMinute) {
    return 'warn';
  }
  return 'in';
}

export function fmtMinSec(totalSec) {
  const sec = Math.max(0, Math.floor(Number(totalSec) || 0));
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

export function fmtElapsed(totalSec) {
  const sec = Math.max(0, Math.floor(Number(totalSec) || 0));
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  if (h > 0) {
    return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  return `${m}:${String(s).padStart(2, '0')}`;
}

export function newEventId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `evt_${Date.now()}_${Math.random().toString(16).slice(2)}`;
}

export function statusLabel(status) {
  switch (status) {
    case 'in':
      return '安全';
    case 'warn':
      return '预警';
    case 'danger':
      return '危险';
    case 'out':
      return '已撤出';
    case 'pending':
      return '预录入';
    default:
      return '预录入';
  }
}

export function worstStatus(statuses) {
  const rank = { danger: 4, warn: 3, in: 2, pending: 1, out: 0 };
  let worst = 'out';
  statuses.forEach((item) => {
    if ((rank[item] || 0) > (rank[worst] || 0)) {
      worst = item;
    }
  });
  return worst;
}
