import './login.css';
import { login, changePassword } from '../../api/auth.js';
import { clearSession, getMe, hasSession, homeHashOf } from '../../stores/session.js';

const SLOGAN = ['对党忠诚', '纪律严明', '赴汤蹈火', '竭诚为民'];
const HINT = '消防站使用本单位首拼账号登录；大队账号查看所属队站；支队账号可查看全部单位实时内攻状态。';
const MUST_CHANGE = '首次登录必须修改密码，新密码不少于8位。';

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

function field(labelText, inputId, type, placeholder) {
  const wrap = el('div', 'login-field');
  const label = el('label');
  label.setAttribute('for', inputId);
  label.textContent = labelText;
  const input = el('input');
  input.id = inputId;
  input.type = type;
  input.placeholder = placeholder;
  input.autocomplete = type === 'password' ? 'current-password' : 'username';
  input.autocapitalize = 'none';
  wrap.appendChild(label);
  wrap.appendChild(input);
  return { wrap, input };
}

export function renderLoginPage(root, options) {
  const opts = options || {};
  root.innerHTML = '';
  const gate = el('div', 'login-gate');
  gate.appendChild(el('div', 'login-scene'));

  const slogan = el('div', 'login-slogan');
  SLOGAN.forEach((line) => slogan.appendChild(el('span', '', line)));
  gate.appendChild(slogan);

  const brand = el('div', 'login-brand');
  const badge = el('div', 'login-badge', '消');
  badge.setAttribute('aria-hidden', 'true');
  brand.appendChild(badge);
  brand.appendChild(el('div', 'login-brand-title', '齐齐哈尔市消防救援支队'));
  gate.appendChild(brand);

  const form = el('form', 'login-box');
  form.appendChild(el('h2', '', opts.mustChange ? '首次登录修改密码' : '作战指挥辅助决策平台登录'));
  form.appendChild(el('p', '', opts.mustChange ? MUST_CHANGE : HINT));

  const userField = field('账号', 'loginUser', 'text', '作战指挥辅助决策平台');
  const passField = field('密码', 'loginPass', 'password', opts.mustChange ? '当前密码' : '密码');
  const currentMe = getMe();
  if (opts.mustChange && currentMe && currentMe.username) {
    userField.input.value = currentMe.username;
    userField.input.readOnly = true;
  }
  form.appendChild(userField.wrap);
  form.appendChild(passField.wrap);

  let newPassInput = null;
  if (opts.mustChange) {
    const next = field('新密码', 'loginNewPass', 'password', '不少于8位');
    next.input.autocomplete = 'new-password';
    form.appendChild(next.wrap);
    newPassInput = next.input;
  }

  const error = el('div', 'login-error', opts.message || '');
  form.appendChild(error);

  const submit = el('button', 'login-btn', opts.mustChange ? '确认改密并进入' : '登录');
  submit.type = 'submit';
  form.appendChild(submit);

  const clearBtn = el('button', 'login-switch-btn', '清除当前账号并重新登录');
  clearBtn.type = 'button';
  form.appendChild(clearBtn);
  gate.appendChild(form);
  root.appendChild(gate);

  clearBtn.addEventListener('click', () => {
    clearSession();
    error.textContent = '已清除当前账号，请输入站级、大队或支队账号';
    userField.input.value = '';
    passField.input.value = '';
    if (newPassInput) {
      newPassInput.value = '';
    }
    userField.input.focus();
  });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const username = (userField.input.value || '').trim().toLowerCase();
    const password = (passField.input.value || '').trim();
    if (!username || !password) {
      error.textContent = '请输入账号和密码';
      return;
    }
    submit.disabled = true;
    error.textContent = '';
    try {
      if (opts.mustChange) {
        const nextPassword = (newPassInput.value || '').trim();
        if (nextPassword.length < 8) {
          error.textContent = '新密码不少于8位';
          submit.disabled = false;
          return;
        }
        if (!hasSession()) {
          const data = await login(username, password);
          if (!data.mustChangePassword) {
            goHome(data.me);
            return;
          }
        }
        const changed = await changePassword(password, nextPassword);
        goHome(changed.me);
        return;
      }
      const data = await login(username, password);
      if (data.mustChangePassword) {
        window.location.hash = '#/login?change=1';
        if (typeof opts.onMustChange === 'function') {
          opts.onMustChange();
        }
        return;
      }
      goHome(data.me);
    } catch (err) {
      error.textContent = err.message || '账号或密码错误';
    } finally {
      submit.disabled = false;
    }
  });

  setTimeout(() => userField.input.focus(), 0);
}

function goHome(me) {
  window.location.hash = homeHashOf(me);
}
