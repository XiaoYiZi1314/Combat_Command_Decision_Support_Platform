import web_webview from '@ohos.web.webview';
import { CcdsJsBridge } from '../bridge/CcdsJsBridge';

@Entry
@Component
struct Index {
  controller: web_webview.WebviewController = new web_webview.WebviewController();
  private bridge: CcdsJsBridge = new CcdsJsBridge();

  build() {
    Column() {
      Web({ src: $rawfile('www/index.html'), controller: this.controller })
        .javaScriptAccess(true)
        .domStorageAccess(true)
        .fileAccess(true)
        .onPageEnd(() => {
          this.controller.runJavaScript(
            "window.CCDS_API_BASE=window.CCDS_API_BASE||'';"
          );
        })
        .javaScriptProxy({
          object: this.bridge,
          name: 'CcdsNativeBridge',
          methodList: ['invoke', 'capabilities'],
          controller: this.controller
        })
        .width('100%')
        .height('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#0a0e14')
  }
}
