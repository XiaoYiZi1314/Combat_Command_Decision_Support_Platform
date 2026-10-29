import '../water/water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { el } from '../water/shared.js';

const INSTALL_STEPS = [
  '把安装包（APK）拷到手机上，或扫描管理员提供的下载二维码。',
  '点击安装包，按系统提示允许「安装未知来源应用」。',
  '安装完成后打开「作战指挥辅助决策平台」，首次启动会提示免责声明，确认后进入登录页。',
  '用本单位首拼账号登录，进入本站内攻主界面。',
  '如需 NFC 扫描，进入手机设置允许本应用使用 NFC 与相机权限。'
];

const OPERATION_STEPS = [
  '登录：消防站用单位首拼账号；大队用首拼加 dd；支队账号可看全部单位实时内攻。',
  '模式：主界面顶部选择测试、训练或出警。只有出警且已有人入场，支队首页才会出现本站。',
  '录入：花名册选人或 NFC 扫描预录入，确认气瓶压力与规格后入场开始计时。',
  '监控：人员卡片按压力与剩余时间显示安全、预警、危险，颜色与阈值以本站保存的为准。',
  '复测与撤出：安全员可随时复测压力校正剩余时间；人员撤出后卡片标记已撤出。',
  '归档：全部撤出后可归档本次行动，归档记录带模式，可在内攻历史中按训练或出警筛选。',
  '水源：可查看本单位或全市水源，导入全市检查表，不好用的卡需提交审批后生效。'
];

function renderGuide(root, title, steps) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', title));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = '#/version';
  });
  head.appendChild(back);
  page.appendChild(head);
  const body = el('div', 'water-body');
  steps.forEach((step, index) => {
    body.appendChild(el('div', 'supply-line', `${index + 1}. ${step}`));
  });
  body.appendChild(el('div', 'supply-hint',
    '教程内容为内置离线说明，可在拿到正式图文教程后替换。'));
  page.appendChild(body);
  root.appendChild(page);
  return homeHashOf(me);
}

export function renderInstallGuidePage(root) {
  renderGuide(root, '安装教程', INSTALL_STEPS);
}

export function renderOperationGuidePage(root) {
  renderGuide(root, '操作教程', OPERATION_STEPS);
}
