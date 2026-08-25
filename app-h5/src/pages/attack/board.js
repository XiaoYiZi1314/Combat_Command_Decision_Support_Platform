import {
  SCBA,
  fmtElapsed,
  fmtMinSec,
  remainSecOf,
  resolveStatus,
  statusLabel,
  worstStatus
} from '../../lib/scba.js';

export function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  if (text) {
    node.textContent = text;
  }
  return node;
}

export function livePerson(person, nowMs) {
  const copy = Object.assign({}, person);
  const entered = copy.enteredAt ? new Date(copy.enteredAt).getTime() : 0;
  if (copy.status === 'out' || copy.status === 'pending' || !entered) {
    copy.liveRemain = null;
    copy.liveElapsed = 0;
    copy.liveStatus = copy.status || 'pending';
    return copy;
  }
  const elapsedSec = Math.max(0, Math.floor((nowMs - entered) / 1000));
  const modified = copy.gmtModified ? new Date(copy.gmtModified).getTime() : entered;
  const sinceMeasure = Math.max(0, Math.floor((nowMs - Math.max(entered, modified)) / 1000));
  copy.liveElapsed = elapsedSec;
  copy.liveRemain = Math.max(0, remainSecOf(
    copy.currentPressure,
    copy.cylType,
    copy.workLevel,
    copy.scene,
    copy.personalK
  ) - sinceMeasure);
  copy.liveStatus = resolveStatus(copy.status, copy.currentPressure, copy.liveRemain);
  return copy;
}

export function livePersons(attack, nowMs) {
  return ((attack && attack.persons) || []).map((person) => livePerson(person, nowMs || Date.now()));
}

export function countsOf(persons) {
  const counts = { in: 0, warn: 0, danger: 0, out: 0, pending: 0 };
  persons.forEach((p) => {
    const key = p.liveStatus;
    if (counts[key] != null) {
      counts[key] += 1;
    }
  });
  return counts;
}

export function paintStats(view) {
  const statsEl = view.statsEl;
  const filter = view.filter;
  const persons = view.persons || [];
  const counts = countsOf(persons);
  statsEl.innerHTML = '';
  [
    ['in', '安全', 'var(--blue)', counts.in],
    ['warn', '预警', 'var(--amber)', counts.warn],
    ['danger', '危险', 'var(--red)', counts.danger],
    ['out', '已撤出', 'var(--dim)', counts.out]
  ].forEach((item) => {
    const box = el('div', filter === item[0] ? 'stat active' : 'stat');
    const num = el('div', 'num', String(item[3]));
    num.style.color = item[2];
    box.appendChild(num);
    box.appendChild(el('div', 'label', item[1]));
    box.addEventListener('click', () => {
      if (typeof view.onFilter === 'function') {
        view.onFilter(filter === item[0] ? '' : item[0]);
      }
    });
    statsEl.appendChild(box);
  });
}

export function paintPersonCard(person, focusId) {
  const card = el('div', `card ${person.liveStatus}`);
  card.dataset.personId = String(person.id);
  if (String(focusId) === String(person.id)) {
    card.classList.add('focus');
  }
  const top = el('div', 'top');
  const name = el('div', 'name', person.displayName || '');
  name.appendChild(el('span', 'cyl-tag', `${person.cylType || '6.8'}L`));
  if (person.groupName) {
    name.appendChild(el('span', 'group-tag', person.groupName));
  }
  if (person.temp) {
    name.appendChild(el('span', 'temp-tag', '临时'));
  }
  if (person.calibrated) {
    name.appendChild(el('span', 'curve-tag', '标定'));
  }
  top.appendChild(name);
  top.appendChild(el('div', `status s-${person.liveStatus}`, statusLabel(person.liveStatus)));
  card.appendChild(top);

  const metrics = el('div', 'metrics');
  const pressure = Number(person.currentPressure || 0);
  const remain = person.liveRemain;
  const pCls = pressure <= SCBA.dangerPressure ? 'c-danger' : (pressure <= SCBA.warnPressure ? 'c-warn' : 'c-ok');
  const rCls = remain != null && remain <= SCBA.dangerTimeSec ? 'c-danger' : (remain != null && remain <= SCBA.warnTimeSec ? 'c-warn' : 'c-ok');
  metrics.appendChild(metric('当前压力', pressure.toFixed(1), 'MPa', pCls));
  metrics.appendChild(metric('剩余时间', person.liveStatus === 'pending' || person.liveStatus === 'out' ? '--' : fmtMinSec(remain), '', rCls));
  metrics.appendChild(metric('已作业', person.liveStatus === 'pending' ? '--' : fmtElapsed(person.liveElapsed), '', 'c-ok'));
  card.appendChild(metrics);

  const bar = el('div', 'bar');
  const fill = el('div', 'bar-fill');
  const init = Number(person.initPressure || 0);
  fill.style.width = init > 0 ? `${Math.min(100, pressure / init * 100)}%` : '0%';
  fill.style.background = pressure <= SCBA.dangerPressure ? 'var(--red)' : (pressure <= SCBA.warnPressure ? 'var(--amber)' : 'var(--blue)');
  bar.appendChild(fill);
  card.appendChild(bar);
  return card;
}

export function paintPeopleAndGroups(view) {
  const peoplePage = view.peoplePage;
  const groupPage = view.groupPage;
  const persons = view.persons || [];
  const filter = view.filter;
  const stationName = view.stationName || '本站';
  peoplePage.innerHTML = '';
  const filtered = filter ? persons.filter((p) => p.liveStatus === filter) : persons;
  if (!filtered.length) {
    const empty = el('div', 'empty');
    empty.appendChild(el('div', 'station-badge', stationName.slice(0, 1)));
    empty.appendChild(el('div', 'station', stationName));
    empty.appendChild(el('p', '', '暂无内攻人员'));
    peoplePage.appendChild(empty);
    paintGroups(groupPage, persons);
    return;
  }
  filtered.forEach((person) => {
    const card = typeof view.buildCard === 'function' ? view.buildCard(person) : paintPersonCard(person, view.focusId);
    peoplePage.appendChild(card);
  });
  paintGroups(groupPage, persons);
}

function paintGroups(groupPage, persons) {
  groupPage.innerHTML = '';
  const map = {};
  persons.forEach((p) => {
    const name = p.groupName || SCBA.ungrouped;
    if (!map[name]) {
      map[name] = { name, items: [] };
    }
    map[name].items.push(p);
  });
  const names = Object.keys(map);
  if (!names.length) {
    groupPage.appendChild(el('div', 'empty', '暂无战斗编组'));
    return;
  }
  names.forEach((name) => {
    const row = map[name];
    const card = el('div', 'group-overview-card');
    const worst = worstStatus(row.items.map((item) => item.liveStatus));
    card.appendChild(el('div', 'gc-name', `${name} · ${statusLabel(worst)}`));
    const counts = countsOf(row.items);
    card.appendChild(el('div', 'gc-counts', `安全 ${counts.in}  预警 ${counts.warn}  危险 ${counts.danger}  预录入 ${counts.pending}`));
    groupPage.appendChild(card);
  });
}

function metric(label, value, unit, cls) {
  const box = el('div', 'metric');
  const val = el('div', `val ${cls}`, unit ? `${value} ${unit}` : value);
  box.appendChild(val);
  box.appendChild(el('div', 'lbl', label));
  return box;
}
