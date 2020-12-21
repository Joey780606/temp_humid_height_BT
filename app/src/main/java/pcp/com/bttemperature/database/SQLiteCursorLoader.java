package pcp.com.bttemperature.database;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;

public abstract class SQLiteCursorLoader extends AsyncTaskLoader<Cursor> {
    private Cursor mCursor;

    /* access modifiers changed from: protected */
    public abstract Cursor loadCursor();

    public SQLiteCursorLoader(Context context) {
        super(context);
    }

    @Override // android.content.AsyncTaskLoader
    public Cursor loadInBackground() {
        Cursor cursor = loadCursor();
        if (cursor != null) {
            cursor.getCount();
        }
        return cursor;
    }

    public void deliverResult(Cursor data) {
        Cursor oldCursor = this.mCursor;
        this.mCursor = data;
        if (isStarted()) {
            super.deliverResult((Cursor) data);
        }
        if (oldCursor != null && oldCursor != data && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    /* access modifiers changed from: protected */
    public void onStartLoading() {
        if (this.mCursor != null) {
            deliverResult(this.mCursor);
        }
        if (takeContentChanged() || this.mCursor == null) {
            forceLoad();
        }
    }

    /* access modifiers changed from: protected */
    public void onStopLoading() {
        cancelLoad();
    }

    public void onCanceled(Cursor data) {
        if (data != null && !data.isClosed()) {
            data.close();
        }
    }

    /* access modifiers changed from: protected */
    public void onReset() {
        super.onReset();
        onStopLoading();
        if (this.mCursor != null && !this.mCursor.isClosed()) {
            this.mCursor.close();
        }
        this.mCursor = null;
    }
}
