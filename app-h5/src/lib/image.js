/**
 * 图片压缩与 base64 转换。
 *
 * 拍照研判的图片会随请求发给本后端，峰值必须控制在 2MB 以内：
 * 长边压到 1600、质量 0.8，超过上限继续降质量直至合规。
 */

const MAX_EDGE = 1600;
const QUALITY = 0.8;
const MIN_QUALITY = 0.4;
const MAX_BASE64_LENGTH = 1_900_000;

/**
 * File 转 base64（不含 data: 前缀）。
 *
 * @param {File} file 文件
 * @returns {Promise<string>} base64
 */
export function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = String(reader.result || '');
      const comma = result.indexOf(',');
      resolve(comma > 0 ? result.slice(comma + 1) : result);
    };
    reader.onerror = () => reject(new Error('图片读取失败'));
    reader.readAsDataURL(file);
  });
}

/**
 * 压缩图片并返回 base64。无法压缩时回退原图。
 *
 * @param {File} file 图片文件
 * @returns {Promise<{base64: string, contentType: string}>} 压缩结果
 */
export async function compressImage(file) {
  const contentType = file.type || 'image/jpeg';
  const raw = await fileToBase64(file);
  if (typeof document === 'undefined') {
    return { base64: raw, contentType };
  }
  try {
    const bitmap = await loadBitmap(file);
    if (!bitmap) {
      return { base64: raw, contentType };
    }
    const scale = Math.min(1, MAX_EDGE / Math.max(bitmap.width, bitmap.height));
    const width = Math.round(bitmap.width * scale);
    const height = Math.round(bitmap.height * scale);
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return { base64: raw, contentType };
    }
    ctx.drawImage(bitmap, 0, 0, width, height);
    let quality = QUALITY;
    let dataUrl = canvas.toDataURL('image/jpeg', quality);
    while (dataUrl.length > MAX_BASE64_LENGTH && quality > MIN_QUALITY) {
      quality -= 0.1;
      dataUrl = canvas.toDataURL('image/jpeg', quality);
    }
    if (dataUrl.length >= raw.length && raw.length <= MAX_BASE64_LENGTH) {
      return { base64: raw, contentType };
    }
    const comma = dataUrl.indexOf(',');
    return {
      base64: comma > 0 ? dataUrl.slice(comma + 1) : dataUrl,
      contentType: 'image/jpeg'
    };
  } catch (err) {
    return { base64: raw, contentType };
  }
}

function loadBitmap(file) {
  if (typeof createImageBitmap === 'function') {
    return createImageBitmap(file).catch(() => loadViaImage(file));
  }
  return loadViaImage(file);
}

function loadViaImage(file) {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => {
      URL.revokeObjectURL(url);
      resolve(image);
    };
    image.onerror = () => {
      URL.revokeObjectURL(url);
      resolve(null);
    };
    image.src = url;
  });
}

/**
 * base64 体积估算（字节）。
 *
 * @param {string} base64 base64 文本
 * @returns {number} 字节数
 */
export function base64Bytes(base64) {
  const text = String(base64 || '');
  if (!text) {
    return 0;
  }
  return Math.floor((text.length * 3) / 4);
}
