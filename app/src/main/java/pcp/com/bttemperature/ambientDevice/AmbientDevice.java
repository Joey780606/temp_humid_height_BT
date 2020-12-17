package pcp.com.bttemperature.ambientDevice;

import android.util.Log;

import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import pcp.com.bttemperature.ble.parser.AmbientDeviceAdvObject;

public class AmbientDevice {
    private static final String JSON_ADDRESS = "address";
    private static final String JSON_AUTO_ON_OFF_ENABLED = "autoOnOffEnabled";
    private static final String JSON_AUTO_START_HR = "autoStartHr";
    private static final String JSON_AUTO_START_MIN = "autoStartMin";
    private static final String JSON_AUTO_STOP_HR = "autoStopHr";
    private static final String JSON_AUTO_STOP_MIN = "autoStopMin";
    private static final String JSON_BATTERY_LEVEL = "batteryLevel";
    private static final String JSON_BROADCAST_SEC = "broadcastSec";
    private static final String JSON_CURRENT_HUMIDITY = "currentHumidity";
    private static final String JSON_CURRENT_LIGHT_LEVEL = "currentLightLevel";
    private static final String JSON_CURRENT_TEMPC = "currentTempC";
    private static final String JSON_DEVICE_TYPE = "deviceType";
    private static final String JSON_DST_ENABLED = "dstEnabled";
    private static final String JSON_FILE_STRUCTURE_VERSION = "jsonVersion";
    private static final String JSON_FIRMWARE_VERSION = "firmwareVersion";
    private static final String JSON_LCD_DISPLAY_INDEX = "lcdDisplayIndex";
    private static final String JSON_LCD_ENABLED = "lcdEnabled";
    private static final String JSON_LCD_SCROLL = "lcdScroll";
    private static final String JSON_LED_AUTO_BRIGHTNESS_ENABLED = "ledAutoBrightnessEnabled";
    private static final String JSON_LED_BRIGHTNESS_LEVEL = "ledBrightnessLevel";
    private static final String JSON_LED_ENABLED = "ledEnabled";
    private static final String JSON_LED_NORMAL_COLOR_B = "ledNormalColorB";
    private static final String JSON_LED_NORMAL_COLOR_G = "ledNormalColorG";
    private static final String JSON_LED_NORMAL_COLOR_R = "ledNormalColorR";
    private static final String JSON_LOGGING_ENABLED = "loggingEnabled";
    private static final String JSON_LOGGING_SEC = "loggingSec";
    private static final String JSON_MAX_HUMIDITY = "maxHumidity";
    private static final String JSON_MAX_LIGHT_LEVEL = "maxLightLevel";
    private static final String JSON_MAX_TEMPC = "maxTempC";
    private static final String JSON_MEASURE_LIGHT_ENABLED = "measureLightEnabled";
    private static final String JSON_MEASURE_LIGHT_SEC = "measureLightSec";
    private static final String JSON_MEASURE_TEMP_HUMIDITY_ENABLED = "measureTempHumidityEnabled";
    private static final String JSON_MEASURE_TEMP_HUMIDITY_SEC = "measureTempHumiditySec";
    private static final String JSON_MIN_HUMIDITY = "minHumidity";
    private static final String JSON_MIN_LIGHT_LEVEL = "minLightLevel";
    private static final String JSON_MIN_TEMPC = "minTempC";
    private static final String JSON_NAME = "name";
    private static final String JSON_NAME_HEX = "nameHex";
    private static final String JSON_SORT_INDEX = "sortIndex";
    private static final String JSON_TEMP_UNITS_C = "tempUnitsC";
    private static final String JSON_TIMESTAMP_ADDED = "timestampAdded";
    private static final String JSON_TIMESTAMP_BASE = "timestampBase";
    private static final String JSON_TIMESTAMP_BROADCAST = "timestampBroadcast";
    private static final String JSON_TIMESTAMP_SYNC = "timestampSync";
    private static final String JSON_TIME_FORMAT_24 = "timeFormat24";
    private final int JSON_VERSION;
    public boolean autoOnOffEnabled;
    public int autoStartHr;
    public int autoStartMin;
    public int autoStopHr;
    public int autoStopMin;
    public int batteryLevel;
    public int broadcastSec;
    public double currentHumidity;
    public long currentLightLevel;
    public double currentTempC;
    private int deviceType;
    public boolean dstEnabled;
    private String firmwareVersion;
    public int lcdDisplayIndex;
    public boolean lcdEnabled;
    public boolean lcdScroll;
    public boolean ledAutoBrightnessEnabled;
    public int ledBrightnessLevel;
    public boolean ledEnabled;
    public int ledNormalColorB;
    public int ledNormalColorG;
    public int ledNormalColorR;
    public boolean loggingEnabled;
    public int loggingSec;
    private String mAddress;
    private int mJsonVersion;
    private String mName;
    private String mNameHex;
    public boolean mSyncAlertSettings;
    public boolean mSyncBattery;
    public boolean mSyncConditionsCurrent;
    public boolean mSyncConditionsData;
    public boolean mSyncDST;
    public boolean mSyncLCDSettings;
    public boolean mSyncLEDSettings;
    public boolean mSyncReadSettings;
    public boolean mSyncSensorSettings;
    public SyncState mSyncState;
    public boolean mSyncTime;
    private double maxHumidity;
    private long maxLightLevel;
    private double maxTempC;
    public boolean measureLightEnabled;
    public int measureLightSec;
    public boolean measureTempHumidityEnabled;
    public int measureTempHumiditySec;
    private double minHumidity;
    private long minLightLevel;
    private double minTempC;
    public int sortIndex;
    public boolean tempUnitsC;
    public boolean timeFormat24;
    private final long timestampAdded;
    public long timestampBase;
    private long timestampBroadcast;
    private long timestampSync;

    public enum SyncState {
        BLE_OBJECT_SYNC_STATE_Idle,
        BLE_OBJECT_SYNC_STATE_Discovering,
        BLE_OBJECT_SYNC_STATE_AwaitingHighSpeed,
        BLE_OBJECT_SYNC_STATE_Battery,
        BLE_OBJECT_SYNC_STATE_Time,
        BLE_OBJECT_SYNC_STATE_DST,
        BLE_OBJECT_SYNC_STATE_ConditionsCurrent,
        BLE_OBJECT_SYNC_STATE_ConditionsData,
        BLE_OBJECT_SYNC_STATE_Notify,
        BLE_OBJECT_SYNC_STATE_LEDSettings,
        BLE_OBJECT_SYNC_STATE_LCDSettings,
        BLE_OBJECT_SYNC_STATE_SensorSettings,
        BLE_OBJECT_SYNC_STATE_ReadSettings,
        BLE_OBJECT_SYNC_STATE_AlertSettings
    }

    public AmbientDevice(AmbientDeviceAdvObject object, String sensorString, int listIndex) {
        this.JSON_VERSION = 1;
        this.mSyncState = SyncState.BLE_OBJECT_SYNC_STATE_Idle;
        this.mSyncTime = false;
        this.mSyncDST = false;
        this.mSyncConditionsCurrent = false;
        this.mSyncConditionsData = false;
        this.mSyncBattery = false;
        this.mSyncLEDSettings = false;
        this.mSyncLCDSettings = false;
        this.mSyncSensorSettings = false;
        this.mSyncReadSettings = false;
        this.mSyncAlertSettings = false;
        this.timestampAdded = new Date().getTime();
        this.mNameHex = object.getHexName();
        this.mName = sensorString + " " + this.mNameHex;
        this.mAddress = object.getAddress();
        this.batteryLevel = object.getBatteryLevel();
        this.deviceType = object.getDeviceType();
        this.lcdEnabled = true;
        this.lcdScroll = true;
        this.lcdDisplayIndex = 0;
        this.tempUnitsC = true;
        this.timeFormat24 = false;
        this.dstEnabled = true;
        this.ledEnabled = true;
        this.ledAutoBrightnessEnabled = true;
        this.ledBrightnessLevel = 5;
        this.autoOnOffEnabled = false;
        this.autoStartHr = 9;
        this.autoStartMin = 0;
        this.autoStopHr = 21;
        this.autoStopMin = 0;
        this.ledNormalColorR = object.getNormalColorR();
        this.ledNormalColorG = object.getNormalColorG();
        this.ledNormalColorB = object.getNormalColorB();
        this.loggingEnabled = true;
        this.loggingSec = DateTimeConstants.SECONDS_PER_HOUR;
        this.measureTempHumidityEnabled = true;
        this.measureTempHumiditySec = 10;
        this.measureLightEnabled = true;
        this.measureLightSec = 10;
        this.broadcastSec = 15;
        this.currentTempC = object.getCurrentTempC();
        this.currentHumidity = object.getCurrentHumidity();
        this.currentLightLevel = object.getCurrentLightLevel();
        this.batteryLevel = object.getBatteryLevel();
        this.firmwareVersion = object.getFirmwareVersion();
        this.timestampBase = 0;
        this.sortIndex = listIndex;
        this.mJsonVersion = 1;
        this.mSyncState = SyncState.BLE_OBJECT_SYNC_STATE_Idle;
    }

    public AmbientDevice(JSONObject json) throws JSONException {
        this.JSON_VERSION = 1;
        this.mSyncState = SyncState.BLE_OBJECT_SYNC_STATE_Idle;
        this.mSyncTime = false;
        this.mSyncDST = false;
        this.mSyncConditionsCurrent = false;
        this.mSyncConditionsData = false;
        this.mSyncBattery = false;
        this.mSyncLEDSettings = false;
        this.mSyncLCDSettings = false;
        this.mSyncSensorSettings = false;
        this.mSyncReadSettings = false;
        this.mSyncAlertSettings = false;
        this.mName = json.getString(JSON_NAME);
        this.mNameHex = json.getString(JSON_NAME_HEX);
        this.mAddress = json.getString("address");
        this.batteryLevel = json.getInt(JSON_BATTERY_LEVEL);
        this.deviceType = json.getInt(JSON_DEVICE_TYPE);
        this.sortIndex = json.getInt(JSON_SORT_INDEX);
        this.lcdEnabled = json.getBoolean(JSON_LCD_ENABLED);
        this.lcdScroll = json.getBoolean(JSON_LCD_SCROLL);
        this.lcdDisplayIndex = json.getInt(JSON_LCD_DISPLAY_INDEX);
        this.tempUnitsC = json.getBoolean(JSON_TEMP_UNITS_C);
        this.timeFormat24 = json.getBoolean(JSON_TIME_FORMAT_24);
        this.ledEnabled = json.getBoolean(JSON_LED_ENABLED);
        this.ledAutoBrightnessEnabled = json.getBoolean(JSON_LED_AUTO_BRIGHTNESS_ENABLED);
        this.ledBrightnessLevel = json.getInt(JSON_LED_BRIGHTNESS_LEVEL);
        this.autoOnOffEnabled = json.getBoolean(JSON_AUTO_ON_OFF_ENABLED);
        this.autoStartHr = json.getInt(JSON_AUTO_START_HR);
        this.autoStartMin = json.getInt(JSON_AUTO_START_MIN);
        this.autoStopHr = json.getInt(JSON_AUTO_STOP_HR);
        this.autoStopMin = json.getInt(JSON_AUTO_STOP_MIN);
        this.ledNormalColorR = json.getInt(JSON_LED_NORMAL_COLOR_R);
        this.ledNormalColorG = json.getInt(JSON_LED_NORMAL_COLOR_G);
        this.ledNormalColorB = json.getInt(JSON_LED_NORMAL_COLOR_B);
        this.dstEnabled = json.getBoolean(JSON_DST_ENABLED);
        this.currentTempC = json.getDouble(JSON_CURRENT_TEMPC);
        this.currentHumidity = json.getDouble(JSON_CURRENT_HUMIDITY);
        this.currentLightLevel = json.getLong(JSON_CURRENT_LIGHT_LEVEL);
        this.maxTempC = json.getDouble(JSON_MAX_TEMPC);
        this.minTempC = json.getDouble(JSON_MIN_TEMPC);
        this.maxHumidity = json.getDouble(JSON_MAX_HUMIDITY);
        this.minHumidity = json.getDouble(JSON_MIN_HUMIDITY);
        this.maxLightLevel = json.getLong(JSON_MAX_LIGHT_LEVEL);
        this.minLightLevel = json.getLong(JSON_MIN_LIGHT_LEVEL);
        this.timestampAdded = json.getLong(JSON_TIMESTAMP_ADDED);
        this.timestampBroadcast = json.getLong(JSON_TIMESTAMP_BROADCAST);
        this.timestampSync = json.getLong(JSON_TIMESTAMP_SYNC);
        this.loggingEnabled = json.getBoolean(JSON_LOGGING_ENABLED);
        this.loggingSec = json.getInt(JSON_LOGGING_SEC);
        this.measureTempHumidityEnabled = json.getBoolean(JSON_MEASURE_TEMP_HUMIDITY_ENABLED);
        this.measureTempHumiditySec = json.getInt(JSON_MEASURE_TEMP_HUMIDITY_SEC);
        this.measureLightEnabled = json.getBoolean(JSON_MEASURE_LIGHT_ENABLED);
        this.measureLightSec = json.getInt(JSON_MEASURE_LIGHT_SEC);
        this.broadcastSec = json.getInt(JSON_BROADCAST_SEC);
        this.timestampBase = json.getLong(JSON_TIMESTAMP_BASE);
        this.mJsonVersion = json.getInt(JSON_FILE_STRUCTURE_VERSION);
        this.firmwareVersion = json.getString(JSON_FIRMWARE_VERSION);
        this.mSyncState = SyncState.BLE_OBJECT_SYNC_STATE_Idle;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(JSON_NAME, this.mName);
        json.put(JSON_NAME_HEX, this.mNameHex);
        json.put("address", this.mAddress);
        json.put(JSON_BATTERY_LEVEL, this.batteryLevel);
        json.put(JSON_DEVICE_TYPE, this.deviceType);
        json.put(JSON_SORT_INDEX, this.sortIndex);
        json.put(JSON_LCD_ENABLED, this.lcdEnabled);
        json.put(JSON_LCD_SCROLL, this.lcdScroll);
        json.put(JSON_LCD_DISPLAY_INDEX, this.lcdDisplayIndex);
        json.put(JSON_TEMP_UNITS_C, this.tempUnitsC);
        json.put(JSON_TIME_FORMAT_24, this.timeFormat24);
        json.put(JSON_LED_ENABLED, this.ledEnabled);
        json.put(JSON_LED_AUTO_BRIGHTNESS_ENABLED, this.ledAutoBrightnessEnabled);
        json.put(JSON_LED_BRIGHTNESS_LEVEL, this.ledBrightnessLevel);
        json.put(JSON_AUTO_ON_OFF_ENABLED, this.autoOnOffEnabled);
        json.put(JSON_AUTO_START_HR, this.autoStartHr);
        json.put(JSON_AUTO_START_MIN, this.autoStartMin);
        json.put(JSON_AUTO_STOP_HR, this.autoStopHr);
        json.put(JSON_AUTO_STOP_MIN, this.autoStopMin);
        json.put(JSON_LED_NORMAL_COLOR_R, this.ledNormalColorR);
        json.put(JSON_LED_NORMAL_COLOR_G, this.ledNormalColorG);
        json.put(JSON_LED_NORMAL_COLOR_B, this.ledNormalColorB);
        json.put(JSON_DST_ENABLED, this.dstEnabled);
        json.put(JSON_CURRENT_TEMPC, this.currentTempC);
        json.put(JSON_CURRENT_HUMIDITY, this.currentHumidity);
        json.put(JSON_CURRENT_LIGHT_LEVEL, this.currentLightLevel);
        json.put(JSON_MAX_TEMPC, this.maxTempC);
        json.put(JSON_MIN_TEMPC, this.minTempC);
        json.put(JSON_MAX_HUMIDITY, this.maxHumidity);
        json.put(JSON_MIN_HUMIDITY, this.minHumidity);
        json.put(JSON_MAX_LIGHT_LEVEL, this.maxLightLevel);
        json.put(JSON_MIN_LIGHT_LEVEL, this.minLightLevel);
        json.put(JSON_TIMESTAMP_ADDED, this.timestampAdded);
        json.put(JSON_TIMESTAMP_BROADCAST, this.timestampBroadcast);
        json.put(JSON_TIMESTAMP_SYNC, this.timestampSync);
        json.put(JSON_LOGGING_ENABLED, this.loggingEnabled);
        json.put(JSON_LOGGING_SEC, this.loggingSec);
        json.put(JSON_MEASURE_TEMP_HUMIDITY_ENABLED, this.measureTempHumidityEnabled);
        json.put(JSON_MEASURE_TEMP_HUMIDITY_SEC, this.measureTempHumiditySec);
        json.put(JSON_MEASURE_LIGHT_ENABLED, this.measureLightEnabled);
        json.put(JSON_MEASURE_LIGHT_SEC, this.measureLightSec);
        json.put(JSON_BROADCAST_SEC, this.broadcastSec);
        json.put(JSON_TIMESTAMP_BASE, this.timestampBase);
        json.put(JSON_FILE_STRUCTURE_VERSION, this.mJsonVersion);
        json.put(JSON_FIRMWARE_VERSION, this.firmwareVersion);
        return json;
    }

    public String getName() {
        return this.mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getDeviceType() {
        return this.deviceType;
    }

    public String getHexName() {
        return this.mNameHex;
    }

    public String getAddress() {
        return this.mAddress;
    }

    public void setTimestampBase(long timestamp) {
        this.timestampBase = timestamp;
    }

    public long getTimestampBase() {
        return this.timestampBase;
    }

    public long getTimestampAdded() {
        return this.timestampAdded;
    }

    public long getTimestampBroadcast() {
        return this.timestampBroadcast;
    }

    public long getTimestampSync() {
        return this.timestampSync;
    }

    public void updateTimestampSync() {
        this.timestampSync = new Date().getTime();
    }

    public String getFirmwareVersion() {
        return this.firmwareVersion;
    }

    public void setFirmwareVersion(String firmware) {
        this.firmwareVersion = firmware;
    }

    public void setMaxTempC(double temp) {
        this.maxTempC = temp;
        updateTimestampBroadcast();
    }

    public void setMinTempC(double temp) {
        this.minTempC = temp;
        updateTimestampBroadcast();
    }

    public void setMaxHumidity(double humidity) {
        this.maxHumidity = humidity;
        updateTimestampBroadcast();
    }

    public void setMinHumidity(double humidity) {
        this.minHumidity = humidity;
        updateTimestampBroadcast();
    }

    public void setMaxLightLevel(long light) {
        this.maxLightLevel = light;
        updateTimestampBroadcast();
    }

    public void setMinLightLevel(long light) {
        this.minLightLevel = light;
        updateTimestampBroadcast();
    }

    public double getMaxTempC() {
        return this.maxTempC;
    }

    public double getMinTempC() {
        return this.minTempC;
    }

    public double getMaxHumidity() {
        return this.maxHumidity;
    }

    public double getMinHumidity() {
        return this.minHumidity;
    }

    public long getMaxLightLevel() {
        return this.maxLightLevel;
    }

    public long getMinLightLevel() {
        return this.minLightLevel;
    }

    private void updateTimestampBroadcast() {
        this.timestampBroadcast = new Date().getTime();
    }

    public void setTimestampBase() {
        DateTimeZone.getDefault().getOffset(System.currentTimeMillis());
        this.timestampBase = new Date().getTime();
        Log.d("AmbientDevice", "timestampBase: " + this.timestampBase);
    }

    public void processBroadcast(AmbientDeviceAdvObject object) {
        this.currentTempC = object.getCurrentTempC();
        this.currentHumidity = object.getCurrentHumidity();
        this.currentLightLevel = object.getCurrentLightLevel();
        this.batteryLevel = object.getBatteryLevel();
        this.maxTempC = object.getMaxTempC();
        this.minTempC = object.getMinTempC();
        this.maxHumidity = object.getMaxHumidity();
        this.minHumidity = object.getMinHumidity();
        this.maxLightLevel = object.getMaxLightLevel();
        this.minLightLevel = object.getMinLightLevel();
        updateTimestampBroadcast();
    }
}
