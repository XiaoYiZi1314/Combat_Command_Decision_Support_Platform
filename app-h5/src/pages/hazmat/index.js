import '../water/water.css';
import { getMe, homeHashOf } from '../../stores/session.js';
import { searchHazmat, visionHazmat } from '../../api/assist.js';
import { el } from '../water/shared.js';
import { compressImage } from '../../lib/image.js';

const DISCLAIMER = '本结果仅作辅助参考，不替代现场指挥。';
// 5.3：任何结果都必须写明判定依据
const RULE_NOTE = '以现场侦检、运输单据、安全技术说明书和登记信息为准；认不准请说明不确定。';
const MAX_TEXT = 500;

// 5.4：与 AI 助手共用同一套压缩实现，两处入口不合并，但底层一致

export function renderHazmatPage(root) {
  root.innerHTML = '';
  const me = getMe() || {};
  const page = el('div', 'water-page');
  const head = el('div', 'water-head');
  head.appendChild(el('h2', '', '危化品查询'));
  const back = el('button', 'water-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = homeHashOf(me);
  });
  head.appendChild(back);
  page.appendChild(head);

  const body = el('div', 'water-body');
  body.appendChild(el('div', 'supply-hint', DISCLAIMER));

  const searchForm = el('div', 'water-form');
  const queryInput = document.createElement('input');
  queryInput.type = 'search';
  // 5.3：检索框明确写成名称 / UN编号 / CAS号，示例用真实物质与 UN 编号
  queryInput.placeholder = '危化品名称 / UN编号 / CAS号，如：液氨、汽油、LNG、UN1005';
  queryInput.maxLength = 64;
  searchForm.appendChild(row('检索', queryInput));
  const searchBtn = el('button', 'wf-save', '云端数据库搜索');
  searchBtn.type = 'button';
  searchForm.appendChild(searchBtn);
  body.appendChild(searchForm);

  const aiForm = el('div', 'water-form');
  aiForm.appendChild(el('h3', '', '拍照 / 文字研判'));
  const textInput = document.createElement('textarea');
  textInput.rows = 3;
  textInput.maxLength = MAX_TEXT;
  textInput.placeholder = '补充描述（无网时禁用）';
  aiForm.appendChild(row('文字', textInput));
  const fileInput = document.createElement('input');
  fileInput.type = 'file';
  fileInput.accept = 'image/jpeg,image/png';
  fileInput.className = 'hidden-file';
  const cameraInput = document.createElement('input');
  cameraInput.type = 'file';
  cameraInput.accept = 'image/jpeg,image/png';
  cameraInput.capture = 'environment';
  cameraInput.className = 'hidden-file';
  const pickBtn = el('button', 'wf-cancel', '图库');
  pickBtn.type = 'button';
  pickBtn.addEventListener('click', () => fileInput.click());
  const cameraBtn = el('button', 'wf-cancel', '拍照');
  cameraBtn.type = 'button';
  cameraBtn.addEventListener('click', () => cameraInput.click());
  const fileHint = el('div', 'water-msg', '未选择图片');
  aiForm.appendChild(pickBtn);
  aiForm.appendChild(cameraBtn);
  aiForm.appendChild(fileInput);
  aiForm.appendChild(cameraInput);
  aiForm.appendChild(fileHint);
  const visionBtn = el('button', 'wf-save', 'AI 研判');
  visionBtn.type = 'button';
  aiForm.appendChild(visionBtn);
  body.appendChild(aiForm);

  const msg = el('div', 'water-msg');
  body.appendChild(msg);
  const result = el('div', 'supply-result');
  body.appendChild(result);
  page.appendChild(body);
  root.appendChild(page);

  function row(label, input) {
    const wrap = el('div', 'wf-row');
    wrap.appendChild(el('label', '', label));
    wrap.appendChild(input);
    return wrap;
  }

  function setMsg(text, error) {
    msg.className = error ? 'water-msg error' : 'water-msg';
    msg.textContent = text || '';
  }

  function online() {
    return typeof navigator === 'undefined' ? true : navigator.onLine;
  }

  function syncOffline() {
    const off = !online();
    visionBtn.disabled = off;
    textInput.disabled = off;
    pickBtn.disabled = off;
    cameraBtn.disabled = off;
    if (off) {
      setMsg('无网：已禁用 AI。检索若无本地缓存则失败。', true);
    }
  }

  function bindFile(input) {
    input.addEventListener('change', () => {
      const file = input.files && input.files[0];
      if (!file) {
        return;
      }
      if (input !== fileInput) {
        fileInput.value = '';
      } else {
        cameraInput.value = '';
      }
      fileHint.textContent = file.name;
    });
  }

  bindFile(fileInput);
  bindFile(cameraInput);

  searchBtn.addEventListener('click', async () => {
    const query = queryInput.value.trim();
    if (!query) {
      setMsg('请输入名称、俗称、UN 或 CAS', true);
      return;
    }
    setMsg('检索中…');
    result.innerHTML = '';
    try {
      const data = await searchHazmat(query);
      const items = (data && data.items) || [];
      // 5.3：降级时明确写明「已改用备用库」，不要空着
      if (data && data.degraded && data.degradedNote) {
        result.appendChild(el('div', 'supply-hint', data.degradedNote));
      }
      if (!items.length) {
        result.appendChild(el('div', 'water-empty',
          '登记库未命中。可再用下方拍照或文字研判辅助判断。'));
        setMsg(data && data.degraded ? '已改用备用库，仍未命中' : '未找到匹配条目');
        return;
      }
      items.forEach((item) => {
        const card = el('div', 'water-card');
        const title = el('div', 'wc-name');
        title.appendChild(el('b', '', item.name || '未命名'));
        card.appendChild(title);
        const meta = [item.alias, item.unNumber, item.casNumber].filter(Boolean).join(' · ');
        if (meta) {
          card.appendChild(el('div', 'wc-info', meta));
        }
        if (item.hazardSummary) {
          card.appendChild(el('div', 'wc-addr', item.hazardSummary));
        }
        if (item.responseHint) {
          card.appendChild(el('div', 'wc-info', item.responseHint));
        }
        result.appendChild(card);
      });
      setMsg(`共 ${items.length} 条`);
    } catch (err) {
      setMsg(err.message || '检索失败', true);
    }
  });

  visionBtn.addEventListener('click', async () => {
    if (!online()) {
      setMsg('无网：已禁用 AI', true);
      return;
    }
    const text = textInput.value.trim();
    const file = (fileInput.files && fileInput.files[0]) || (cameraInput.files && cameraInput.files[0]);
    if (!text && !file) {
      setMsg('请提供图片或文字描述', true);
      return;
    }
    setMsg('研判中…');
    try {
      const body = { text: text || undefined };
      if (file) {
        const compressed = await compressImage(file);
        body.imageContentType = compressed.contentType;
        body.imageBase64 = compressed.base64;
      }
      const data = await visionHazmat(body);
      result.innerHTML = '';
      if (data.possibleName) {
        result.appendChild(el('div', 'supply-line', `可能物质：${data.possibleName}`));
      }
      if (data.hazardSummary) {
        result.appendChild(el('div', 'supply-line', data.hazardSummary));
      }
      if (data.advice) {
        result.appendChild(el('div', 'supply-line', data.advice));
      }
      // 5.3：结果里固定写明依据，认不准要说明不确定
      result.appendChild(el('div', 'supply-hint',
        data.disclaimer || DISCLAIMER));
      result.appendChild(el('div', 'supply-hint', RULE_NOTE));
      setMsg('');
    } catch (err) {
      setMsg(err.message || '研判失败', true);
    }
  });

  window.addEventListener('online', syncOffline);
  window.addEventListener('offline', syncOffline);
  syncOffline();
}
