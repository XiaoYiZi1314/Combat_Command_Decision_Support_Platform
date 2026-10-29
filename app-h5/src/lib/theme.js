/**
 * 户外模式开关。
 *
 * <p>只切换根节点上的 outdoor 类，具体配色全部由 outdoor.css 以 `.outdoor` 前缀
 * 且限定在主界面与指挥首页子树内实现，其余页面（值班表、设置、水源、车辆）保持深色。</p>
 */

const OUTDOOR_KEY = 'ccds_outdoor_mode';

/**
 * 读取户外模式开关。
 *
 * @returns {boolean} 是否开启
 */
export function isOutdoor() {
  try {
    return localStorage.getItem(OUTDOOR_KEY) === '1';
  } catch (err) {
    return false;
  }
}

/**
 * 应用户外模式到根节点。
 *
 * @param {boolean} enabled 是否开启
 */
export function applyOutdoor(enabled) {
  const root = document.documentElement;
  if (!root) {
    return;
  }
  if (enabled) {
    root.classList.add('outdoor');
  } else {
    root.classList.remove('outdoor');
  }
}

/**
 * 保存并应用户外模式。
 *
 * @param {boolean} enabled 是否开启
 * @returns {boolean} 当前状态
 */
export function setOutdoor(enabled) {
  try {
    localStorage.setItem(OUTDOOR_KEY, enabled ? '1' : '0');
  } catch (err) {
    // 存储不可用时只影响持久化，不影响本次显示
  }
  applyOutdoor(enabled);
  return enabled;
}

/**
 * 冷启动时恢复上次选择。
 */
export function restoreOutdoor() {
  applyOutdoor(isOutdoor());
}

/**
 * 渲染一个户外模式开关按钮。
 *
 * @param {Function} onChange 状态变化回调
 * @returns {HTMLButtonElement} 按钮
 */
export function buildOutdoorToggle(onChange) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'btn-icon outdoor-toggle';
  const paint = () => {
    const on = isOutdoor();
    button.textContent = on ? '☀' : '☾';
    button.setAttribute('aria-label', on ? '关闭户外模式' : '打开户外模式');
    button.title = on ? '关闭户外模式' : '打开户外模式';
  };
  paint();
  button.addEventListener('click', () => {
    const next = setOutdoor(!isOutdoor());
    paint();
    if (typeof onChange === 'function') {
      onChange(next);
    }
  });
  return button;
}
