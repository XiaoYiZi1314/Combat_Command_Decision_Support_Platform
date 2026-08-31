const fs = require('fs');
const manifest = JSON.parse(fs.readFileSync('vehicle_manifest.json', 'utf8'));

const nameToId = {
  '海山路消防站': 4, '永安街消防站': 3, '向华路消防站': 13, '红旗路消防站': 6,
  '奋斗街消防站': 17, '南树园消防站': 5, '华溪消防站': 7, '通北路消防站': 8,
  '海河路特勤站': 2, '安顺路消防站': 1, '宝石消防站': 10, '光明路消防站': 9,
  '胜利消防站': 12, '新兴消防站': 11, '新和街消防站': 20, '金鼎消防站': 19,
  '府右街消防站': 18, '新明路消防站': 14, '建设路消防站': 16, '惠民街消防站': 21,
  '学府路消防站': 15, '南城路消防站': 22, '战勤保障分队': 23
};
// 大队级条目落到该大队首站（与本仓库种子顺序一致）
const brigadeFirst = {
  '龙沙大队': 1, '铁锋大队': 5, '建华大队': 7, '富区大队': 9, '昂区大队': 11,
  '梅区大队': 12, '碾区大队': 13, '龙江大队': 14, '依安大队': 15, '泰来大队': 16,
  '甘南大队': 17, '富裕大队': 18, '讷河大队': 19, '克山大队': 20, '克东大队': 21,
  '拜泉大队': 22
};

function mapStation(name) {
  if (name === '支队本级') return 24;
  if (nameToId[name]) return nameToId[name];
  if (name === '特勤大队') return 2;
  if (name === '龙江大队正阳路消防救援站') return 14;
  if (brigadeFirst[name]) return brigadeFirst[name];
  return null;
}

function esc(s) {
  return String(s == null ? '' : s).replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}
function q(s) {
  if (s == null || String(s) === '') return 'NULL';
  return "'" + esc(s) + "'";
}

const unmapped = new Set();
const rows = [];
manifest.forEach(v => {
  const sid = mapStation(v.station);
  if (sid == null) { unmapped.add(v.station); return; }
  rows.push('(' + sid + ', ' + q(v.type) + ', ' + q(v.vehicleType) + ', ' + q(v.plate) + ', '
    + q(v.oldPlate) + ', ' + q(v.status || '执勤') + ', ' + q(v.model) + ', ' + q(v.maker) + ', '
    + q(v.waterCap) + ', ' + q(v.foamCap) + ', ' + q(v.powderCap) + ', ' + q(v.workHeight) + ', '
    + q(v.engineNo) + ', ' + q(v.vin) + ', ' + q(v.color) + ', ' + q(v.madeDate) + ', '
    + q(v.equipDate) + ', ' + q(v.notes) + ')');
});
console.log('unmapped:', [...unmapped]);
console.log('rows:', rows.length);
fs.writeFileSync('vehicle_seed_rows.sql', rows.join(',\n'));
