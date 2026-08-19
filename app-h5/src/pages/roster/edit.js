import './roster.css';
import { getMe } from '../../stores/session.js';
import { createProfile, fetchProfile, updateProfile, deleteProfile } from '../../api/roster.js';
import { currentRosterStationId } from './index.js';

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

function field(labelText, id, value, type, readOnly) {
  const row = el('div', 'pe-row');
  row.appendChild(el('label', '', labelText));
  const input = document.createElement(type === 'select' ? 'select' : 'input');
  input.id = id;
  if (type === 'select') {
    ['6.8', '9'].forEach((item) => {
      const opt = document.createElement('option');
      opt.value = item;
      opt.textContent = `${item}L`;
      input.appendChild(opt);
    });
    input.value = value || '6.8';
  } else {
    input.type = type || 'text';
    input.value = value == null ? '' : String(value);
  }
  if (readOnly) {
    input.disabled = true;
  }
  row.appendChild(input);
  return { row, input };
}

export function renderRosterEditPage(root, params) {
  const profileId = params && params.id;
  const isNew = !profileId || profileId === 'new';
  const me = getMe() || {};
  root.innerHTML = '';
  const page = el('div', 'roster-page');
  const head = el('div', 'roster-head');
  const title = el('h2', '', isNew ? '新建档案' : '编辑档案');
  const back = el('button', 'roster-back', '返回');
  back.type = 'button';
  back.addEventListener('click', () => {
    window.location.hash = '#/roster';
  });
  head.appendChild(title);
  head.appendChild(back);
  page.appendChild(head);
  const body = el('div', 'roster-body');
  const msg = el('div', 'roster-msg');
  body.appendChild(msg);
  const form = el('div', 'pe-form');
  body.appendChild(form);
  page.appendChild(body);
  root.appendChild(page);

  const stationId = currentRosterStationId();

  async function draw(prof, writable) {
    form.innerHTML = '';
    title.textContent = writable ? (isNew ? '新建档案' : '编辑档案') : '查看档案';
    const name = field('姓名', 'peName', prof.name, 'text', !writable);
    const role = field('职务', 'peRole', prof.title, 'text', !writable);
    const rank = field('消防救援衔', 'peRank', prof.rankName, 'text', !writable);
    const phone = field('电话号', 'pePhone', writable ? (prof.phone || '') : (prof.phoneMasked || ''), 'tel', !writable);
    const nfc = field('NFC', 'peNfcTag', prof.nfcTag, 'text', !writable);
    const cyl = field('气瓶', 'peCyl', prof.cylType, 'select', !writable);
    const morning = field('早检压力', 'peMorningPressure', prof.morningPressure, 'number', !writable);
    morning.input.step = '0.1';
    const height = field('身高cm', 'peHeight', prof.heightCm, 'number', !writable);
    const weight = field('体重kg', 'peWeight', prof.weightKg, 'number', !writable);
    const age = field('年龄', 'peAge', prof.age, 'number', !writable);
    [name, role, rank, phone, nfc, cyl, morning, height, weight, age].forEach((item) => form.appendChild(item.row));
    if (!writable && prof.stationName) {
      form.appendChild(field('所属单位', 'peStation', prof.stationName, 'text', true).row);
    }
    form.appendChild(el('h4', 'section-title', '空呼校准记录'));
    const cals = prof.calibrations || [];
    if (cals.length) {
      cals.forEach((cal) => {
        form.appendChild(el('div', 'pe-cal', `${cal.source || '标定'}，压力${cal.pressure || 0}MPa，完全用尽${cal.fullTimeSec || 0}秒`));
      });
    } else {
      form.appendChild(el('div', 'pe-cal', '暂无。可在空呼时间计算器选择该人员后保存。'));
    }
    if (writable) {
      const save = el('button', 'pe-save', '保存');
      save.type = 'button';
      save.addEventListener('click', async () => {
        const body = {
          name: name.input.value.trim(),
          title: role.input.value.trim(),
          rankName: rank.input.value.trim(),
          phone: phone.input.value.trim(),
          nfcTag: nfc.input.value.trim(),
          cylType: cyl.input.value,
          morningPressure: morning.input.value ? Number(morning.input.value) : null,
          heightCm: height.input.value ? Number(height.input.value) : null,
          weightKg: weight.input.value ? Number(weight.input.value) : null,
          age: age.input.value ? Number(age.input.value) : null
        };
        if (!body.name) {
          msg.className = 'roster-msg error';
          msg.textContent = '姓名不能为空';
          return;
        }
        if (!body.cylType) {
          msg.className = 'roster-msg error';
          msg.textContent = '气瓶规格必填';
          return;
        }
        try {
          if (isNew) {
            await createProfile(stationId, body);
          } else {
            await updateProfile(stationId, profileId, body);
          }
          window.location.hash = '#/roster';
        } catch (err) {
          msg.className = 'roster-msg error';
          msg.textContent = err.message || '保存失败';
        }
      });
      form.appendChild(save);
      if (!isNew) {
        const del = el('button', 'pe-del', '删除档案');
        del.type = 'button';
        del.addEventListener('click', async () => {
          if (!window.confirm('确认删除该档案？')) {
            return;
          }
          try {
            await deleteProfile(stationId, profileId);
            window.location.hash = '#/roster';
          } catch (err) {
            msg.className = 'roster-msg error';
            msg.textContent = err.message || '删除失败';
          }
        });
        form.appendChild(del);
      }
    }
  }

  if (isNew) {
    draw({ cylType: '6.8', calibrations: [] }, me.role === 'station');
    return;
  }
  fetchProfile(stationId, profileId).then((prof) => {
    draw(prof, me.role === 'station');
  }).catch((err) => {
    msg.className = 'roster-msg error';
    msg.textContent = err.message || '档案不存在';
  });
}
