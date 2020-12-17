package pcp.com.bttemperature.ambientDevice;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

import pcp.com.bttemperature.database.ConditionDatabaseHelper;
import pcp.com.bttemperature.database.ConditionObject;

public class AmbientDeviceManager {
    private static final String FILENAME = "ambientDevices.json";
    private static final String TAG = AmbientDeviceManager.class.getSimpleName();
    private static AmbientDeviceManager sAmbientDeviceManager;
    private ArrayList<AmbientDevice> mAmbientDevices = new ArrayList<>();
    private Context mAppContext;
    private ConditionDatabaseHelper mHelper;
    private AmbientDeviceJSONSerializer mSerializer = new AmbientDeviceJSONSerializer(this.mAppContext, FILENAME);

    private AmbientDeviceManager(Context appContext) {
        this.mAppContext = appContext;
        try {
            this.mAmbientDevices = this.mSerializer.loadAmbientDevices();
        } catch (Exception e) {
            this.mAmbientDevices = new ArrayList<>();
            Log.e(TAG, "Error loading alarmBoxes: " + e);
        }
        this.mHelper = new ConditionDatabaseHelper(this.mAppContext);
    }

    public static AmbientDeviceManager get(Context c) {
        if (sAmbientDeviceManager == null) {
            sAmbientDeviceManager = new AmbientDeviceManager(c.getApplicationContext());
        }
        return sAmbientDeviceManager;
    }

    public ArrayList<AmbientDevice> getAmbientDevices() {
        return this.mAmbientDevices;
    }

    public AmbientDevice getAmbientDevice(String address) {
        Iterator<AmbientDevice> it = this.mAmbientDevices.iterator();
        while (it.hasNext()) {
            AmbientDevice a = it.next();
            if (a.getAddress().equals(address)) {
                return a;
            }
        }
        return null;
    }

    public AmbientDevice getAmbientDevice(int index) {
        Iterator<AmbientDevice> it = this.mAmbientDevices.iterator();
        while (it.hasNext()) {
            AmbientDevice a = it.next();
            if (a.sortIndex == index) {
                return a;
            }
        }
        return null;
    }

    public int getAmbientDeviceCount() {
        return this.mAmbientDevices.size();
    }

    public void swapAmbientDeviceIndexForIndex(int index1, int index2) {
        AmbientDevice ambient1 = getAmbientDevice(index1);
        AmbientDevice ambient2 = getAmbientDevice(index2);
        int i1 = ambient1.sortIndex;
        ambient1.sortIndex = ambient2.sortIndex;
        ambient2.sortIndex = i1;
        saveAmbientDevices();
    }

    public void addAmbientDevice(AmbientDevice a) {
        if (getAmbientDevice(a.getAddress()) == null) {
            this.mAmbientDevices.add(a);
        }
    }

    public void deleteAmbientDevice(AmbientDevice a) {
        ArrayList<AmbientDevice> mTempArray = new ArrayList<>();
        for (int i = 0; i < this.mAmbientDevices.size(); i++) {
            mTempArray.add(getAmbientDevice(i));
        }
        mTempArray.remove(a);
        for (int i2 = 0; i2 < mTempArray.size(); i2++) {
            mTempArray.get(i2).sortIndex = i2;
        }
        deleteConditions(a.getAddress());
        this.mAmbientDevices.remove(a);
        saveAmbientDevices();
    }

    public void deleteConditionsForAmbientDevice(String a) {
        deleteConditions(a);
    }

    public boolean saveAmbientDevices() {
        try {
            this.mSerializer.saveAmbientDevices(this.mAmbientDevices);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving ambientDevices: ", e);
            return false;
        }
    }

    public long getLastTimestampForConditionsLog(String address) {
        ConditionDatabaseHelper.ConditionCursor cc = this.mHelper.queryLastCondition(address);
        if (cc.getCount() > 0) {
            cc.moveToFirst();
            long timestamp = cc.getCondition().getTimestamp();
            cc.close();
            return timestamp;
        }
        cc.close();
        return 0;
    }

    public boolean conditionExistsBefore(String address, long timestamp) {
        ConditionDatabaseHelper.ConditionCursor cc = this.mHelper.queryOneConditionBeforeTimestamp(address, timestamp);
        if (cc.getCount() > 0) {
            cc.close();
            return true;
        }
        cc.close();
        return false;
    }

    public boolean conditionExistsEqual(String address, long timestamp) {
        ConditionDatabaseHelper.ConditionCursor cc = this.mHelper.queryOneConditionEqualTimestamp(address, timestamp);
        if (cc.getCount() > 0) {
            cc.close();
            return true;
        }
        cc.close();
        return false;
    }

    public ConditionObject conditionEqual(String address, long timestamp) {
        ConditionDatabaseHelper.ConditionCursor cc = this.mHelper.queryOneConditionEqualTimestamp(address, timestamp);
        if (cc.getCount() <= 0) {
            return null;
        }
        cc.moveToFirst();
        ConditionObject condition = cc.getCondition();
        cc.close();
        return condition;
    }

    private void deleteConditions(String deviceAddress) {
        this.mHelper.deleteConditions(deviceAddress);
    }

    public void insertCondition(String deviceAddress, long timestamp, double passedTempC, double passedHumidity, long passedLight) {
        this.mHelper.insertCondition(deviceAddress, timestamp, passedTempC, passedHumidity, passedLight);
    }

    public void updateConditionTimestamp(long dbId, long newTimestamp) {
        this.mHelper.updateConditionTimestamp(dbId, newTimestamp);
    }

    public ConditionDatabaseHelper.ConditionCursor queryConditionsAll(String address) {
        return this.mHelper.queryConditionsAll(address);
    }

    public ConditionDatabaseHelper.ConditionCursor queryConditionsBeforeTimestamp(String address, long timestamp) {
        return this.mHelper.queryConditionsBeforeTimestamp(address, timestamp);
    }

    public ConditionDatabaseHelper.ConditionCursor queryConditionsAfterTimestamp(String address, long timestamp) {
        return this.mHelper.queryConditionsAfterTimestamp(address, timestamp);
    }

    public ConditionDatabaseHelper.ConditionCursor queryConditionsBetweenTimestamps(String address, long timestamp1, long timestamp2) {
        return this.mHelper.queryConditionsBetweenTimestamps(address, timestamp1, timestamp2);
    }
}
