let pageHandler = null;

export function setPageBackHandler(handler) {
  pageHandler = typeof handler === 'function' ? handler : null;
}

export function consumePageBack() {
  if (typeof pageHandler === 'function') {
    return Boolean(pageHandler());
  }
  return false;
}

export function confirmLeaveMain() {
  return window.confirm('确定退出？本机卡片缓存会保留。');
}
