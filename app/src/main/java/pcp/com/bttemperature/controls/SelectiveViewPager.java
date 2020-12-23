package pcp.com.bttemperature.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class SelectiveViewPager extends ViewPager {
    private boolean paging = true;

    public SelectiveViewPager(Context context) {
        super(context);
    }

    public SelectiveViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.support.p000v4.view.ViewPager
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (this.paging) {
            return super.onInterceptTouchEvent(e);
        }
        return false;
    }

    public void setPaging(boolean p) {
        this.paging = p;
    }
}
