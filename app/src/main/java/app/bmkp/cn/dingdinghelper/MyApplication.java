package app.bmkp.cn.dingdinghelper;

import android.app.Application;

import app.bmkp.cn.dingdinghelper.helper.PreferenceHelper;

/**
 * Created by wangpan on 2018/9/15.
 */

public class MyApplication extends Application {

    private static MyApplication instance = null;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
    }

    public static MyApplication getInstance() {
        return instance;
    }

    public PreferenceHelper getPreferenceHelper(){
        return new PreferenceHelper(this);
    }
}
