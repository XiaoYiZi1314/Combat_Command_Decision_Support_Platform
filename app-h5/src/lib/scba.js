import { DEFAULT_SCBA_SETTINGS, getScbaSettings } from '../stores/settings.js';

export const SCBA = {
  warnPressure: 10,
  dangerPressure: 4,
  warnTimeSec: 12 * 60,
  dangerTimeSec: 5 * 60,
  cylVolume: { '6.8': 6.8, 9: 9 },
  rmv: { light: 41, moderate: 55, heavy: 68 },
  met: { light: 6.7, moderate: 8.9, heavy: 11.5 },
  sceneFactor: { flat: 1, highrise: 1.19, dark: 1.09, large: 1.1, search: 1.25 },
  sceneParts: {
    flat: { metScale: 1, stress: 1, env: 1 },
    highrise: { metScale: 1.12, stress: 1.04, env: 1.02 },
    dark: { metScale: 0.95, stress: 1.12, env: 1.02 },
    large: { metScale: 1.04, stress: 1.04, env: 1.02 },
    search: { metScale: 1.12, stress: 1.08, env: 1.03 }
  },
  defaultK: 6.35,
  personalKMinFactor: 0.58,
  personalKMaxFactor: 1.75,
  rmvMin: 22,
  rmvMax: 105,
  rateMin: 0.05,
  rateMax: 1.9,
  demoHeightMin: 120,
  demoWeightMin: 35,
  bsaRef: 1.84,
  bsaCoef: 0.26,
  bsaDivisor: 3600,
  bmiHigh: 28,
  bmiHighCoef: 0.018,
  bmiHighAddMax: 0.16,
  bmiLow: 20,
  bmiLowCoef: 0.01,
  bmiLowAddMax: 0.06,
  ageHigh: 35,
  ageHighCoef: 0.005,
  ageHighAddMax: 0.18,
  ageYoung: 25,
  ageYoungFactor: 0.98,
  demoMin: 0.78,
  demoMax: 1.38,
  defaultBodyBlend: 0.35,
  defaultBodyMin: 0.9,
  defaultBodyMax: 1.12,
  blendFormulaWithCal: 0.25,
  blendCalOnly: 0.75,
  blendCalClampMin: 0.92,
  blendCalClampMax: 1.4,
  remainBandLow: 8,
  remainBandMid: 15,
  remainLowMin: 0.7,
  remainLowMax: 1.1,
  remainLowAdd: 0.7,
  remainMidMin: 0.72,
  remainMidMax: 1.14,
  remainMidAdd: 1.2,
  remainHighMax: 1.18,
  remainHighAdd: 2,
  calAlarmPressure: 5,
  calFactorMin: 0.62,
  calFactorMax: 1.85,
  midSensitivity: 0.14,
  lowSensitivity: 0.22,
  intensityClamp: 0.1,
  defaultPressure: 25,
  pressureMin: 0,
  pressureMax: 30,
  pressureStep: 0.1,
  evacRatio: 0.8,
  secondsPerMinute: 60,
  pressureToVolume: 10,
  ungrouped: '未分组'
};

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

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

export function demographicFactor(heightCm, weightKg, age) {
  const height = Number(heightCm);
  const weight = Number(weightKg);
  if (!(height > SCBA.demoHeightMin && weight > SCBA.demoWeightMin)) {
    return 1;
  }
  const bsa = Math.sqrt(height * weight / SCBA.bsaDivisor);
  const heightM = height / 100;
  const bmi = weight / (heightM * heightM);
  let factor = 1 + (bsa - SCBA.bsaRef) * SCBA.bsaCoef;
  if (bmi > SCBA.bmiHigh) {
    factor *= 1 + Math.min(SCBA.bmiHighAddMax, (bmi - SCBA.bmiHigh) * SCBA.bmiHighCoef);
  } else if (bmi < SCBA.bmiLow) {
    factor *= 1 + Math.min(SCBA.bmiLowAddMax, (SCBA.bmiLow - bmi) * SCBA.bmiLowCoef);
  }
  const years = Number(age);
  if (years > SCBA.ageHigh) {
    factor *= 1 + Math.min(SCBA.ageHighAddMax, (years - SCBA.ageHigh) * SCBA.ageHighCoef);
  } else if (years > 0 && years < SCBA.ageYoung) {
    factor *= SCBA.ageYoungFactor;
  }
  return clamp(factor, SCBA.demoMin, SCBA.demoMax);
}

function defaultBodyScale(demo) {
  const factor = demographicFactor(demo && demo.heightCm, demo && demo.weightKg, demo && demo.age);
  return clamp(1 + (factor - 1) * SCBA.defaultBodyBlend, SCBA.defaultBodyMin, SCBA.defaultBodyMax);
}

function scenePartsOf(scene) {
  return SCBA.sceneParts[scene] || SCBA.sceneParts.flat;
}

function metOf(workLevel) {
  return SCBA.met[workLevel] || SCBA.met.moderate;
}

export function rmvFromK(k, workLevel, scene) {
  const parts = scenePartsOf(scene);
  const rmv = Number(k) * metOf(workLevel) * parts.metScale * parts.stress * parts.env;
  return clamp(rmv, SCBA.rmvMin, SCBA.rmvMax);
}

function pressureRate(rmv, cylType) {
  const denom = SCBA.pressureToVolume * cylVolumeOf(cylType);
  if (!(denom > 0)) {
    return SCBA.rateMin;
  }
  return clamp(rmv / denom, SCBA.rateMin, SCBA.rateMax);
}

function formulaRateOf(cylType, workLevel, scene, demo) {
  const rmv = rmvFromK(SCBA.defaultK, workLevel, scene) * defaultBodyScale(demo);
  return pressureRate(rmv, cylType);
}

function calibrationRateOf(cylType, workLevel, scene, personalK) {
  const k = Number(personalK);
  if (!(k > 0)) {
    return null;
  }
  return pressureRate(rmvFromK(k, workLevel, scene), cylType);
}

function blendedRate(formulaRate, calRate) {
  if (!(calRate > 0)) {
    return formulaRate;
  }
  const mixed = formulaRate * SCBA.blendFormulaWithCal + calRate * SCBA.blendCalOnly;
  return clamp(mixed, formulaRate * SCBA.blendCalClampMin, formulaRate * SCBA.blendCalClampMax);
}

function clampRemainMinutes(pressure, rawMin, formulaMin) {
  let minFactor = SCBA.remainMidMin;
  let maxFactor = SCBA.remainHighMax;
  let extra = SCBA.remainHighAdd;
  if (pressure <= SCBA.remainBandLow) {
    minFactor = SCBA.remainLowMin;
    maxFactor = SCBA.remainLowMax;
    extra = SCBA.remainLowAdd;
  } else if (pressure <= SCBA.remainBandMid) {
    minFactor = SCBA.remainMidMin;
    maxFactor = SCBA.remainMidMax;
    extra = SCBA.remainMidAdd;
  }
  return clamp(rawMin, formulaMin * minFactor, formulaMin * maxFactor + extra);
}

function remainingSec(pressure, blendRate, formulaRate) {
  if (!(pressure > 0) || !(blendRate > 0)) {
    return 0;
  }
  const rawMin = pressure / blendRate;
  const formulaMin = formulaRate > 0 ? pressure / formulaRate : rawMin;
  const minutes = clampRemainMinutes(pressure, rawMin, formulaMin);
  return Math.max(0, Math.round(minutes * SCBA.secondsPerMinute));
}

function demoOf(person) {
  return {
    heightCm: person && person.heightCm,
    weightKg: person && person.weightKg,
    age: person && person.age
  };
}

function elapsedMinutesOf(person, nowMs) {
  const anchor = person.manualPressureAt || person.gmtModified || person.enteredAt;
  const anchorMs = anchor ? new Date(anchor).getTime() : 0;
  if (!Number.isFinite(anchorMs) || !anchorMs) {
    return 0;
  }
  const pausedAtMs = person.timerPausedAt ? new Date(person.timerPausedAt).getTime() : 0;
  const effectiveNow = pausedAtMs > 0 ? Math.min(nowMs, pausedAtMs) : nowMs;
  const previousPausedMin = Math.max(0, Number(person.pressurePausedSeconds || 0)) / SCBA.secondsPerMinute;
  return Math.max(0, (effectiveNow - anchorMs) / 60000 - previousPausedMin);
}

export function estimateNow(person, nowMs) {
  const measured = Number(person.pressureAnchor != null
    ? person.pressureAnchor : (person.currentPressure || person.initPressure || 0));
  const lifecycle = person.lifecycleStatus || person.status;
  if (lifecycle === 'out' || lifecycle === 'scanout' || !person.enteredAt) {
    return { pressure: measured, remainSec: lifecycle === 'pending' ? 0 : null };
  }
  if (lifecycle === 'pending') {
    return { pressure: measured, remainSec: 0 };
  }
  const demo = demoOf(person);
  const formula = formulaRateOf(person.cylType, person.workLevel, person.scene, demo);
  const cal = calibrationRateOf(person.cylType, person.workLevel, person.scene, person.personalK);
  const blend = blendedRate(formula, cal);
  const livePressure = Math.max(0, measured - blend * elapsedMinutesOf(person, nowMs));
  return {
    pressure: livePressure,
    remainSec: remainingSec(livePressure, blend, formula)
  };
}

export function remainSecOf(pressure, cylType, workLevel, scene, personalK, demo) {
  const p = Number(pressure);
  if (!(p > 0)) {
    return 0;
  }
  const formula = formulaRateOf(cylType, workLevel, scene, demo);
  const cal = calibrationRateOf(cylType, workLevel, scene, personalK);
  const blend = blendedRate(formula, cal);
  return remainingSec(p, blend, formula);
}

export function calibrationK(pressure, cylType, fullTimeSec) {
  const minutes = Number(fullTimeSec) / SCBA.secondsPerMinute;
  if (!(minutes > 0) || !(Number(pressure) > 0)) {
    return null;
  }
  const rmv = cylVolumeOf(cylType) * Number(pressure) * SCBA.pressureToVolume / minutes;
  const raw = rmv / SCBA.met.heavy;
  return clamp(raw, SCBA.defaultK * SCBA.personalKMinFactor, SCBA.defaultK * SCBA.personalKMaxFactor);
}

function intensityMultiplier(workLevel, demoFactor) {
  if (workLevel === 'heavy') {
    return 1;
  }
  const base = SCBA.rmv.heavy / rmvOf(workLevel);
  const sensitivity = workLevel === 'light' ? SCBA.lowSensitivity : SCBA.midSensitivity;
  const actual = base * (1 - (demoFactor - 1) * sensitivity);
  return clamp(actual, base * (1 - SCBA.intensityClamp), base * (1 + SCBA.intensityClamp));
}

export function calibrationPreview(input) {
  const pressure = Number(input.pressure);
  const fullTimeSec = Number(input.fullTimeSec);
  const minutes = fullTimeSec / SCBA.secondsPerMinute;
  const volume = cylVolumeOf(input.cylType);
  const demoFactor = demographicFactor(input.heightCm, input.weightKg, input.age);
  const theoryMin = pressure * SCBA.pressureToVolume * volume / SCBA.rmv.heavy;
  const f = clamp(theoryMin / minutes, SCBA.calFactorMin, SCBA.calFactorMax);
  const rmvHeavy = volume * pressure * SCBA.pressureToVolume / minutes;
  const k = calibrationK(pressure, input.cylType, fullTimeSec);
  const alarmSec = Math.round(fullTimeSec * Math.max(0, pressure - SCBA.calAlarmPressure) / pressure);
  const evacSec = Math.round(alarmSec * SCBA.evacRatio);
  const heavyMin = minutes;
  const moderateMin = heavyMin * intensityMultiplier('moderate', demoFactor);
  const lightMin = heavyMin * intensityMultiplier('light', demoFactor);
  return {
    f,
    rmvHeavy,
    k,
    fullSec: Math.round(fullTimeSec),
    alarmSec,
    evacSec,
    moderateSec: Math.round(moderateMin * SCBA.secondsPerMinute),
    lightSec: Math.round(lightMin * SCBA.secondsPerMinute),
    highriseHeavySec: Math.round(heavyMin / sceneFactorOf('highrise') * SCBA.secondsPerMinute),
    searchHeavySec: Math.round(heavyMin / sceneFactorOf('search') * SCBA.secondsPerMinute)
  };
}

/**
 * 判定安全 / 预警 / 危险。
 *
 * @param {string} status 生命周期状态
 * @param {number} pressure 当前压力
 * @param {number} remainSec 剩余秒
 * @param {object} [stationThresholds] 站级阈值（随快照下发）；为空时回落本机设置
 * @returns {string} in / warn / danger / out / pending
 */
export function resolveStatus(status, pressure, remainSec, stationThresholds) {
  if (status === 'out' || status === 'scanout') {
    return 'out';
  }
  if (status === 'pending' || !status) {
    return 'pending';
  }
  // 3.3：站上保存过阈值就以站级阈值为准，指挥端不再用自己设备的设置重算
  const settings = normalizeThresholds(stationThresholds) || getScbaSettings();
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

/**
 * 把服务端下发的阈值换算成与 settings 同形状。
 *
 * @param {object} thresholds 站级阈值
 * @returns {object|null} 设置形状，非法时返回 null
 */
export function normalizeThresholds(thresholds) {
  if (!thresholds) {
    return null;
  }
  const warnPressure = Number(thresholds.warnPressure);
  const dangerPressure = Number(thresholds.dangerPressure);
  const warnTimeMin = Number(thresholds.warnTimeMin);
  const dangerTimeMin = Number(thresholds.dangerTimeMin);
  if (!Number.isFinite(warnPressure) || !Number.isFinite(dangerPressure)) {
    return null;
  }
  return {
    warnPressure,
    dangerPressure,
    warnTimeMin: Number.isFinite(warnTimeMin) ? warnTimeMin : DEFAULT_SCBA_SETTINGS.warnTimeMin,
    dangerTimeMin: Number.isFinite(dangerTimeMin) ? dangerTimeMin : DEFAULT_SCBA_SETTINGS.dangerTimeMin,
    defaultWorkLevel: thresholds.defaultWorkLevel || DEFAULT_SCBA_SETTINGS.defaultWorkLevel,
    voiceEnabled: thresholds.voiceEnabled !== false
  };
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
