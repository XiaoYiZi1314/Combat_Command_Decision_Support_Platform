package com.ccds.shell;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.ccds.shell.bridge.CcdsJsBridge;
import com.ccds.shell.nfc.NfcSession;
import com.ccds.shell.sensor.HeadingStore;
import com.ccds.shell.sensor.LocationStore;

import java.security.MessageDigest;

import org.json.JSONObject;

/**
 * 安卓薄壳：只承载 H5，并提供 NFC / 定位 / 罗盘。
 *
 * @author ccds
 * @since 0.1.0
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 32;

    private static final String ASSET_URL = "https://appassets.androidplatform.net/assets/www/index.html";

    private static final String ASSET_PREFIX = "https://appassets.androidplatform.net/assets/www/";

    private WebView webView;

    private NfcSession nfcSession;

    private HeadingStore headingStore;

    private LocationStore locationStore;

    private CcdsJsBridge bridge;

    /** 打包内置测试证书指纹，惰性初始化；仅 debug 构建使用。 */
    private byte[] packagedCertFingerprint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcSession = new NfcSession(this);
        headingStore = new HeadingStore(this);
        locationStore = new LocationStore(this);
        bridge = new CcdsJsBridge(this, nfcSession, headingStore, locationStore);
        webView = findViewById(R.id.webView);
        setupWebView();
        nfcSession.onNewIntent(getIntent());
        if (locationStore.permitted()) {
            locationStore.start();
        } else {
            requestLocationPermission();
        }
        headingStore.start();
        webView.loadUrl(ASSET_URL);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setWebChromeClient(new WebChromeClient());
        final androidx.webkit.WebViewAssetLoader assetLoader = new androidx.webkit.WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new androidx.webkit.WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // 将虚拟域名的静态资源请求映射到打包 assets，避开 file:// 对 ES module 的 CORS 限制
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (isPackedAsset(request.getUrl())) {
                    return false;
                }
                // 非打包资源一律交给系统浏览器，壳内不加载外部页面
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (isPackedAsset(url)) {
                    injectHost();
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (BuildConfig.DEBUG_NFC && isTrustedTestCa(error)) {
                    // 仅 debug 检测包：信任打包内置的测试自签证书。
                    // release 包 BuildConfig.DEBUG_NFC 恒为 false，不会走到这里。
                    handler.proceed();
                    return;
                }
                handler.cancel();
            }
        });
        webView.addJavascriptInterface(bridge, CcdsJsBridge.NAME);
    }

    private boolean isPackedAsset(Uri uri) {
        if (uri == null) {
            return false;
        }
        return isPackedAsset(uri.toString());
    }

    private boolean isPackedAsset(String url) {
        if (url == null) {
            return false;
        }
        return url.equals(ASSET_URL) || url.startsWith(ASSET_PREFIX);
    }

    /**
     * 判断 SSL 错误是否由打包内置的测试自签证书引起。
     * 仅 debug 构建使用；证书指纹与内置证书不一致时一律不信任。
     *
     * @param error WebView 上报的 SSL 错误
     * @return 服务器证书与内置测试证书指纹一致时为 true
     */
    private boolean isTrustedTestCa(SslError error) {
        if (error.getPrimaryError() != SslError.SSL_UNTRUSTED) {
            return false;
        }
        if (packagedCertFingerprint == null) {
            packagedCertFingerprint = fingerprintOf(readRawCert(R.raw.ccds_test_ca));
        }
        byte[] presented = fingerprintOf(presentedCert(error.getCertificate()));
        return packagedCertFingerprint != null && MessageDigest.isEqual(packagedCertFingerprint, presented);
    }

    /**
     * 从 WebView 上报的证书还原 X509 证书。
     *
     * @param cert WebView 上报的证书
     * @return X509 证书，无法还原时返回 null
     */
    private java.security.cert.X509Certificate presentedCert(SslCertificate cert) {
        if (cert == null) {
            return null;
        }
        android.os.Bundle state = SslCertificate.saveState(cert);
        byte[] encoded = state.getByteArray("x509-certificate");
        if (encoded == null) {
            return null;
        }
        try {
            java.security.cert.CertificateFactory factory = java.security.cert.CertificateFactory.getInstance("X.509");
            return (java.security.cert.X509Certificate) factory.generateCertificate(
                    new java.io.ByteArrayInputStream(encoded));
        } catch (java.security.cert.CertificateException e) {
            return null;
        }
    }

    /**
     * 读取打包内置的 PEM 证书。
     *
     * @param resId raw 资源 ID
     * @return X509 证书，解析失败返回 null
     */
    private java.security.cert.X509Certificate readRawCert(int resId) {
        try (java.io.InputStream in = getResources().openRawResource(resId)) {
            java.security.cert.CertificateFactory factory = java.security.cert.CertificateFactory.getInstance("X.509");
            return (java.security.cert.X509Certificate) factory.generateCertificate(in);
        } catch (java.io.IOException | java.security.cert.CertificateException e) {
            return null;
        }
    }

    /**
     * 计算 X509 证书 SHA-256 指纹。
     *
     * @param cert X509 证书
     * @return 指纹，证书为空时返回空数组
     */
    private byte[] fingerprintOf(java.security.cert.X509Certificate cert) {
        try {
            if (cert == null) {
                return new byte[0];
            }
            return java.security.MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
        } catch (java.security.cert.CertificateEncodingException | java.security.NoSuchAlgorithmException e) {
            return new byte[0];
        }
    }

    private void injectHost() {
        String apiBase = BuildConfig.API_BASE == null ? "" : BuildConfig.API_BASE;
        String js = "window.CCDS_API_BASE=" + JSONObject.quote(apiBase) + ";";
        webView.evaluateJavascript(js, null);
    }

    /**
     * 向系统申请定位权限；已授权则直接开定位。
     */
    public void requestLocationPermission() {
        if (locationStore.permitted()) {
            locationStore.start();
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, REQ_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_LOCATION) {
            return;
        }
        if (hasLocationPermission(grantResults)) {
            locationStore.start();
        }
    }

    private boolean hasLocationPermission(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcSession.enableForeground();
        headingStore.start();
        if (locationStore.permitted()) {
            locationStore.start();
        }
    }

    @Override
    protected void onPause() {
        nfcSession.disableForeground();
        headingStore.stop();
        locationStore.stop();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        nfcSession.onNewIntent(intent);
    }

    @Override
    public void onBackPressed() {
        webView.evaluateJavascript("(function(){try{return window.CcdsHost&&window.CcdsHost.onBack?window.CcdsHost.onBack():false;}catch(e){return false;}})()", value -> {
            if ("true".equals(value) || "\"true\"".equals(value)) {
                return;
            }
            if (webView.canGoBack()) {
                webView.goBack();
                return;
            }
            MainActivity.super.onBackPressed();
        });
    }

    @Override
    protected void onDestroy() {
        if (bridge != null) {
            bridge.shutdown();
        }
        headingStore.stop();
        locationStore.stop();
        super.onDestroy();
    }
}
