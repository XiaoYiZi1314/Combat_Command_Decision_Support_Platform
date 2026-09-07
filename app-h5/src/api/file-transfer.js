import { bridge } from '../bridge/index.js';

/**
 * 把 Blob 保存到本地：Android 壳内走原生保存到系统下载目录（系统弹出保存提示），
 * 浏览器环境降级为 <a download> 触发浏览器下载。
 *
 * @param {Blob} blob 文件内容
 * @param {string} fileName 目标文件名（含扩展名）
 * @returns {Promise<string>} 保存位置描述（如 Downloads/xxx.xlsx 或 browser-download）
 */
export async function saveBlobFile(blob, fileName) {
  const result = await bridge.saveFile(fileName, blob);
  if (result.ok) {
    return result.data && result.data.path ? result.data.path : '设备存储';
  }
  if (result.errorCode !== 'UNSUPPORTED') {
    throw new Error('保存文件失败');
  }
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
  return 'browser-download';
}

/**
 * 弹出文件选择框：Android 壳内走系统文件选择器（结果经 __ccdsFilePicked 事件回推），
 * 浏览器环境降级为 <input type="file">。
 *
 * @param {string} accept input 的 accept 属性（浏览器降级用）
 * @returns {Promise<File|null>} 选中的文件，取消选择时返回 null
 */
export function pickLocalFile(accept) {
  return new Promise((resolve) => {
    bridge.pickFile().then((result) => {
      if (!result.ok) {
        // 浏览器或桥不支持：降级 input[type=file]
        resolveViaInput(accept, resolve);
        return;
      }
      // 壳内：等待原生 __ccdsFilePicked 事件
      const timer = setTimeout(() => {
        window.removeEventListener('__ccdsFilePicked', onPicked);
        resolve(null);
      }, 120000);
      function onPicked(event) {
        clearTimeout(timer);
        window.removeEventListener('__ccdsFilePicked', onPicked);
        const detail = event.detail || {};
        if (!detail.ok || !detail.data) {
          resolve(null);
          return;
        }
        const { fileName, base64 } = detail.data;
        const bytes = base64ToBytes(base64);
        const mime = guessMime(fileName);
        resolve(new File([bytes], fileName || 'import.xlsx', { type: mime }));
      }
      window.addEventListener('__ccdsFilePicked', onPicked);
    });
  });
}

function resolveViaInput(accept, resolve) {
  const input = document.createElement('input');
  input.type = 'file';
  if (accept) {
    input.accept = accept;
  }
  input.addEventListener('change', () => {
    const file = input.files && input.files[0];
    resolve(file || null);
  });
  input.click();
}

function base64ToBytes(base64) {
  const binary = atob(base64 || '');
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function guessMime(fileName) {
  const lower = String(fileName || '').toLowerCase();
  if (lower.endsWith('.xlsx')) {
    return 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
  }
  if (lower.endsWith('.xls')) {
    return 'application/vnd.ms-excel';
  }
  if (lower.endsWith('.csv')) {
    return 'text/csv';
  }
  return 'application/octet-stream';
}
