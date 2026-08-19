import './styles/tokens.css';
import { currentHash, matchRoute } from './routes.js';
import { renderBlankPage } from './pages/blank.js';
import { renderLoginPage } from './pages/login/index.js';
import { renderLegalPage } from './pages/legal/index.js';
import { fetchMe, refreshSession } from './api/auth.js';
import {
  getAccessToken,
  getMe,
  getRefreshToken,
  hasSession,
  homeHashOf,
  isLegalAgreed,
  saveSession
} from './stores/session.js';

const app = document.getElementById('app');
let bootstrapped = false;

function queryFlag(hash, name) {
  const qIndex = hash.indexOf('?');
  if (qIndex < 0) {
    return false;
  }
  return new URLSearchParams(hash.slice(qIndex + 1)).get(name) === '1';
}

function renderShell(route, params, hash) {
  app.innerHTML = '';
  const shell = document.createElement('div');
  shell.className = 'shell';
  const header = document.createElement('header');
  header.className = 'header';
  const h1 = document.createElement('h1');
  const me = getMe();
  h1.textContent = me && me.stationName ? me.stationName : '作战指挥辅助决策平台';
  const sub = document.createElement('div');
  sub.className = 'sub';
  sub.textContent = route.title;
  header.appendChild(h1);
  header.appendChild(sub);
  const page = document.createElement('section');
  page.className = 'page';
  renderBlankPage(page, route, params, hash);
  shell.appendChild(header);
  shell.appendChild(page);
  app.appendChild(shell);
}

async function restoreSession() {
  if (!hasSession()) {
    return null;
  }
  try {
    const me = await fetchMe();
    saveSession({
      accessToken: getAccessToken(),
      refreshToken: getRefreshToken(),
      me
    });
    return me;
  } catch (err) {
    if (err.code === 'NETWORK') {
      return getMe();
    }
    const refreshed = await refreshSession();
    return refreshed ? refreshed.me : null;
  }
}

async function render() {
  const hash = currentHash();
  const pathOnly = hash.split('?')[0];
  const { route, params } = matchRoute(pathOnly);

  if (!isLegalAgreed() && route.path !== '/legal') {
    window.location.hash = '#/legal';
    return;
  }

  if (route.path === '/legal') {
    if (isLegalAgreed()) {
      window.location.hash = hasSession() ? homeHashOf(getMe()) : '#/login';
      return;
    }
    app.innerHTML = '';
    renderLegalPage(app);
    return;
  }

  if (!bootstrapped) {
    bootstrapped = true;
    const me = await restoreSession();
    if (me && route.path === '/login' && !me.mustChangePassword) {
      window.location.hash = homeHashOf(me);
      return;
    }
  }

  if (route.path === '/login') {
    app.innerHTML = '';
    renderLoginPage(app, {
      mustChange: queryFlag(hash, 'change') || Boolean(getMe() && getMe().mustChangePassword),
      onMustChange() {
        render();
      }
    });
    return;
  }

  if (!hasSession()) {
    window.location.hash = '#/login';
    return;
  }

  const me = getMe();
  if (me && me.mustChangePassword && route.path !== '/login') {
    window.location.hash = '#/login?change=1';
    return;
  }

  renderShell(route, params, hash);
}

window.addEventListener('hashchange', () => {
  render();
});

if (!window.location.hash) {
  window.location.hash = '#/login';
} else {
  render();
}
