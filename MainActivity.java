package dz.attendance.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * يحمّل التطبيق (index.html) من assets عبر عنوان https آمن (appassets.androidplatform.net)
 * ليعمل localStorage وIndexedDB والكاميرا بشكل مستقر، ويوفّر جسرًا للحفظ والطباعة.
 */
public class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/assets/index.html";
    private static final int REQ_FILE = 11, REQ_CAMERA = 12, REQ_SAVE = 13;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingPermission;
    private byte[] pendingBytes;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setSupportZoom(false);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (HOST.equals(u.getHost())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) { }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        boolean wantsCamera = false;
                        for (String r : request.getResources()) {
                            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) wantsCamera = true;
                        }
                        if (!wantsCamera) {
                            request.deny();
                            return;
                        }
                        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                        } else {
                            pendingPermission = request;
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
                        }
                    }
                });
            }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(START_URL);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_CAMERA && pendingPermission != null) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
                pendingPermission.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
            else
                pendingPermission.deny();
            pendingPermission = null;
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_FILE) {
            if (fileCallback != null) {
                Uri[] r = WebChromeClient.FileChooserParams.parseResult(res, data);
                fileCallback.onReceiveValue(r);
                fileCallback = null;
            }
        } else if (req == REQ_SAVE) {
            byte[] bytes = pendingBytes;
            pendingBytes = null;
            if (res == RESULT_OK && data != null && data.getData() != null && bytes != null) {
                try {
                    OutputStream os = getContentResolver().openOutputStream(data.getData());
                    if (os == null) throw new Exception("no stream");
                    os.write(bytes);
                    os.close();
                    Toast.makeText(this, "تم حفظ الملف", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "تعذّر حفظ الملف", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        // يغلق النافذة المنبثقة للتطبيق إن وُجدت، وإلا يخرج إلى الشاشة الرئيسية
        web.evaluateJavascript(
                "(function(){try{if(typeof Modal!=='undefined'&&Modal.stack&&Modal.stack.length){Modal.close();return 1}}catch(e){}return 0})()",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String v) {
                        if (!"1".equals(v)) moveTaskToBack(true);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.removeJavascriptInterface("AndroidBridge");
            web.destroy();
        }
        super.onDestroy();
    }

    /** الجسر بين صفحة الويب والنظام: حفظ ملف، طباعة. */
    private class Bridge {
        @JavascriptInterface
        public void saveFile(final String name, final String mime, final String base64) {
            final byte[] bytes;
            try {
                bytes = Base64.decode(base64, Base64.DEFAULT);
            } catch (Exception e) {
                return;
            }
            ui.post(new Runnable() {
                @Override
                public void run() {
                    pendingBytes = bytes;
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType(mime == null || mime.isEmpty() ? "application/octet-stream" : mime);
                    i.putExtra(Intent.EXTRA_TITLE, name == null || name.isEmpty() ? "file" : name);
                    try {
                        startActivityForResult(i, REQ_SAVE);
                    } catch (Exception e) {
                        pendingBytes = null;
                        Toast.makeText(MainActivity.this, "تعذّر حفظ الملف", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        @JavascriptInterface
        public void print() {
            ui.post(new Runnable() {
                @Override
                public void run() {
                    PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                    if (pm == null) {
                        fireAfterPrint();
                        return;
                    }
                    PrintDocumentAdapter adapter = web.createPrintDocumentAdapter("attendance");
                    final PrintJob job = pm.print("attendance", adapter, new PrintAttributes.Builder().build());
                    pollJob(job, 0);
                }
            });
        }
    }

    /** يراقب مهمة الطباعة ثم يُعلم الصفحة (afterprint) ليعيد التطبيق واجهته. */
    private void pollJob(final PrintJob job, final int n) {
        ui.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean done = job.isStarted() || job.isCompleted() || job.isCancelled() || job.isFailed() || n > 240;
                if (done) {
                    // مهلة قصيرة لتكتمل كتابة المستند قبل إخفاء منطقة الطباعة
                    ui.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            fireAfterPrint();
                        }
                    }, 1500);
                } else {
                    pollJob(job, n + 1);
                }
            }
        }, 500);
    }

    private void fireAfterPrint() {
        web.evaluateJavascript("window.dispatchEvent(new Event('afterprint'))", null);
    }
}
