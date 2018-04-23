package com.xhcy.library;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xhcy on 2018/4/23.
 */

public class PointsView extends View {

    private final String TAG = "PointsView";
    private final int SWITCH_POINT = 100;

    private Context mContext;
    private Paint mPaint;

    @ColorInt
    private int mDefaultPointColor;
    private int mDefaultPointSpace;
    private int mDefaultPointRadius;
    private int mDefaultPointCount;
    private int mDefaultSwitchTime;
    private float mDefaultPointMinAlpha;
    private float mDefaultPointMaxAlpha;

    @ColorInt
    private int mPointColor;
    private int mPointSpace;        // 间距
    private int mPointRadius;       // 半径
    private int mPointCount;        // 个数
    private long mSwitchTime;
    private float mPointMinAlpha;
    private float mPointMaxAlpha;
    private float mPaintAlpha;

    private List<Point> mPointList;
    private int mPointDiameter;         // 直径
    private int mPointsViewWidth;       // View内容的宽度
    private int mSelectedPointIndex;
    private boolean mIsFirst = true;
    private MyHandler mHandler;

    public PointsView(Context context) {
        this(context, null);
    }

    public PointsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PointsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mPointList = new ArrayList<>();
        mHandler = new MyHandler(this);

        mDefaultPointColor = Color.argb(255, 0, 0, 0);
        mDefaultPointSpace = dp2px(12.5f);
        mDefaultPointRadius = dp2px(3.75f);
        mDefaultPointCount = 3;
        mDefaultSwitchTime = 2200;
        mDefaultPointMinAlpha = 0.3f;
        mDefaultPointMaxAlpha = 1.0f;
        mPaintAlpha = 1.0f;

        TypedArray ta = mContext.obtainStyledAttributes(attrs, R.styleable.PointsView);
        mPointColor = ta.getColor(R.styleable.PointsView_pv_point_color, mDefaultPointColor);
        mPointSpace = ta.getDimensionPixelOffset(R.styleable.PointsView_pv_point_space, mDefaultPointSpace);
        mPointRadius = ta.getDimensionPixelOffset(R.styleable.PointsView_pv_point_radius, mDefaultPointRadius);
        mPointCount = ta.getInt(R.styleable.PointsView_pv_point_count, mDefaultPointCount);
        mSwitchTime = ta.getInt(R.styleable.PointsView_pv_point_switch_time, mDefaultSwitchTime);
        mPointMinAlpha = ta.getFloat(R.styleable.PointsView_pv_point_min_alpha, mDefaultPointMinAlpha);
        mPointMaxAlpha = mDefaultPointMaxAlpha;
        ta.recycle();

        mPointDiameter = mPointRadius * 2;
        mPointsViewWidth = mPointDiameter * mPointCount + mPointSpace * (mPointCount - 1);

        for (int i = 0; i < mPointCount; i++) {
            int x = mPointDiameter * i + mPointSpace * i + mPointRadius;
            Point point = new Point(x, mPointRadius);
            mPointList.add(point);
        }

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mPointColor);

        mHandler.sendEmptyMessageDelayed(SWITCH_POINT, 600);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < mPointList.size(); i++) {
            Point point = mPointList.get(i);
            if (i == mSelectedPointIndex)
                mPaint.setAlpha((int) (255 * mPaintAlpha));
            else
                mPaint.setAlpha((int) (255 * mPointMinAlpha));
            canvas.drawCircle(point.x, point.y, mPointRadius, mPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureWidth = measure(widthMeasureSpec, true);
        int measureHeight = measure(heightMeasureSpec, false);
        setMeasuredDimension(measureWidth, measureHeight);
        for (int i = 0; i < mPointList.size(); i++) {
            Point point = mPointList.get(i);
            int x = (measureWidth / 2 - mPointsViewWidth / 2 + mPointRadius) + mPointDiameter * i + mPointSpace * i;
            point.x = x;
            point.y = measureHeight / 2;
        }
        invalidate();
    }

    private int measure(int measureSpec, boolean isWidth) {
        int result;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        int padding = isWidth ? getPaddingLeft() + getPaddingRight() : getPaddingTop() + getPaddingBottom();
        if (mode == MeasureSpec.EXACTLY) {
            result = size;
        } else {
            if (isWidth) {
                result = mPointsViewWidth + padding;
            } else {
                result = mPointDiameter + padding;
            }
        }
        return result;
    }

    private void switchPoint() {
        ValueAnimator anim;
        if (mIsFirst)
            anim = ValueAnimator.ofFloat(mPointMaxAlpha, mPointMinAlpha);
        else
            anim = ValueAnimator.ofFloat(mPointMinAlpha, mPointMaxAlpha, mPointMinAlpha);
        anim.setInterpolator(new LinearInterpolator());
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIsFirst = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mSelectedPointIndex += 1;
                if (mSelectedPointIndex > mPointList.size() - 1) mSelectedPointIndex = 0;
                mHandler.sendEmptyMessage(SWITCH_POINT);
            }
        });
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPaintAlpha = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        long duration;
        if (mIsFirst)
            duration = mSwitchTime / 2;
        else
            duration = mSwitchTime;
        anim.setDuration(duration);
        anim.start();
    }

    private class MyHandler extends Handler {

        WeakReference<PointsView> mPointsViewWeakReference;

        public MyHandler(PointsView pointsView) {
            mPointsViewWeakReference = new WeakReference<>(pointsView);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SWITCH_POINT:
                    switchPoint();
                    break;
                default:
                    break;
            }
        }

    }

    private int dp2px(float dipValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

}
