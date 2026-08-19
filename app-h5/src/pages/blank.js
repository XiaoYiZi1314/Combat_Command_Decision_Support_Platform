export function renderBlankPage(root, route, params, hash) {
  const keys = Object.keys(params || {});
  const extra = keys.length
    ? keys.map((key) => `${key}=${params[key]}`).join(' ')
    : '一期空页';
  root.innerHTML = '';

  const title = document.createElement('h2');
  title.textContent = route.title;

  const desc = document.createElement('p');
  desc.textContent = extra;

  const hashLine = document.createElement('p');
  hashLine.className = 'hash';
  hashLine.textContent = hash;

  root.appendChild(title);
  root.appendChild(desc);
  root.appendChild(hashLine);
}
