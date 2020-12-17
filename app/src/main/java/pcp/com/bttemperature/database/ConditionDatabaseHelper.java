package pcp.com.bttemperature.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ConditionDatabaseHelper extends SQLiteOpenHelper {
    public static final String COLUMN_CONDITIONS_ADDRESS = "address";
    public static final String COLUMN_CONDITIONS_HUMIDITY = "humidity";
    public static final String COLUMN_CONDITIONS_LIGHT = "light";
    public static final String COLUMN_CONDITIONS_TEMPC = "tempC";
    public static final String COLUMN_CONDITIONS_TIMESTAMP = "timestamp";
    private static final String DB_NAME = "conditions.sqlite";
    private static final String TABLE_CONDITIONS = "conditions";
    private static final int VERSION = 1;

    public ConditionDatabaseHelper(Context context) {
        super(context, DB_NAME, (SQLiteDatabase.CursorFactory) null, 1);
    }

    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table conditions (_id integer primary key autoincrement, timestamp integer, tempC real, humidity real, light integer, address text)");
    }

    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
    }

    public void deleteConditions(String deviceAddress) {
        Log.d("helper", "deletedConditions: " + getWritableDatabase().delete(TABLE_CONDITIONS, "address = ?", new String[]{deviceAddress}));
    }

    public long insertCondition(String deviceAddress, long timestamp, double passedTempC, double passedHumidity, long passedLight) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CONDITIONS_ADDRESS, deviceAddress);
        cv.put(COLUMN_CONDITIONS_TIMESTAMP, Long.valueOf(timestamp));
        cv.put(COLUMN_CONDITIONS_TEMPC, Double.valueOf(passedTempC));
        cv.put(COLUMN_CONDITIONS_HUMIDITY, Double.valueOf(passedHumidity));
        cv.put(COLUMN_CONDITIONS_LIGHT, Long.valueOf(passedLight));
        return getWritableDatabase().insert(TABLE_CONDITIONS, null, cv);
    }

    public void updateConditionTimestamp(long dbId, long newTimestamp) {
        ContentValues args = new ContentValues();
        args.put(COLUMN_CONDITIONS_TIMESTAMP, Long.valueOf(newTimestamp));
        getWritableDatabase().update(TABLE_CONDITIONS, args, "_id=" + dbId, null);
    }

    public ConditionCursor queryConditionsAll(String address) {
        return new ConditionCursor(getReadableDatabase().query(TABLE_CONDITIONS, null, "address = ?", new String[]{address}, null, null, "timestamp asc"));
    }

    public ConditionCursor queryConditionsBeforeTimestamp(String address, long timestamp) {
        return new ConditionCursor(getReadableDatabase().query(TABLE_CONDITIONS, null, "address = ? AND timestamp < ?", new String[]{address, String.valueOf(timestamp)}, null, null, "timestamp asc"));
    }

    public ConditionCursor queryOneConditionBeforeTimestamp(String address, long timestamp) {
        return new ConditionCursor(getReadableDatabase().query(TABLE_CONDITIONS, null, "address = ? AND timestamp < ?", new String[]{address, String.valueOf(timestamp)}, null, null, "timestamp asc", "1"));
    }

    public ConditionCursor queryOneConditionEqualTimestamp(String address, long timestamp) {
        return new ConditionCursor(getReadableDatabase().query(TABLE_CONDITIONS, null, "address = ? AND timestamp = ?", new String[]{address, String.valueOf(timestamp)}, null, null, "timestamp asc", "1"));
    }

    public ConditionCursor queryConditionsAfterTimestamp(String address, long timestamp) {
        return new ConditionCursor(getReadableDatabase().query(TABLE_CONDITIONS, null, "address = ? AND timestamp > ?", new String[]{address, String.valueOf(timestamp)}, null, null, "timestamp asc"));
    }

    public ConditionCursor queryConditionsBetweenTimestamps(String address, long timestamp1, long timestamp2) {
        return new ConditionCursor(getReadableDatabase().query(TABLE_CONDITIONS, null, "address = ? AND timestamp >= ? AND timestamp <= ?", new String[]{address, String.valueOf(timestamp1), String.valueOf(timestamp2)}, null, null, "timestamp asc"));
    }

    public ConditionCursor queryLastCondition(String address) {
        return new ConditionCursor(getReadableDatabase().query(TABLE_CONDITIONS, null, "address = ?", new String[]{address}, null, null, "timestamp desc", "1"));
    }

    public static class ConditionCursor extends CursorWrapper {
        public ConditionCursor(Cursor c) {
            super(c);
        }

        public ConditionObject getCondition() {
            if (isBeforeFirst() || isAfterLast()) {
                return null;
            }
            return new ConditionObject(getLong(getColumnIndex(ConditionDatabaseHelper.COLUMN_CONDITIONS_TIMESTAMP)), getDouble(getColumnIndex(ConditionDatabaseHelper.COLUMN_CONDITIONS_TEMPC)), (double) getInt(getColumnIndex(ConditionDatabaseHelper.COLUMN_CONDITIONS_HUMIDITY)), getLong(getColumnIndex(ConditionDatabaseHelper.COLUMN_CONDITIONS_LIGHT)));
        }
    }
}
