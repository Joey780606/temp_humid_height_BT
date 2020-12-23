package pcp.com.bttemperature.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;


import org.joda.time.DateTimeConstants;

import java.util.ArrayList;

import pcp.com.bttemperature.R;

public class LightGraph extends View {
    private static final String TAG = LightGraph.class.getSimpleName();
    private ArrayList<Number> arrayLight;
    private ArrayList<Number> arrayTimeline;
    private int barHeight;
    private int marginBottom;
    private int marginTop;
    private long timestamp1;
    private long timestamp2;

    public LightGraph(Context context) {
        super(context);
        this.timestamp1 = 0;
        this.timestamp2 = 0;
        this.arrayLight = new ArrayList<>();
        this.arrayTimeline = new ArrayList<>();
    }

    public LightGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.timestamp1 = 0;
        this.timestamp2 = 0;
        //context.obtainStyledAttributes(attrs, R.styleable.ColorBar).recycle();
        this.arrayLight = new ArrayList<>();
        this.arrayTimeline = new ArrayList<>();
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == 1073741824) {
            width = widthSize;
        } else if (widthMode == Integer.MIN_VALUE) {
            width = Math.min((int) DateTimeConstants.MILLIS_PER_SECOND, widthSize);
        } else {
            width = 1000;
        }
        if (heightMode == 1073741824) {
            height = heightSize;
        } else if (heightMode == Integer.MIN_VALUE) {
            height = Math.min(500, heightSize);
        } else {
            height = 500;
        }
        this.marginTop = height / 5;
        this.marginBottom = this.marginTop;
        this.barHeight = (height - this.marginTop) - this.marginBottom;
        setMeasuredDimension(width, height);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long timestampLength = this.timestamp2 - this.timestamp1;
        long previousTimestamp = 0;
        for (int i = 0; i < this.arrayLight.size(); i++) {
            if (previousTimestamp == 0) {
                previousTimestamp = this.arrayTimeline.get(i).longValue();
            } else {
                long timestamp = this.arrayTimeline.get(i).longValue();
                double boxLength = (((double) (timestamp - previousTimestamp)) / ((double) timestampLength)) * ((double) getWidth());
                double startX = (((double) (previousTimestamp - this.timestamp1)) / ((double) timestampLength)) * ((double) getWidth());
                previousTimestamp = timestamp;
                canvas.drawRect((float) startX, (float) this.marginTop, (float) (boxLength + startX), (float) (this.marginTop + this.barHeight), paintForInt(Math.log((double) this.arrayLight.get(i).longValue()) / Math.log(65535.0d)));
            }
        }
    }

    private Paint paintForInt(double logNumDecimal) {
        int decNum = (int) Math.round(255.0d * logNumDecimal);
        Paint p = new Paint(1);
        p.setColor(Color.parseColor(String.format("#FF%02X%02X%02X", Integer.valueOf(decNum), Integer.valueOf(decNum), Integer.valueOf(decNum))));
        p.setStyle(Paint.Style.FILL);
        return p;
    }

    public void drawLightBar(ArrayList<Number> lightValues, ArrayList<Number> timelineValues, long lowerTimestamp, long upperTimestamp) {
        this.arrayLight = lightValues;
        this.arrayTimeline = timelineValues;
        this.timestamp1 = lowerTimestamp;
        this.timestamp2 = upperTimestamp;
        invalidate();
        requestLayout();
    }

    public void resetLightBar() {
        this.arrayLight.clear();
        this.arrayTimeline.clear();
        invalidate();
        requestLayout();
    }
}
