import './drawer.css';
import { getMe } from '../../stores/session.js';

/**
 * 「更多功能」抽屉：与客户 APK 同款分区磁贴布局。
 * 每项 [label, icon, hash, hqHidden]；hqHidden 表示支队/汇总账号隐藏。
 */
const ZONES = [
  {
    title: '人员与信息',
    items: [
      ['NFC联动记录', 'nfc.png', '#/nfc', true],
      ['人员档案', 'roster.png', '#/roster'],
      ['值班表', 'duty.png', '#/duty'],
      ['空呼时间校准计算器', 'scba.png', '#/scba']
    ]
  },
  {
    title: '水源与车辆信息',
    items: [
      ['水源档案', 'hydrant.png', '#/water'],
      ['车辆档案', 'vehicle.png', '#/vehicles']
    ]
  },
  {
    title: '现场辅助',
    items: [
      ['供水计算', 'supply.png', '#/supply'],
      ['重点单位', 'unit.png', '#/key-units'],
      ['二维码共享实时状态', 'share.png', '#/share'],
      ['内攻历史', 'history.png', '#/attack-history'],
      ['天气方位', 'compass.png', '#/weather'],
      ['危化品', 'hazmat.png', '#/hazmat'],
      ['AI助手', 'ai.png', '#/ai']
    ]
  },
  {
    title: '文书与系统',
    items: [
      ['文书生成', 'doc.png', '#/doc'],
      ['版本信息', 'version.png', '#/version']
    ]
  }
];

function el(tag, className, text) {
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
 * 建立抽屉 DOM。h5 基础路径为 './'，图标用相对地址 more_fn/x.png。
 *
 * @param {Function} onClose 关闭回调
 * @returns {DocumentFragment} 抽屉节点
 */
export function buildDrawer(onClose) {
  const me = getMe() || {};
  const wrap = document.createDocumentFragment();
  const mask = el('div', 'drawer-mask');
  const drawer = el('aside', 'drawer');
  const headRow = el('div', 'drawer-head');
  headRow.appendChild(el('h2', '', '更多功能'));
  const close = el('button', 'drawer-close', '✕');
  close.type = 'button';
  close.addEventListener('click', onClose);
  headRow.appendChild(close);
  drawer.appendChild(headRow);
  const body = el('div', 'drawer-body');
  ZONES.forEach((zone) => {
    const section = el('section', 'drawer-zone');
    section.appendChild(el('div', 'drawer-zone-title', zone.title));
    const grid = el('div', 'drawer-grid');
    zone.items.forEach((item) => {
      const btn = el('button', 'drawer-tile');
      btn.type = 'button';
      const icon = el('span', 'drawer-tile-icon');
      const img = document.createElement('img');
      img.src = `more_fn/${item[1]}`;
      img.alt = '';
      icon.appendChild(img);
      btn.appendChild(icon);
      btn.appendChild(el('span', 'drawer-tile-label', item[0]));
      /* 客户 APK：支队/汇总账号不开放 NFC 联动记录 */
      if (item[3] && me.role === 'hq') {
        btn.classList.add('is-hidden');
      }
      btn.addEventListener('click', () => {
        if (btn.classList.contains('is-hidden')) {
          return;
        }
        onClose();
        window.location.hash = item[2];
      });
      grid.appendChild(btn);
    });
    section.appendChild(grid);
    body.appendChild(section);
  });
  drawer.appendChild(body);
  mask.addEventListener('click', onClose);
  wrap.appendChild(mask);
  wrap.appendChild(drawer);
  return wrap;
}
