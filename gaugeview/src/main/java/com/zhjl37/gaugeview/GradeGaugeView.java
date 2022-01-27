package com.zhjl37.gaugeview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION_CODES;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import java.text.DecimalFormat;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

public class GradeGaugeView extends View {
    public static final int NO_POSITION = -1;

    private static final float ASPECT_WIDTH_HEIGHT_RATIO = 2f;

    private static final float ANGLE_START = 180f;
    private static final float ANGLE_SWEEP = 180f;

    private static final int SCALE_COUNT = 50;
    private static final int SCALE_COUNT_EACH_GROUP = 5;
    private static final float ANGLE_SCALE_STEP = ((float) ANGLE_SWEEP / SCALE_COUNT);

    @ColorInt
    private static final int DEFAULT_COLOR = Color.parseColor("#41000000");

    private static final String DEFAULT_FORMAT = "0.##";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(DEFAULT_FORMAT);

    private int gravity = Gravity.CENTER;
    // todo
    private boolean useGradient;

    private Adapter adapter;
    private float current;
    private CharSequence currentStr = DECIMAL_FORMAT.format(0f);
    private CharSequence label;

    private float innerRadius;
    private float thickness;

    private PaintFlagsDrawFilter paintFlagsDrawFilter;

    private Paint arcPaint;
    private Paint arcTrackPaint;

    private final RectF arcRect = new RectF();
    private final RectF drawRect = new RectF();
    //private final PointF centerPoint = new PointF();

    private TextPaint gradeLabelPaint;
    private float gradeLabelPadding;

    private Paint scalePaint;
    private float scaleStrokeWidth;
    private float scaleStrokeLength;
    private float scalePadding;

    private Paint cursorPaint;
    private Drawable cursorDrawable;
    private float cursorPadding;

    private TextPaint valuePaint;
    private final Rect valueRect = new Rect();

    private TextPaint labelPaint;
    private float labelPadding;

    public GradeGaugeView(Context context) {
        this(context, null);
    }

    public GradeGaugeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GradeGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    public GradeGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (isInEditMode()) {
            setAdapter(new Adapter4Test());
            setCurrent(20f);
            setLabel("BMI");
        }

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.GradeGaugeView, defStyleAttr, defStyleRes);

        int gravity = a.getInt(R.styleable.GradeGaugeView_android_gravity, Gravity.CENTER);
        setGravity(gravity);

        boolean useGradient = a.getBoolean(R.styleable.GradeGaugeView_useGradient, false);
        setUseGradient(useGradient);

        a.recycle();

        paintFlagsDrawFilter = new PaintFlagsDrawFilter(
                0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        thickness = dp2px(8);

        arcTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcTrackPaint.setStyle(Paint.Style.STROKE);
        arcTrackPaint.setColor(DEFAULT_COLOR);
        arcTrackPaint.setStrokeWidth(thickness);

        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(thickness);

        gradeLabelPadding = dp2px(8);

        gradeLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        gradeLabelPaint.setTextAlign(Align.CENTER);
        gradeLabelPaint.setColor(DEFAULT_COLOR);
        gradeLabelPaint.setTextSize(dp2px(12));

        scaleStrokeWidth = dp2px(1);
        scaleStrokeLength = dp2px(8);
        scalePadding = dp2px(4);

        scalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scalePaint.setStyle(Paint.Style.STROKE);

        cursorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_cursor);
        cursorPadding = dp2px(16);
        cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cursorPaint.setTextAlign(Align.CENTER);

        labelPadding = dp2px(16);

        valuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        valuePaint.setTextAlign(Align.CENTER);
        valuePaint.setFakeBoldText(true);
        valuePaint.setTextSize(dp2px(36));

        labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        labelPaint.setTextAlign(Align.CENTER);
        labelPaint.setColor(DEFAULT_COLOR);
        labelPaint.setTextSize(dp2px(16));
    }

    public void setGravity(int gravity) {
        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.CENTER_HORIZONTAL;
        }
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.CENTER_VERTICAL;
        }

        if (gravity != this.gravity) {
            invalidate();
        }

        this.gravity = gravity;
    }

    public void setUseGradient(boolean useGradient) {
        if (useGradient != this.useGradient) {
            invalidate();
        }

        this.useGradient = useGradient;
    }

    public void setAdapter(Adapter adapter) {
        if (this.adapter != adapter) {
            this.adapter = adapter;
            invalidate();
        }
    }

    public void setCurrent(float current) {
        if (this.current != current) {
            this.current = current;
            this.currentStr = DECIMAL_FORMAT.format(current);
            invalidate();
        }
    }

    public void setLabel(CharSequence valueLabel) {
        if (!TextUtils.equals(valueLabel, this.label)) {
            this.label = valueLabel;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int paddingHorizontal = getPaddingHorizontal();
        int widthSize = getSuggestedMinimumWidth() + getPaddingHorizontal();
        int widthSizeAndState = resolveSizeAndState(widthSize, widthMeasureSpec, 0);
        widthSize = widthSizeAndState & MEASURED_SIZE_MASK;

        int heightSizeNoPadding = (int) ((widthSize - paddingHorizontal) / ASPECT_WIDTH_HEIGHT_RATIO);
        int heightSize = heightSizeNoPadding + getPaddingVertical();
        int heightSizeAndState = resolveSizeAndState(heightSize, heightMeasureSpec, 0);
        heightSize = heightSizeAndState & MEASURED_SIZE_MASK;

        setMeasuredDimension(widthSize, heightSize);
    }

    private int getPaddingHorizontal() {
        return getPaddingLeft() + getPaddingRight();
    }

    private int getPaddingVertical() {
        return getPaddingTop() + getPaddingBottom();
    }

    @Override
    public int getPaddingBottom() {
        return super.getPaddingBottom() + (int) (scaleStrokeWidth * 2);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float gradeTextSize = getGradeLabelTextSize();
        float gradeTextUsedSize = gradeTextSize + gradeLabelPadding;

        float widthNoPadding = w - getPaddingHorizontal();
        float radiusMeasuredByWidth = (widthNoPadding - gradeTextUsedSize * 2) / ASPECT_WIDTH_HEIGHT_RATIO;

        float heightNoPadding = h - getPaddingVertical();
        float radiusMeasuredByHeight = heightNoPadding - gradeTextUsedSize;

        float radius = Math.min(radiusMeasuredByWidth, radiusMeasuredByHeight);
        innerRadius = radius - thickness;
        arcRect.set(-radius, -radius, radius, radius);

        drawRect.set(0, 0, radius * 2, radius);

        float drawRectDx = getPaddingLeft() + gradeTextUsedSize;
        switch ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK)) {
            case Gravity.LEFT:
                drawRectDx += 0;
                break;

            case Gravity.RIGHT:
                drawRectDx += radiusMeasuredByWidth * 2 - drawRect.width();
                break;

            default:
                drawRectDx += (radiusMeasuredByWidth * 2 - drawRect.width()) / 2;
                break;
        }

        float drawRectDy = getPaddingTop() + gradeTextUsedSize;
        switch ((gravity & Gravity.VERTICAL_GRAVITY_MASK)) {
            case Gravity.TOP:
                drawRectDy += 0;
                break;

            case Gravity.BOTTOM:
                drawRectDy += radiusMeasuredByHeight - drawRect.height();
                break;

            default:
                drawRectDy += (radiusMeasuredByHeight - drawRect.height()) / 2;
                break;
        }

        drawRect.offset(drawRectDx, drawRectDy);
    }

    private float getGradeLabelTextSize() {
        FontMetrics fontMetrics = gradeLabelPaint.getFontMetrics();
        return fontMetrics.bottom - fontMetrics.top + fontMetrics.leading;
    }

    private float getCenterPointXForDraw() {
        return drawRect.centerX();
    }

    private float getCenterPointYForDraw() {
        return drawRect.bottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.setDrawFilter(paintFlagsDrawFilter);

        canvas.save();
        canvas.translate(getCenterPointXForDraw(), getCenterPointYForDraw());
        canvas.rotate(ANGLE_START);

        drawArc(canvas);
        drawScales(canvas);
        drawCursor(canvas);
        drawLabelAndValue(canvas);

        canvas.restore();
    }

    private void drawArc(Canvas canvas) {
        canvas.drawArc(arcRect, 0, ANGLE_SWEEP, false, arcTrackPaint);

        if (adapter != null && adapter.getCount() > 0) {
            float minValue = adapter.getMinValue();
            float maxValue = adapter.getMaxValue();
            float values = maxValue - minValue;
            if (values != 0) {
                float ratio = ANGLE_SWEEP / values;
                for (int i = 0; i < adapter.getCount(); i++) {
                    float gradeMinScale = adapter.getMinValue(i);
                    float gradeMaxScale = adapter.getMaxValue(i);

                    float startAngle = (gradeMinScale - minValue) * ratio;
                    float sweepAngle = (gradeMaxScale - gradeMinScale) * ratio;

                    arcPaint.setColor(adapter.getColor(i));
                    canvas.drawArc(arcRect, startAngle, sweepAngle, false, arcPaint);

                    CharSequence gradeTitle = adapter.getTitle(i);
                    if (!TextUtils.isEmpty(gradeTitle)) {
                        float centerAngle = startAngle + sweepAngle / 2f;
                        drawArcText(canvas, centerAngle, gradeTitle);
                    }
                }
            }
        }
    }

    private void drawArcText(Canvas canvas, float angle, CharSequence text) {
        canvas.save();

        final float angleOffset = 90;
        canvas.rotate(angle + angleOffset);

        int textLength = text.length();
        float y = -(innerRadius + thickness + gradeLabelPadding);
        canvas.drawText(text, 0, textLength, 0, y, gradeLabelPaint);

        canvas.restore();
    }

    private void drawScales(Canvas canvas) {
        final float angleOffset = 90;
        for (int i = 0; i <= SCALE_COUNT; i++) {
            canvas.save();

            float current = ANGLE_SCALE_STEP * i;
            canvas.rotate(current + angleOffset);
            canvas.translate(0, -innerRadius);

            if (adapter != null && adapter.getCount() > 0) {
                float minValue = adapter.getMinValue();
                float maxValue = adapter.getMaxValue();
                float values = maxValue - minValue;

                float currentValue = current * values / ANGLE_SWEEP;
                int gradeColor = getColorByValue(currentValue);
                scalePaint.setColor(gradeColor);
            } else {
                scalePaint.setColor(DEFAULT_COLOR);
            }

            float scaleStrokeWidth = this.scaleStrokeWidth;
            float scaleStrokeLength = this.scaleStrokeLength;
            if (i % SCALE_COUNT_EACH_GROUP == 0) {
                scaleStrokeWidth *= 2;
                scaleStrokeLength *= 1.5;
            }
            scalePaint.setStrokeWidth(scaleStrokeWidth);

            canvas.drawLine(0, scalePadding, 0, scaleStrokeLength, scalePaint);

            canvas.restore();
        }
    }

    private int getColorByValue(float value) {
        if (adapter == null || adapter.getCount() == 0) {
            return DEFAULT_COLOR;
        }

        if (value >= adapter.getMaxValue()) {
            return adapter.getColor(adapter.getCount() - 1);
        }

        int position = adapter.getPositionByValue(value);
        if (position == NO_POSITION) {
            return DEFAULT_COLOR;
        }

        return adapter.getColor(position);
    }

    private void drawCursor(Canvas canvas) {
        if (adapter == null || adapter.getCount() == 0) {
            return;
        }

        canvas.save();

        final float angleOffset = 90;
        float minValue = adapter.getMinValue();
        float maxValue = adapter.getMaxValue();
        float values = maxValue - minValue;
        if (values != 0) {
            float ratio = ANGLE_SWEEP / values;
            float angle = (current - minValue) * ratio;
            canvas.rotate(angle + angleOffset);

            float dy = -innerRadius + scaleStrokeLength * 1.5f + cursorPadding;
            canvas.translate(0, dy);

            int gradeColor = getColorByValue(current);
            cursorPaint.setColor(gradeColor);

            int width = cursorDrawable.getIntrinsicWidth();
            int height = cursorDrawable.getIntrinsicHeight();
            cursorDrawable.setBounds(0, 0, width, height);
            cursorDrawable.setColorFilter(gradeColor, PorterDuff.Mode.SRC_ATOP);

            cursorDrawable.draw(canvas);
        }

        canvas.restore();
    }

    private void drawLabelAndValue(Canvas canvas) {
        canvas.save();
        canvas.rotate(-ANGLE_START);

        int textColor = getColorByValue(current);
        valuePaint.setColor(textColor);

        if (!TextUtils.isEmpty(label)) {
            int textLength = currentStr.length();
            canvas.drawText(currentStr, 0, textLength, 0, 0, valuePaint);

            valuePaint.getTextBounds(currentStr.toString(), 0, textLength, valueRect);

            int labelLength = label.length();
            float labelY = -(valueRect.height() + labelPadding);
            canvas.drawText(label, 0, labelLength, 0, labelY, labelPaint);
        }

        canvas.restore();

    }

    public int dp2px(int values) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (values * density + 0.5f);
    }

    public static abstract class Adapter {
        public abstract int getCount();

        public abstract CharSequence getTitle(int position);

        public abstract float getMinValue(int position);

        public abstract float getMaxValue(int position);

        @ColorInt
        public abstract int getColor(int position);

        public float getMinValue() {
            return getMinValue(0);
        }

        public float getMaxValue() {
            return getMaxValue(getCount() - 1);
        }

        public int getPositionByValue(float value) {
            for (int i = 0; i < getCount(); i++) {
                float min = getMinValue(i);
                float max = getMaxValue(i);
                if (value >= min && value < max) {
                    return i;
                }
            }
            return GradeGaugeView.NO_POSITION;
        }
    }

    public static class Adapter4Test extends Adapter {
        private static final String[] TITLES = {"偏瘦", "正常", "过重", "肥胖"};
        private static final float[][] SCALES = {
                {0, 18.5f}, {18.5f, 24f}, {24f, 28f}, {28f, 40f}
        };
        private static final int[] COLORS = {
                Color.parseColor("#7cffb2"),
                Color.parseColor("#58d9f9"),
                Color.parseColor("#fddd60"),
                Color.parseColor("#ff6e76")
        };

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public CharSequence getTitle(int position) {
            return TITLES[position];
        }

        @Override
        public float getMinValue(int position) {
            return SCALES[position][0];
        }

        @Override
        public float getMaxValue(int position) {
            return SCALES[position][1];
        }

        @Override
        public int getColor(int position) {
            return COLORS[position];
        }
    }
}
