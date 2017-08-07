package me.keeganlee.kandroid.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.widget.TextView;
import me.keeganlee.kandroid.R;
/**
 * Created by lwp on 2016/11/21 0021.
 */

public class RechargeButton extends TextView {
    int titleTextSize = 0;
    int contentTextSize = 0;
    private boolean isNewLine = true;
    private SpannableString span;

    public RechargeButton(Context context) {
        super(context);
        init(context, null);
    }

    public RechargeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RechargeButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RechargeButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.rechargeButton);// TypedArray是一个数组容器
            isNewLine = a.getBoolean(R.styleable.rechargeButton_isNewLine, true);
            titleTextSize = a.getInteger(R.styleable.rechargeButton_titleTextSize, 0);// 平时
            contentTextSize = a.getInteger(R.styleable.rechargeButton_contentTextSize, 0);// 点击
            titleTextSize = (int) (titleTextSize * context.getResources().getDisplayMetrics().scaledDensity);
            contentTextSize = (int) (contentTextSize
                    * context.getResources().getDisplayMetrics().scaledDensity);
            a.recycle();// 释放
        }
    }

    public void setText(String value, String desc) {
        try {
            if (value == null) {
                value = "";
            }
            if (desc == null) {
                desc = "";
            }
            String str = value;
            if (!"".equals(desc)) {
                if (isNewLine) {
                    str = str + "\n" + desc;
                } else {
                    str = str + " " + desc;
                }
            }

            // 根据要显示的String得到SpannableString
            span = new SpannableString(str);

            // 设置头部字体大小
            if (titleTextSize != 0) {
                span.setSpan(new AbsoluteSizeSpan(titleTextSize), 0, value.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            // 设置内容部字体大小
            if (desc != null) {
                // 显示优惠信息
                if (contentTextSize != 0) {
                    span.setSpan(new AbsoluteSizeSpan(contentTextSize), value.length(), str.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            setText(span);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
