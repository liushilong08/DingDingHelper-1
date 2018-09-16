package app.bmkp.cn.dingdinghelper.service;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import app.bmkp.cn.dingdinghelper.MyApplication;
import app.bmkp.cn.dingdinghelper.R;
import app.bmkp.cn.dingdinghelper.event.AutoDaKaEvent;
import app.bmkp.cn.dingdinghelper.helper.PreferenceHelper;
import app.bmkp.cn.dingdinghelper.utils.MyUtils;
import de.greenrobot.event.EventBus;

/**
 * Created by wangpan on 2018/9/14.
 */

public class DingDingHelperService extends AccessibilityService {

    private static final String TAG = DingDingHelperService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 10001;

    private PowerManager.WakeLock mWakeLock;
    private int mDStep = -1;

    //解锁屏幕
    private static final int UNLOCK_SCREEN = 1;
    //打开钉钉
    private static final int OPEN_DING_DING = 2;

    //钉钉签到步骤 点击工作-点击签到跳转签到界面-点击签到-点击提交
    private static final int STEP_PREPARE_CLICK_WORK = 0;
    private static final int STEP_PREPARE_GOTO_SIGN = 1;
    private static final int STEP_PREPARE_CLICK_SIGN = 2;
    private static final int STEP_PREPARE_CLICK_SUBMIT = 3;
    private static final int STEP_SIGN_COMPLETED = 4;

    //发送QQ回执消息步骤 打开聊天界面-设置回执消息-点击发送
    private int mQStep = -1;

    private static final int STEP_PREPARE_SEND_MSG = 1;
    private static final int STEP_SEND_MSG_COMPLETED = 2;

    //QQ指令
    private static final String CMD_SIGN = "打卡";

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UNLOCK_SCREEN:
                    unlockScreen();
                    break;
                case OPEN_DING_DING:
                    openDingDing();
                    break;
            }
        }
    };

    public void acquireWakeLock(String tag) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
        mWakeLock.acquire();
    }

    public void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    public PreferenceHelper getPreferenceHelper() {
        return MyApplication.getInstance().getPreferenceHelper();
    }

    /**
     * 解锁屏幕
     */
    private void unlockScreen() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("tag", "屏幕解锁中");
                MyUtils.executeCMD("input swipe 300 1000 300 100 ");
                mHandler.obtainMessage(OPEN_DING_DING).sendToTarget();
            }
        }, 1000);
    }

    /**
     * 打开钉钉
     */
    private void openDingDing() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("tag", "正在打开钉钉app");
                boolean b = MyUtils.startApplicationByPackageName(
                        DingDingHelperService.this,
                        MyUtils.DING_DING_PACKAGE_NAME);
                if (b) {
                    Log.e("tag", "钉钉app打开成功");
                    //初始化步骤标记和一些值
                    mDStep = STEP_PREPARE_CLICK_WORK;
                    //用来判断签到页面是否加载完成
                    mLastChangedTime = -1;
                    mCheckCount = 0;
                } else {
                    Log.e("tag", "openDingDing 钉钉打开失败");
                }
            }
        }, 3000);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //创建前台通知
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);

        acquireWakeLock(TAG);

        EventBus.getDefault().register(this);
    }

    /**
     * 创建前台通知
     */
    private Notification createNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentText("钉钉打卡助手已启动");
        builder.setContentTitle("钉钉打卡助手");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setTicker("钉钉打卡助手");
        builder.setAutoCancel(true);
        builder.setWhen(System.currentTimeMillis());
        return builder.build();
    }

    /**
     * 取消通知
     */
    private void clearNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);
    }

    /**
     * 时间改变广播, 每分钟执行一次
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean autoDaka = getPreferenceHelper().getAutoDaka();
            if (autoDaka) {
                int onWorkHour = getPreferenceHelper().getOnWorkHour();
                int onWorkMinute = getPreferenceHelper().getOnWorkMinute();

                int offWorkHour = getPreferenceHelper().getOffWorkHour();
                int offWorkMinute = getPreferenceHelper().getOffWorkMinute();

                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR);
                int minute = calendar.get(Calendar.MINUTE);
                if ((hour == onWorkHour && minute == onWorkMinute)
                        || (hour == offWorkHour && minute == offWorkMinute)) {
                    Log.e("tag", "时间改变广播, 准备打卡");
                    prepareDaKa();
                }
            }
        }
    };

    /**
     * 当服务启动的时候调用
     */
    @Override
    protected void onServiceConnected() {
        Log.e("tag", "钉钉打卡助手服务已启动");
        Toast.makeText(this, "钉钉打卡助手服务已启动", Toast.LENGTH_SHORT).show();

        boolean autoDaka = getPreferenceHelper().getAutoDaka();
        if (autoDaka) {
            //初始化时间监听广播, 实现自动打卡
            initTimeChangeReceiver();
        }
    }

    /**
     * 注册时间改变广播
     */
    private void initTimeChangeReceiver() {
        Log.e("tag", "时间改变广播初始化成功");
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
        registerReceiver(mReceiver, intentFilter);
    }

    /**
     * 反注册时间改变广播
     */
    private void unInitTimeChangeReceiver() {
        Log.e("tag", "时间改变广播已解除");
        unregisterReceiver(mReceiver);
    }

    /**
     * 自动打卡开关事件
     *
     * @param event
     */
    public void onEventMainThread(AutoDaKaEvent event) {
        Log.e("tag", "自动打卡开关事件: " + event.toggle);
        if (event.toggle) {
            initTimeChangeReceiver();
        } else {
            unInitTimeChangeReceiver();
        }
    }

    /**
     * 监听窗口变化的回调
     *
     * @param event
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        String packageName = event.getPackageName().toString();
        Log.v("tag", "收到事件: eventType: " + eventType + ", packageName: " + packageName);
        if (MyUtils.DING_DING_PACKAGE_NAME.equals(packageName)) {
            //钉钉app事件
            handleDingDingEvent(event);
        } else if (MyUtils.TIM_PACKAGE_NAME.equals(packageName) || MyUtils.QQ_PACKAGE_NAME.equals(packageName)) {
            //TIM和QQ事件
            handleQQEvent(event);
        }
    }

    /**
     * 重置步骤标记
     */
    private void resetStepFlag() {
        mDStep = -1;
        mQStep = -1;
    }

    /**
     * 处理钉钉事件
     *
     * @param event
     */
    private void handleDingDingEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.d("tag", "handleDingDingEvent getRootInActiveWindow() 为空");
            return;
        }
        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            switch (mDStep) {
                case STEP_PREPARE_CLICK_WORK:
                    clickWork();
                    break;
                case STEP_PREPARE_GOTO_SIGN:
                    gotoSign();
                    break;
                case STEP_PREPARE_CLICK_SIGN:
                    prepareClickSign();
                    break;
                case STEP_PREPARE_CLICK_SUBMIT:
                    clickSubmit();
                    break;
            }
        }
    }

    /**
     * 打开钉钉后的第四步, 点击提交完成签到
     */
    private void clickSubmit() {
        sleep(1000);
        Log.e("tag", "寻找包含提交按钮的WebView");
        Rect bounds = new Rect();
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        AccessibilityNodeInfo signSubmitNodeInfo = null;
        ArrayList<AccessibilityNodeInfo> list = new ArrayList<>();
        findAllChildNodeInfo(rootNodeInfo, list);
        if (!list.isEmpty()) {
            for (AccessibilityNodeInfo child : list) {
                CharSequence className = child.getClassName();
                if (!TextUtils.isEmpty(className) && className.equals("android.webkit.WebView")) {
                    //找到了包含了提交按钮的WebView控件
                    signSubmitNodeInfo = child;
                    break;
                }
            }
        }
        if (signSubmitNodeInfo != null) {
            Log.e("tag", "点击提交节点");
            signSubmitNodeInfo.getBoundsInScreen(bounds);
            //left: 0, top: 216, right: 1080, bottom: 1920
            //Log.d("tag", "left: " + bounds.left + ", top: " + bounds.top + ", right: " + bounds.right + ", bottom: " + bounds.bottom);
            float x = bounds.centerX();
            float y = (bounds.bottom - bounds.top) * 0.95f + bounds.top;
            //Log.e("tag", "x: " + x + ", y: " + y);
            boolean result = MyUtils.clickScreen(x, y);
            if (result) {
                Log.e("tag", "点击提交节点成功");
                mDStep = STEP_SIGN_COMPLETED;
                //发送QQ回执消息
                openQQ();
            } else {
                Log.e("tag", "点击提交节点失败");
            }
        } else {
            Log.e("tag", "找不到包含提交按钮的WebView");
        }
    }

    /**
     * 发送QQ回执消息
     */
    private void openQQ() {
        sleep(3000);
        Log.e("tag", "打开QQ, 准备发送回执消息");
        String qqNumber = getPreferenceHelper().getQQNumber();
        if (TextUtils.isEmpty(qqNumber)) {
            Log.e("tag", "回执QQ号没有配置");
            return;
        }
        boolean result = MyUtils.openQQChat(this, qqNumber);
        if (result) {
            Log.e("tag", "打开QQ聊天界面成功");
            mQStep = STEP_PREPARE_SEND_MSG;
        } else {
            Log.e("tag", "打开QQ聊天界面失败");
        }
    }

    private long mLastChangedTime;
    private int mCheckCount = 0;
    //最多检测20次, 每次延迟500ms, 所以最多只能持续10秒, 所以超过10秒还没加载成功就会失败
    private final int mMaxCheckCount = 20;
    /**
     * 检查签到界面是否加载成功
     */
    private Runnable mCheckSignLoadFinished = new Runnable() {
        @Override
        public void run() {
            mCheckCount++;
            long crtTime = System.currentTimeMillis();
            if (crtTime - mLastChangedTime >= 2000) {
                Log.e("tag", "签到页加载完毕");
                clickSign();
            } else {
                if (mCheckCount < mMaxCheckCount) {
                    mHandler.postDelayed(this, 500);
                } else {
                    Log.e("tag", "签到页加载失败");
                }
            }
        }
    };

    /**
     * 打开钉钉后的第三步中的点击签到跳转提交界面
     */
    private void clickSign() {
        //sleep(1000);
        Log.e("tag", "寻找包含签到圆圈的WebView");
        Rect bounds = new Rect();
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        AccessibilityNodeInfo signNodeInfo = null;
        ArrayList<AccessibilityNodeInfo> list = new ArrayList<>();
        findAllChildNodeInfo(rootNodeInfo, list);
        if (!list.isEmpty()) {
            for (AccessibilityNodeInfo child : list) {
                CharSequence className = child.getClassName();
                if (!TextUtils.isEmpty(className) && className.equals("android.webkit.WebView")) {
                    //找到了签到的WebView控件
                    signNodeInfo = child;
                    break;
                }
            }
        }
        if (signNodeInfo != null) {
            Log.e("tag", "点击签到圆圈节点");
            signNodeInfo.getBoundsInScreen(bounds);
            //left: 0, top: 216, right: 1080, bottom: 1920
            //Log.d("tag", "left: " + bounds.left + ", top: " + bounds.top + ", right: " + bounds.right + ", bottom: " + bounds.bottom);
            float x = bounds.centerX();
            float y = (bounds.bottom - bounds.top) * 0.6f + bounds.top;
            //Log.e("tag", "x: " + x + ", y: " + y);
            boolean result = MyUtils.clickScreen(x, y);
            if (result) {
                Log.e("tag", "点击签到圆圈节点成功");
                mDStep = STEP_PREPARE_CLICK_SUBMIT;
            } else {
                Log.e("tag", "点击签到圆圈节点失败");
            }
        } else {
            Log.e("tag", "找不到包含签到圆圈的WebView");
        }
    }

    /**
     * 查找指定节点下的所有子节点
     *
     * @param rootNodeInfo
     * @param list
     */
    private void findAllChildNodeInfo(AccessibilityNodeInfo rootNodeInfo, ArrayList<AccessibilityNodeInfo> list) {
        if (rootNodeInfo != null && list != null) {
            int count = rootNodeInfo.getChildCount();
            for (int i = 0; i < count; i++) {
                AccessibilityNodeInfo child = rootNodeInfo.getChild(i);
                findAllChildNodeInfo(child, list);
                list.add(child);
            }
        }
    }

    /**
     * 打开钉钉后的第三步, 准备点击签到
     */
    private void prepareClickSign() {
        long crtTime = System.currentTimeMillis();
        if (mLastChangedTime == -1) {
            mLastChangedTime = crtTime;
            mHandler.removeCallbacks(mCheckSignLoadFinished);
            mHandler.postDelayed(mCheckSignLoadFinished, 1000);
            return;
        }
        mLastChangedTime = crtTime;
    }

    /**
     * 打开钉钉后的第二步, 点击跳转签到
     */
    private void gotoSign() {
        sleep(1000);
        Log.e("tag", "寻找点击跳转签到的节点");
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        AccessibilityNodeInfo gotoSignNodeInfo = null;
        List<AccessibilityNodeInfo> nodeInfos = rootNodeInfo.findAccessibilityNodeInfosByText("签到");
        if (!nodeInfos.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
                CharSequence text = nodeInfo.getText();
                CharSequence className = nodeInfo.getClassName();
                if (!TextUtils.isEmpty(text) && !TextUtils.isEmpty(className)
                        && text.equals("签到") && className.equals("android.widget.TextView")) {
                    gotoSignNodeInfo = nodeInfo;
                    break;
                }
            }
        }
        if (gotoSignNodeInfo != null) {
            Log.e("tag", "点击跳转签到的节点");
            boolean result = clickNodeInfo(gotoSignNodeInfo);
            if (result) {
                Log.e("tag", "跳转签到界面成功");
                mDStep = STEP_PREPARE_CLICK_SIGN;
            } else {
                Log.e("tag", "跳转签到界面失败");
            }
        } else {
            Log.e("tag", "找不到点击跳转签到的节点");
        }
    }

    /**
     * 打开钉钉后的第一步, 点击工作
     */
    private void clickWork() {
        sleep(1000);
        Log.e("tag", "寻找底部工作节点");
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        AccessibilityNodeInfo workNodeInfo = null;
        List<AccessibilityNodeInfo> nodeInfos = rootNodeInfo.findAccessibilityNodeInfosByText("工作");
        if (!nodeInfos.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
                CharSequence text = nodeInfo.getText();
                CharSequence className = nodeInfo.getClassName();
                if (!TextUtils.isEmpty(text) && !TextUtils.isEmpty(className)
                        && text.equals("工作") && className.equals("android.widget.TextView")) {
                    workNodeInfo = nodeInfo;
                    break;
                }
            }
        }
        if (workNodeInfo != null) {
            Log.e("tag", "点击底部工作节点");
            boolean result = clickNodeInfo(workNodeInfo);
            if (result) {
                Log.e("tag", "进入工作界面成功");
                mDStep = STEP_PREPARE_GOTO_SIGN;
            } else {
                Log.e("tag", "进入工作界面失败");
            }
        } else {
            Log.e("tag", "找不到底部工作节点");
        }
    }

    /**
     * 对节点执行点击操作
     *
     * @param nodeInfo
     * @return
     */
    private boolean clickNodeInfo(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return false;
        }
        if (nodeInfo.isClickable()) {
            Log.w("tag", "clickNodeInfo 正在点击节点");
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        } else {
            Log.w("tag", "clickNodeInfo 节点不可点击, 准备点击父节点");
            AccessibilityNodeInfo parent = nodeInfo.getParent();
            if (parent == null) {
                return false;
            }
            return clickNodeInfo(parent);
        }
    }

    /**
     * 准备打卡, 关闭钉钉, 打开钉钉
     */
    private void prepareDaKa() {
        //强制关闭钉钉app
        MyUtils.forceStopApplicationByPackageName(MyUtils.DING_DING_PACKAGE_NAME);
        //重置步骤标记
        resetStepFlag();
        //点亮屏幕并且解锁
        wakeUpAndUnlock(this);
    }

    /**
     * 点亮屏幕并且解锁
     */
    private void wakeUpAndUnlock(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean screenOn = powerManager.isScreenOn();
        if (!screenOn) {
            // 获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
            //点亮屏幕
            wakeLock.acquire(1000);
            //释放
            wakeLock.release();
        }
        //屏幕解锁
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
        boolean b = keyguardManager.inKeyguardRestrictedInputMode();
        if (b) {
            Log.e("tag", "手机锁定");
            //已锁屏
            KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("unLock");
            keyguardLock.reenableKeyguard();
            keyguardLock.disableKeyguard();
            //执行解锁屏幕命令, 需要root权限
            mHandler.obtainMessage(UNLOCK_SCREEN).sendToTarget();
        } else {
            Log.e("tag", "手机未锁定");
            //未锁屏
            mHandler.obtainMessage(OPEN_DING_DING).sendToTarget();
        }
    }

    /**
     * 处理QQ事件
     *
     * @param event
     */
    private void handleQQEvent(AccessibilityEvent event) {
        String nickName = getPreferenceHelper().getQQNickName();
        if (TextUtils.isEmpty(nickName)) {
            Log.e("tag", "QQ昵称没有配置");
            return;
        }
        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Parcelable data = event.getParcelableData();
            if (data != null && data instanceof Notification) {
                Notification notification = (Notification) data;
                if (TextUtils.isEmpty(notification.tickerText)) {
                    return;
                }
                String cmdStr = notification.tickerText.toString();
                Log.d("tag", "监听到QQ消息通知: " + cmdStr);
                String cmd = nickName + ": " + CMD_SIGN;
                if (cmd.equals(cmdStr)) {
                    Log.e("tag", "接收到QQ打卡指令, 准备打卡");
                    prepareDaKa();
                }
            }
        } else if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if (nodeInfo == null) {
                Log.d("tag", "handleQQEvent getRootInActiveWindow() 为空");
                return;
            }
            if (mQStep == STEP_PREPARE_SEND_MSG) {
                sendReceiptMsg();
            }
        }
    }

    /**
     * 发送QQ消息第一步, 找到EditText并设置值
     */
    private void sendReceiptMsg() {
        sleep(1000);
        Log.e("tag", "寻找聊天界面EditText节点");
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        AccessibilityNodeInfo editTextNodeInfo = null;
        List<AccessibilityNodeInfo> nodeInfos = rootNodeInfo.findAccessibilityNodeInfosByText("发送");
        if (!nodeInfos.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
                CharSequence text = nodeInfo.getText();
                CharSequence className = nodeInfo.getClassName();
                if (!TextUtils.isEmpty(text) && !TextUtils.isEmpty(className)
                        && text.equals("发送") && className.equals("android.widget.TextView")) {
                    AccessibilityNodeInfo parent = nodeInfo.getParent();
                    int count = parent.getChildCount();
                    if (count == 2) {
                        AccessibilityNodeInfo child = parent.getChild(0);
                        CharSequence className1 = child.getClassName();
                        if (!TextUtils.isEmpty(className1) && className1.equals("android.widget.EditText")) {
                            editTextNodeInfo = child;
                            break;
                        }
                    }
                }
            }
        }
        if (editTextNodeInfo != null) {
            Log.e("tag", "找到聊天界面EditText节点, 开始设置值");
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "打卡成功");
            editTextNodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            clickSend();
        } else {
            Log.e("tag", "没有找到聊天界面EditText节点");
        }
    }

    /**
     * 发送QQ消息的第二步, 点击发送按钮
     */
    private void clickSend() {
        sleep(500);
        Log.e("tag", "寻找聊天界面发送按钮节点");
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        AccessibilityNodeInfo sendBtnNodeInfo = null;
        List<AccessibilityNodeInfo> nodeInfos = rootNodeInfo.findAccessibilityNodeInfosByText("发送");
        if (!nodeInfos.isEmpty()) {
            for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
                CharSequence text = nodeInfo.getText();
                CharSequence className = nodeInfo.getClassName();
                if (!TextUtils.isEmpty(text) && !TextUtils.isEmpty(className)
                        && text.equals("发送") && className.equals("android.widget.TextView")) {
                    sendBtnNodeInfo = nodeInfo;
                    break;
                }
            }
        }
        if (sendBtnNodeInfo != null) {
            Log.e("tag", "找到聊天界面发送按钮节点, 点击发送");
            clickNodeInfo(sendBtnNodeInfo);
            mQStep = STEP_SEND_MSG_COMPLETED;
            sleep(1000);
            MyUtils.clickHome(this);
        } else {
            Log.e("tag", "没有找到聊天界面发送按钮节点");
        }
    }

    /**
     * 延迟指定时间
     *
     * @param time
     */
    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 当服务中断的时候调用
     */
    @Override
    public void onInterrupt() {
        Log.e("tag", "DingDingHelperService onInterrupt..");

        unInitTimeChangeReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);

        clearNotification();

        releaseWakeLock();
    }

}
