export const ROUTES = [
  { path: '/login', title: '登录', file: 'login' },
  { path: '/legal', title: '免责声明', file: 'legal' },
  { path: '/attack', title: '内攻主界面', file: 'attack' },
  { path: '/attack/quick-add', title: '快速录入', file: 'attack-quick-add' },
  { path: '/settings', title: '参数设置', file: 'settings' },
  { path: '/roster', title: '花名册', file: 'roster' },
  { path: '/roster/:id', title: '编辑档案', file: 'roster-edit' },
  { path: '/weather', title: '天气与方位', file: 'weather' },
  { path: '/scba', title: '空呼时间计算器', file: 'scba' },
  { path: '/water', title: '水源档案', file: 'water' },
  { path: '/water/map', title: '水源地图', file: 'water-map' },
  { path: '/share', title: '二维码共享', file: 'share' },
  { path: '/hazmat', title: '危化品查询', file: 'hazmat' },
  { path: '/duty', title: '值班表', file: 'duty' },
  { path: '/supply', title: '供水计算', file: 'supply' },
  { path: '/supply/map', title: '选火灾地点', file: 'supply-map' },
  { path: '/ai', title: 'AI 助手', file: 'ai' },
  { path: '/key-units', title: '重点单位', file: 'key-units' },
  { path: '/key-units/:id', title: '预案详情', file: 'key-units-edit' },
  { path: '/hq', title: '指挥汇总', file: 'hq' },
  { path: '/hq/station/:stationId', title: '单站态势', file: 'hq-station' },
  { path: '/version', title: '版本信息', file: 'version' }
];

const PARAM = /^:([A-Za-z0-9_]+)$/;

function splitPath(hashPath) {
  const raw = String(hashPath || '').replace(/^#/, '');
  const path = raw.startsWith('/') ? raw : `/${raw}`;
  return path.split('/').filter(Boolean);
}

export function matchRoute(hashPath) {
  const parts = splitPath(hashPath);
  for (const route of ROUTES) {
    const expected = route.path.split('/').filter(Boolean);
    if (expected.length !== parts.length) {
      continue;
    }
    const params = {};
    let matched = true;
    for (let i = 0; i < expected.length; i += 1) {
      const token = expected[i];
      const param = PARAM.exec(token);
      if (param) {
        params[param[1]] = decodeURIComponent(parts[i]);
        continue;
      }
      if (token !== parts[i]) {
        matched = false;
        break;
      }
    }
    if (matched) {
      return { route, params };
    }
  }
  return {
    route: { path: '/blank', title: '未匹配路由', file: 'blank' },
    params: {}
  };
}

export function currentHash() {
  const hash = window.location.hash || '#/login';
  return hash.startsWith('#') ? hash : `#${hash}`;
}
