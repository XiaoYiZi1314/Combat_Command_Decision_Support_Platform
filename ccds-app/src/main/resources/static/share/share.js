(function () {
  'use strict';

  var POLL_MS = 15000;
  var STATUS = {
    in: { text: '安全', className: 'in' },
    warn: { text: '预警', className: 'warn' },
    danger: { text: '危险', className: 'danger' },
    out: { text: '已撤出', className: 'out' }
  };
  var stationName = document.getElementById('stationName');
  var updateTime = document.getElementById('updateTime');
  var notice = document.getElementById('notice');
  var groups = document.getElementById('groups');
  var persons = document.getElementById('persons');
  var token = tokenFromPath();
  var timer = null;

  function tokenFromPath() {
    var match = window.location.pathname.match(/^\/s\/([A-Za-z0-9]+)$/);
    return match ? match[1] : '';
  }

  function node(tag, className, text) {
    var item = document.createElement(tag);
    if (className) item.className = className;
    if (text != null) item.textContent = text;
    return item;
  }

  function statusOf(value) {
    return STATUS[value] || { text: '未知', className: 'in' };
  }

  function formatTime(value) {
    if (!value) return '暂无现场更新时间';
    return '现场更新：' + String(value).replace('T', ' ').slice(0, 19);
  }

  function remainText(seconds) {
    if (seconds == null) return '--';
    var safe = Math.max(0, Number(seconds) || 0);
    return Math.floor(safe / 60) + '分' + String(safe % 60).padStart(2, '0') + '秒';
  }

  function render(data) {
    stationName.textContent = data.stationName || '内攻实时状态';
    updateTime.textContent = formatTime(data.lastUpdateTime);
    notice.className = 'notice live';
    notice.textContent = '共享有效 · 页面每15秒自动更新';
    groups.innerHTML = '';
    persons.innerHTML = '';
    (data.groups || []).forEach(function (group) {
      var status = statusOf(group.worstStatus);
      var card = node('article', 'group ' + status.className);
      card.appendChild(node('strong', '', group.groupName || '未分组'));
      card.appendChild(node('span', '', String(group.count || 0) + '人 · ' + status.text));
      groups.appendChild(card);
    });
    if (!(data.persons || []).length) {
      persons.appendChild(node('div', 'empty', '当前暂无未撤出人员'));
      return;
    }
    (data.persons || []).forEach(function (person) {
      var status = statusOf(person.status);
      var card = node('article', 'person ' + status.className);
      var head = node('div', 'person-head');
      head.appendChild(node('strong', '', person.name || '未命名'));
      head.appendChild(node('span', 'badge', status.text));
      card.appendChild(head);
      card.appendChild(node('div', 'person-meta', (person.groupName || '未分组') + ' · ' + (person.cylType || '气瓶规格未知')));
      var values = node('div', 'person-data');
      values.appendChild(dataBlock('当前压力', person.currentPressure == null ? '--' : person.currentPressure + ' MPa'));
      values.appendChild(dataBlock('估算剩余', remainText(person.remainSec)));
      card.appendChild(values);
      persons.appendChild(card);
    });
  }

  function dataBlock(label, value) {
    var block = node('div', 'data');
    block.appendChild(node('label', '', label));
    block.appendChild(node('b', '', value));
    return block;
  }

  function fail(message, terminal) {
    notice.className = 'notice error';
    notice.textContent = message || '共享链接已失效';
    groups.innerHTML = '';
    persons.innerHTML = '';
    persons.appendChild(node('div', 'empty', '请联系现场人员重新生成二维码'));
    if (terminal && timer) {
      window.clearTimeout(timer);
      timer = null;
    }
  }

  function scheduleNext() {
    if (timer) window.clearTimeout(timer);
    timer = window.setTimeout(load, POLL_MS);
  }

  function load() {
    if (!token) {
      fail('共享链接无效', true);
      return;
    }
    fetch('/api/v1/s/' + encodeURIComponent(token), { headers: { Accept: 'application/json' }, cache: 'no-store' })
      .then(function (response) { return response.json(); })
      .then(function (payload) {
        if (!payload || payload.code !== '0') {
          fail(payload && payload.message ? payload.message : '共享链接已失效', true);
          return;
        }
        render(payload.data || {});
        scheduleNext();
      })
      .catch(function () {
        notice.className = 'notice error';
        notice.textContent = '网络连接失败，将自动重试';
        scheduleNext();
      });
  }

  load();
  window.addEventListener('pagehide', function () {
    if (timer) window.clearTimeout(timer);
  });
})();
