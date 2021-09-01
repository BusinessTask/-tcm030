package com.eyecool.finger.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.eyecool.finger.demo.base.BaseActivity;
import com.eyecool.finger.demo.utils.ToastUtils;
import com.eyecool.fp.client.Callback;
import com.eyecool.fp.client.TcFingerClient;
import com.eyecool.fp.entity.FingerResult;
import com.eyecool.fp.util.FpConst;
import com.eyecool.fp.util.Logs;
import com.eyecool.fp.util.SerialPortUtil;

/**
 * 建行指纹加密模块Activity
 */
public class SerialPortIDCActivity extends BaseActivity {

    private static final String TAG = SerialPortIDCActivity.class.getSimpleName();

    /**
     * 操作成功
     */
    private static final String RESULT_SUCC = "0";
    SerialPortUtil mSerialPortUtil;
    private Button mConnBtn;
    private Button mDisConnBtn;
    private Button mCancelBtn;
    private Button mGetDevInfoBtn;
    private TextView mHintTv;
    private ImageView mImageView;
    private String mPortPath = "/dev/ttyS1";
    private int mBaudrate = 115200;

    private TcFingerClient mClient;
    private Context mContext;
    private byte[] mTemplate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serial_port_idc);
        mContext = this;

        SharedPreferences setting = PreferenceManager
                .getDefaultSharedPreferences(this);
        String device = setting.getString("DEVICE", "");
        int baudrate = Integer.parseInt(setting.getString("BAUDRATE", "0"));
        mPortPath = device;
        mBaudrate = baudrate;

        mSerialPortUtil = SerialPortUtil.getInstance(getApplicationContext());
        mSerialPortUtil.setProtocol(SerialPortUtil.Protocol.CCB_ENC);

        mConnBtn = (Button) findViewById(R.id.connBtn);
        mDisConnBtn = (Button) findViewById(R.id.disconnBtn);
        mCancelBtn = (Button) findViewById(R.id.cancelBtn);
        mGetDevInfoBtn = findViewById(R.id.getInfoBtn);
        mHintTv = findViewById(R.id.hintTv);
        mImageView = (ImageView) findViewById(R.id.imgV);

        mConnBtn.setOnClickListener(v -> connectDevice());
        mDisConnBtn.setOnClickListener(v -> disconnectDevice());
        mCancelBtn.setOnClickListener(v -> cancel());
        mGetDevInfoBtn.setOnClickListener(v -> getDevInfo());

        findViewById(R.id.getSNBtn).setOnClickListener(v -> getSN());
        findViewById(R.id.registerFingerBtn).setOnClickListener(v -> registerFinger());
        findViewById(R.id.readFingerBtn).setOnClickListener(v -> readFinger());
        findViewById(R.id.readFingerAndMatchBtn).setOnClickListener(v -> readFingerAndMatch());
        findViewById(R.id.setBaudrateBtn).setOnClickListener(v -> setBaudrate());
        findViewById(R.id.registerDevBtn).setOnClickListener(v -> registerToDev());
        findViewById(R.id.searchBtn).setOnClickListener(v -> searchInDev());
        findViewById(R.id.deleteDevBtn).setOnClickListener(v -> deleteIdFromDev());
        findViewById(R.id.getIdListBtn).setOnClickListener(v -> getIdListInDev());
    }

    private void cancel() {
        if (mClient != null) {
            mClient.tcCancel();
        }
    }

    private void setBaudrate() {
    }

    /**
     * 获取sn
     */
    private void getSN() {
        if (!isDeviceReady()) {
            return;
        }
        FingerResult result = mClient.tcGetDevSn();
        mHintTv.setText(result.getResult());
    }

    /**
     * 获取设备信息
     */
    private void getDevInfo() {
        if (!isDeviceReady()) {
            return;
        }

        FingerResult result = mClient.tcGetDevFWInfo();
        mHintTv.setText(result.getResult());
    }

    /**
     * 指纹模板登记
     */
    private void registerFinger() {
        if (!isDeviceReady()) {
            return;
        }
        showMsg("请按手指");

        mClient.tcEnroll(new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                displayMessage("注册成功...");
                byte[] template = result.getTemplate();
                ToastUtils.showToast(mContext, "模板长度："
                        + (template == null ? 0 : template.length));
                mTemplate = template;
                ToastUtils.showToast(mContext, Base64.encodeToString(template, Base64.NO_WRAP));
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
                    default:
                        break;
                }
            }

            @Override
            public void onError(int errorCode) {
                ToastUtils.showToast(mContext, "注册失败errorCode:"
                        + errorCode);
                displayMessage("注册失败", errorCode);
            }
        }, false, 30000);
    }

    /**
     * 读取指纹特征
     */
    private void readFinger() {
        if (!isDeviceReady()) {
            return;
        }
        showMsg("请按手指");
        mClient.tcGetFeature(5000, new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                displayMessage("获取特征成功");
                byte[] feature = result.getTemplate();
                Logs.i(TAG, "特征长度：" + (feature == null ? 0 : feature.length) + " " + Base64.encodeToString(feature, Base64.NO_WRAP));

                showFingerImg(result.getImgBytes());
            }

            @Override
            public void onError(int errorCode) {
                ToastUtils.showToast(mContext, "获取特征失败：" + errorCode);
                displayMessage("获取特征失败", errorCode);
            }
        });
    }

    /**
     * 读取指纹并比对
     */
    private void readFingerAndMatch() {
        if (!isDeviceReady()) {
            return;
        }
        if (mTemplate == null) {
            ToastUtils.showToast(this, "请先注册指纹");
            return;
        }
        showMsg("请按手指");

        mClient.tcGetFeature(5000, new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                displayMessage("获取特征成功");
                byte[] feature = result.getTemplate();
                Logs.i(TAG, "特征长度：" + (feature == null ? 0 : feature.length));

                showFingerImg(result.getImgBytes());

                FingerResult result1 = mClient.tcMatch(mTemplate, result.getTemplate());
                if (result1.getStatus() >= 0) {
                    displayMessage("匹配成功 Score:" + result1.getScore());
                } else {
                    displayMessage("匹配失败 Score:" + result1.getScore());
                }
            }

            @Override
            public void onError(int errorCode) {
                ToastUtils.showToast(mContext, "获取特征失败：" + errorCode);
                displayMessage("获取特征失败", errorCode);
            }
        });
    }

    private void registerToDev() {
        if (!isDeviceReady()) {
            return;
        }
        showMsg("请按手指");
        showMsg("请按手指");
        mClient.tcGetFeature(5000, new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                displayMessage("获取特征成功");
                byte[] feature = result.getTemplate();
                Logs.i(TAG, "特征长度：" + (feature == null ? 0 : feature.length) + " " + Base64.encodeToString(feature, Base64.NO_WRAP));

                FingerResult result1 = mClient.tcDevRegister(1, feature);
                if (result1.getStatus() >= 0) {
                    displayMessage("注册成功");
                } else {
                    displayMessage("注册失败:" + result1.getStatus());
                }

                showFingerImg(result.getImgBytes());
            }

            @Override
            public void onError(int errorCode) {
                ToastUtils.showToast(mContext, "获取特征失败：" + errorCode);
                displayMessage("获取特征失败", errorCode);
            }
        });
    }


    private void searchInDev() {
        if (!isDeviceReady()) {
            return;
        }
        showMsg("请按手指");
        mClient.tcDevSearch(5000, new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                displayMessage("获取特征成功");
                byte[] feature = result.getTemplate();
                Logs.i(TAG, "特征长度：" + (feature == null ? 0 : feature.length));

                showFingerImg(result.getImgBytes());

                displayMessage("查询到Id：" + result.getId());
            }

            @Override
            public void onError(int errorCode) {
                ToastUtils.showToast(mContext, "获取特征失败：" + errorCode);
                displayMessage("获取特征失败", errorCode);
            }
        });
    }

    private void deleteIdFromDev() {
        if (!isDeviceReady()) {
            return;
        }
        FingerResult result = mClient.tcDevDelete(1);
        if (result.getStatus() >= 0) {
            displayMessage("删除成功");
        } else {
            if (result.getStatus() == FpConst.TCY_EMPT) {
                displayMessage("删除失败 不存在的Id");
            } else {
                displayMessage("删除失败 code:" + result.getStatus());
            }
        }
    }

    private void getIdListInDev() {
        if (!isDeviceReady()) {
            return;
        }
        FingerResult result = mClient.tcDevGetListId();
        if (result.getStatus() >= 0) {
            displayMessage("获取注册列表：" + result.getIdList().toString());
        } else {
            displayMessage("获取失败");
        }
    }

    private void disconnectDevice() {
        mSerialPortUtil.closeDevice();
        mClient = null;
    }

    private void connectDevice() {
        mClient = mSerialPortUtil.openDevice(mPortPath, mBaudrate);
    }

    private void showMsg(String text) {
        mHintTv.post(() -> mHintTv.setText(text));
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
                errorMsg = errorCode + "";
                break;
        }
        return errorMsg;
    }

    private void showFingerImg(byte[] fingerImg) {
        if (fingerImg == null) {
            mImageView.setImageBitmap(null);
            return;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ALPHA_8;
        Bitmap bitmap = BitmapFactory.decodeByteArray(fingerImg, 0,
                fingerImg.length, options);

        mImageView.setImageBitmap(bitmap);
    }
}
