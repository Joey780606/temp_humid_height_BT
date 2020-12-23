package pcp.com.bttemperature.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;

import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import pcp.com.bttemperature.ambientDevice.AmbientDevice;

public class AmbientDeviceService extends Service {
    public static final String ACTION_AMBIENT_SETTINGS_RECEIVED = "com.celaer.android.ambient.ACTION_AMBIENT_SETTINGS_RECEIVED";
    public static final String ACTION_CONDITIONS_CURRENT_RECEIVED = "com.celaer.android.ambient.ACTION_CONDITIONS_CURRENT_RECEIVED";
    public static final String ACTION_CONDITIONS_DATA_RECEIVED = "com.celaer.android.ambient.ACTION_CONDITIONS_DATA_RECEIVED";
    public static final String ACTION_DATA_AVAILABLE = "com.celaer.android.ambient.ACTION_DATA_AVAILABLE";
    public static final String ACTION_ERROR = "com.celaer.android.ambient.ACTION_ERROR";
    public static final String ACTION_GATT_CONNECTED = "com.celaer.android.ambient.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "com.celaer.android.ambient.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = "com.celaer.android.ambient.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_SUBSCRIPTION_FINISHED = "com.celaer.android.ambient.ACTION_SUBSCRIPTION_FINISHED";
    public static final String ACTION_WRITE_SUCCESSFUL = "com.celaer.android.ambient.ACTION_WRITE_SUCCESSFUL";
    public static final UUID AMBIENT_SERVICE_UUID = UUID.fromString("A40D0070-B30F-40EE-9878-B5CC23880000");
    public static final UUID AMBIENT_SETTINGS_CHARACTERISTIC_UUID = UUID.fromString("A40D0071-B30F-40EE-9878-B5CC23880000");
    public static final UUID BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB");
    public static final UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    public static final UUID CCC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final String CHAR_UUID = "com.celaer.android.ambient.CHAR_UUID";
    public static final UUID CLOCK_SERVICE_UUID = UUID.fromString("A40D0010-B30F-40EE-9878-B5CC23880000");
    public static final UUID CONDITIONS_DATA_CHARACTERISTIC_UUID = UUID.fromString("A40D0052-B30F-40EE-9878-B5CC23880000");
    public static final UUID CONDITIONS_SERVICE_UUID = UUID.fromString("A40D0050-B30F-40EE-9878-B5CC23880000");
    public static final UUID CURRENT_CHARACTERISTIC_UUID = UUID.fromString("A40D0051-B30F-40EE-9878-B5CC23880000");
    public static final String EXTRAS_CONDITIONS = "EXTRAS_CONDITIONS";
    public static final String EXTRA_DATA = "com.celaer.android.ambient.EXTRA_DATA";
    public static final UUID NEXT_DST_CHARACTERISTIC_UUID = UUID.fromString("A40D0012-B30F-40EE-9878-B5CC23880000");
    public static final byte OP_ALERT_SETTINGS = 120;
    public static final byte OP_BROADCAST_PERIOD = 119;
    public static final int OP_INDICATE = 235;
    public static final byte OP_LCD_SETTINGS = 116;
    public static final byte OP_LED_AUTO_ON_OFF_TIME = 115;
    public static final byte OP_LED_DEMO = 121;
    public static final byte OP_LED_NORMAL_COLOR = 113;
    public static final byte OP_LED_SETTINGS = 112;
    public static final byte OP_LED_SETTINGS_ALL = 122;
    public static final byte OP_LED_WARNING_COLOR = 114;
    public static final byte OP_LOGGING_SETTINGS = 118;
    public static final byte OP_MEASUREMENT_SETTINGS = 117;
    public static final byte OP_SENSOR_SETTINGS_ALL = 123;
    public static final byte OP_SETTINGS_ALL_1 = 126;
    public static final byte OP_SETTINGS_ALL_2 = Byte.MAX_VALUE;
    public static final byte OP_SETTINGS_REQ = 125;
    public static final int OP_TEST_MODE = 234;
    private static final int STATE_CONNECTED = 2;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_DISCONNECTED = 0;
    private static final String TAG = AmbientDeviceService.class.getSimpleName();
    public static final UUID TIME_CHARACTERISTIC_UUID = UUID.fromString("A40D0011-B30F-40EE-9878-B5CC23880000");
    private static boolean isSubscribing = false;
    private static BluetoothGattService mAmbientService;
    private static BluetoothGattCharacteristic mAmbientSettingsCharacteristic;
    private static BluetoothGattDescriptor mAmbientSettingsDataDescriptor;
    private static BluetoothGattCharacteristic mBatteryLevelCharacteristic;
    private static BluetoothGattDescriptor mBatteryLevelDescriptor;
    private static BluetoothGattService mBatteryService;
    private static BluetoothGatt mBluetoothGatt;
    private static BluetoothGattService mClockService;
    private static BluetoothGattCharacteristic mConditionsCurrentCharacteristic;
    private static BluetoothGattDescriptor mConditionsCurrentDescriptor;
    private static BluetoothGattCharacteristic mConditionsDataCharacteristic;
    private static BluetoothGattDescriptor mConditionsDataDescriptor;
    private static BluetoothGattService mConditionsService;
    private static BluetoothGattCharacteristic mDSTCharacteristic;
    private static BluetoothGattCharacteristic mTimeCharacteristic;
    private static boolean sIsWriting = false;
    private static final Queue<Object> sWriteQueue = new ConcurrentLinkedQueue();
    private byte[] clockBytes = new byte[8];
    private Handler handler;
    private final IBinder mBinder = new LocalBinder();
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private int mConnectionState = 0;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        /* class com.celaer.android.ambient.ble.AmbientDeviceService.C03002 */

        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == 2) {
                Log.i(AmbientDeviceService.TAG, "Connected to GATT server.");
                AmbientDeviceService.this.mConnectionState = 2;
                AmbientDeviceService.this.broadcastUpdate(AmbientDeviceService.ACTION_GATT_CONNECTED);
                AmbientDeviceService.this.refreshDeviceCache(AmbientDeviceService.mBluetoothGatt);
            } else if (newState == 0) {
                Log.i(AmbientDeviceService.TAG, "Disconnected from GATT server.");
                AmbientDeviceService.this.mConnectionState = 0;
                AmbientDeviceService.this.broadcastUpdate(AmbientDeviceService.ACTION_GATT_DISCONNECTED);
                AmbientDeviceService.mBluetoothGatt.close();
            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == 0) {
                Log.d(AmbientDeviceService.TAG, "services discovered successfully");
                for (BluetoothGattService service : AmbientDeviceService.getSupportedGattServices()) {
                    if (service.getUuid().equals(AmbientDeviceService.CLOCK_SERVICE_UUID)) {
                        Log.d(AmbientDeviceService.TAG, "Clock Service Discovered");
                        BluetoothGattService unused = AmbientDeviceService.mClockService = service;
                    } else if (service.getUuid().equals(AmbientDeviceService.CONDITIONS_SERVICE_UUID)) {
                        Log.d(AmbientDeviceService.TAG, "Conditions Service Discovered");
                        BluetoothGattService unused2 = AmbientDeviceService.mConditionsService = service;
                    } else if (service.getUuid().equals(AmbientDeviceService.AMBIENT_SERVICE_UUID)) {
                        Log.d(AmbientDeviceService.TAG, "Ambient Service Discovered");
                        BluetoothGattService unused3 = AmbientDeviceService.mAmbientService = service;
                    } else if (service.getUuid().equals(AmbientDeviceService.BATTERY_SERVICE_UUID)) {
                        Log.d(AmbientDeviceService.TAG, "Battery Service Discovered");
                        BluetoothGattService unused4 = AmbientDeviceService.mBatteryService = service;
                    } else {
                        Log.d(AmbientDeviceService.TAG, "Unknown Service: " + service.getUuid().toString());
                    }
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        if (characteristic.getUuid().equals(AmbientDeviceService.TIME_CHARACTERISTIC_UUID)) {
                            Log.d(AmbientDeviceService.TAG, "Time Characteristic Discovered");
                            BluetoothGattCharacteristic unused5 = AmbientDeviceService.mTimeCharacteristic = characteristic;
                        } else if (characteristic.getUuid().equals(AmbientDeviceService.NEXT_DST_CHARACTERISTIC_UUID)) {
                            Log.d(AmbientDeviceService.TAG, "DST Characteristic Discovered");
                            BluetoothGattCharacteristic unused6 = AmbientDeviceService.mDSTCharacteristic = characteristic;
                        } else if (characteristic.getUuid().equals(AmbientDeviceService.CURRENT_CHARACTERISTIC_UUID)) {
                            Log.d(AmbientDeviceService.TAG, "Conditions Current Characteristic Discovered");
                            BluetoothGattCharacteristic unused7 = AmbientDeviceService.mConditionsCurrentCharacteristic = characteristic;
                            BluetoothGattDescriptor unused8 = AmbientDeviceService.mConditionsCurrentDescriptor = characteristic.getDescriptor(AmbientDeviceService.CCC_UUID);
                        } else if (characteristic.getUuid().equals(AmbientDeviceService.CONDITIONS_DATA_CHARACTERISTIC_UUID)) {
                            Log.d(AmbientDeviceService.TAG, "Conditions Data Characteristic Discovered");
                            BluetoothGattCharacteristic unused9 = AmbientDeviceService.mConditionsDataCharacteristic = characteristic;
                            BluetoothGattDescriptor unused10 = AmbientDeviceService.mConditionsDataDescriptor = characteristic.getDescriptor(AmbientDeviceService.CCC_UUID);
                        } else if (characteristic.getUuid().equals(AmbientDeviceService.BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                            Log.d(AmbientDeviceService.TAG, "Battery Level Characteristic Discovered");
                            BluetoothGattCharacteristic unused11 = AmbientDeviceService.mBatteryLevelCharacteristic = characteristic;
                            BluetoothGattDescriptor unused12 = AmbientDeviceService.mBatteryLevelDescriptor = characteristic.getDescriptor(AmbientDeviceService.CCC_UUID);
                        } else if (characteristic.getUuid().equals(AmbientDeviceService.AMBIENT_SETTINGS_CHARACTERISTIC_UUID)) {
                            Log.d(AmbientDeviceService.TAG, "Ambient Settings Characteristic Discovered");
                            BluetoothGattCharacteristic unused13 = AmbientDeviceService.mAmbientSettingsCharacteristic = characteristic;
                            BluetoothGattDescriptor unused14 = AmbientDeviceService.mAmbientSettingsDataDescriptor = characteristic.getDescriptor(AmbientDeviceService.CCC_UUID);
                        } else {
                            Log.d(AmbientDeviceService.TAG, "Unknown Characteristic: " + characteristic.getUuid().toString());
                        }
                    }
                }
                AmbientDeviceService.this.broadcastUpdate(AmbientDeviceService.ACTION_GATT_SERVICES_DISCOVERED);
                return;
            }
            Log.w(AmbientDeviceService.TAG, "onServicesDiscovered ERROR: " + status);
            AmbientDeviceService.this.broadcastUpdate(AmbientDeviceService.ACTION_ERROR);
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (AmbientDeviceService.sIsWriting) {
                boolean unused = AmbientDeviceService.sIsWriting = false;
                AmbientDeviceService.this.nextWrite();
            } else if (status == 0) {
                AmbientDeviceService.this.broadcastUpdate(AmbientDeviceService.ACTION_WRITE_SUCCESSFUL, characteristic);
            }
        }

        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (AmbientDeviceService.sIsWriting) {
                boolean unused = AmbientDeviceService.sIsWriting = false;
                AmbientDeviceService.this.nextWrite();
            }
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == 0 && characteristic.getUuid().equals(AmbientDeviceService.BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                AmbientDeviceService.this.broadcastUpdate(AmbientDeviceService.ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(AmbientDeviceService.CURRENT_CHARACTERISTIC_UUID)) {
                AmbientDeviceService.this.broadcastUpdate(AmbientDeviceService.ACTION_CONDITIONS_CURRENT_RECEIVED, characteristic);
            } else if (characteristic.getUuid().equals(AmbientDeviceService.CONDITIONS_DATA_CHARACTERISTIC_UUID)) {
                AmbientDeviceService.this.broadcastUpdate(AmbientDeviceService.ACTION_CONDITIONS_DATA_RECEIVED, characteristic);
            } else if (characteristic.getUuid().equals(AmbientDeviceService.AMBIENT_SETTINGS_CHARACTERISTIC_UUID)) {
                AmbientDeviceService.this.broadcastUpdate(AmbientDeviceService.ACTION_AMBIENT_SETTINGS_RECEIVED, characteristic);
            }
        }
    };
    private Runnable rDelayedClockWrite;

    public void onCreate() {
        super.onCreate();
        this.handler = new Handler();
        this.rDelayedClockWrite = new Runnable() {
            /* class com.celaer.android.ambient.ble.AmbientDeviceService.RunnableC02991 */

            public void run() {
                AmbientDeviceService.this.sendTimeNow();
            }
        };
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void broadcastUpdate(String action) {
        sendBroadcast(new Intent(action));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic) {
        Intent intent = new Intent(action);
        if (intent.getAction().equals(ACTION_WRITE_SUCCESSFUL)) {
            intent.putExtra(CHAR_UUID, characteristic.getUuid());
        } else if (intent.getAction().equals(ACTION_CONDITIONS_CURRENT_RECEIVED)) {
            intent.putExtra(EXTRAS_CONDITIONS, characteristic.getValue());
        } else if (intent.getAction().equals(ACTION_CONDITIONS_DATA_RECEIVED)) {
            intent.putExtra(EXTRAS_CONDITIONS, characteristic.getValue());
        } else if (intent.getAction().equals(ACTION_AMBIENT_SETTINGS_RECEIVED)) {
            intent.putExtra(EXTRAS_CONDITIONS, characteristic.getValue());
        } else {
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                intent.putExtra(CHAR_UUID, characteristic.getUuid());
                intent.putExtra(EXTRA_DATA, data);
            }
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public AmbientDeviceService getService() {
            return AmbientDeviceService.this;
        }
    }

    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        if (this.mBluetoothManager == null) {
            this.mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (this.mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        this.mBluetoothAdapter = this.mBluetoothManager.getAdapter();
        if (this.mBluetoothAdapter != null) {
            return true;
        }
        Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        return false;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Log.d(TAG, "refreshingDeviceCache");
            Method localMethod = gatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                return ((Boolean) localMethod.invoke(gatt, new Object[0])).booleanValue();
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "An exception occurred while refreshing device");
            return false;
        }
    }

    public boolean isConnected() {
        if (this.mConnectionState == 2) {
            return true;
        }
        return false;
    }

    public void subscribeToNotifications() {
        isSubscribing = true;
        if (mBatteryLevelCharacteristic != null) {
            mBatteryLevelDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            write(mBatteryLevelDescriptor);
            mBluetoothGatt.setCharacteristicNotification(mBatteryLevelCharacteristic, true);
        }
        if (mConditionsCurrentCharacteristic != null) {
            mConditionsCurrentDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            write(mConditionsCurrentDescriptor);
            mBluetoothGatt.setCharacteristicNotification(mConditionsCurrentCharacteristic, true);
        }
        if (mConditionsDataCharacteristic != null) {
            mConditionsDataDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            write(mConditionsDataDescriptor);
            mBluetoothGatt.setCharacteristicNotification(mConditionsDataCharacteristic, true);
        }
        if (mAmbientSettingsCharacteristic != null) {
            mAmbientSettingsDataDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            write(mAmbientSettingsDataDescriptor);
            mBluetoothGatt.setCharacteristicNotification(mAmbientSettingsCharacteristic, true);
        }
    }

    public void subscribeToConditionsNotifications() {
        isSubscribing = true;
        if (mConditionsDataCharacteristic != null) {
            mConditionsDataDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            write(mConditionsDataDescriptor);
            mBluetoothGatt.setCharacteristicNotification(mConditionsDataCharacteristic, true);
        }
    }

    private synchronized void write(Object o) {
        if (!sWriteQueue.isEmpty() || sIsWriting) {
            sWriteQueue.add(o);
        } else {
            doWrite(o);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private synchronized void nextWrite() {
        Log.d(TAG, "write finished: " + sWriteQueue.isEmpty());
        if (sWriteQueue.isEmpty() && isSubscribing) {
            isSubscribing = false;
            broadcastUpdate(ACTION_SUBSCRIPTION_FINISHED);
        }
        if (!sWriteQueue.isEmpty() && !sIsWriting) {
            doWrite(sWriteQueue.poll());
        }
    }

    private synchronized void doWrite(Object o) {
        Log.d(TAG, "doWrite");
        if (o instanceof BluetoothGattCharacteristic) {
            sIsWriting = true;
            mBluetoothGatt.writeCharacteristic((BluetoothGattCharacteristic) o);
        } else if (o instanceof BluetoothGattDescriptor) {
            sIsWriting = true;
            mBluetoothGatt.writeDescriptor((BluetoothGattDescriptor) o);
        } else {
            nextWrite();
        }
    }

    public boolean discoverServices() {
        if (mBluetoothGatt != null) {
            return mBluetoothGatt.discoverServices();
        }
        return false;
    }

    public boolean connect(String address) {
        if (this.mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        //Log.v(TAG, "Joey 201222 start pair 003");
        if (mBluetoothGatt != null) {
            Log.d(TAG, "gatt.close()");
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        BluetoothDevice device = this.mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        refreshDeviceCache(mBluetoothGatt);
        mBluetoothGatt = device.connectGatt(this, false, this.mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        this.mConnectionState = 1;
        return true;
    }

    public void disconnect() {
        if (this.mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.e(TAG, "disconnect called");
        mBluetoothGatt.disconnect();
    }

    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    public static List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            return null;
        }
        return mBluetoothGatt.getServices();
    }

    public void writeClockTime(boolean deleteLogData) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int year = cal.get(1);
        int month = cal.get(2) + 1;
        int day = cal.get(5);
        int hour = cal.get(11);
        int minute = cal.get(12);
        int second = cal.get(13);
        int millis = cal.get(14);
        Log.d(TAG, "clock write REQ: " + year + "/" + month + "/" + day + " " + hour + ":" + minute + ":" + second + ":" + millis);
        if (deleteLogData) {
            this.clockBytes[7] = 1;
        } else {
            this.clockBytes[7] = 0;
        }
        if (second > 54 || second < 5) {
            this.handler.postDelayed(this.rDelayedClockWrite, (long) (((10 - ((second + 6) % 60)) * DateTimeConstants.MILLIS_PER_SECOND) - millis));
            return;
        }
        this.handler.postDelayed(this.rDelayedClockWrite, (long) (2000 - millis));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void sendTimeNow() {
        Calendar cal = Calendar.getInstance();
        long millis = new Date().getTime();
        if (millis % 1000 > 500) {
            millis += 500;
        }
        cal.setTime(new Date(millis));
        int year = cal.get(1);
        int month = cal.get(2) + 1;
        int day = cal.get(5);
        int hour = cal.get(11);
        int minute = cal.get(12);
        int second = cal.get(13);
        Log.d(TAG, "clock write NOW: " + year + "/" + month + "/" + day + " " + hour + ":" + minute + ":" + second + ":" + cal.get(14));
        this.clockBytes[0] = (byte) (year / 100);
        this.clockBytes[1] = (byte) (year % 100);
        this.clockBytes[2] = (byte) month;
        this.clockBytes[3] = (byte) day;
        this.clockBytes[4] = (byte) hour;
        this.clockBytes[5] = (byte) minute;
        this.clockBytes[6] = (byte) second;
        mTimeCharacteristic.setValue(this.clockBytes);
        mBluetoothGatt.writeCharacteristic(mTimeCharacteristic);
    }

    public void writeDst(boolean dstEnabled) {
        TimeZone tz = TimeZone.getDefault();
        Log.d(TAG, "TZ ID: " + tz.getID() + " Display: " + tz.getDisplayName() + " Savings: " + tz.getDSTSavings() + " Offset: " + tz.getRawOffset());
        if (!dstEnabled || !tz.useDaylightTime()) {
            byte[] tempByte = {(byte) ((DateTimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1800000) + 24)};
            Log.d(TAG, "DST Array: " + String.format("0x%02X", Byte.valueOf(tempByte[0])) + " raw: " + ((int) tempByte[0]));
            mDSTCharacteristic.setValue(tempByte);
            mBluetoothGatt.writeCharacteristic(mDSTCharacteristic);
            return;
        }
        byte[] array = new byte[10];
        DateTimeZone defaultZone = DateTimeZone.getDefault();
        long current = System.currentTimeMillis();
        long next = defaultZone.nextTransition(current);
        if (next == current) {
            next = defaultZone.nextTransition(1 + next);
        }
        boolean isStandardNow = defaultZone.isStandardOffset(current);
        int nowOffset = defaultZone.getOffset(current);
        int nextOffset = defaultZone.getOffset(next);
        Log.d(TAG, "default:" + defaultZone.getID() + " isStandardNow:" + isStandardNow + " nowOffset:" + nowOffset + " nextOffset:" + nextOffset);
        Date date = new Date(next);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(1);
        int day = cal.get(5);
        int hour = cal.get(11);
        int minute = cal.get(12);
        int second = cal.get(13);
        array[0] = (byte) (year / 100);
        array[1] = (byte) (year % 100);
        array[2] = (byte) (cal.get(2) + 1);
        array[3] = (byte) day;
        array[4] = (byte) (hour - ((nextOffset - nowOffset) / DateTimeConstants.MILLIS_PER_HOUR));
        array[5] = (byte) minute;
        array[6] = (byte) second;
        array[7] = (byte) Math.abs((nextOffset - nowOffset) / DateTimeConstants.MILLIS_PER_MINUTE);
        if (nextOffset > nowOffset) {
            array[8] = 1;
        } else {
            array[8] = 0;
        }
        array[9] = (byte) ((nowOffset / 1800000) + 24);
        Log.d(TAG, "DST Array: " + ((int) array[0]) + " " + ((int) array[1]) + " " + ((int) array[2]) + " " + ((int) array[3]) + " " + ((int) array[4]) + " " + ((int) array[5]) + " " + ((int) array[6]) + " " + ((int) array[7]) + " " + ((int) array[8]) + " " + ((int) array[9]));
        mDSTCharacteristic.setValue(array);
        mBluetoothGatt.writeCharacteristic(mDSTCharacteristic);
    }

    public void readBatteryLevel() {
        mBluetoothGatt.readCharacteristic(mBatteryLevelCharacteristic);
    }

    public void readSettings() {
        mAmbientSettingsCharacteristic.setValue(new byte[]{OP_SETTINGS_REQ, 1});
        mBluetoothGatt.writeCharacteristic(mAmbientSettingsCharacteristic);
    }

    public void writeLEDSettings(AmbientDevice device) {
        byte[] array = new byte[12];
        int settingsByte = 0;
        if (device.ledEnabled) {
            settingsByte = 1;
        }
        if (device.ledAutoBrightnessEnabled) {
            settingsByte |= 2;
        }
        int settingsByte2 = settingsByte | (device.ledBrightnessLevel << 2);
        if (device.autoOnOffEnabled) {
            settingsByte2 |= 64;
        }
        array[0] = OP_LED_SETTINGS_ALL;
        array[1] = (byte) settingsByte2;
        array[2] = (byte) device.ledNormalColorR;
        array[3] = (byte) device.ledNormalColorG;
        array[4] = (byte) device.ledNormalColorB;
        array[5] = -1;
        array[6] = 0;
        array[7] = 0;
        array[8] = (byte) device.autoStartHr;
        array[9] = (byte) device.autoStartMin;
        array[10] = (byte) device.autoStopHr;
        array[11] = (byte) device.autoStopMin;
        Log.d(TAG, "writeLEDSettings " + String.format("%02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X", Byte.valueOf(array[0]), Byte.valueOf(array[1]), Byte.valueOf(array[2]), Byte.valueOf(array[3]), Byte.valueOf(array[4]), Byte.valueOf(array[5]), Byte.valueOf(array[6]), Byte.valueOf(array[7]), Byte.valueOf(array[8]), Byte.valueOf(array[9]), Byte.valueOf(array[10]), Byte.valueOf(array[11])));
        mAmbientSettingsCharacteristic.setValue(array);
        mBluetoothGatt.writeCharacteristic(mAmbientSettingsCharacteristic);
    }

    public void writeLCDSettings(AmbientDevice device) {
        int settingsByte;
        byte[] array = new byte[2];
        int settingsByte2 = 0;
        if (device.lcdEnabled) {
            settingsByte2 = 1;
        }
        if (device.tempUnitsC) {
            settingsByte2 |= 2;
        }
        if (device.timeFormat24) {
            settingsByte2 |= 4;
        }
        if (device.lcdScroll) {
            settingsByte = settingsByte2 | 8;
        } else {
            settingsByte = settingsByte2 | 16 | (device.lcdDisplayIndex << 5);
        }
        array[0] = OP_LCD_SETTINGS;
        array[1] = (byte) settingsByte;
        Log.d(TAG, "writeLCDSettings " + String.format("%02X %02X", Byte.valueOf(array[0]), Byte.valueOf(array[1])));
        mAmbientSettingsCharacteristic.setValue(array);
        mBluetoothGatt.writeCharacteristic(mAmbientSettingsCharacteristic);
    }

    public void writeSensorSettings(AmbientDevice device) {
        byte[] array = new byte[11];
        int settingsByte = 0;
        if (device.measureTempHumidityEnabled) {
            settingsByte = 3;
        }
        if (device.measureLightEnabled) {
            settingsByte |= 4;
        }
        array[0] = OP_SENSOR_SETTINGS_ALL;
        array[1] = (byte) settingsByte;
        array[2] = (byte) (device.measureTempHumiditySec >> 8);
        array[3] = (byte) (device.measureTempHumiditySec & MotionEvent.ACTION_MASK);
        array[4] = (byte) (device.measureLightSec >> 8);
        array[5] = (byte) (device.measureLightSec & MotionEvent.ACTION_MASK);
        if (device.loggingEnabled) {
            array[6] = 1;
        } else {
            array[6] = 0;
        }
        array[7] = (byte) (device.loggingSec >> 8);
        array[8] = (byte) (device.loggingSec & MotionEvent.ACTION_MASK);
        array[9] = (byte) (device.broadcastSec >> 8);
        array[10] = (byte) (device.broadcastSec & MotionEvent.ACTION_MASK);
        Log.d(TAG, "writeSensorSettings " + String.format("%02X %02X %02X %02X %02X %02X %02X %02X %02X %02X %02X", Byte.valueOf(array[0]), Byte.valueOf(array[1]), Byte.valueOf(array[2]), Byte.valueOf(array[3]), Byte.valueOf(array[4]), Byte.valueOf(array[5]), Byte.valueOf(array[6]), Byte.valueOf(array[7]), Byte.valueOf(array[8]), Byte.valueOf(array[9]), Byte.valueOf(array[10])));
        mAmbientSettingsCharacteristic.setValue(array);
        mBluetoothGatt.writeCharacteristic(mAmbientSettingsCharacteristic);
    }

    public void writeLEDDemo(int red, int green, int blue, int seconds) {
        mAmbientSettingsCharacteristic.setValue(new byte[]{OP_LED_DEMO, (byte) red, (byte) green, (byte) blue, (byte) seconds});
        mBluetoothGatt.writeCharacteristic(mAmbientSettingsCharacteristic);
    }

    public void deleteConditionsLog() {
        mConditionsDataCharacteristic.setValue(new byte[]{85});
        mBluetoothGatt.writeCharacteristic(mConditionsDataCharacteristic);
    }

    public void requestConditionsDataAll() {
        mConditionsDataCharacteristic.setValue(new byte[]{2});
        mBluetoothGatt.writeCharacteristic(mConditionsDataCharacteristic);
    }

    public void requestConditionsDataMore() {
        mConditionsDataCharacteristic.setValue(new byte[]{10});
        mBluetoothGatt.writeCharacteristic(mConditionsDataCharacteristic);
    }

    public void requestConditionsDataAfter(long timestampMillis) {
        byte[] array = {4, (byte) ((int) ((timestampMillis >> 24) & 255)), (byte) ((int) ((timestampMillis >> 16) & 255)), (byte) ((int) ((timestampMillis >> 8) & 255)), (byte) ((int) (timestampMillis & 255))};
        Log.d(TAG, "requestConditionsDataAfter " + String.format("%02X %02X %02X %02X %02X", Byte.valueOf(array[0]), Byte.valueOf(array[1]), Byte.valueOf(array[2]), Byte.valueOf(array[3]), Byte.valueOf(array[4])));
        mConditionsDataCharacteristic.setValue(array);
        mBluetoothGatt.writeCharacteristic(mConditionsDataCharacteristic);
    }
}
