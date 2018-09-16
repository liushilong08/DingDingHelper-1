package app.bmkp.cn.dingdinghelper.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import app.bmkp.cn.dingdinghelper.MyApplication;
import app.bmkp.cn.dingdinghelper.helper.PreferenceHelper;


/**
 * Created by wangpan on 2018/9/15.
 */

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public PreferenceHelper getPreferenceHelper() {
        return MyApplication.getInstance().getPreferenceHelper();
    }
}
