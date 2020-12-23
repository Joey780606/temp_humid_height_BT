package pcp.com.bttemperature.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import org.joda.time.DateTimeConstants;

import java.util.ArrayList;

import pcp.com.bttemperature.R;
import pcp.com.bttemperature.ble.AmbientDeviceService;
import pcp.com.bttemperature.parse.ParseException;

public class ColorBar extends View {
    private static final String TAG = ColorBar.class.getSimpleName();
    private int barBottom;
    private int barComponentWidth;
    private int barHeight;
    private int barTop;
    private int barWidth;
    private int internalMarginLeft;
    private int internalMarginRight;
    private ArrayList<Paint> mColorBarPaints;
    private GestureDetector mDetector;
    private Paint mFillPaint;
    private int[] mSectorX;
    private int mSelectedIndex;
    private Paint mStrokePaint;
    private OnColorBarChangedListener onColorBarChangedListener;
    private int selectedColorBlue;
    private int selectedColorGreen;
    private int selectedColorRed;

    public interface OnColorBarChangedListener {
        void onColorBarChangeFinished(int i, int i2, int i3);

        void onColorBarChanged(int i, int i2, int i3);
    }

    public ColorBar(Context context) {
        super(context);
        initPaintArray();
        this.mStrokePaint = new Paint(1);
        this.mStrokePaint.setColor(Color.parseColor("#FF333333"));
        this.mStrokePaint.setStyle(Paint.Style.STROKE);
        this.mStrokePaint.setStrokeWidth(5.0f);
        this.mFillPaint = new Paint(1);
        this.mFillPaint.setColor(Color.parseColor("#FF333333"));
        this.mFillPaint.setStyle(Paint.Style.FILL);
        this.mDetector = new GestureDetector(getContext(), new mListener());
        this.selectedColorRed = redValueForSector(this.mSelectedIndex) & 255;
        this.selectedColorGreen = greenValueForSector(this.mSelectedIndex) & 255;
        this.selectedColorBlue = blueValueForSector(this.mSelectedIndex) & 255;
    }

    /* JADX INFO: finally extract failed */
    public ColorBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        //TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ColorBar);
        try {
            //this.mSelectedIndex = a.getInt(0, 0);
            //a.recycle();
            initPaintArray();
            this.mStrokePaint = new Paint(1);
            this.mStrokePaint.setColor(Color.parseColor("#FF333333"));
            this.mStrokePaint.setStyle(Paint.Style.STROKE);
            this.mStrokePaint.setStrokeWidth(5.0f);
            this.mFillPaint = new Paint(1);
            this.mFillPaint.setColor(Color.parseColor("#FF333333"));
            this.mFillPaint.setStyle(Paint.Style.FILL);
            this.mDetector = new GestureDetector(getContext(), new mListener());
            this.selectedColorRed = redValueForSector(this.mSelectedIndex) & 255;
            this.selectedColorGreen = greenValueForSector(this.mSelectedIndex) & 255;
            this.selectedColorBlue = blueValueForSector(this.mSelectedIndex) & 255;
        } catch (Throwable th) {
            //a.recycle();
            throw th;
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        this.barHeight = ParseException.CACHE_MISS;
        this.barTop = 90;
        this.barBottom = this.barTop + this.barHeight;
        int desiredHeight = this.barBottom + 50;
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
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }
        this.internalMarginLeft = (int) (((double) width) * 0.1d);
        this.internalMarginRight = (int) (((double) width) * 0.1d);
        this.barComponentWidth = ((width - this.internalMarginLeft) - this.internalMarginRight) / 36;
        this.barWidth = this.barComponentWidth * 36;
        this.internalMarginLeft = (width - this.barWidth) / 2;
        this.internalMarginRight = (width - this.barWidth) / 2;
        setMeasuredDimension(width, height);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < 36; i++) {
            int leftMargin = this.internalMarginLeft + (this.barComponentWidth * i);
            canvas.drawRect((float) leftMargin, (float) this.barTop, (float) (this.barComponentWidth + leftMargin), (float) this.barBottom, this.mColorBarPaints.get(i));
        }
        int leftMargin2 = this.internalMarginLeft + (this.mSelectedIndex * this.barComponentWidth);
        canvas.drawRect((float) leftMargin2, (float) this.barTop, (float) (this.barComponentWidth + leftMargin2), (float) this.barBottom, this.mStrokePaint);
        Path path = new Path();
        path.moveTo((float) leftMargin2, (float) (this.barTop - this.barComponentWidth));
        path.lineTo((float) (this.barComponentWidth + leftMargin2), (float) (this.barTop - this.barComponentWidth));
        path.lineTo((float) (((double) leftMargin2) + (((double) this.barComponentWidth) / 2.0d)), (float) this.barTop);
        path.lineTo((float) leftMargin2, (float) (this.barTop - this.barComponentWidth));
        path.close();
        canvas.drawPath(path, this.mFillPaint);
        float previewBoxLeft = (float) (((((double) this.internalMarginLeft) + (((double) this.barComponentWidth) / 2.0d)) - (((double) ParseException.USERNAME_MISSING) / 2.0d)) + ((double) (this.mSelectedIndex * this.barComponentWidth)));
        float previewBoxBottom = (float) (this.barTop - this.barComponentWidth);
        float previewBoxTop = previewBoxBottom - ((float) 60);
        Paint colorPaint = new Paint(1);
        colorPaint.setStyle(Paint.Style.FILL);
        colorPaint.setColor(Color.parseColor(colorStringForSector(this.mSelectedIndex)));
        canvas.drawRect(previewBoxLeft, previewBoxTop, previewBoxLeft + ((float) ParseException.USERNAME_MISSING), previewBoxBottom, colorPaint);
        canvas.drawRect(previewBoxLeft, previewBoxTop, previewBoxLeft + ((float) ParseException.USERNAME_MISSING), previewBoxBottom, this.mStrokePaint);
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean result = this.mDetector.onTouchEvent(event);
        if (result) {
            return result;
        }
        if (event.getAction() == 1) {
            try {
                this.onColorBarChangedListener.onColorBarChangeFinished(this.selectedColorRed, this.selectedColorGreen, this.selectedColorBlue);
            } catch (NullPointerException e) {
            }
            return true;
        } else if (event.getAction() != 2) {
            return result;
        } else {
            updateSelectionForTouch(event.getX());
            return true;
        }
    }

    class mListener extends GestureDetector.SimpleOnGestureListener {
        mListener() {
        }

        public boolean onDown(MotionEvent e) {
            ColorBar.this.updateSelectionForTouch(e.getX());
            return true;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateSelectionForTouch(float x) {
        int newSelectionIndex;
        if (x < ((float) (this.internalMarginLeft + this.barComponentWidth))) {
            newSelectionIndex = 0;
        } else if (x > ((float) ((getWidth() - this.internalMarginRight) - this.barComponentWidth))) {
            newSelectionIndex = 35;
        } else {
            newSelectionIndex = (int) ((x - ((float) this.internalMarginLeft)) / ((float) this.barComponentWidth));
        }
        if (newSelectionIndex != this.mSelectedIndex) {
            this.mSelectedIndex = newSelectionIndex;
            invalidate();
            requestLayout();
            this.selectedColorRed = redValueForSector(this.mSelectedIndex) & 255;
            this.selectedColorGreen = greenValueForSector(this.mSelectedIndex) & 255;
            this.selectedColorBlue = blueValueForSector(this.mSelectedIndex) & 255;
            try {
                this.onColorBarChangedListener.onColorBarChanged(this.selectedColorRed, this.selectedColorGreen, this.selectedColorBlue);
            } catch (NullPointerException e) {
            }
        }
    }

    public void setOnColorBarChangedListener(OnColorBarChangedListener listener) {
        this.onColorBarChangedListener = listener;
    }

    public int getSelectedIndex() {
        return this.mSelectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.mSelectedIndex = selectedIndex;
        invalidate();
        requestLayout();
        this.selectedColorRed = redValueForSector(this.mSelectedIndex) & 255;
        this.selectedColorGreen = greenValueForSector(this.mSelectedIndex) & 255;
        this.selectedColorBlue = blueValueForSector(this.mSelectedIndex) & 255;
    }

    public void setSelectedColor(int red, int green, int blue) {
        int selectedIndex;
        if (red == 0) {
            if (green == 255) {
                selectedIndex = 18 + getOffsetForValue(blue, false);
            } else {
                selectedIndex = 12 + getOffsetForValue(green, true);
            }
        } else if (red == 255) {
            if (green == 0) {
                selectedIndex = 0 + getOffsetForValue(blue, true);
            } else {
                selectedIndex = 30 + getOffsetForValue(green, false);
            }
        } else if (green == 0) {
            selectedIndex = 6 + getOffsetForValue(red, false);
        } else {
            selectedIndex = 24 + getOffsetForValue(red, true);
        }
        this.mSelectedIndex = selectedIndex;
        invalidate();
        requestLayout();
        this.selectedColorRed = redValueForSector(this.mSelectedIndex) & 255;
        this.selectedColorGreen = greenValueForSector(this.mSelectedIndex) & 255;
        this.selectedColorBlue = blueValueForSector(this.mSelectedIndex) & 255;
    }

    private int getOffsetForValue(int value, boolean increasing) {
        if (increasing) {
            if (value < 42) {
                return 0;
            }
            if (value < 85) {
                return 1;
            }
            if (value < 127) {
                return 2;
            }
            if (value < 170) {
                return 3;
            }
            if (value < 212) {
                return 4;
            }
            if (value < 255) {
                return 5;
            }
            return 6;
        } else if (value > 212) {
            return 0;
        } else {
            if (value > 170) {
                return 1;
            }
            if (value > 127) {
                return 2;
            }
            if (value > 85) {
                return 3;
            }
            if (value > 42) {
                return 4;
            }
            if (value > 0) {
                return 5;
            }
            return 6;
        }
    }

    private void initPaintArray() {
        this.mColorBarPaints = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            Paint p = new Paint(1);
            p.setColor(Color.parseColor(colorStringForSector(i)));
            p.setStyle(Paint.Style.FILL);
            this.mColorBarPaints.add(p);
        }
    }

    private String colorStringForSector(int sector) {
        return String.format("#%02X%02X%02X", Byte.valueOf(redValueForSector(sector)), Byte.valueOf(greenValueForSector(sector)), Byte.valueOf(blueValueForSector(sector)));
    }

    private byte redValueForSector(int sector) {
        return colorValueForIndex((sector + 24) % 36);
    }

    private byte greenValueForSector(int sector) {
        return colorValueForIndex(sector);
    }

    private byte blueValueForSector(int sector) {
        return colorValueForIndex((sector + 12) % 36);
    }

    private byte colorValueForIndex(int index) {
        if (index < 13) {
            return 0;
        }
        if (index < 18) {
            switch (index) {
                case 13:
                    return 42;
                case 14:
                    return 85;
                case 15:
                    return AmbientDeviceService.OP_SETTINGS_ALL_2;
                case 16:
                    return -86;
                case 17:
                    return -44;
                default:
                    return 0;
            }
        } else if (index < 31) {
            return -1;
        } else {
            switch (index) {
                case 31:
                    return -44;
                case 32:
                    return -86;
                case 33:
                    return AmbientDeviceService.OP_SETTINGS_ALL_2;
                case 34:
                    return 85;
                case 35:
                    return 42;
                default:
                    return 0;
            }
        }
    }
}
