package com.eyecool.finger.demo;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.eyecool.finger.demo.base.BaseActivity;
import com.eyecool.finger.demo.utils.ToastUtils;
import com.eyecool.fp.client.Callback;
import com.eyecool.fp.client.TcFingerClient;
import com.eyecool.fp.entity.FingerResult;
import com.eyecool.fp.util.ExecutorUtil;
import com.eyecool.fp.util.FileUtils;
import com.eyecool.fp.util.FpConfig;
import com.eyecool.fp.util.FpConst;
import com.eyecool.fp.util.FpImage;
import com.eyecool.fp.util.Logs;
import com.eyecool.fp.util.OnUsbConnectListener;
import com.eyecool.fp.util.USBUtil;
import com.eyecool.fp.util.USBUtil.Protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 天诚盛业大容量存储指纹设备Demo
 *
 * @author wangzhi
 */
public class UsbActivity extends BaseActivity {

    private static final String TAG = UsbActivity.class.getSimpleName();

    private static final String BASE_PATH = Environment
            .getExternalStorageDirectory().getPath();
    private static final String TEMPLATE_PATH = BASE_PATH + "/Tc_Template.bin";
    USBUtil mUsbUtil;
    TcFingerClient mClient;
    private Switch mDeepSwitch;
    private Button mConnBtn;
    private Button mDisConnBtn;
    private Button mCancelBtn;
    private Button mPressBtn;
    private Button mGetSnBtn;
    private Button mGetImgBtn;
    private Button mGetFeatureBtn;
    private Button mGetInfoBtn;
    private Button mEnrollBtn;
    private Button mVerifyBtn;
    private Button mUpdateBtn;

    private ImageView mImageView;
    private TextView mHintTv;
    private byte[] mTemplate;
    private Context mContext;
    private int index = 0;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb);
        mContext = this;

        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage("断开连接中...");
        mProgressDialog.setCancelable(false);

        int featureType = getIntent().getIntExtra(EXTRA_FEATURE_TYPE, FpConfig.FEATURE_TYPE_BASE64);
        Logs.i(TAG, "featureType:" + featureType);

        mUsbUtil = USBUtil.getInstance(getApplicationContext());
        // 设置设备使用协议
        mUsbUtil.setProtocol(Protocol.SIMPLE);
        // 设置特征类型
        FpConfig.setFeatureType(featureType);
        // 设置出图规格
        FpConfig.setImageType(FpConfig.IMAGE_BIG);
        // 设置质量分数
        FpConfig.setQualityScore(40);
        // 注册按3次判断指纹图像位置及质量（默认false）
//        FpConfig.setCheckFingerQty(true);
        // 注册广播
        mUsbUtil.registerMonitor();

        mDeepSwitch = findViewById(R.id.deepSwitch);
        mConnBtn = findViewById(R.id.connBtn);
        mDisConnBtn = findViewById(R.id.disconnBtn);
        mCancelBtn = findViewById(R.id.cancelBtn);
        mPressBtn = findViewById(R.id.pressBtn);
        mGetSnBtn = findViewById(R.id.getSnBtn);
        mGetImgBtn = findViewById(R.id.getImgBtn);
        mGetFeatureBtn = findViewById(R.id.getFeatureBtn);
        mGetInfoBtn = findViewById(R.id.getInfoBtn);
        mEnrollBtn = findViewById(R.id.enrollBtn);
        mVerifyBtn = findViewById(R.id.verifyBtn);
        mImageView = findViewById(R.id.imgV);
        mUpdateBtn = findViewById(R.id.updateBtn);
        mHintTv = findViewById(R.id.hintTv);

        mConnBtn.setOnClickListener(v -> connBtnClick());
        mDisConnBtn.setOnClickListener(v -> disconnBtnClick());
        mCancelBtn.setOnClickListener(v -> cancelBtnClick());
        mPressBtn.setOnClickListener(v -> pressBtnClick());
        mGetSnBtn.setOnClickListener(v -> getSnBtnClick());
        mGetImgBtn.setOnClickListener(v -> getImgBtnClick());
        mGetFeatureBtn.setOnClickListener(v -> getFeatureBtnClick());
        mGetInfoBtn.setOnClickListener(v -> getDevInfo());
        mEnrollBtn.setOnClickListener(v -> enrollBtnClick());
        mVerifyBtn.setOnClickListener(v -> verifyBtnClick());
        mUpdateBtn.setOnClickListener(v -> updateBtnClick());

        mUsbUtil.setConnectListener(new OnUsbConnectListener() {

            @Override
            public void onConnected(TcFingerClient client) {
                ToastUtils.showToast(UsbActivity.this, "设备连接成功");
                // 获取指纹操作客户端
                mClient = client;
            }

            @Override
            public void onConnectFailed(int code, String msg) {
                ToastUtils.showToast(UsbActivity.this, msg);
            }

            @Override
            public void onDisconnected() {
                super.onDisconnected();
                mProgressDialog.dismiss();
            }

            @Override
            public void onAttach(UsbDevice usbDevice) {
                // TODO Auto-generated method stub
                super.onAttach(usbDevice);
                // connBtnClick();
            }

            @Override
            public void onDettach(UsbDevice usbDevice) {
                // TODO Auto-generated method stub
                super.onDettach(usbDevice);
            }
        });
        mTemplate = getTemplateFromDisk();
    }

    protected void connBtnClick() {
        mUsbUtil.connectDevice();
    }

    protected void disconnBtnClick() {
        mProgressDialog.show();
        mUsbUtil.asynDisconnectDevice();
    }

    protected void pressBtnClick() {
        if (!isDeviceReady()) {
            return;
        }
        ExecutorUtil.exec(() -> {
            int status = mClient.tcDetectFinger();
            runOnUiThread(() -> {
                displayMessage("isPressed: " + status);
                ToastUtils.showToast(this, "isPressed = " + status);
            });

        });
    }

    private void updateBtnClick() {
        if (!isDeviceReady()) {
            return;
        }
    }

    protected void cancelBtnClick() {
        if (mClient == null) {
            ToastUtils.showToast(this, "设备未连接");
            return;
        }
        mClient.tcCancel();
    }

    protected void getDevInfo() {
        if (!isDeviceReady()) {
            return;
        }
        mClient.tcGetDevFWInfo(new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                Logs.i(TAG, "dev info:" + result.getResult());
                ToastUtils.showToast(UsbActivity.this, result.getResult());
                displayMessage(result.getResult());
            }

            @Override
            public void onError(int errorCode) {
                ToastUtils
                        .showToast(UsbActivity.this, "errorCode:" + errorCode);
                displayMessage("获取设备信息失败", errorCode);
            }
        });
    }

    protected void getSnBtnClick() {
        if (!isDeviceReady()) {
            return;
        }
        mClient.tcGetDevSn(new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                Logs.i(TAG, "sn:" + result.getResult());
                ToastUtils.showToast(UsbActivity.this, result.getResult());
                displayMessage(result.getResult());
            }

            @Override
            public void onError(int errorCode) {
                ToastUtils
                        .showToast(UsbActivity.this, "errorCode:" + errorCode);
                displayMessage("获取设备SN失败", errorCode);
            }
        });
    }

    protected void getImgBtnClick() {
        if (!isDeviceReady()) {
            return;
        }
        final long start = System.currentTimeMillis();
        displayMessage("获取图像...");
        mClient.tcGetImage(3000, new Callback() {

            @Override
            public void onSuccess(final FingerResult result) {
                ToastUtils.showToast(
                        UsbActivity.this,
                        "status:"
                                + result.getStatus()
                                + " img bytes:"
                                + (result.getImgBytes() == null ? null : result
                                .getImgBytes().length));

                showFingerImg(result.getImgBytes());
                displayMessage("获取图片时间：" + (System.currentTimeMillis() - start)
                        + "ms");

                FileUtils.writeFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/FingerTest/" + System.currentTimeMillis() + ".bmp", result.getImgBytes());
            }

            @Override
            public void onError(int errorCode) {
                ToastUtils
                        .showToast(UsbActivity.this, "errorCode:" + errorCode);
                displayMessage("获取图像失败", errorCode);
            }
        });
    }

    protected void getFeatureBtnClick() {
        if (!isDeviceReady()) {
            return;
        }
        displayMessage("获取特征...");
        mClient.tcGetFeature(5000, new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                displayMessage("获取特征成功");
                byte[] feature = result.getTemplate();
                Logs.i(TAG, "特征长度：" + (feature == null ? 0 : feature.length));
                showFingerImg(result.getImgBytes());

                Logs.i(TAG, "fea:" + new String(feature));
                saveTemplateToDisk(feature, BASE_PATH + "/Tc_Feature.bin");
            }

            @Override
            public void onError(int errorCode) {
                ToastUtils.showToast(UsbActivity.this, "获取特征失败：" + errorCode);
                displayMessage("获取特征失败", errorCode);
            }
        });
    }

    protected void enrollBtnClick() {
        if (!isDeviceReady()) {
            return;
        }
        displayMessage("开始注册");
        index = 0;
        mClient.tcEnroll(new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                displayMessage("注册成功...");
                byte[] template = result.getTemplate();
                ToastUtils.showToast(UsbActivity.this, "模板长度："
                        + (template == null ? 0 : template.length));
                saveTemplateToDisk(template, TEMPLATE_PATH);
                mTemplate = template;
            }

            @Override
            public void onProcess(int code, FingerResult result) {
                super.onProcess(code, result);
                switch (code) {
                    case FpConst.TCY_CAPTUREIMAGE:
                        displayMessage("请按捺指纹...");
                        break;
                    case FpConst.TCY_GETIMAGE:
                        showFingerImg(result.getImgBytes());
                        break;
                    case FpConst.TCY_TOLV:
                        displayMessage("请抬起手指...");
                        break;
                    case FpConst.TCY_TOO_LEFT:
                        displayMessage("手指偏左...");
                        break;
                    case FpConst.TCY_TOO_RIGHT:
                        displayMessage("手指偏右...");
                        break;
                    case FpConst.TCY_TOO_UP:
                        displayMessage("手指偏上...");
                        break;
                    case FpConst.TCY_TOO_DOWN:
                        displayMessage("手指偏下...");
                        break;
                    case FpConst.TCY_TOO_WET:
                        displayMessage("手指太湿...");
                        break;
                    case FpConst.TCY_TOO_DRY:
                        displayMessage("手指干...");
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(int errorCode) {
                ToastUtils.showToast(UsbActivity.this, "注册失败errorCode:"
                        + errorCode);
                displayMessage("注册失败", errorCode);
            }
        }, 30000);
    }

    protected void verifyBtnClick() {
        if (!isDeviceReady()) {
            return;
        }

        displayMessage("开始认证");
        mClient.tcGetFeature(5000, new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
//                String fea = Base64.encodeToString(result.getTemplate(), Base64.NO_WRAP);
                byte[] feature = result.getTemplate();

                showFingerImg(result.getImgBytes());

                FingerResult result2 = mClient.tcMatch(feature, mTemplate);
                mClient.tcMatch(feature, mTemplate);

                if (result2.getStatus() >= 0) {
                    displayMessage("匹配成功 Score:" + result2.getScore());
                } else {
                    displayMessage("匹配失败 Score:" + result2.getScore());
                }
            }

            @Override
            public void onError(int errorCode) {
                displayMessage("比对失败", errorCode);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mClient != null) {
            mClient.tcCancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUsbUtil.unRegisterMonitor();
        mUsbUtil.asynDisconnectDevice();
    }

    private byte[] getTemplateFromDisk() {
        File file = new File(TEMPLATE_PATH);
        if (!file.exists()) {
            return null;
        }

        FileInputStream in = null;

        try {
            in = new FileInputStream(file);
            int length = in.available();

            byte[] buffer = new byte[length];
            in.read(buffer);

            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            // 关闭创建的流对象
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showFingerImg(byte[] fingerImg) {
        if (fingerImg == null) {
            mImageView.setImageBitmap(null);
            return;
        }
        byte[] showFingerImg = fingerImg;
        if (mDeepSwitch.isChecked()) {
            showFingerImg = FpImage.deepenBMP(fingerImg);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ALPHA_8;
        Bitmap bitmap = BitmapFactory.decodeByteArray(showFingerImg, 0,
                showFingerImg.length, options);

        mImageView.setImageBitmap(bitmap);
    }

    private void displayMessage(String msg) {
        mHintTv.setText(msg);
    }

    private void displayMessage(String msg, int errorCode) {
        displayMessage(msg + "/" + parseErrorCode(errorCode));
    }

    private String parseErrorCode(int errorCode) {
        String errorMsg = "";
        switch (errorCode) {
            case FpConst.TCY_NSAM:
                errorMsg = "手指不同";
                break;
            case FpConst.TCY_TMOT:
                errorMsg = "超时";
                break;
            case FpConst.TCY_NLNK:
                errorMsg = "设备断开";
                break;
            case FpConst.TCY_CNCL:
                errorMsg = "取消";
                break;
            case FpConst.DOG_NONE:
                errorMsg = "未授权";
                break;
            default:
                break;
        }
        return errorMsg;
    }

    private boolean isDeviceReady() {
        if (mClient == null) {
            ToastUtils.showToast(this, "设备未连接");
            return false;
        }
        if (mClient.isBusy()) {
            ToastUtils.showToast(this, "设备忙");
            return false;
        }
        showFingerImg(null);
        displayMessage("");
        return true;
    }

    private void saveTemplateToDisk(byte[] bytes, String filePath) {
        if (bytes == null) {
            return;
        }
        FileUtils.writeFile(filePath, bytes);
    }
}