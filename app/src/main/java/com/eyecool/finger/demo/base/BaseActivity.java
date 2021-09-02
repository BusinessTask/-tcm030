package com.eyecool.finger.demo.base;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

import androidx.appcompat.app.AppCompatActivity;

import com.eyecool.finger.demo.utils.MeasureUtil;

public class BaseActivity extends AppCompatActivity {

    public static final String EXTRA_FEATURE_TYPE = "com.eyecool.finger.FEATURE_TYPE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private ViewGroup getContentView() {
        ViewGroup view = (ViewGroup) getWindow().getDecorView();
        ViewGroup content = (ViewGroup) view.getChildAt(0);
        return content;
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ViewGroup viewGroup = getContentView();
        int[] screenSize = MeasureUtil.getScreenSize(this);
        if (screenSize[0] > screenSize[1]) {
            LayoutParams lp = (LayoutParams) viewGroup.getLayoutParams();
            lp.width = screenSize[0] / 2;
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            viewGroup.setLayoutParams(lp);
        }
    }

}
