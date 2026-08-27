const SETTINGS_KEY = 'ccds_scba_settings';
const PRESSURE_MAX = 30;

export const DEFAULT_SCBA_SETTINGS = Object.freeze({
  warnPressure: 10,
  dangerPressure: 4,
  warnTimeMin: 12,
  dangerTimeMin: 5,
  defaultWorkLevel: 'moderate',
  voiceEnabled: true
});

function normalize(raw) {
  const source = raw && typeof raw === 'object' ? raw : {};
  const settings = {
    warnPressure: Number(source.warnPressure),
    dangerPressure: Number(source.dangerPressure),
    warnTimeMin: Number(source.warnTimeMin),
    dangerTimeMin: Number(source.dangerTimeMin),
    defaultWorkLevel: ['light', 'moderate', 'heavy'].includes(source.defaultWorkLevel)
      ? source.defaultWorkLevel : DEFAULT_SCBA_SETTINGS.defaultWorkLevel,
    voiceEnabled: source.voiceEnabled == null
      ? DEFAULT_SCBA_SETTINGS.voiceEnabled : Boolean(source.voiceEnabled)
  };
  if (!(settings.warnPressure > 0 && settings.warnPressure <= PRESSURE_MAX)) {
    settings.warnPressure = DEFAULT_SCBA_SETTINGS.warnPressure;
  }
  if (!(settings.dangerPressure >= 0 && settings.dangerPressure < settings.warnPressure)) {
    settings.dangerPressure = DEFAULT_SCBA_SETTINGS.dangerPressure;
  }
  if (!(settings.warnTimeMin > 0 && settings.warnTimeMin <= 120)) {
    settings.warnTimeMin = DEFAULT_SCBA_SETTINGS.warnTimeMin;
  }
  if (!(settings.dangerTimeMin >= 0 && settings.dangerTimeMin < settings.warnTimeMin)) {
    settings.dangerTimeMin = DEFAULT_SCBA_SETTINGS.dangerTimeMin;
  }
  return settings;
}

export function getScbaSettings() {
  try {
    return normalize(JSON.parse(localStorage.getItem(SETTINGS_KEY) || 'null'));
  } catch (err) {
    console.warn('scba settings parse failed', err.name);
    return normalize(null);
  }
}

export function saveScbaSettings(settings) {
  const normalized = normalize(settings);
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(normalized));
  return normalized;
}

export function resetScbaSettings() {
  localStorage.removeItem(SETTINGS_KEY);
  return getScbaSettings();
}
