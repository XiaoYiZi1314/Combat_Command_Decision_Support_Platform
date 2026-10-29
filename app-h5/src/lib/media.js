/**
 * 用浏览器内置解码器获取 MP4 真实时长与 3 张关键帧。
 * 不信任用户手填时长；设备不支持解码时明确报错，不产生虚假判定。
 */
const MAX_VIDEO_MS = 10000;
const MEDIA_TIMEOUT_MS = 15000;
const FRAME_WIDTH = 640;

function once(target, successEvent, errorEvent = 'error') {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => finish(new Error('视频解码超时')), MEDIA_TIMEOUT_MS);
    function finish(error) {
      clearTimeout(timeout);
      target.removeEventListener(successEvent, success);
      target.removeEventListener(errorEvent, failure);
      if (error) reject(error);
      else resolve();
    }
    function success() { finish(null); }
    function failure() { finish(new Error('无法解码此视频，请使用 MP4 格式')); }
    target.addEventListener(successEvent, success, { once: true });
    target.addEventListener(errorEvent, failure, { once: true });
  });
}

function frameAsFile(video, index) {
  const canvas = document.createElement('canvas');
  const ratio = Math.min(1, FRAME_WIDTH / Math.max(video.videoWidth, video.videoHeight));
  canvas.width = Math.max(1, Math.round(video.videoWidth * ratio));
  canvas.height = Math.max(1, Math.round(video.videoHeight * ratio));
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('当前设备无法提取视频画面');
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error('视频画面提取失败'));
        return;
      }
      resolve(new File([blob], `water-frame-${index + 1}.jpg`, { type: 'image/jpeg' }));
    }, 'image/jpeg', 0.76);
  });
}

async function seekFrame(video, time) {
  const position = Math.min(Math.max(0, video.duration - 0.001), time);
  if (Math.abs(video.currentTime - position) < 0.001) {
    if (video.readyState < 2) {
      await once(video, 'loadeddata');
    }
    return;
  }
  const seeked = once(video, 'seeked');
  video.currentTime = position;
  await seeked;
}

export async function inspectWaterVideo(file) {
  if (!file || file.type !== 'video/mp4') {
    throw new Error('请选择 MP4 格式视频');
  }
  const url = URL.createObjectURL(file);
  const video = document.createElement('video');
  video.preload = 'auto';
  video.muted = true;
  video.playsInline = true;
  try {
    const loaded = once(video, 'loadedmetadata');
    video.src = url;
    video.load();
    await loaded;
    const duration = video.duration;
    if (!Number.isFinite(duration) || duration <= 0) {
      throw new Error('视频时长无法识别');
    }
    const durationMs = Math.ceil(duration * 1000);
    if (durationMs > MAX_VIDEO_MS) {
      throw new Error(`出水视频最长 10 秒，当前 ${duration.toFixed(1)} 秒`);
    }
    if (!video.videoWidth || !video.videoHeight) {
      throw new Error('视频没有可读取的画面');
    }
    const frames = [];
    const times = [0.05, duration / 2, Math.max(0.05, duration - 0.05)];
    for (let i = 0; i < times.length; i += 1) {
      await seekFrame(video, times[i]);
      frames.push(await frameAsFile(video, i));
    }
    return { durationMs, frames };
  } finally {
    video.pause();
    video.removeAttribute('src');
    video.load();
    URL.revokeObjectURL(url);
  }
}
