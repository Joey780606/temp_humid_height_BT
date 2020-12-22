package pcp.com.bttemperature.ble.parser;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.MotionEvent;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pcp.com.bttemperature.MainActivity;
import pcp.com.bttemperature.parse.ParseException;
import 	androidx.core.view.MotionEventCompat;

public class AmbientDeviceAdvObject {
    private static final String TAG = AmbientDeviceAdvObject.class.getSimpleName();

    public final int HUMIDITY_INVALID = ParseException.OBJECT_NOT_FOUND;
    public final long LIGHT_INVALID = 65535;
    public final double TEMP_INVALID = 71.0d;
    public final int UV_INVALID = MotionEvent.ACTION_MASK;

    private final String address;
    private int alertByte;
    private int batteryLevel;
    private boolean broadcastMode;
    public boolean createTimestampBase = false;
    private double currentHumidity;
    private long currentLightLevel;
    private double currentTempC;
    private int currentUV;
    private boolean dataExists;
    public boolean deleteTempDataAfterPairing = false;
    private BluetoothDevice device;
    private int deviceType;
    private int firmware1;
    private int firmware2;
    private int firmware3;
    private String firmwareVersion;
    private String hexName;
    private double maxHumidity;
    private long maxLightLevel;
    private double maxTempC;
    private int maxUV;
    private double minHumidity;
    private long minLightLevel;
    private double minTempC;
    private int minUV;
    private int normalColorB;
    private int normalColorG;
    private int normalColorR;
    private String uuid;

    public AmbientDeviceAdvObject(BluetoothDevice device2, byte[] scanRecord) {
        this.address = device2.getAddress();
        this.device = device2;
        String tempUUID = "";
        String tempVersion = "";
        int[] addressBytes = {0, 0, 0, 0, 0, 0};
        this.currentTempC = 71.0d;
        this.maxTempC = 71.0d;
        this.minTempC = 71.0d;
        this.currentHumidity = 101.0d;
        this.maxHumidity = 101.0d;
        this.minHumidity = 101.0d;
        this.currentLightLevel = 65535;
        this.maxLightLevel = 65535;
        this.minLightLevel = 65535;
        this.currentUV = MotionEvent.ACTION_MASK;
        this.maxUV = MotionEvent.ACTION_MASK;
        this.minUV = MotionEvent.ACTION_MASK;
        this.uuid = "0000";
        this.broadcastMode = true;

        String scanInfo;

        if(scanRecord.length > 60) {
            scanInfo = String.format("len: %d, ScanInfo=%02X %02X %02X %02X %02X %02X %02X %02X %02X %02X\n%02X %02X %02X %02X %02X %02X %02X %02X %02X %02X\n%02X %02X %02X %02X %02X %02X %02X %02X %02X %02X\n%02X %02X %02X %02X %02X %02X %02X %02X %02X %02X\n%02X %02X %02X %02X %02X %02X %02X %02X %02X %02X\n%02X %02X %02X %02X %02X %02X %02X %02X %02X %02X", scanRecord.length,
                    scanRecord[0], scanRecord[1], scanRecord[2], scanRecord[3], scanRecord[4], scanRecord[5], scanRecord[6], scanRecord[7], scanRecord[8], scanRecord[9],
                    scanRecord[10], scanRecord[11], scanRecord[12], scanRecord[13], scanRecord[14], scanRecord[15], scanRecord[16], scanRecord[17], scanRecord[18], scanRecord[19],
                    scanRecord[20], scanRecord[21], scanRecord[22], scanRecord[23], scanRecord[24], scanRecord[25], scanRecord[26], scanRecord[27], scanRecord[28], scanRecord[29],
                    scanRecord[30], scanRecord[31], scanRecord[32], scanRecord[33], scanRecord[34], scanRecord[35], scanRecord[36], scanRecord[37], scanRecord[38], scanRecord[39],
                    scanRecord[40], scanRecord[41], scanRecord[42], scanRecord[43], scanRecord[44], scanRecord[45], scanRecord[46], scanRecord[47], scanRecord[48], scanRecord[49],
                    scanRecord[50], scanRecord[51], scanRecord[52], scanRecord[53], scanRecord[54], scanRecord[55], scanRecord[56], scanRecord[57], scanRecord[58], scanRecord[59]);
            //Log.v(TAG, "Joey test 0041:" + scanInfo );
        }
        for (AdRecord record : AdRecord.parseScanRecord(scanRecord)) {
            //Log.v(TAG, "Joey test 0040:" + record.mType + " , " + record.mData.length );
            if (record.mType == 3) {
                if (record.mData.length == 2) {
                    tempUUID = String.format("%02X%02X", Integer.valueOf(record.mData[1]), Integer.valueOf(record.mData[0])).toUpperCase();
                }
            } else if (record.mType == 7) {
                tempUUID = ByteArrayToUUIDString(record.mData);
            } else if (record.mType == 255 && record.mLength == 23) {
                if (record.mLength == 23) {
                    if (record.mData[0] >= 240) {
                        this.broadcastMode = false;
                    } else {
                        this.broadcastMode = true;
                    }
                    int tempDevice = record.mData[0] & 15;
                    if (this.broadcastMode) {
                        this.currentTempC = (((double) ((record.mData[1] * 10) + (record.mData[2] & 15))) - 400.0d) / 10.0d;
                        this.currentHumidity = ((double) record.mData[3]) + (((double) (record.mData[2] >> 4)) / 10.0d);
                        this.currentLightLevel = (long) ((record.mData[4] << 8) | record.mData[5]);
                        this.batteryLevel = record.mData[20] & MotionEvent.ACTION_MASK ;
                        float maxTemp2 = (float) (((double) ((float) (((double) ((float) ((record.mData[6] * 10) + (record.mData[7] >> 4)))) - 400.0d))) / 10.0d);
                        float minTemp2 = (float) (((double) ((float) (((double) ((float) ((record.mData[8] * 10) + (record.mData[7] & 15)))) - 400.0d))) / 10.0d);
                        this.maxTempC = (double) maxTemp2;
                        this.minTempC = (double) minTemp2;
                        this.maxHumidity = ((double) record.mData[9]) + (((double) (record.mData[15] >> 4)) / 10.0d);
                        this.minHumidity = ((double) record.mData[10]) + (((double) (record.mData[15] & 15)) / 10.0d);
                        this.maxLightLevel = (long) ((record.mData[11] << 8) | record.mData[12]);
                        this.minLightLevel = (long) ((record.mData[13] << 8) | record.mData[14]);
                        this.alertByte = record.mData[21];
                        this.hexName = this.address.substring(0, 2) + this.address.substring(3, 5);
                    } else {
                        addressBytes[0] = record.mData[1];
                        addressBytes[1] = record.mData[2];
                        addressBytes[2] = record.mData[3];
                        addressBytes[3] = record.mData[4];
                        addressBytes[4] = record.mData[5];
                        addressBytes[5] = record.mData[6];
                        if ((record.mData[7] & 1) == 0) {
                            this.dataExists = false;
                        } else if ((record.mData[7] & 1) == 1) {
                            this.dataExists = true;
                        }
                        if ((record.mData[7] & 2) == 0) {
                            Log.d("parser", "previously paired sensor");
                        } else if ((record.mData[7] & 2) == 2) {
                            Log.d("parser", "never paired sensor");
                        }
                        this.normalColorR = record.mData[8];
                        this.normalColorG = record.mData[9];
                        this.normalColorB = record.mData[10];
                        this.currentTempC = (((double) ((record.mData[11] * 10) + (record.mData[12] & 15))) - 400.0d) / 10.0d;
                        this.currentHumidity = ((double) record.mData[13]) + (((double) (record.mData[12] >> 4)) / 10.0d);
                        this.currentLightLevel = (long) ((record.mData[14] << 8) | record.mData[15]);
                        this.batteryLevel = record.mData[18];
                        this.firmware1 = record.mData[19];
                        this.firmware2 = record.mData[20];
                        this.firmware3 = record.mData[21];
                        tempVersion = String.format("%d.%d.%d", Integer.valueOf(record.mData[19]), Integer.valueOf(record.mData[20]), Integer.valueOf(record.mData[21]));
                        this.hexName = String.format("%02X%02X", Integer.valueOf(addressBytes[0]), Integer.valueOf(addressBytes[1])).substring(0, 3).toUpperCase();
                    }
                    this.uuid = tempUUID;
                    this.firmwareVersion = tempVersion;
                    this.deviceType = tempDevice;
                    if (this.deviceType != 0) {
                        if (this.deviceType != 1) {
                            if (this.deviceType != 2) {
                                if (this.deviceType == 3) {
                                }
                            }
                        }
                    }
                } else {
                    return;
                }
            }
        }
    }

    public String getAddress() {
        return this.address;
    }

    public String getHexName() {
        return this.hexName;
    }

    public String getUUID() {
        return this.uuid;
    }

    public String getFirmwareVersion() {
        return this.firmwareVersion;
    }

    public int getDeviceType() {
        return this.deviceType;
    }

    public BluetoothDevice getDevice() {
        return this.device;
    }

    public void setDevice(BluetoothDevice device2) {
        this.device = device2;
    }

    public boolean getDataExists() {
        return this.dataExists;
    }

    public int getNormalColorR() {
        return this.normalColorR;
    }

    public int getNormalColorG() {
        return this.normalColorG;
    }

    public int getNormalColorB() {
        return this.normalColorB;
    }

    public double getCurrentTempC() {
        return this.currentTempC;
    }

    public double getCurrentHumidity() {
        return this.currentHumidity;
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

    public long getCurrentLightLevel() {
        return this.currentLightLevel;
    }

    public long getMaxLightLevel() {
        return this.maxLightLevel;
    }

    public long getMinLightLevel() {
        return this.minLightLevel;
    }

    public int getCurrentUV() {
        return this.currentUV;
    }

    public int getMaxUV() {
        return this.maxUV;
    }

    public int getMinUV() {
        return this.minUV;
    }

    public boolean isBroadcastMode() {
        return this.broadcastMode;
    }

    public int getBatteryLevel() {
        return this.batteryLevel;
    }

    public int[] getFirmwareVersionComponents() {
        return new int[]{this.firmware1, this.firmware2, this.firmware3};
    }

    private static class AdRecord {
        public int[] mData;
        public int mLength;
        public int mType;

        public AdRecord(int length, int type, int[] data) {
            this.mLength = length;
            this.mType = type;
            this.mData = data;
        }

        public static List<AdRecord> parseScanRecord(byte[] scanRecord) {
            List<AdRecord> records = new ArrayList<>();
            int index = 0;
            while (true) {
                if (index >= scanRecord.length || index + 1 >= scanRecord.length) {
                    break;
                }
                int index2 = index + 1;
                byte b = scanRecord[index];
                if (b == 0) {
                    break;
                }
                int type = AmbientDeviceAdvObject.byteToUnsignedInt(scanRecord[index2]);
                if (type == 0) {
                    break;
                }
                byte[] data = Arrays.copyOfRange(scanRecord, index2 + 1, index2 + b);
                int[] dataInt = new int[data.length];
                for (int i = 0; i < data.length; i++) {
                    dataInt[i] = AmbientDeviceAdvObject.byteToUnsignedInt(data[i]);
                }
                //Log.v(TAG, "Joey test 0041 parsing:" + b + " , " + type + " , " + data.length);
                records.add(new AdRecord(b, type, dataInt));
                index = index2 + b;
            }
            return records;
        }
    }

    public static int byteToUnsignedInt(byte b) {
        return b & 255;
    }

    private static String ByteArrayToUUIDString(int[] ba) {
        StringBuilder sb = new StringBuilder((ba.length * 2) + 4);
        for (int tempI = ba.length - 1; tempI >= 0; tempI--) {
            sb.append(String.format("%02X", Integer.valueOf(ba[tempI])));
            if (tempI == 12 || tempI == 10 || tempI == 8 || tempI == 6) {
                sb.append("-");
            }
        }
        return sb.toString();
    }
}
