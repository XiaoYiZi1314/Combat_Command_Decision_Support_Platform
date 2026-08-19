import './styles/tokens.css';
import { bridge } from './bridge/index.js';
import { currentHash, matchRoute } from './routes.js';
import { renderBlankPage } from './pages/blank.js';

const app = document.getElementById('app');

function render() {
  const hash = currentHash();
  const { route, params } = matchRoute(hash);
  app.innerHTML = '';

  const shell = document.createElement('div');
  shell.className = 'shell';

  const header = document.createElement('header');
  header.className = 'header';
  const h1 = document.createElement('h1');
  h1.textContent = '作战指挥辅助决策平台';
  const sub = document.createElement('div');
  sub.className = 'sub';
  sub.textContent = '一期骨架';
  header.appendChild(h1);
  header.appendChild(sub);

  const page = document.createElement('section');
  page.className = 'page';
  renderBlankPage(page, route, params, hash);

  const mock = document.createElement('button');
  mock.type = 'button';
  mock.className = 'btn-glass primary';
  mock.textContent = '模拟 NFC';
  mock.addEventListener('click', async () => {
    const result = await bridge.nfcRead();
    mock.textContent = result.ok ? `NFC ${result.data.tag}` : '本机无 NFC';
  });
  page.appendChild(mock);

  shell.appendChild(header);
  shell.appendChild(page);
  app.appendChild(shell);
}

window.addEventListener('hashchange', render);
if (!window.location.hash) {
  window.location.hash = '#/login';
} else {
  render();
}

window.CcdsBridge = bridge;
