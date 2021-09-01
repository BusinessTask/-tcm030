package com.eyecool.finger.demo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.eyecool.finger.demo.base.BaseActivity;
import com.eyecool.fp.util.FpConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 指纹首页索引
 *
 * @author wangzhi
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int REQUEST_PERMISSIONS_CODE = 11;
    int tempWitch = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.usbBtn).setOnClickListener(v -> usbFinger());
        findViewById(R.id.serialBtn).setOnClickListener(v -> serialFinger());
        findViewById(R.id.idcSerialBtn).setOnClickListener(v -> idcSerialFinger());
        findViewById(R.id.serialPortSettingBtn).setOnClickListener(v -> serialPortSetting());
        requestPermissions();
    }

    private void usbFinger() {
        featureTypeDialogChoice();
    }

    private void serialFinger() {
        startActivity(new Intent(this, SerialPortActivity.class));
    }

    private void idcSerialFinger() {
        startActivity(new Intent(this, SerialPortIDCActivity.class));
    }

    private void serialPortSetting() {
        startActivity(new Intent(this, SerialPortPreferences.class));
    }

    private void featureTypeDialogChoice() {
        final String items[] = {"普通商行特征", "公安GA特征", "国际版特征"};
        tempWitch = 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this, 0);
        builder.setTitle("选择特征类型")
                .setSingleChoiceItems(items, 0,
                        (dialog, which) -> {
                            Toast.makeText(MainActivity.this, items[which],
                                    Toast.LENGTH_SHORT).show();
                            tempWitch = which;
                        })
                .setPositiveButton("确定", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, "确定", Toast.LENGTH_SHORT)
                            .show();

                    int featureType = FpConfig.FEATURE_TYPE_OLDBASE64;
                    switch (tempWitch) {
                        case 0:
                            featureType = FpConfig.FEATURE_TYPE_BASE64;
                            break;
                        case 1:
                            featureType = FpConfig.FEATURE_TYPE_GA;
                            break;
                        case 2:
                            featureType = FpConfig.FEATURE_INTERNATIONAL_TC_ISO_19794_2_2011;
                            break;
                    }

                    Intent intent = new Intent(this, UsbActivity.class);
                    intent.putExtra(BaseActivity.EXTRA_FEATURE_TYPE, featureType);
                    startActivity(intent);
                });
        builder.create().show();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = null;
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                permissions = new ArrayList<>();
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (permissions == null) {
            } else {
                String[] permissionArray = new String[permissions.size()];
                permissions.toArray(permissionArray);
                // Request the permission. The result will be received
                // in onRequestPermissionResult()
                requestPermissions(permissionArray, REQUEST_PERMISSIONS_CODE);
            }
        } else {
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Request for WRITE_EXTERNAL_STORAGE permission.
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_PERMISSIONS_CODE) {
            }
        } else {
            // Permission request was denied.
            Toast.makeText(this, R.string.txt_error_permission, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
