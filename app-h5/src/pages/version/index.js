import '../water/water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { el } from '../water/shared.js';
import { bridge } from '../../bridge/index.js';
import { apiRequest, authHeader } from '../../api/client.js';

const IGNORED_KEY = 'ccds_version_ignored';

function readIgnored() {
  try {
    return Number(localStorage.getItem(IGNORED_KEY) || 0);
  } catch (err) {
    return 0;
  }
}

function writeIgnored(code) {
  try {
    localStorage.setItem(IGNORED_KEY, String(code));
  } catch (err) {
    // 存储不可用只影响「忽略本版」的持久化
  }
}

/**
 * 读取本机壳版本号，读不到返回 null。
 */
function localVersion() {
  return bridge.getVersion().then((result) => {
    if (!result || !result.ok || !result.data) {
      return null;
    }
    return {
      name: result.data.versionName || '',
      code: result.data.versionCode == null ? null : Number(result.data.versionCode)
    };
  }).catch(() => null);
}

export function renderVersionPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', '版本信息'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(me);
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  // 5.2：去掉「一期」字样，写软件名称、当前版本名与版本编号
  const info = el('div', 'hq-card');
  info.appendChild(el('div', 'name', '作战指挥辅助决策平台'));
  const meta = el('div', 'hq-meta', '读取中…');
  info.appendChild(meta);
  body.appendChild(info);

  const msg = el('div', 'water-msg');
  body.appendChild(msg);

  const actions = el('div', 'water-actions');
  const checkBtn = el('button', '', '检查更新');
  checkBtn.type = 'button';
  actions.appendChild(checkBtn);
  body.appendChild(actions);

  const result = el('div', 'supply-result');
  body.appendChild(result);

  const guides = el('div', 'water-actions');
  const installBtn = el('button', '', '安装教程');
  const operationBtn = el('button', '', '操作教程');
  const legalBtn = el('button', '', '免责声明');
  installBtn.type = 'button';
  operationBtn.type = 'button';
  legalBtn.type = 'button';
  installBtn.addEventListener('click', () => {
    window.location.hash = '#/guide/install';
  });
  operationBtn.addEventListener('click', () => {
    window.location.hash = '#/guide/operation';
  });
  legalBtn.addEventListener('click', () => {
    window.location.hash = '#/legal?view=1';
  });
  guides.appendChild(installBtn);
  guides.appendChild(operationBtn);
  guides.appendChild(legalBtn);
  body.appendChild(guides);

  page.appendChild(body);
  root.appendChild(page);

  function setMsg(text, error) {
    msg.className = error ? 'water-msg error' : 'water-msg';
    msg.textContent = text || '';
  }

  let current = null;
  Promise.all([localVersion(), fetchServerVersion()]).then(([shell, server]) => {
    current = shell;
    const versionName = (server && server.versionName) || (shell && shell.name) || 'unknown';
    const versionCode = server && server.versionCode != null
      ? server.versionCode : (shell && shell.code);
    meta.textContent = versionCode == null ? versionName : `${versionName}（版本编号 ${versionCode}）`;
  }).catch(() => {
    meta.textContent = '未知版本';
  });

  checkBtn.addEventListener('click', async () => {
    setMsg('检查更新中…');
    result.innerHTML = '';
    let server;
    try {
      server = await fetchServerVersion();
    } catch (err) {
      setMsg('检查更新失败，请检查网络后重试', true);
      return;
    }
    if (!server) {
      setMsg('检查更新失败：服务端未返回版本信息', true);
      return;
    }
    const shell = await localVersion();
    const localCode = shell && shell.code != null ? shell.code : (server.versionCode || 0);
    const remoteCode = Number(server.versionCode || 0);
    if (remoteCode <= localCode) {
      setMsg('当前已是最新版本');
      return;
    }
    if (readIgnored() >= remoteCode) {
      setMsg('已忽略该版本，可在更新页手动查看');
      return;
    }
    setMsg(`发现新版本 ${server.versionName}（版本编号 ${remoteCode}）`);
    if (server.releaseNotes) {
      result.appendChild(el('div', 'supply-line', `更新说明：${server.releaseNotes}`));
    }
    const buttons = el('div', 'water-actions');
    const open = el('button', 'wf-save', '打开更新页');
    open.type = 'button';
    open.addEventListener('click', () => {
      if (server.updatePageUrl) {
        bridge.openUrl(server.updatePageUrl);
      } else {
        setMsg('未配置更新页地址，请联系管理员', true);
      }
    });
    // 强制更新时不给「稍后」与「忽略」
    buttons.appendChild(open);
    if (!server.force) {
      const later = el('button', 'wf-cancel', '稍后提醒');
      later.type = 'button';
      later.addEventListener('click', () => {
        result.innerHTML = '';
        setMsg('已稍后提醒');
      });
      const ignore = el('button', 'wf-cancel', '忽略本版');
      ignore.type = 'button';
      ignore.addEventListener('click', () => {
        writeIgnored(remoteCode);
        result.innerHTML = '';
        setMsg('已忽略该版本');
      });
      buttons.appendChild(later);
      buttons.appendChild(ignore);
    }
    result.appendChild(buttons);
  });
}

function fetchServerVersion() {
  return apiRequest('/app/version', { headers: authHeader() }).catch((err) => {
    if (err && err.code === 'NETWORK') {
      throw err;
    }
    // 未登录等场景下拿不到也不阻塞展示壳版本
    return null;
  });
}
