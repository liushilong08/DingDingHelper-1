package app.bmkp.cn.dingdinghelper.helper;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by wangpan on 2018/9/14.
 */

public class PreferenceHelper {

    private SharedPreferences mPreferences;

    public PreferenceHelper(Context context) {
        mPreferences = context.getSharedPreferences("user_sharedPref", Context.MODE_PRIVATE);
    }

    public String getQQNickName() {
        return mPreferences.getString("qq_nick_name", "");
    }

    public void setQQNickName(String nickName) {
        mPreferences.edit().putString("qq_nick_name", nickName).apply();
    }

    public String getQQNumber() {
        return mPreferences.getString("qq_number", "");
    }

    public void setQQNumber(String qqNumber) {
        mPreferences.edit().putString("qq_number", qqNumber).apply();
    }

    public void setOnWorkHour(int hour) {
        mPreferences.edit().putInt("on_work_hour", hour).apply();
    }

    public int getOnWorkHour() {
        return mPreferences.getInt("on_work_hour", 0);
    }


    public void setOnWorkMinute(int minute) {
        mPreferences.edit().putInt("on_work_minute", minute).apply();
    }

    public int getOnWorkMinute() {
        return mPreferences.getInt("on_work_minute", 0);
    }

    public void setOffWorkHour(int hour) {
        mPreferences.edit().putInt("off_work_hour", hour).apply();
    }

    public int getOffWorkHour() {
        return mPreferences.getInt("off_work_hour", 0);
    }


    public void setOffWorkMinute(int minute) {
        mPreferences.edit().putInt("off_work_minute", minute).apply();
    }

    public int getOffWorkMinute() {
        return mPreferences.getInt("off_work_minute", 0);
    }

    public void setAutoDaka(boolean b) {
        mPreferences.edit().putBoolean("auto_daka", b).apply();
    }

    public boolean getAutoDaka() {
        return mPreferences.getBoolean("auto_daka", false);
    }
}
