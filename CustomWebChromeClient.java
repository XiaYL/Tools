package wendu.jsbdemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * Created by xiayanlei on 2019/11/13.
 * webview支持拍照和文件选择
 */

public class CustomWebChromeClient extends WebChromeClient {

    private static final String TAG = "CustomWebChromeClient";
    private static final int CHOOSE_FILE = 1000;
    private Activity context;
    private ValueCallback<Uri[]> valueCallback;
    private Uri localCameraUri;

    public CustomWebChromeClient(Activity context) {
        this.context = context;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        boolean allowMultiple = fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;//是否允许多选
        String[] mimeTypes = fileChooserParams.getAcceptTypes();//文件选择类型
        chooseImage(filePathCallback, allowMultiple, mimeTypes);
        return true;
    }

    private void chooseImage(ValueCallback<Uri[]> valueCallback, boolean multiple, String[] mimeTypes) {
        this.valueCallback = valueCallback;//用于向webview返回uri
        Intent pickFileIntent = new Intent(Intent.ACTION_GET_CONTENT);//默认是文件选择的intent
        pickFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        if (multiple) {
            pickFileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);//允许一次选择多张
        }
        pickFileIntent.setType("*/*");
        if (mimeTypes != null) {
            pickFileIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);//设置文件选择类型
        }
        Intent chooserIntent = Intent.createChooser(pickFileIntent, "File Chooser");
        localCameraUri = createCameraUri();//获取拍照存放资源
        if (localCameraUri != null) {// 添加相机拍摄
            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, localCameraUri);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{captureIntent});
        }
        context.startActivityForResult(chooserIntent, CHOOSE_FILE);
    }

    public void postH5File(int requestCode, int resultCode, Intent data) {
        if (valueCallback == null || requestCode != CHOOSE_FILE) {
            return;
        }
        Uri[] results = null;
        try {
            String dataString = data.getDataString();
            if (dataString != null) {//只有一张图片的时候
                results = new Uri[]{Uri.parse(dataString)};
            } else {//同时选择了多张图片
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
            }
            valueCallback.onReceiveValue(results);//回调uri给webview
        } catch (Exception e) {
            e.printStackTrace();
            if (localCameraUri != null && (data == null || data.getData() == null)) {//相机拍照
                results = new Uri[]{localCameraUri};
            }
            valueCallback.onReceiveValue(results);//失败了也需要通知webview
        } finally {
            valueCallback = null;
            localCameraUri = null;
        }
    }

    /**
     * @return 拍照存放资源路径
     */
    protected Uri createCameraUri() {
        return null;
    }
}
