import '../water/water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { el } from '../water/shared.js';
import { bridge } from '../../bridge/index.js';
import { addRecord, aliasOf, readRecords, removeRecord, setAlias } from '../../stores/nfc.js';
import { fetchRoster } from '../../api/roster.js';

/**
 * NFC 联动记录：单个录入 / 批量顺序登记，别名绑定人员编号。
 * 仅本地记录，不落库；与客户 APK 的 NFC联动记录 面板一致。
 */
export function renderNfcPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', 'NFC联动记录'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(me);
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  const nfcReady = bridge.hasNfc();
  if (!nfcReady) {
    body.appendChild(el('div', 'supply-hint', '本机无 NFC 或未安装 APP，扫描登记不可用；已有记录仍可查看与删除。'));
  }

  const state = {
    batch: null,
    roster: { profiles: [] }
  };

  /* 操作按钮 */
  const actions = el('div', 'water-actions');
  const singleBtn = el('button', '', '单个NFC录入');
  singleBtn.type = 'button';
  singleBtn.addEventListener('click', () => {
    if (!nfcReady) {
      setMsg('本机无 NFC，无法录入', true);
      return;
    }
    state.batch = null;
    scanSingle();
  });
  const batchBtn = el('button', '', '批量NFC录入');
  batchBtn.type = 'button';
  batchBtn.addEventListener('click', () => {
    if (!nfcReady) {
      setMsg('本机无 NFC，无法录入', true);
      return;
    }
    openBatchRange();
  });
  actions.appendChild(singleBtn);
  actions.appendChild(batchBtn);
  body.appendChild(actions);

  body.appendChild(el('div', 'nfc-help-text',
    '扫描到的NFC编号必须和人员档案里的NFC编号一致；档案已填写早检查空呼压力时，匹配成功后直接进入火场。'));

  const msg = el('div', 'water-msg');
  body.appendChild(msg);

  const list = el('div', 'water-list');
  body.appendChild(list);
  page.appendChild(body);
  root.appendChild(page);

  function setMsg(text, error) {
    msg.className = error ? 'water-msg error' : 'water-msg';
    msg.textContent = text || '';
  }

  async function loadRoster() {
    const stationId = me.role === 'station' && me.stationId ? String(me.stationId) : '';
    if (!stationId) {
      return;
    }
    try {
      state.roster = await fetchRoster(stationId);
    } catch (err) {
      state.roster = { profiles: [] };
    }
  }

  function matchProfile(tag) {
    const profiles = (state.roster && state.roster.profiles) || [];
    const codeOf = (value) => String(value || '').replace(/[\s:：\-_]/g, '').toUpperCase();
    return profiles.find((prof) => {
      const nfc = codeOf(prof.nfcTag);
      if (!nfc) {
        return false;
      }
      if (nfc === codeOf(tag)) {
        return true;
      }
      return aliasOf(tag) && nfc === codeOf(aliasOf(tag));
    });
  }

  function renderList() {
    list.innerHTML = '';
    const records = readRecords();
    if (!records.length) {
      list.appendChild(el('div', 'water-empty', '暂无扫描记录'));
      return;
    }
    records.slice().reverse().forEach((record) => {
      const matched = Boolean(matchProfile(record.tag));
      const card = el('div', 'water-card');
      const titleRow = el('div', 'vehicle-title-row');
      const titleBox = el('div', 'vehicle-title-text');
      const badge = matched ? '已联动' : '已登记';
      titleBox.appendChild(el('div', 'vehicle-name', `${badge} ${record.name || aliasOf(record.tag) || record.tag}`));
      const time = record.time ? new Date(record.time).toLocaleString('zh-CN') : '';
      titleBox.appendChild(el('div', 'vehicle-line',
        `卡：${record.tag}${aliasOf(record.tag) ? ` → 编号：${aliasOf(record.tag)}` : ''}　${time}`));
      titleRow.appendChild(titleBox);
      const del = el('button', 'vehicle-op-btn danger', '删除');
      del.type = 'button';
      del.addEventListener('click', () => {
        if (!window.confirm('确认删除该条扫描记录？')) {
          return;
        }
        removeRecord(record.tag);
        renderList();
      });
      titleRow.appendChild(del);
      card.appendChild(titleRow);
      list.appendChild(card);
    });
  }

  async function scanOnce() {
    const result = await bridge.nfcRead();
    if (!result || !result.ok || !result.data || !result.data.tag) {
      const code = result && result.errorCode ? result.errorCode : '';
      if (code === 'NO_NFC' || code === 'UNSUPPORTED') {
        throw new Error('本机无 NFC');
      }
      throw new Error('未读到标签，请重试');
    }
    return String(result.data.tag);
  }

  async function scanSingle() {
    setMsg('请将 NFC 卡扣贴近手机背面扫描');
    let tag;
    try {
      tag = await scanOnce();
    } catch (err) {
      setMsg(err.message || '扫描失败', true);
      return;
    }
    const alias = window.prompt(`扫描到标签：${tag}\n请输入要绑定的人员编号：`, aliasOf(tag) || '');
    if (alias === null) {
      setMsg('已取消绑定');
      return;
    }
    const trimmed = alias.trim();
    if (trimmed) {
      setAlias(tag, trimmed);
    }
    const profile = matchProfile(tag);
    addRecord(tag, trimmed, profile ? profile.name : '');
    setMsg(profile ? `${profile.name} 已联动` : '已登记，未匹配到档案编号');
    renderList();
  }

  function openBatchRange() {
    const start = window.prompt('批量登记起始编号（如 001）：', '001');
    if (start === null) {
      return;
    }
    const end = window.prompt('批量登记结束编号（如 020）：', '020');
    if (end === null) {
      return;
    }
    const codes = buildRange(start.trim(), end.trim());
    if (!codes.length) {
      setMsg('编号范围无效，请输入从小到大的编号', true);
      return;
    }
    state.batch = { codes, index: 0 };
    setMsg(`批量NFC登记开始，共${codes.length}张。请依次扫描卡扣。`);
    scanNextBatch();
  }

  function buildRange(start, end) {
    const norm = (value) => String(value || '').replace(/^0+(?!$)/, '');
    if (!/^\d+$/.test(start) || !/^\d+$/.test(end)) {
      return [];
    }
    const from = Number(norm(start));
    const to = Number(norm(end));
    if (from > to || to - from > 500) {
      return [];
    }
    const codes = [];
    for (let i = from; i <= to; i += 1) {
      codes.push(String(i).padStart(3, '0'));
    }
    return codes;
  }

  async function scanNextBatch() {
    if (!state.batch || state.batch.index >= state.batch.codes.length) {
      if (state.batch) {
        setMsg('批量NFC登记已完成');
        state.batch = null;
      }
      return;
    }
    const code = state.batch.codes[state.batch.index];
    setMsg(`请扫描编号 ${code} 的卡扣（${state.batch.index + 1}/${state.batch.codes.length}）`);
    let tag;
    try {
      tag = await scanOnce();
    } catch (err) {
      setMsg(`${err.message || '扫描失败'}；编号 ${code} 未登记`, true);
      state.batch = null;
      return;
    }
    setAlias(tag, code);
    const profile = matchProfile(tag);
    addRecord(tag, code, profile ? profile.name : '');
    renderList();
    state.batch.index += 1;
    scanNextBatch();
  }

  loadRoster().then(() => renderList());
  renderList();
}
