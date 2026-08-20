import { renderAttackPage } from '../attack/index.js';

export function renderHqStationPage(root, params) {
  const stationId = params && params.stationId ? String(params.stationId) : '';
  return renderAttackPage(root, {
    stationId,
    readonly: true
  });
}
