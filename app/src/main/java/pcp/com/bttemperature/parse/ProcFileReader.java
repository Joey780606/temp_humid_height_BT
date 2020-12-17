//package pcp.com.bttemperature.parse;
//
//import android.os.Process;
//import android.util.Log;
//
//import java.io.File;
//
//public class ProcFileReader {
//    public static final int CANNOT_DETERMINE_OPEN_FDS = -1;
//    public static final int SECURITY_EXCEPTION = -2;
//    private static final Class<?> TAG = ProcFileReader.class;
//
//    ProcFileReader() {
//    }
//
//    public static int getOpenFDCount() {
//        String[] FD_DIRS;
//        try {
//            for (String str : new String[]{String.format("/proc/%s/fd", Integer.valueOf(Process.myPid())), "/proc/self/fd", String.format("/proc/%s/fd", Integer.valueOf(Process.myTid()))}) {
//                String[] fdFiles = new File(str).list();
//                if (fdFiles != null) {
//                    return fdFiles.length;
//                }
//            }
//            return -1;
//        } catch (SecurityException e) {
//            Log.e(TAG.toString(), e.getMessage());
//            return -2;
//        }
//    }
//
//    /* JADX WARNING: Removed duplicated region for block: B:15:0x0035  */
//    /* JADX WARNING: Removed duplicated region for block: B:18:0x003c  */
//    /* JADX WARNING: Removed duplicated region for block: B:21:0x0043  */
//    /* JADX WARNING: Removed duplicated region for block: B:32:? A[RETURN, SYNTHETIC] */
//    /* JADX WARNING: Removed duplicated region for block: B:34:? A[RETURN, SYNTHETIC] */
//    /* Code decompiled incorrectly, please refer to instructions dump. */
//    public static com.parse.ProcFileReader.OpenFDLimits getOpenFDLimits() {
//        /*
//            r3 = 0
//            r1 = 0
//            java.util.Scanner r2 = new java.util.Scanner     // Catch:{ IOException -> 0x0032, NoSuchElementException -> 0x0039, all -> 0x0040 }
//            java.io.File r4 = new java.io.File     // Catch:{ IOException -> 0x0032, NoSuchElementException -> 0x0039, all -> 0x0040 }
//            java.lang.String r5 = "/proc/self/limits"
//            r4.<init>(r5)     // Catch:{ IOException -> 0x0032, NoSuchElementException -> 0x0039, all -> 0x0040 }
//            r2.<init>(r4)     // Catch:{ IOException -> 0x0032, NoSuchElementException -> 0x0039, all -> 0x0040 }
//            java.lang.String r4 = "Max open files"
//            r5 = 5000(0x1388, float:7.006E-42)
//            java.lang.String r4 = r2.findWithinHorizon(r4, r5)     // Catch:{ IOException -> 0x004d, NoSuchElementException -> 0x004a, all -> 0x0047 }
//            if (r4 != 0) goto L_0x001e
//            if (r2 == 0) goto L_0x001d
//            r2.close()
//        L_0x001d:
//            return r3
//        L_0x001e:
//            com.parse.ProcFileReader$OpenFDLimits r4 = new com.parse.ProcFileReader$OpenFDLimits
//            java.lang.String r5 = r2.next()
//            java.lang.String r6 = r2.next()
//            r4.<init>(r5, r6)
//            if (r2 == 0) goto L_0x0030
//            r2.close()
//        L_0x0030:
//            r3 = r4
//            goto L_0x001d
//        L_0x0032:
//            r0 = move-exception
//        L_0x0033:
//            if (r1 == 0) goto L_0x001d
//            r1.close()
//            goto L_0x001d
//        L_0x0039:
//            r0 = move-exception
//        L_0x003a:
//            if (r1 == 0) goto L_0x001d
//            r1.close()
//            goto L_0x001d
//        L_0x0040:
//            r3 = move-exception
//        L_0x0041:
//            if (r1 == 0) goto L_0x0046
//            r1.close()
//        L_0x0046:
//            throw r3
//        L_0x0047:
//            r3 = move-exception
//            r1 = r2
//            goto L_0x0041
//        L_0x004a:
//            r0 = move-exception
//            r1 = r2
//            goto L_0x003a
//        L_0x004d:
//            r0 = move-exception
//            r1 = r2
//            goto L_0x0033
//        */
//        throw new UnsupportedOperationException("Method not decompiled: com.parse.ProcFileReader.getOpenFDLimits():com.parse.ProcFileReader$OpenFDLimits");
//    }
//
//    public static class OpenFDLimits {
//        public final String hardLimit;
//        public final String softLimit;
//
//        public OpenFDLimits(String softLimit2, String hardLimit2) {
//            this.softLimit = softLimit2;
//            this.hardLimit = hardLimit2;
//        }
//    }
//}
