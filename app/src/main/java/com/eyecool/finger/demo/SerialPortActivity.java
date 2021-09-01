package com.eyecool.finger.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.eyecool.finger.demo.base.BaseActivity;
import com.eyecool.finger.demo.utils.ToastUtils;
import com.eyecool.fp.client.Callback;
import com.eyecool.fp.client.TcFingerClient;
import com.eyecool.fp.entity.FingerResult;
import com.eyecool.fp.util.FileUtils;
import com.eyecool.fp.util.FpCoder;
import com.eyecool.fp.util.FpConfig;
import com.eyecool.fp.util.Logs;
import com.eyecool.fp.util.SerialPortUtil;
import com.eyecool.fp.util.SerialPortUtil.Protocol;

/**
 * 指纹串口通信Demo
 *
 * @author wangzhi
 */
public class SerialPortActivity extends BaseActivity {

    private static final String TAG = SerialPortActivity.class.getSimpleName();

    private static final String BASE_PATH = Environment
            .getExternalStorageDirectory().getPath();
    private static final String TEMPLATE_PATH = BASE_PATH + "/Tc_Template.bin";

    Button mGetSnBtn;
    Button mGetFeatureBtn;
    Button mRegisterBtn;
    Button mGetImgBtn;
    Button mCancelBtn;
    ImageView mImageView;
    TextView mHintTv;

    SerialPortUtil mSerialPortUtil;
    private String mPortPath = "/dev/ttySAC3";
    private int mBaudrate = 9600;

    private TcFingerClient mTcFingerClient;
    private Context mContext;
    private byte[] mTemplate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serial_port);
        mContext = this;

        SharedPreferences setting = PreferenceManager
                .getDefaultSharedPreferences(this);
        String device = setting.getString("DEVICE", "");
        int baudrate = Integer.parseInt(setting.getString("BAUDRATE", "0"));
        mPortPath = device;
        mBaudrate = baudrate;

        mSerialPortUtil = SerialPortUtil.getInstance(getApplicationContext());
        mSerialPortUtil.setProtocol(Protocol.LASERX);
        mTcFingerClient = mSerialPortUtil.openDevice(mPortPath, mBaudrate);
        // 设置特征类型，base64格式
        FpConfig.setFeatureType(FpConfig.FEATURE_TYPE_BASE64);

        ToastUtils.showToast(this, "path:" + mPortPath + " baud:" + mBaudrate);

        mImageView = findViewById(R.id.imgV);
        mGetSnBtn = findViewById(R.id.snBtn);
        findViewById(R.id.getDevInfoBtn).setOnClickListener(v -> getDevInfo());
        mGetFeatureBtn = findViewById(R.id.featureBtn);
        mRegisterBtn = findViewById(R.id.registerBtn);
        mGetImgBtn = findViewById(R.id.getImgBtn);
        mCancelBtn = findViewById(R.id.cancelBtn);
        mHintTv = findViewById(R.id.hintTv);

        mGetSnBtn.setOnClickListener(v -> getSnBtnClick());
        findViewById(R.id.isPressedBtn).setOnClickListener(v -> isPressedBtnClick());
        mGetFeatureBtn.setOnClickListener(v -> getFeatureBtnClick());
        mRegisterBtn.setOnClickListener(v -> registerBtnClick());
        mGetImgBtn.setOnClickListener(v -> getImgBtnClick());
        mCancelBtn.setOnClickListener(v -> cancelBtnClick());
        findViewById(R.id.setBaudRateBtn).setOnClickListener(v -> setBaudRateBtnClick());
        findViewById(R.id.verifyBtn).setOnClickListener(v -> verifyBtnClick());
    }

    private void getDevInfo() {
        if (mTcFingerClient == null) {
            Toast.makeText(this, "串口未打开", Toast.LENGTH_SHORT).show();
            return;
        }

        FingerResult result = mTcFingerClient.tcGetDevFWInfo();
        mHintTv.setText("设备信息: " + result.getResult());
    }

    protected void isPressedBtnClick() {
        if (mTcFingerClient == null) {
            Toast.makeText(this, "串口未打开", Toast.LENGTH_SHORT).show();
            return;
        }
        int press = mTcFingerClient.tcDetectFinger();
        ToastUtils.showToast(mContext, "perss:" + press);
        mHintTv.setText("isPress:" + press);
    }

    protected void verifyBtnClick() {
        if (mTcFingerClient == null) {
            Toast.makeText(this, "串口未打开", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mTemplate == null) {
            Toast.makeText(this, "请先注册", Toast.LENGTH_SHORT).show();
            return;
        }
        mTcFingerClient.tcGetFeature(10000, new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                FingerResult result2 = mTcFingerClient.tcMatch(
                        result.getTemplate(), mTemplate);
                if (result2.getStatus() < 0) {
                    mHintTv.setText("验证失败 分数：" + result2.getScore());
                    return;
                }
                mHintTv.setText("验证成功 分数：" + result2.getScore());

                FileUtils.writeFile(BASE_PATH + "/Tc_Feature_base64.txt", result.getTemplate());
            }

            @Override
            public void onError(int errorCode) {
                mHintTv.setText("比对失败 errorCode:" + errorCode);
            }
        });
    }

    protected void setBaudRateBtnClick() {
        if (mTcFingerClient == null) {
            Toast.makeText(this, "串口未打开", Toast.LENGTH_SHORT).show();
            return;
        }
        mTcFingerClient.tcSetBaudRate(7, new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                ToastUtils.showToast(mContext, "成功");
                mTcFingerClient = mSerialPortUtil.openDevice(mPortPath, 115200);
            }

            @Override
            public void onError(int errorCode) {

            }
        });
    }

    protected void cancelBtnClick() {
        if (mTcFingerClient == null) {
            Toast.makeText(this, "串口未打开", Toast.LENGTH_SHORT).show();
            return;
        }
        mTcFingerClient.tcCancel();
    }

    protected void getFeatureBtnClick() {
        if (mTcFingerClient == null) {
            Toast.makeText(this, "串口未打开", Toast.LENGTH_SHORT).show();
            return;
        }

        mTcFingerClient.tcGetFeature(30000, new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                byte[] template = result.getTemplate();

                mHintTv.setText("获取特征成功:" + new String(template));
            }

            @Override
            public void onError(int errorCode) {
                mHintTv.setText("获取特征失败 errorCode:" + errorCode);
            }
        });
    }

    protected void getSnBtnClick() {
        if (mTcFingerClient == null) {
            Toast.makeText(this, "串口未打开", Toast.LENGTH_SHORT).show();
            return;
        }

        FingerResult result = mTcFingerClient.tcGetDevSn();
        mHintTv.setText("设备SN: " + result.getResult());
    }

    protected void registerBtnClick() {
        if (mTcFingerClient == null) {
            Toast.makeText(this, "串口未打开", Toast.LENGTH_SHORT).show();
            return;
        }

        mTcFingerClient.tcGetTemplate(30000, new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                mTemplate = result.getTemplate();

                Logs.i(TAG, "tcGetTemplate = " + new String(result.getTemplate()));

                FileUtils.writeFile(BASE_PATH + "/Tc_Template_base64.txt", Base64.encodeToString(mTemplate, Base64.NO_WRAP));
                mHintTv.setText("注册成功");
            }

            @Override
            public void onError(int errorCode) {
                mHintTv.setText("注册失败 errorCode:" + errorCode);
            }
        });
    }

    protected void getImgBtnClick() {
        if (mTcFingerClient == null) {
            Toast.makeText(this, "串口未打开", Toast.LENGTH_SHORT).show();
            return;
        }

        mTcFingerClient.tcSetBaudRate(7);
        mBaudrate = 115200;
        mTcFingerClient = mSerialPortUtil.openDevice(mPortPath, mBaudrate);
        mTcFingerClient.tcGetImage(30000, new Callback() {

            @Override
            public void onSuccess(FingerResult result) {
                Toast.makeText(
                        SerialPortActivity.this,
                        "status:" + result.getStatus() + "imageSize:"
                                + result.getImgBytes().length,
                        Toast.LENGTH_SHORT).show();
                Bitmap bitmap = BitmapFactory.decodeByteArray(
                        result.getImgBytes(), 0, result.getImgBytes().length);
                Bitmap bitmap2 = FpCoder.getRedTransparentBitmap(bitmap);
                mImageView.setImageBitmap(bitmap2);

                reConnect(9600);
            }

            @Override
            public void onError(int errorCode) {
                Toast.makeText(SerialPortActivity.this,
                        "errorCode:" + errorCode, Toast.LENGTH_SHORT).show();
                reConnect(9600);
            }
        });
    }

    private void reConnect(int baudRate) {
        mTcFingerClient.tcSetBaudRate(3);
        mBaudrate = baudRate;
        mTcFingerClient = mSerialPortUtil.openDevice(mPortPath, mBaudrate);
    }

    private void saveTemplateToDisk(byte[] bytes, String filePath) {
        if (bytes == null) {
            return;
        }
        FileUtils.writeFile(filePath, bytes);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSerialPortUtil.closeDevice();
    }
}
