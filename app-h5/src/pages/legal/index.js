import './legal.css';
import { agreeLegal, hasSession, homeHashOf, getMe } from '../../stores/session.js';

const LEGAL_TEXT = '本软件的空呼使用时间由 AI 结合数学预测模型推算，可能与实际使用存在误差。安全员和指挥员应结合火场环境、人员状态和装备情况综合判断并及时提醒内攻人员。灭火救援行动中如因实际情况变化发生人员伤亡，软件制作者不承担责任。本软件已申请软件著作权，供齐齐哈尔支队战友免费使用，请尊重知识产权，未经允许不得抄袭、搬运或二次发布，否则依法承担责任。';

export function renderLegalPage(root) {
  root.innerHTML = '';
  const mask = document.createElement('div');
  mask.className = 'legal-mask';
  mask.setAttribute('role', 'dialog');
  mask.setAttribute('aria-modal', 'true');

  const card = document.createElement('div');
  card.className = 'legal-card';

  const title = document.createElement('div');
  title.className = 'legal-title';
  title.id = 'legalNoticeTitle';
  title.textContent = '免责声明与知识产权保护声明';

  const text = document.createElement('div');
  text.className = 'legal-text';
  text.textContent = LEGAL_TEXT;

  const button = document.createElement('button');
  button.type = 'button';
  button.textContent = '同意并继续';
  button.addEventListener('click', () => {
    agreeLegal();
    if (hasSession()) {
      window.location.hash = homeHashOf(getMe());
      return;
    }
    window.location.hash = '#/login';
  });

  card.appendChild(title);
  card.appendChild(text);
  card.appendChild(button);
  mask.appendChild(card);
  root.appendChild(mask);
}
