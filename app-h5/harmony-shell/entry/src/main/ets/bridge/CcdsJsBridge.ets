/**
 * 鸿蒙壳一期先降级：NFC / 定位 / 罗盘返回 UNSUPPORTED，H5 走手选与提示。
 */
export class CcdsJsBridge {
  capabilities(): string {
    return JSON.stringify({
      nfc: false,
      locate: false,
      heading: false,
      speak: false,
      openUrl: false,
      source: 'harmony-degraded'
    });
  }

  invoke(method: string, payloadJson: string): string {
    const name = method || '';
    if (name === 'getCapabilities') {
      return this.ok(JSON.parse(this.capabilities()));
    }
    if (name === 'getVersion') {
      return this.ok({ versionName: '0.1.0-hmos', versionCode: 1, source: 'harmony-degraded' });
    }
    if (name === 'getDeviceCompatInfo') {
      return this.ok({
        huaweiHarmony: true,
        brand: 'HUAWEI',
        manufacturer: 'HUAWEI',
        model: '',
        display: '',
        sdk: 0,
        source: 'harmony-degraded'
      });
    }
    if (name === 'openUrl') {
      return this.fail('UNSUPPORTED');
    }
    if (name === 'nfcRead') {
      return this.fail('UNSUPPORTED');
    }
    if (name === 'heading' || name === 'locate' || name === 'speak') {
      return this.fail('UNSUPPORTED');
    }
    return this.fail('UNSUPPORTED');
  }

  private ok(data: object): string {
    return JSON.stringify({ ok: true, data, errorCode: '' });
  }

  private fail(code: string): string {
    return JSON.stringify({ ok: false, data: null, errorCode: code });
  }
}
