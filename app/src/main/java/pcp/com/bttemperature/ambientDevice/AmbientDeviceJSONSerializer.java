package pcp.com.bttemperature.ambientDevice;

import android.content.Context;

public class AmbientDeviceJSONSerializer {
    private Context mContext;
    private String mFilename;

    public AmbientDeviceJSONSerializer(Context c, String f) {
        this.mContext = c;
        this.mFilename = f;
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x002c  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x008f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.util.ArrayList<pcp.com.bttemperature.ambientDevice.AmbientDevice> loadAmbientDevices() throws java.io.IOException, org.json.JSONException {
        /*
        // Method dump skipped, instructions count: 160
        */
        throw new UnsupportedOperationException("Method not decompiled: pcp.com.bttemperature.ambient.ambientDevice.AmbientDeviceJSONSerializer.loadAmbientDevices():java.util.ArrayList");
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x003c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void saveAmbientDevices(java.util.ArrayList<pcp.com.bttemperature.ambientDevice.AmbientDevice> r9) throws org.json.JSONException, java.io.IOException {
        /*
            r8 = this;
            org.json.JSONArray r1 = new org.json.JSONArray
            r1.<init>()
            java.util.Iterator r5 = r9.iterator()
        L_0x0009:
            boolean r6 = r5.hasNext()
            if (r6 == 0) goto L_0x001d
            java.lang.Object r0 = r5.next()
            com.celaer.android.ambient.ambientDevice.AmbientDevice r0 = (com.celaer.android.ambient.ambientDevice.AmbientDevice) r0
            org.json.JSONObject r6 = r0.toJSON()
            r1.put(r6)
            goto L_0x0009
        L_0x001d:
            r3 = 0
            android.content.Context r5 = r8.mContext     // Catch:{ all -> 0x0039 }
            java.lang.String r6 = r8.mFilename     // Catch:{ all -> 0x0039 }
            r7 = 0
            java.io.FileOutputStream r2 = r5.openFileOutput(r6, r7)     // Catch:{ all -> 0x0039 }
            java.io.OutputStreamWriter r4 = new java.io.OutputStreamWriter     // Catch:{ all -> 0x0039 }
            r4.<init>(r2)     // Catch:{ all -> 0x0039 }
            java.lang.String r5 = r1.toString()     // Catch:{ all -> 0x0040 }
            r4.write(r5)     // Catch:{ all -> 0x0040 }
            if (r4 == 0) goto L_0x0038
            r4.close()
        L_0x0038:
            return
        L_0x0039:
            r5 = move-exception
        L_0x003a:
            if (r3 == 0) goto L_0x003f
            r3.close()
        L_0x003f:
            throw r5
        L_0x0040:
            r5 = move-exception
            r3 = r4
            goto L_0x003a
        */
        throw new UnsupportedOperationException("Method not decompiled: pcp.com.bttemperature.ambientDevice.AmbientDeviceJSONSerializer.saveAmbientDevices(java.util.ArrayList):void");
    }
}
