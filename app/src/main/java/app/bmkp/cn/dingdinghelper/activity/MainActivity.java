package app.bmkp.cn.dingdinghelper.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import app.bmkp.cn.dingdinghelper.R;
import app.bmkp.cn.dingdinghelper.dialog.SelectTimeDialog;
import app.bmkp.cn.dingdinghelper.event.AutoDaKaEvent;
import app.bmkp.cn.dingdinghelper.service.DingDingHelperService;
import app.bmkp.cn.dingdinghelper.utils.MyUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

public class MainActivity extends BaseActivity {

    @BindView(R.id.tv_root)
    TextView tvRoot;
    @BindView(R.id.tv_status)
    TextView tvStatus;
    @BindView(R.id.btn_to_setting)
    Button btnToSetting;
    @BindView(R.id.et_time)
    EditText etTime;
    @BindView(R.id.et_time2)
    EditText etTime2;
    @BindView(R.id.et_nick_name)
    EditText etNickName;
    @BindView(R.id.et_number)
    EditText etNumber;
    @BindView(R.id.btn_nick_name)
    Button btnNickName;
    @BindView(R.id.btn_number)
    Button btnNumber;
    @BindView(R.id.tv_auto_daka)
    TextView tvAutoDaka;
    @BindView(R.id.btn_aotu_daka)
    Button btnAotuDaka;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initView();
    }

    private void initView() {
        String nickName = getPreferenceHelper().getQQNickName();
        etNickName.setText(nickName);
        String number = getPreferenceHelper().getQQNumber();
        etNumber.setText(number);
        int hour1 = getPreferenceHelper().getOnWorkHour();
        int minute1 = getPreferenceHelper().getOnWorkMinute();
        etTime.setText(hour1 + ":" + minute1);
        int hour2 = getPreferenceHelper().getOffWorkHour();
        int minute2 = getPreferenceHelper().getOffWorkMinute();
        etTime2.setText(hour2 + ":" + minute2);
        //设置自动打卡
        boolean autoDaka = getPreferenceHelper().getAutoDaka();
        boolean started = MyUtils.isAccessibilityServiceStarted(this, DingDingHelperService.class);
        if (started && autoDaka) {
            tvAutoDaka.setText("开");
            btnAotuDaka.setText("关");
        } else {
            getPreferenceHelper().setAutoDaka(false);
            tvAutoDaka.setText("关");
            btnAotuDaka.setText("开");
        }
        boolean b = MyUtils.checkRootPermission();
        if (b) {
            tvRoot.setText("已获取");
        } else {
            tvRoot.setText("未获取");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkService();
    }

    /**
     * 检测自动打卡服务是否已开启
     */
    private void checkService() {
        boolean b = MyUtils.isAccessibilityServiceStarted(this, DingDingHelperService.class);
        if (b) {
            tvStatus.setText("已启动");
            btnToSetting.setVisibility(View.GONE);
        } else {
            tvStatus.setText("未启动");
            btnToSetting.setVisibility(View.VISIBLE);
        }
    }

    @OnClick({R.id.btn_to_setting, R.id.et_time, R.id.et_time2, R.id.btn_nick_name,
            R.id.btn_number, R.id.btn_aotu_daka, R.id.btn_dd, R.id.btn_qq})
    public void viewClick(View view) {
        switch (view.getId()) {
            case R.id.btn_to_setting:
                startAccessibilitySettings();
                break;
            case R.id.et_time:
                setTime1();
                break;
            case R.id.et_time2:
                setTime2();
                break;
            case R.id.btn_nick_name:
                setQQNickName();
                break;
            case R.id.btn_number:
                setQQNumber();
                break;
            case R.id.btn_aotu_daka:
                setAutoDaka();
                break;
            case R.id.btn_dd:
                testDD();
                break;
            case R.id.btn_qq:
                testQQ();
                break;
        }
    }

    /**
     * 测试打开钉钉
     */
    private void testDD() {
        boolean result = MyUtils.startApplicationByPackageName(this, MyUtils.DING_DING_PACKAGE_NAME);
        if (result) {
            Toast.makeText(this, "钉钉打开成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "钉钉打开失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 测试打开QQ
     */
    private void testQQ() {
        String number = getPreferenceHelper().getQQNumber();
        if (!TextUtils.isEmpty(number)) {
            boolean result = MyUtils.openQQChat(this, number);
            if (result) {
                Toast.makeText(this, "QQ打开成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "QQ打开失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "你似乎还没有设置发送指令的QQ号码", Toast.LENGTH_SHORT).show();
        }
    }

    private void setAutoDaka() {
        boolean autoDaka = getPreferenceHelper().getAutoDaka();
        getPreferenceHelper().setAutoDaka(!autoDaka);
        if (autoDaka) {
            EventBus.getDefault().post(new AutoDaKaEvent(false));
            tvAutoDaka.setText("关");
            btnAotuDaka.setText("开");
        } else {
            //检查钉钉助手服务是否已开启
            boolean result = MyUtils.isAccessibilityServiceStarted(this, DingDingHelperService.class);
            if (result) {
                EventBus.getDefault().post(new AutoDaKaEvent(true));
                tvAutoDaka.setText("开");
                btnAotuDaka.setText("关");
            } else {
                Toast.makeText(this, "请先开启[钉钉打卡助手服务]", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setQQNickName() {
        String s = etNickName.getText().toString();
        if (TextUtils.isEmpty(s)) {
            Toast.makeText(this, "QQ昵称不为空", Toast.LENGTH_SHORT).show();
            return;
        }
        getPreferenceHelper().setQQNickName(s);
    }

    private void setQQNumber() {
        String s = etNumber.getText().toString();
        if (TextUtils.isEmpty(s)) {
            Toast.makeText(this, "QQ号码不为空", Toast.LENGTH_SHORT).show();
            return;
        }
        getPreferenceHelper().setQQNumber(s);
    }

    private void setTime1() {
        SelectTimeDialog dialog = new SelectTimeDialog();
        dialog.setOnTimeSelectedListener(new SelectTimeDialog.OnTimeSelectedListener() {
            @Override
            public void onTimeSelected(int hour, int minute) {
                String text = hour + ":" + minute;
                etTime.setText(text);
                getPreferenceHelper().setOnWorkHour(hour);
                getPreferenceHelper().setOnWorkMinute(minute);
            }
        });
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialog, SelectTimeDialog.TAG);
        ft.commitAllowingStateLoss();
    }

    private void setTime2() {
        SelectTimeDialog dialog = new SelectTimeDialog();
        dialog.setOnTimeSelectedListener(new SelectTimeDialog.OnTimeSelectedListener() {
            @Override
            public void onTimeSelected(int hour, int minute) {
                String text = hour + ":" + minute;
                etTime2.setText(text);
                getPreferenceHelper().setOffWorkHour(hour);
                getPreferenceHelper().setOffWorkMinute(minute);
            }
        });
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialog, SelectTimeDialog.TAG);
        ft.commitAllowingStateLoss();
    }

    /**
     * 打开无障碍功能列表
     */
    private void startAccessibilitySettings() {
        Toast.makeText(this, "找到[钉钉打卡助手]并开启", Toast.LENGTH_LONG).show();
        MyUtils.startAccessibilitySettings(this);
    }
}
