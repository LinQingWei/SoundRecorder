package cn.way.soundrecorder.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import cn.way.soundrecorder.R;
import cn.way.soundrecorder.Recorder;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.14
 *     desc  :
 * </pre>
 */

public class VUMeter extends View {
    private static final float PIVOT_RADIUS = 3.5f;
    private static final float PIVOT_Y_OFFSET = 10f;
    private static final float SHADOW_OFFSET = 2.0f;
    private static final float DROPOFF_STEP = 0.18f;
    private static final float SURGE_STEP = 0.35f;
    private static final long ANIMATION_INTERVAL = 70;
    private static final float ANGLE_MIN = (float) Math.PI / 8;
    private static final float ANGLE_MAX = (float) Math.PI * 7 / 8;

    private Paint mPaint, mShadow;
    private float mCurrentAngle;
    private Recorder mRecorder;

    public VUMeter(Context context) {
        this(context, null);
    }

    public VUMeter(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VUMeter(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        Drawable background = context.getResources().getDrawable(R.drawable.bg_gradient);
        setBackground(background);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.WHITE);

        mShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setARGB(60, 0, 0, 0);
    }

    public void setRecorder(Recorder recorder) {
        mRecorder = recorder;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float angle = ANGLE_MIN;
        if (mRecorder != null) {
            angle += (float) (ANGLE_MAX - ANGLE_MIN) * mRecorder.getMaxAmplitude() / 32768;
        }

        if (angle > mCurrentAngle) {
            mCurrentAngle = angle;
        } else {
            mCurrentAngle = Math.max(angle, mCurrentAngle - DROPOFF_STEP);
        }

        mCurrentAngle = Math.min(ANGLE_MAX, mCurrentAngle);

        float w = getWidth();
        float h = getHeight();
        float pivotX = w / 2;
        float pivotY = h - PIVOT_RADIUS - PIVOT_Y_OFFSET;
        float l = h * 4 / 5;
        float sin = (float) Math.sin(mCurrentAngle);
        float cos = (float) Math.cos(mCurrentAngle);
        float x0 = pivotX - l * cos;
        float y0 = pivotY - l * sin;
        canvas.drawLine(x0 + SHADOW_OFFSET, y0 + SHADOW_OFFSET, pivotX + SHADOW_OFFSET, pivotY + SHADOW_OFFSET, mShadow);
        canvas.drawCircle(pivotX + SHADOW_OFFSET, pivotY + SHADOW_OFFSET, PIVOT_RADIUS, mShadow);
        canvas.drawLine(x0, y0, pivotX, pivotY, mPaint);
        canvas.drawCircle(pivotX, pivotY, PIVOT_RADIUS, mPaint);

        if (mRecorder != null && mRecorder.getState() == Recorder.RECORDING_STATE) {
            postInvalidateDelayed(ANIMATION_INTERVAL);
        }
    }
}
