package app.bmkp.cn.dingdinghelper.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

import java.io.DataOutputStream;
import java.util.List;

/**
 * Created by wangpan on 2018/9/15.
 */

public class MyUtils {

    //钉钉包名
    public static final String DING_DING_PACKAGE_NAME = "com.alibaba.android.rimet";
    //TIM包名
    public static final String TIM_PACKAGE_NAME = "com.tencent.tim";
    //QQ包名
    public static final String QQ_PACKAGE_NAME = "com.tencent.mobileqq";

    /**
     * 通过包名启动App
     *
     * @param context
     * @param packageName
     */
    public static boolean startApplicationByPackageName(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return false;
        }
        PackageManager packageManager = context.getPackageManager();
        //先判断能否在设备上找到对应的app
        PackageInfo packageinfo = null;
        try {
            packageinfo = packageManager.getPackageInfo(packageName, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            return false;
        }
        //创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveIntent.setPackage(packageName);
        //通过getPackageManager()的queryIntentActivities方法遍历能处理这个Intent的应用
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(resolveIntent, 0);
        ResolveInfo resolveInfo = resolveInfos.iterator().next();
        if (resolveInfo != null) {
            String startPackageName = resolveInfo.activityInfo.packageName;
            String startClassName = resolveInfo.activityInfo.name;
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName componentName = new ComponentName(startPackageName, startClassName);
            intent.setComponent(componentName);
            context.startActivity(intent);
        }
        return true;
    }

    /**
     * 通过包名强制停止app
     *
     * @param packageName
     */
    public static void forceStopApplicationByPackageName(String packageName) {
        try {
            executeCMD("am force-stop " + packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开无障碍功能列表
     *
     * @param activity
     */
    public static void startAccessibilitySettings(Context activity) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        activity.startActivity(intent);
    }

    /**
     * 判断指定的辅助服务是否已启动
     *
     * @param context
     * @param serviceClazz
     * @return
     */
    public static boolean isAccessibilityServiceStarted(Context context, Class serviceClazz) {
        String name = serviceClazz.getName();
        try {
            int i = Settings.Secure.getInt(context.getContentResolver(), "accessibility_enabled");
            if (i == 1) {
                String s = Settings.Secure.getString(context.getContentResolver(), "enabled_accessibility_services");
                if (!TextUtils.isEmpty(s)) {
                    String[] arr = s.split(":");
                    if (arr.length > 0) {
                        for (int j = 0; j < arr.length; j++) {
                            String serviceName = arr[j];
                            if (serviceName.contains(name)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 执行CMD命令
     *
     * @param cmd
     */
    public static boolean executeCMD(String cmd) {
        try {
            // 申请获取root权限，这一步很重要，不然会没有作用
            Process process = Runtime.getRuntime().exec("su");
            // 获取输出流
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd);
            os.flush();
            os.close();
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    /**
     * 检查app是否有root权限
     *
     * @return
     */
    public static boolean checkRootPermission() {
        try {
            String cmd = "touch /data/root_test.txt";
            Process process = Runtime.getRuntime().exec("su"); //切换到root帐号
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "/n");
            os.writeBytes("exit/n");
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 模拟点击屏幕上的指定位置
     *
     * @param x
     * @param y
     */
    public static boolean clickScreen(float x, float y) {
        return executeCMD("input tap " + x + " " + y);
    }

    /**
     * 打开QQ和指定联系人的聊天界面
     *
     * @param context
     * @param qqNumber
     */
    public static boolean openQQChat(Context context, String qqNumber) {
        try {
            String url = "mqqwpa://im/chat?chat_type=wpa&uin=" + qqNumber;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            //Service的Context启动Activity需要设置这个, 不然启动不了
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 模拟点击Home键
     *
     * @param context
     */
    public static void clickHome(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        context.startActivity(intent);
    }
}
