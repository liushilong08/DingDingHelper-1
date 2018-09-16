package app.bmkp.cn.dingdinghelper.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TimePicker;

import app.bmkp.cn.dingdinghelper.R;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by wangpan on 2018/9/14.
 */

public class SelectTimeDialog extends DialogFragment implements View.OnClickListener {

    public static final String TAG = SelectTimeDialog.class.getSimpleName();

    @BindView(R.id.time_picker)
    TimePicker timePicker;
    @BindView(R.id.btn_ok)
    Button btnOk;

    private int mHour;
    private int mMinute;

    private OnTimeSelectedListener mOnTimeSelectedListener;

    public void setOnTimeSelectedListener(OnTimeSelectedListener listener) {
        this.mOnTimeSelectedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_time_dialog, container, false);
        ButterKnife.bind(this, view);
        initView();
        return view;
    }

    public void setHour(int hour) {
        this.mHour = hour;
    }

    public void setMinute(int minute) {
        this.mMinute = minute;
    }

    private void initView() {
        //设置24小时格式
        timePicker.setIs24HourView(true);
        timePicker.setCurrentHour(mHour);
        timePicker.setCurrentMinute(mMinute);
        btnOk.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int hour = timePicker.getCurrentHour();
        int minute = timePicker.getCurrentMinute();
        dismiss();
        if (mOnTimeSelectedListener != null) {
            mOnTimeSelectedListener.onTimeSelected(hour, minute);
        }
    }

    public interface OnTimeSelectedListener {

        void onTimeSelected(int hour, int minute);
    }
}
