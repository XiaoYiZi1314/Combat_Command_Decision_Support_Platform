package com.ccds.shell;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
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

import org.json.JSONObject;

/**
 * 安卓薄壳：只承载 H5，并提供 NFC / 定位 / 罗盘。
 *
 * @author ccds
 * @since 0.1.0
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 32;

    private static final String ASSET_URL = "file:///android_asset/www/index.html";

    private static final String ASSET_PREFIX = "file:///android_asset/www/";

    private WebView webView;

    private NfcSession nfcSession;

    private HeadingStore headingStore;

    private LocationStore locationStore;

    private CcdsJsBridge bridge;

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
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) {
                    return true;
                }
                return !isPackedAsset(request.getUrl());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (isPackedAsset(url)) {
                    injectHost();
                }
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
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationStore.start();
        }
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
