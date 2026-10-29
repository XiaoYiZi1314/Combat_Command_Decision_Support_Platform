import test from 'node:test';
import assert from 'node:assert/strict';
import { inspectWaterVideo } from '../src/lib/media.js';

function withFakeVideo(duration, run) {
  const originalDocument = globalThis.document;
  const originalCreate = URL.createObjectURL;
  const originalRevoke = URL.revokeObjectURL;
  let revoked = false;
  const video = new EventTarget();
  let position = 0;
  video.duration = duration;
  video.videoWidth = 1920;
  video.videoHeight = 1080;
  video.readyState = 4;
  Object.defineProperty(video, 'currentTime', {
    get() { return position; },
    set(value) {
      position = value;
      queueMicrotask(() => video.dispatchEvent(new Event('seeked')));
    }
  });
  video.load = () => queueMicrotask(() => video.dispatchEvent(new Event('loadedmetadata')));
  video.pause = () => {};
  video.removeAttribute = () => {};
  globalThis.document = {
    createElement(name) {
      if (name === 'video') return video;
      return {
        width: 0,
        height: 0,
        getContext() { return { drawImage() {} }; },
        toBlob(callback) { callback(new Blob(['frame'], { type: 'image/jpeg' })); }
      };
    }
  };
  URL.createObjectURL = () => 'blob:video-test';
  URL.revokeObjectURL = () => { revoked = true; };
  return Promise.resolve().then(run).finally(() => {
    globalThis.document = originalDocument;
    URL.createObjectURL = originalCreate;
    URL.revokeObjectURL = originalRevoke;
    assert.equal(revoked, true);
  });
}

test('5 秒 MP4 自动检测真实时长并提取三帧', async () => {
  await withFakeVideo(5, async () => {
    const result = await inspectWaterVideo(new File(['test'], 'water.mp4', { type: 'video/mp4' }));
    assert.equal(result.durationMs, 5000);
    assert.equal(result.frames.length, 3);
    assert.ok(result.frames.every((frame) => frame.type === 'image/jpeg'));
  });
});

test('超过 10 秒的 MP4 在上传前被拒绝', async () => {
  await withFakeVideo(10.1, async () => {
    await assert.rejects(
      inspectWaterVideo(new File(['test'], 'water.mp4', { type: 'video/mp4' })),
      /最长 10 秒/
    );
  });
});

test('非 MP4 文件被拒绝', async () => {
  await assert.rejects(
    inspectWaterVideo(new File(['test'], 'water.mov', { type: 'video/quicktime' })),
    /MP4/
  );
});
