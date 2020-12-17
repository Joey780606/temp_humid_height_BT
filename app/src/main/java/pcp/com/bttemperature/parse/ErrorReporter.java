//package pcp.com.bttemperature.parse;
//
//import android.app.ActivityManager;
//import android.content.Context;
//import android.content.pm.PackageInfo;
//import android.os.Build;
//import android.os.Environment;
//import android.os.PowerManager;
//import android.os.Process;
//import android.os.StatFs;
//import android.os.SystemClock;
//import android.provider.Settings;
//import android.telephony.TelephonyManager;
//import android.text.TextUtils;
//import android.text.format.Time;
//import android.util.DisplayMetrics;
//import android.util.Log;
//import android.view.Display;
//import android.view.WindowManager;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FilenameFilter;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.io.Writer;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import androidx.core.content.FileProvider;
//import pcp.com.bttemperature.database.CSVWriter;
//
///* access modifiers changed from: package-private */
//public class ErrorReporter implements Thread.UncaughtExceptionHandler {
//    public static final String ACRA_DIRNAME = "cr/reports";
//    private static final CrashReportType[] ALL_REPORT_TYPES = {CrashReportType.ACRA_CRASH_REPORT, CrashReportType.NATIVE_CRASH_REPORT, CrashReportType.ANR_REPORT};
//    private static final String ANDROID_RUNTIME_ART = "ART";
//    private static final String ANDROID_RUNTIME_DALVIK = "DALVIK";
//    private static final String ANDROID_RUNTIME_UNKNOWN = "UNKNOWN";
//    public static final String CRASH_ATTACHMENT_DUMMY_STACKTRACE = "crash attachment";
//    public static final long DEFAULT_MAX_REPORT_SIZE = 51200;
//    private static int DEFAULT_TRACE_COUNT_LIMIT = 5;
//    public static final String DUMPFILE_EXTENSION = ".dmp";
//    public static final String DUMP_DIR = "cr/minidumps";
//    private static final String IS_PROCESSING_ANOTHER_EXCEPTION = "IS_PROCESSING_ANOTHER_EXCEPTION";
//    private static final String JAVA_BOOT_CLASS_PATH = "java.boot.class.path";
//    private static final String KNOWN_ART_JAR = "/system/framework/core-libart.jar";
//    private static final String KNOWN_DALVIK_JAR = "/system/framework/core.jar";
//    public static final long MAX_REPORT_AGE = 86400000;
//    public static final int MAX_SEND_REPORTS = 5;
//    private static int MAX_TRACE_COUNT_LIMIT = 20;
//    private static final long MIN_TEMP_REPORT_AGE = 600000;
//    public static final long NATIVE_MAX_REPORT_SIZE = 512000;
//    public static final long PREALLOCATED_FILESIZE = 51200;
//    public static final String PREALLOCATED_REPORTFILE = "reportfile.prealloc";
//    public static final String REPORTFILE_EXTENSION = ".stacktrace";
//    public static final String SIGQUIT_DIR = "traces";
//    public static final long SIGQUIT_MAX_REPORT_SIZE = 122880;
//    public static final String TEMP_REPORTFILE_EXTENSION = ".temp_stacktrace";
//    private static final Pattern VERSION_CODE_REGEX = Pattern.compile("^\\d+-[a-zA-Z0-9_\\-]+-(\\d+)\\.(temp_stacktrace|stacktrace)$");
//    private static ErrorReporter mInstanceSingleton = null;
//    private static final String mInternalException = "ACRA_INTERNAL=java.lang.Exception: An exception occurred while trying to collect data about an ACRA internal error\n\tat com.parse.acra.ErrorReporter.handleException(ErrorReporter.java:810)\n\tat com.parse.acra.ErrorReporter.handleException(ErrorReporter.java:866)\n\tat com.parse.acra.ErrorReporter.uncaughtException(ErrorReporter.java:666)\n\tat java.lang.ThreadGroup.uncaughtException(ThreadGroup.java:693)\n\tat java.lang.ThreadGroup.uncaughtException(ThreadGroup.java:690)\n";
//    private static AtomicBoolean mProcessingCrash = new AtomicBoolean(false);
//    private final SimpleTraceLogger activityLogger = new SimpleTraceLogger(MAX_TRACE_COUNT_LIMIT);
//    private final Time mAppStartDate = new Time();
//    private String mAppVersionCode;
//    private String mAppVersionName;
//    private final Map<ReportField, String> mConstantFields = new HashMap();
//    private Context mContext;
//    private boolean mCurrentlyProcessingOOM = false;
//    private final Map<ReportField, String> mDeviceSpecificFields = new HashMap();
//    private Thread.UncaughtExceptionHandler mDfltExceptionHandler;
//    private FileProvider mFileProvider;
//    private boolean mHasNativeCrashDumpOnInit = false;
//    Map<String, String> mInstanceCustomParameters = new ConcurrentHashMap();
//    Map<String, CustomReportDataSupplier> mInstanceLazyCustomParameters = new ConcurrentHashMap();
//    private boolean mIsInternalBuild;
//    private LogBridge mLogBridge;
//    private long mMaxReportSize = 51200;
//    private PackageManagerWrapper mPackageManager;
//    private ArrayList<ReportSender> mReportSenders = new ArrayList<>();
//    private final Object mShouldContinueProcessingExceptionLock = new Object();
//    private volatile String mUserId;
//    private File preallocFile = null;
//    private String processNameByAms;
//    private boolean processNameByAmsReady;
//    private volatile boolean sendInMemoryReport = false;
//    private boolean usePreallocatedFile = false;
//
//    ErrorReporter() {
//    }
//
//    public enum CrashReportType {
//        ACRA_CRASH_REPORT(ErrorReporter.ACRA_DIRNAME, 51200, null, ErrorReporter.REPORTFILE_EXTENSION, ErrorReporter.TEMP_REPORTFILE_EXTENSION),
//        NATIVE_CRASH_REPORT(ErrorReporter.DUMP_DIR, ErrorReporter.NATIVE_MAX_REPORT_SIZE, ReportField.MINIDUMP, ErrorReporter.DUMPFILE_EXTENSION),
//        ANR_REPORT(ErrorReporter.SIGQUIT_DIR, ErrorReporter.SIGQUIT_MAX_REPORT_SIZE, ReportField.SIGQUIT, ErrorReporter.REPORTFILE_EXTENSION, ErrorReporter.TEMP_REPORTFILE_EXTENSION);
//
//        private final ReportField attachmentField;
//        private final long defaultMaxSize;
//        private final String directory;
//        private final String[] fileExtensions;
//
//        private CrashReportType(String directory2, long maxSize, ReportField attachmentField2, String... fileExtensions2) {
//            this.directory = directory2;
//            this.defaultMaxSize = maxSize;
//            this.attachmentField = attachmentField2;
//            this.fileExtensions = fileExtensions2;
//        }
//    }
//
//    /* access modifiers changed from: package-private */
//    public final class ReportsSenderWorker extends Thread {
//        private Throwable exception;
//        private CrashReportData mInMemoryReportToSend;
//        private final CrashReportType[] mReportTypesToSend;
//
//        public ReportsSenderWorker(ErrorReporter errorReporter, CrashReportData inMemoryReportToSend) {
//            this(new CrashReportType[0]);
//            this.mInMemoryReportToSend = inMemoryReportToSend;
//        }
//
//        public ReportsSenderWorker(CrashReportType... reportTypesToSend) {
//            this.exception = null;
//            this.mReportTypesToSend = reportTypesToSend;
//        }
//
//        public void run() {
//            PowerManager.WakeLock wakeLock = null;
//            try {
//                PowerManager.WakeLock wakeLock2 = acquireWakeLock();
//                if (this.mInMemoryReportToSend != null) {
//                    ErrorReporter.this.sendInMemoryReport(ErrorReporter.this.mContext, this.mInMemoryReportToSend);
//                } else {
//                    ErrorReporter.this.checkAndSendReports(ErrorReporter.this.mContext, this.mReportTypesToSend);
//                }
//                if (wakeLock2 != null && wakeLock2.isHeld()) {
//                    wakeLock2.release();
//                }
//            } catch (Throwable th) {
//                if (0 != 0 && wakeLock.isHeld()) {
//                    wakeLock.release();
//                }
//                throw th;
//            }
//        }
//
//        public Throwable getException() {
//            return this.exception;
//        }
//
//        private PowerManager.WakeLock acquireWakeLock() {
//            if (!new PackageManagerWrapper(ErrorReporter.this.mContext).hasPermission("android.permission.WAKE_LOCK")) {
//                return null;
//            }
//            PowerManager.WakeLock wakeLock = ((PowerManager) ErrorReporter.this.mContext.getSystemService("power")).newWakeLock(1, "crash reporting wakelock");
//            wakeLock.setReferenceCounted(false);
//            wakeLock.acquire();
//            return wakeLock;
//        }
//    }
//
//    public LogBridge getLogBridge() {
//        return this.mLogBridge;
//    }
//
//    public void setLogBridge(LogBridge bridge) {
//        this.mLogBridge = bridge;
//    }
//
//    public String getUserId() {
//        return this.mUserId;
//    }
//
//    public void setUserId(String userId) {
//        this.mUserId = userId;
//    }
//
//    public String putCustomData(String key, String value) {
//        if (value != null) {
//            return this.mInstanceCustomParameters.put(key, value);
//        }
//        return removeCustomData(key);
//    }
//
//    public String removeCustomData(String key) {
//        return this.mInstanceCustomParameters.remove(key);
//    }
//
//    public String getCustomData(String key) {
//        return this.mInstanceCustomParameters.get(key);
//    }
//
//    public void putLazyCustomData(String key, CustomReportDataSupplier valueSupplier) {
//        this.mInstanceLazyCustomParameters.put(key, valueSupplier);
//    }
//
//    public String dumpCustomDataToString(Map<String, String> extras, Throwable throwable) {
//        StringBuilder customInfo = new StringBuilder();
//        dumpCustomDataMap(customInfo, this.mInstanceCustomParameters);
//        if (extras != null) {
//            dumpCustomDataMap(customInfo, extras);
//        }
//        dumpLazyCustomDataMap(customInfo, this.mInstanceLazyCustomParameters, throwable);
//        return customInfo.toString();
//    }
//
//    private void dumpLazyCustomDataMap(StringBuilder sb, Map<String, CustomReportDataSupplier> params, Throwable throwable) {
//        for (Map.Entry<String, CustomReportDataSupplier> entry : params.entrySet()) {
//            String key = entry.getKey();
//            try {
//                String value = entry.getValue().getCustomData(throwable);
//                if (value != null) {
//                    dumpCustomDataEntry(sb, key, value);
//                }
//            } catch (Throwable th) {
//                Log.e(ACRA.LOG_TAG, "Caught throwable while getting custom report data", th);
//            }
//        }
//    }
//
//    private void dumpCustomDataMap(StringBuilder sb, Map<String, String> params) {
//        for (Map.Entry<String, String> entry : params.entrySet()) {
//            dumpCustomDataEntry(sb, entry.getKey(), entry.getValue());
//        }
//    }
//
//    private void dumpCustomDataEntry(StringBuilder sb, String key, String value) {
//        String key2;
//        String value2;
//        if (key != null) {
//            key2 = key.replace(CSVWriter.DEFAULT_LINE_END, "\\n");
//        } else {
//            key2 = null;
//        }
//        if (value != null) {
//            value2 = value.replace(CSVWriter.DEFAULT_LINE_END, "\\n");
//        } else {
//            value2 = null;
//        }
//        sb.append(key2).append(" = ").append(value2).append(CSVWriter.DEFAULT_LINE_END);
//    }
//
//    private String getProcessNameFromAmsOrNull() {
//        if (this.processNameByAmsReady) {
//            return this.processNameByAms;
//        }
//        this.processNameByAms = null;
//        int pid = Process.myPid();
//        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
//        if (am == null) {
//            return this.processNameByAms;
//        }
//        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
//        if (processes == null) {
//            return this.processNameByAms;
//        }
//        Iterator i$ = processes.iterator();
//        while (true) {
//            if (!i$.hasNext()) {
//                break;
//            }
//            ActivityManager.RunningAppProcessInfo rai = i$.next();
//            if (rai.pid == pid) {
//                this.processNameByAms = rai.processName;
//                break;
//            }
//        }
//        this.processNameByAmsReady = true;
//        return this.processNameByAms;
//    }
//
//    private void resetProcessNameByAmsCache() {
//        this.processNameByAms = null;
//        this.processNameByAmsReady = false;
//    }
//
//    private String getProcessNameFromAms() {
//        String processName = getProcessNameFromAmsOrNull();
//        if (processName == null) {
//            return "n/a";
//        }
//        return processName;
//    }
//
//    /* JADX WARNING: Removed duplicated region for block: B:11:0x0022 A[SYNTHETIC, Splitter:B:11:0x0022] */
//    /* JADX WARNING: Removed duplicated region for block: B:14:0x0027 A[ORIG_RETURN, RETURN, SYNTHETIC] */
//    /* JADX WARNING: Removed duplicated region for block: B:21:? A[RETURN, SYNTHETIC] */
//    /* Code decompiled incorrectly, please refer to instructions dump. */
//    private String getProcessName() {
//        /*
//            r7 = this;
//            java.lang.String r3 = r7.getProcessNameFromAmsOrNull()
//            if (r3 != 0) goto L_0x0025
//            r0 = 0
//            java.io.FileReader r4 = new java.io.FileReader     // Catch:{ IOException -> 0x002a }
//            java.lang.String r5 = "/proc/self/cmdline"
//            r4.<init>(r5)     // Catch:{ IOException -> 0x002a }
//            java.io.BufferedReader r1 = new java.io.BufferedReader     // Catch:{ IOException -> 0x002a }
//            r5 = 128(0x80, float:1.794E-43)
//            r1.<init>(r4, r5)     // Catch:{ IOException -> 0x002a }
//            java.lang.String r3 = r1.readLine()     // Catch:{ IOException -> 0x003c }
//            if (r3 == 0) goto L_0x001f
//            java.lang.String r3 = r3.trim()     // Catch:{ IOException -> 0x003c }
//        L_0x001f:
//            r0 = r1
//        L_0x0020:
//            if (r0 == 0) goto L_0x0025
//            r0.close()     // Catch:{ IOException -> 0x0033 }
//        L_0x0025:
//            if (r3 != 0) goto L_0x0029
//            java.lang.String r3 = ""
//        L_0x0029:
//            return r3
//        L_0x002a:
//            r2 = move-exception
//        L_0x002b:
//            java.lang.String r5 = "CrashReporting"
//            java.lang.String r6 = "Failed to get process name."
//            android.util.Log.e(r5, r6, r2)
//            goto L_0x0020
//        L_0x0033:
//            r2 = move-exception
//            java.lang.String r5 = "CrashReporting"
//            java.lang.String r6 = "Failed to close file."
//            android.util.Log.e(r5, r6, r2)
//            goto L_0x0025
//        L_0x003c:
//            r2 = move-exception
//            r0 = r1
//            goto L_0x002b
//        */
//        throw new UnsupportedOperationException("Method not decompiled: com.parse.ErrorReporter.getProcessName():java.lang.String");
//    }
//
//    private String getJailStatus() {
//        String buildTags = Build.TAGS;
//        if (buildTags != null && buildTags.contains("test-keys")) {
//            return "yes";
//        }
//        try {
//            if (new File("/system/app/Superuser.apk").exists()) {
//                return "yes";
//            }
//        } catch (Exception ex) {
//            Log.e(ACRA.LOG_TAG, "Failed to find Superuser.pak", ex);
//        }
//        Map<String, String> env = System.getenv();
//        if (env != null) {
//            String[] dirs = env.get("PATH").split(":");
//            int len$ = dirs.length;
//            for (int i$ = 0; i$ < len$; i$++) {
//                try {
//                    File suFile = new File(dirs[i$] + "/" + "su");
//                    if (suFile != null && suFile.exists()) {
//                        return "yes";
//                    }
//                } catch (Exception ex2) {
//                    Log.e(ACRA.LOG_TAG, "Failed to find su binary in the PATH", ex2);
//                }
//            }
//        }
//        return "no";
//    }
//
//    private long getProcessUptime() {
//        return Process.getElapsedCpuTime();
//    }
//
//    private long getDeviceUptime() {
//        return SystemClock.elapsedRealtime();
//    }
//
//    public static ErrorReporter getInstance() {
//        if (mInstanceSingleton == null) {
//            mInstanceSingleton = new ErrorReporter();
//        }
//        return mInstanceSingleton;
//    }
//
//    public void init(Context context, boolean isInternalBuild, FileProvider fileProvider) {
//        if (this.mDfltExceptionHandler == null) {
//            this.mDfltExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
//            this.mIsInternalBuild = isInternalBuild;
//            this.mContext = context;
//            this.mFileProvider = fileProvider;
//            PackageInfo pi = new PackageManagerWrapper(context).getPackageInfo();
//            if (pi != null) {
//                this.mAppVersionCode = Integer.toString(pi.versionCode);
//                this.mAppVersionName = pi.versionName != null ? pi.versionName : "not set";
//            }
//            this.mPackageManager = new PackageManagerWrapper(context);
//            String osVersion = System.getProperty("os.version");
//            boolean isCyanogenmod = osVersion != null ? osVersion.contains("cyanogenmod") : false;
//            this.mAppStartDate.setToNow();
//            try {
//                this.mConstantFields.put(ReportField.ANDROID_ID, Settings.Secure.getString(context.getContentResolver(), "android_id"));
//                this.mConstantFields.put(ReportField.APP_VERSION_CODE, this.mAppVersionCode);
//                this.mConstantFields.put(ReportField.APP_VERSION_NAME, this.mAppVersionName);
//                this.mConstantFields.put(ReportField.PACKAGE_NAME, context.getPackageName());
//                this.mConstantFields.put(ReportField.PHONE_MODEL, Build.MODEL);
//                this.mConstantFields.put(ReportField.ANDROID_VERSION, Build.VERSION.RELEASE);
//                this.mConstantFields.put(ReportField.OS_VERSION, osVersion);
//                this.mConstantFields.put(ReportField.IS_CYANOGENMOD, Boolean.toString(isCyanogenmod));
//                this.mConstantFields.put(ReportField.BRAND, Build.BRAND);
//                this.mConstantFields.put(ReportField.PRODUCT, Build.PRODUCT);
//                String absolutePath = "n/a";
//                File filesDir = context.getFilesDir();
//                if (filesDir != null) {
//                    absolutePath = filesDir.getAbsolutePath();
//                }
//                this.mConstantFields.put(ReportField.FILE_PATH, absolutePath);
//                if (Build.VERSION.SDK_INT >= 9) {
//                    this.mConstantFields.put(ReportField.SERIAL, Build.SERIAL);
//                    if (pi != null) {
//                        this.mConstantFields.put(ReportField.APP_INSTALL_TIME, formatTimestamp(pi.firstInstallTime));
//                        this.mConstantFields.put(ReportField.APP_UPGRADE_TIME, formatTimestamp(pi.lastUpdateTime));
//                    }
//                }
//            } catch (Exception e) {
//                Log.e(ACRA.LOG_TAG, "failed to install constants", e);
//            }
//            this.preallocFile = fileForName(this.mFileProvider, ACRA_DIRNAME, PREALLOCATED_REPORTFILE);
//            createPreallocatedReportFile();
//        }
//    }
//
//    private String formatTimestamp(long time) {
//        Time t = new Time();
//        t.set(time);
//        return t.format3339(false);
//    }
//
//    /* JADX WARNING: Removed duplicated region for block: B:20:0x0034 A[SYNTHETIC, Splitter:B:20:0x0034] */
//    /* JADX WARNING: Removed duplicated region for block: B:25:0x003d A[SYNTHETIC, Splitter:B:25:0x003d] */
//    /* JADX WARNING: Removed duplicated region for block: B:37:? A[RETURN, SYNTHETIC] */
//    /* Code decompiled incorrectly, please refer to instructions dump. */
//    private void createPreallocatedReportFile() {
//        /*
//            r10 = this;
//            r2 = 0
//            java.io.File r5 = r10.preallocFile     // Catch:{ IOException -> 0x002a }
//            boolean r5 = r5.exists()     // Catch:{ IOException -> 0x002a }
//            if (r5 != 0) goto L_0x0024
//            r5 = 10240(0x2800, float:1.4349E-41)
//            byte[] r0 = new byte[r5]     // Catch:{ IOException -> 0x002a }
//            java.io.FileOutputStream r3 = new java.io.FileOutputStream     // Catch:{ IOException -> 0x002a }
//            java.io.File r5 = r10.preallocFile     // Catch:{ IOException -> 0x002a }
//            r3.<init>(r5)     // Catch:{ IOException -> 0x002a }
//            r4 = 0
//        L_0x0015:
//            long r6 = (long) r4
//            r8 = 51200(0xc800, double:2.5296E-319)
//            int r5 = (r6 > r8 ? 1 : (r6 == r8 ? 0 : -1))
//            if (r5 >= 0) goto L_0x0023
//            r3.write(r0)     // Catch:{ IOException -> 0x0048, all -> 0x0045 }
//            int r5 = r0.length     // Catch:{ IOException -> 0x0048, all -> 0x0045 }
//            int r4 = r4 + r5
//            goto L_0x0015
//        L_0x0023:
//            r2 = r3
//        L_0x0024:
//            if (r2 == 0) goto L_0x0029
//            r2.close()     // Catch:{ IOException -> 0x0041 }
//        L_0x0029:
//            return
//        L_0x002a:
//            r1 = move-exception
//        L_0x002b:
//            java.lang.String r5 = "CrashReporting"
//            java.lang.String r6 = "Failed to pre-allocate crash report file"
//            android.util.Log.e(r5, r6, r1)     // Catch:{ all -> 0x003a }
//            if (r2 == 0) goto L_0x0029
//            r2.close()     // Catch:{ IOException -> 0x0038 }
//            goto L_0x0029
//        L_0x0038:
//            r5 = move-exception
//            goto L_0x0029
//        L_0x003a:
//            r5 = move-exception
//        L_0x003b:
//            if (r2 == 0) goto L_0x0040
//            r2.close()     // Catch:{ IOException -> 0x0043 }
//        L_0x0040:
//            throw r5
//        L_0x0041:
//            r5 = move-exception
//            goto L_0x0029
//        L_0x0043:
//            r6 = move-exception
//            goto L_0x0040
//        L_0x0045:
//            r5 = move-exception
//            r2 = r3
//            goto L_0x003b
//        L_0x0048:
//            r1 = move-exception
//            r2 = r3
//            goto L_0x002b
//        */
//        throw new UnsupportedOperationException("Method not decompiled: com.parse.ErrorReporter.createPreallocatedReportFile():void");
//    }
//
//    private static long getAvailableInternalMemorySize() {
//        try {
//            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
//            return ((long) stat.getAvailableBlocks()) * ((long) stat.getBlockSize());
//        } catch (Exception e) {
//            return -1;
//        }
//    }
//
//    private static long getTotalInternalMemorySize() {
//        try {
//            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
//            return ((long) stat.getBlockCount()) * ((long) stat.getBlockSize());
//        } catch (Exception e) {
//            return -1;
//        }
//    }
//
//    private void populateConstantDeviceData(CrashReportData crashReport, Writer writer) {
//        for (Map.Entry<ReportField, String> entry : getConstantDeviceData().entrySet()) {
//            put(entry.getKey(), entry.getValue(), crashReport, writer);
//        }
//    }
//
//    private Map<ReportField, String> getConstantDeviceData() {
//        Map<ReportField, String> map;
//        String deviceId;
//        synchronized (this.mDeviceSpecificFields) {
//            if (this.mDeviceSpecificFields.isEmpty()) {
//                this.mDeviceSpecificFields.put(ReportField.BUILD, ReflectionCollector.collectConstants(Build.class));
//                this.mDeviceSpecificFields.put(ReportField.JAIL_BROKEN, getJailStatus());
//                this.mDeviceSpecificFields.put(ReportField.INSTALLATION_ID, Installation.m12id(this.mFileProvider));
//                this.mDeviceSpecificFields.put(ReportField.TOTAL_MEM_SIZE, Long.toString(getTotalInternalMemorySize()));
//                if (this.mPackageManager.hasPermission("android.permission.READ_PHONE_STATE") && (deviceId = ((TelephonyManager) this.mContext.getSystemService("phone")).getDeviceId()) != null) {
//                    this.mDeviceSpecificFields.put(ReportField.DEVICE_ID, deviceId);
//                }
//                this.mDeviceSpecificFields.put(ReportField.DISPLAY, toString(((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay()));
//                this.mDeviceSpecificFields.put(ReportField.ENVIRONMENT, ReflectionCollector.collectStaticGettersResults(Environment.class));
//                this.mDeviceSpecificFields.put(ReportField.DEVICE_FEATURES, DeviceFeaturesCollector.getFeatures(this.mContext));
//                this.mDeviceSpecificFields.put(ReportField.SETTINGS_SYSTEM, SettingsCollector.collectSystemSettings(this.mContext));
//                this.mDeviceSpecificFields.put(ReportField.SETTINGS_SECURE, SettingsCollector.collectSecureSettings(this.mContext));
//                if (Build.VERSION.SDK_INT >= 19) {
//                    this.mDeviceSpecificFields.put(ReportField.IS_LOW_RAM_DEVICE, Boolean.toString(((ActivityManager) this.mContext.getSystemService("activity")).isLowRamDevice()));
//                }
//                this.mDeviceSpecificFields.put(ReportField.ANDROID_RUNTIME, getAndroidRuntime());
//            }
//            map = this.mDeviceSpecificFields;
//        }
//        return map;
//    }
//
//    private String getAndroidRuntime() {
//        if (Build.VERSION.SDK_INT < 19) {
//            return ANDROID_RUNTIME_DALVIK;
//        }
//        String bootClassPath = System.getProperty(JAVA_BOOT_CLASS_PATH);
//        if (bootClassPath != null) {
//            if (bootClassPath.contains(KNOWN_ART_JAR)) {
//                return ANDROID_RUNTIME_ART;
//            }
//            if (bootClassPath.contains(KNOWN_DALVIK_JAR)) {
//                return ANDROID_RUNTIME_DALVIK;
//            }
//        }
//        return ANDROID_RUNTIME_UNKNOWN;
//    }
//
//    private void retrieveCrashTimeData(Context context, Throwable e, ReportField[] fields, CrashReportData crashReport, Writer writer) throws Exception {
//        ProcFileReader.OpenFDLimits limits;
//        String activityLogDump;
//        List<ReportField> fieldsList = Arrays.asList(fields);
//        if (fieldsList.contains(ReportField.REPORT_ID)) {
//            put(ReportField.REPORT_ID, UUID.randomUUID().toString(), crashReport, writer);
//        }
//        if (fieldsList.contains(ReportField.PROCESS_NAME)) {
//            put(ReportField.PROCESS_NAME, getProcessName(), crashReport, writer);
//        }
//        if (fieldsList.contains(ReportField.USER_APP_START_DATE)) {
//            put(ReportField.USER_APP_START_DATE, this.mAppStartDate.format3339(false), crashReport, writer);
//        }
//        if (fieldsList.contains(ReportField.PROCESS_UPTIME)) {
//            put(ReportField.PROCESS_UPTIME, Long.toString(getProcessUptime()), crashReport, writer);
//        }
//        if (fieldsList.contains(ReportField.DEVICE_UPTIME)) {
//            put(ReportField.DEVICE_UPTIME, Long.toString(getDeviceUptime()), crashReport, writer);
//        }
//        if (fieldsList.contains(ReportField.CRASH_CONFIGURATION)) {
//            put(ReportField.CRASH_CONFIGURATION, ConfigurationInspector.toString(context.getResources().getConfiguration()), crashReport, writer);
//        }
//        if (fieldsList.contains(ReportField.AVAILABLE_MEM_SIZE)) {
//            put(ReportField.AVAILABLE_MEM_SIZE, Long.toString(getAvailableInternalMemorySize()), crashReport, writer);
//        }
//        if (fieldsList.contains(ReportField.DUMPSYS_MEMINFO)) {
//            put(ReportField.DUMPSYS_MEMINFO, DumpSysCollector.collectMemInfo(context), crashReport, writer);
//        }
//        if (fieldsList.contains(ReportField.USER_CRASH_DATE)) {
//            Time curDate = new Time();
//            curDate.setToNow();
//            put(ReportField.USER_CRASH_DATE, curDate.format3339(false), crashReport, writer);
//        }
//        if (fieldsList.contains(ReportField.ACTIVITY_LOG)) {
//            if (e instanceof OutOfMemoryError) {
//                activityLogDump = this.activityLogger.toString();
//            } else {
//                activityLogDump = this.activityLogger.toString(DEFAULT_TRACE_COUNT_LIMIT);
//            }
//            put(ReportField.ACTIVITY_LOG, activityLogDump, crashReport, writer);
//        }
//        if (fieldsList.contains(ReportField.PROCESS_NAME_BY_AMS)) {
//            put(ReportField.PROCESS_NAME_BY_AMS, getProcessNameFromAms(), crashReport, writer);
//        }
//        resetProcessNameByAmsCache();
//        if (fieldsList.contains(ReportField.OPEN_FD_COUNT)) {
//            put(ReportField.OPEN_FD_COUNT, String.valueOf(ProcFileReader.getOpenFDCount()), crashReport, writer);
//        }
//        if ((fieldsList.contains(ReportField.OPEN_FD_SOFT_LIMIT) || fieldsList.contains(ReportField.OPEN_FD_HARD_LIMIT)) && (limits = ProcFileReader.getOpenFDLimits()) != null) {
//            if (fieldsList.contains(ReportField.OPEN_FD_SOFT_LIMIT)) {
//                put(ReportField.OPEN_FD_SOFT_LIMIT, limits.softLimit, crashReport, writer);
//            }
//            if (fieldsList.contains(ReportField.OPEN_FD_HARD_LIMIT)) {
//                put(ReportField.OPEN_FD_HARD_LIMIT, limits.hardLimit, crashReport, writer);
//            }
//        }
//        if (Build.VERSION.SDK_INT >= 16 && this.mIsInternalBuild) {
//            if (fieldsList.contains(ReportField.LOGCAT)) {
//                put(ReportField.LOGCAT, LogCatCollector.collectLogCat(null), crashReport, writer);
//            }
//            if (fieldsList.contains(ReportField.EVENTSLOG)) {
//                put(ReportField.EVENTSLOG, LogCatCollector.collectLogCat("events"), crashReport, writer);
//            }
//            if (fieldsList.contains(ReportField.RADIOLOG)) {
//                put(ReportField.RADIOLOG, LogCatCollector.collectLogCat("radio"), crashReport, writer);
//            }
//            if (fieldsList.contains(ReportField.DROPBOX)) {
//                put(ReportField.DROPBOX, DropBoxCollector.read(this.mContext, ACRA.getConfig().additionalDropBoxTags()), crashReport, writer);
//            }
//        }
//        if (fieldsList.contains(ReportField.LARGE_MEM_HEAP) && Build.VERSION.SDK_INT >= 11) {
//            put(ReportField.LARGE_MEM_HEAP, DumpSysCollector.collectLargerMemoryInfo(context), crashReport, writer);
//        }
//    }
//
//    private static String toString(Display display) {
//        DisplayMetrics metrics = new DisplayMetrics();
//        display.getMetrics(metrics);
//        StringBuilder result = new StringBuilder();
//        result.append("width=").append(display.getWidth()).append('\n').append("height=").append(display.getHeight()).append('\n').append("pixelFormat=").append(display.getPixelFormat()).append('\n').append("refreshRate=").append(display.getRefreshRate()).append("fps").append('\n').append("metrics.density=x").append(metrics.density).append('\n').append("metrics.scaledDensity=x").append(metrics.scaledDensity).append('\n').append("metrics.widthPixels=").append(metrics.widthPixels).append('\n').append("metrics.heightPixels=").append(metrics.heightPixels).append('\n').append("metrics.xdpi=").append(metrics.xdpi).append('\n').append("metrics.ydpi=").append(metrics.ydpi);
//        return result.toString();
//    }
//
//    public void uncaughtException(Thread t, Throwable e) {
//        Log.e(ACRA.LOG_TAG, "ParseCrashReporting caught a " + e.getClass().getSimpleName() + " exception for " + this.mContext.getPackageName() + ". Building report.");
//        this.usePreallocatedFile = true;
//        boolean wasProcessingCrash = mProcessingCrash.getAndSet(true);
//        Map<String, String> extra = null;
//        try {
//            Map<String, String> extra2 = new HashMap<>();
//            try {
//                extra2.put(IS_PROCESSING_ANOTHER_EXCEPTION, String.valueOf(wasProcessingCrash));
//                extra = extra2;
//            } catch (OutOfMemoryError e2) {
//                extra = extra2;
//            }
//        } catch (OutOfMemoryError e3) {
//        }
//        ReportsSenderWorker worker = handleException(e, extra);
//        if (worker != null) {
//            while (worker.isAlive()) {
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e1) {
//                    Log.e(ACRA.LOG_TAG, "Error : ", e1);
//                }
//            }
//            Throwable workerException = worker.getException();
//            if (workerException != null) {
//                Log.e(ACRA.LOG_TAG, "ReportsWorkerSender failed with exception", workerException);
//                handleExceptionInternal(workerException, extra, null, getReportFieldsForException(e), false);
//            }
//        }
//        if (this.mDfltExceptionHandler != null) {
//            this.mDfltExceptionHandler.uncaughtException(t, e);
//        }
//    }
//
//    private void writeToLogBridge(String tag, String message, Throwable t, String overrideStackTrace) {
//        LogBridge logBridge = getLogBridge();
//        if (logBridge != null) {
//            if (overrideStackTrace != null) {
//                logBridge.log(tag, message + CSVWriter.DEFAULT_LINE_END + overrideStackTrace, null);
//            } else {
//                logBridge.log(tag, message, t);
//            }
//        } else if (overrideStackTrace != null) {
//            Log.e(tag, message + CSVWriter.DEFAULT_LINE_END + overrideStackTrace);
//        } else {
//            Log.e(tag, message, t);
//        }
//    }
//
//    private String throwableToString(Throwable e) {
//        if (e == null) {
//            e = new Exception("Report requested by developer");
//        }
//        Writer result = new StringWriter();
//        PrintWriter printWriter = new PrintWriter(result);
//        e.printStackTrace(printWriter);
//        printWriter.close();
//        return result.toString();
//    }
//
//    private void gatherCrashData(String stackTrace, Throwable e, ReportField[] fields, CrashReportData crashReport, Writer w, Map<String, String> extras) throws Exception {
//        if (fields == null) {
//            fields = ACRA.MINIMAL_REPORT_FIELDS;
//        }
//        put(ReportField.UID, getUserId(), crashReport, w);
//        put(ReportField.STACK_TRACE, stackTrace, crashReport, w);
//        for (Map.Entry<ReportField, String> entry : this.mConstantFields.entrySet()) {
//            put(entry.getKey(), entry.getValue(), crashReport, w);
//        }
//        retrieveCrashTimeData(this.mContext, e, fields, crashReport, w);
//        populateConstantDeviceData(crashReport, w);
//        put(ReportField.CUSTOM_DATA, dumpCustomDataToString(extras, e), crashReport, w);
//    }
//
//    private void put(ReportField key, String value, CrashReportData crashReport, Writer writer) {
//        try {
//            if (this.sendInMemoryReport) {
//                writer = null;
//            }
//            crashReport.put(key, value, writer);
//        } catch (IOException e) {
//            this.sendInMemoryReport = true;
//        }
//    }
//
//    public void writeReportToStream(Throwable e, OutputStream os) throws Exception {
//        gatherCrashData(throwableToString(e), e, ACRA.ALL_CRASH_REPORT_FIELDS, new CrashReportData(), CrashReportData.getWriter(os), null);
//    }
//
//    public ReportsSenderWorker handleException(Throwable e) {
//        return handleException(e, null);
//    }
//
//    public ReportsSenderWorker handleException(Throwable e, Map<String, String> extras) {
//        return handleExceptionInternal(e, extras, null, getReportFieldsForException(e), !(e instanceof OutOfMemoryError));
//    }
//
//    /* JADX WARNING: Removed duplicated region for block: B:18:0x00c8 A[Catch:{ Exception -> 0x0128, all -> 0x017e }] */
//    /* JADX WARNING: Removed duplicated region for block: B:21:0x00db A[SYNTHETIC, Splitter:B:21:0x00db] */
//    /* JADX WARNING: Removed duplicated region for block: B:24:0x00f2  */
//    /* JADX WARNING: Removed duplicated region for block: B:61:0x01bc  */
//    /* Code decompiled incorrectly, please refer to instructions dump. */
//    private com.parse.ErrorReporter.ReportsSenderWorker handleExceptionInternal(Throwable r25, Map<String, String> r26, String r27, com.parse.ReportField[] r28, boolean r29) {
//        /*
//        // Method dump skipped, instructions count: 460
//        */
//        throw new UnsupportedOperationException("Method not decompiled: com.parse.ErrorReporter.handleExceptionInternal(java.lang.Throwable, java.util.Map, java.lang.String, com.parse.ReportField[], boolean):com.parse.ErrorReporter$ReportsSenderWorker");
//    }
//
//    public ReportsSenderWorker handleException(Throwable e, String stackTrace, Map<String, String> extras) {
//        return handleExceptionInternal(e, extras, stackTrace, getReportFieldsForException(e), true);
//    }
//
//    public void handleExceptionWithCustomFields(Exception e, Map<String, String> data, ReportField[] fields) {
//        handleExceptionInternal(e, data, null, fields, true);
//    }
//
//    private void sendCrashReport(CrashReportData errorContent) throws ReportSenderException {
//        boolean sentAtLeastOnce = false;
//        Iterator i$ = this.mReportSenders.iterator();
//        while (i$.hasNext()) {
//            ReportSender sender = i$.next();
//            try {
//                sender.send(errorContent);
//                sentAtLeastOnce = true;
//            } catch (ReportSenderException e) {
//                if (!sentAtLeastOnce) {
//                    throw e;
//                }
//                Log.w(ACRA.LOG_TAG, "ReportSender of class " + sender.getClass().getName() + " failed but other senders completed their task. " + "ParseCrashReporting will not send this report again.");
//            }
//        }
//    }
//
//    private String genCrashReportFileName(Class cause, String fileExtension) {
//        return Long.toString(System.currentTimeMillis()) + "-" + cause.getSimpleName() + (this.mAppVersionCode != null ? "-" + this.mAppVersionCode : "") + fileExtension;
//    }
//
//    public String[] getCrashReportFilesList(String path, final String... extensions) {
//        if (this.mContext == null) {
//            Log.e(ACRA.LOG_TAG, "Trying to get crash reports but crash reporting is not initialized.");
//            return new String[0];
//        }
//        File dir = this.mFileProvider.getFile(path);
//        if (dir != null) {
//            Log.d(ACRA.LOG_TAG, "Looking for error files in " + dir.getAbsolutePath());
//            String[] result = dir.list(new FilenameFilter() {
//                /* class com.parse.ErrorReporter.C03081 */
//
//                public boolean accept(File dir, String name) {
//                    for (String extension : extensions) {
//                        if (name.endsWith(extension)) {
//                            return true;
//                        }
//                    }
//                    return false;
//                }
//            });
//            if (result == null) {
//                return new String[0];
//            }
//            return result;
//        }
//        Log.w(ACRA.LOG_TAG, "Application files directory does not exist! The application may not be installed correctly. Please try reinstalling.");
//        return new String[0];
//    }
//
//    /* access modifiers changed from: package-private */
//    public synchronized void checkAndSendReports(Context context, CrashReportType... types) {
//        Log.d(ACRA.LOG_TAG, "#checkAndSendReports - start");
//        for (CrashReportType reportType : types) {
//            if (CrashReportType.ACRA_CRASH_REPORT == reportType) {
//                checkAndSendAcraReports(context);
//            } else {
//                checkAndSendCrashAttachments(context, reportType);
//            }
//        }
//        Log.d(ACRA.LOG_TAG, "#checkAndSendReports - finish");
//    }
//
//    private void checkAndSendAcraReports(Context context) {
//        String[] reportFiles = getCrashReportFilesList(ACRA_DIRNAME, REPORTFILE_EXTENSION, TEMP_REPORTFILE_EXTENSION);
//        Arrays.sort(reportFiles);
//        int reportsSentCount = 0;
//        String uploadedByProcess = getProcessNameFromAms();
//        for (String curFileName : reportFiles) {
//            if (reportsSentCount >= 5) {
//                deleteFile(ACRA_DIRNAME, curFileName);
//            } else {
//                Log.d(ACRA.LOG_TAG, "Loading file " + curFileName);
//                try {
//                    CrashReportData previousCrashReport = loadAcraCrashReport(context, curFileName);
//                    if (previousCrashReport != null) {
//                        previousCrashReport.put((Enum) ReportField.ACRA_REPORT_FILENAME, (Object) curFileName);
//                        previousCrashReport.put((Enum) ReportField.UPLOADED_BY_PROCESS, (Object) uploadedByProcess);
//                        Log.i(ACRA.LOG_TAG, "Sending file " + curFileName);
//                        sendCrashReport(previousCrashReport);
//                        deleteFile(ACRA_DIRNAME, curFileName);
//                    }
//                    reportsSentCount++;
//                } catch (RuntimeException e) {
//                    Log.e(ACRA.LOG_TAG, "Failed to send crash reports", e);
//                    deleteFile(ACRA_DIRNAME, curFileName);
//                    return;
//                } catch (IOException e2) {
//                    Log.e(ACRA.LOG_TAG, "Failed to load crash report for " + curFileName, e2);
//                    deleteFile(ACRA_DIRNAME, curFileName);
//                    return;
//                } catch (ReportSenderException e3) {
//                    Log.e(ACRA.LOG_TAG, "Failed to send crash report for " + curFileName, e3);
//                    return;
//                }
//            }
//        }
//    }
//
//    /* access modifiers changed from: private */
//    public class CrashAttachmentException extends Throwable {
//        private CrashAttachmentException() {
//        }
//    }
//
//    private int checkAndSendCrashAttachments(Context context, CrashReportType type) {
//        Log.d(ACRA.LOG_TAG, "#checkAndSendCrashAttachments - start");
//        int dumpsSend = 0;
//        String[] dumpFiles = getCrashReportFilesList(type.directory, type.fileExtensions);
//        if (dumpFiles != null && dumpFiles.length > 0) {
//            Arrays.sort(dumpFiles);
//            CrashReportData tempCrashReportData = new CrashReportData();
//            try {
//                gatherCrashData(CRASH_ATTACHMENT_DUMMY_STACKTRACE, new CrashAttachmentException(), ACRA.ALL_CRASH_REPORT_FIELDS, tempCrashReportData, null, null);
//            } catch (Exception e) {
//                put(ReportField.REPORT_LOAD_THROW, "retrieve exception: " + e.getMessage(), tempCrashReportData, null);
//            }
//            int len$ = dumpFiles.length;
//            for (int i$ = 0; i$ < len$; i$++) {
//                String fname = dumpFiles[i$];
//                if (dumpsSend >= 5) {
//                    deleteFile(DUMP_DIR, fname);
//                } else {
//                    try {
//                        CrashReportData reportData = loadCrashAttachment(context, fname, type);
//                        String attachment = "load failed";
//                        if (reportData != null) {
//                            attachment = (String) reportData.get(type.attachmentField);
//                        }
//                        tempCrashReportData.put(ReportField.REPORT_ID, fname.substring(0, fname.lastIndexOf(46)), null);
//                        tempCrashReportData.put(type.attachmentField, attachment, null);
//                        tempCrashReportData.put(ReportField.EXCEPTION_CAUSE, CRASH_ATTACHMENT_DUMMY_STACKTRACE, null);
//                        sendCrashReport(tempCrashReportData);
//                        deleteFile(type.directory, fname);
//                        dumpsSend++;
//                    } catch (ReportSenderException e2) {
//                        Log.e(ACRA.LOG_TAG, "Failed to send crash attachment report " + fname, e2);
//                    } catch (Throwable e3) {
//                        Log.e(ACRA.LOG_TAG, "Failed on crash attachment file " + fname, e3);
//                        deleteFile(type.directory, fname);
//                    }
//                }
//            }
//        }
//        Log.d(ACRA.LOG_TAG, "#checkAndSendCrashAttachments - finish, sent: " + Integer.toString(dumpsSend));
//        return dumpsSend;
//    }
//
//    /* access modifiers changed from: package-private */
//    public synchronized void sendInMemoryReport(Context context, CrashReportData crashReport) {
//        File reportFile;
//        Log.i(ACRA.LOG_TAG, "Sending in-memory report");
//        try {
//            crashReport.put((Enum) ReportField.UPLOADED_BY_PROCESS, (Object) getProcessNameFromAms());
//            sendCrashReport(crashReport);
//            String reportName = (String) crashReport.get(ReportField.ACRA_REPORT_FILENAME);
//            if (!(reportName == null || (reportFile = fileForName(this.mFileProvider, ACRA_DIRNAME, reportName)) == null)) {
//                reportFile.delete();
//            }
//        } catch (Exception e) {
//            Log.e(ACRA.LOG_TAG, "Failed to send in-memory crash report: ", e);
//        }
//    }
//
//    private CrashReportData loadAcraCrashReport(Context context, String fileName) throws IOException {
//        return loadCrashReport(context, fileName, CrashReportType.ACRA_CRASH_REPORT, this.mMaxReportSize);
//    }
//
//    private CrashReportData loadCrashAttachment(Context context, String fileName, CrashReportType type) throws IOException {
//        return loadCrashReport(context, fileName, type, type.defaultMaxSize);
//    }
//
//    private CrashReportData loadCrashReport(Context context, String fileName, CrashReportType crashReportType, long maxSize) throws IOException {
//        CrashReportData crashReport = new CrashReportData();
//        File rptfp = fileForName(this.mFileProvider, crashReportType.directory, fileName);
//        if (System.currentTimeMillis() - rptfp.lastModified() > MAX_REPORT_AGE) {
//            Log.w(ACRA.LOG_TAG, "crash report " + fileName + " was too old; deleted");
//            deleteFile(crashReportType.directory, fileName);
//            return null;
//        } else if (fileName.endsWith(TEMP_REPORTFILE_EXTENSION) && System.currentTimeMillis() - rptfp.lastModified() < MIN_TEMP_REPORT_AGE) {
//            Log.w(ACRA.LOG_TAG, "temp file " + fileName + " is too recent; skipping");
//            return null;
//        } else if (rptfp.length() > maxSize) {
//            Log.w(ACRA.LOG_TAG, "" + rptfp.length() + "-byte crash report " + fileName + " exceeded max size of " + maxSize + " bytes; deleted");
//            deleteFile(crashReportType.directory, fileName);
//            return null;
//        } else {
//            FileInputStream input = new FileInputStream(rptfp);
//            boolean closed = false;
//            try {
//                if (crashReportType == CrashReportType.ACRA_CRASH_REPORT) {
//                    crashReport.load(input);
//                } else {
//                    crashReport.put((Enum) crashReportType.attachmentField, (Object) loadAttachment(input, (int) rptfp.length()));
//                }
//                if (0 == 0) {
//                    input.close();
//                }
//            } catch (Throwable th) {
//                if (!closed) {
//                    input.close();
//                }
//                throw th;
//            }
//            crashReport.put((Enum) ReportField.ACRA_REPORT_FILENAME, (Object) fileName);
//            backfillCrashReportData(crashReport);
//            return crashReport;
//        }
//    }
//
//    /* access modifiers changed from: package-private */
//    public void backfillCrashReportData(CrashReportData crashReport) {
//        boolean hasAppBeenUpgraded = !parseVersionCodeFromFileName(crashReport.getProperty(ReportField.ACRA_REPORT_FILENAME)).equals(this.mAppVersionCode);
//        String reportID = (String) crashReport.get(ReportField.REPORT_ID);
//        if (reportID == null || reportID.length() == 0) {
//            for (Map.Entry<ReportField, String> e : this.mConstantFields.entrySet()) {
//                if (e.getKey().equals(ReportField.APP_VERSION_NAME)) {
//                    if (!hasAppBeenUpgraded) {
//                        crashReport.put((Enum) e.getKey(), (Object) e.getValue());
//                    }
//                } else if (crashReport.get(e.getKey()) == null) {
//                    crashReport.put((Enum) e.getKey(), (Object) e.getValue());
//                }
//            }
//        }
//        String currentUserId = getUserId();
//        String previousUid = (String) crashReport.get(ReportField.UID);
//        if (!TextUtils.isEmpty(currentUserId) && TextUtils.isEmpty(previousUid)) {
//            crashReport.put((Enum) ReportField.UID, (Object) currentUserId);
//        }
//    }
//
//    public String parseVersionCodeFromFileName(String fileName) {
//        if (fileName != null) {
//            Matcher matcher = VERSION_CODE_REGEX.matcher(fileName);
//            if (matcher.matches()) {
//                return matcher.group(1);
//            }
//        }
//        return "";
//    }
//
//    /* JADX WARNING: Removed duplicated region for block: B:18:0x003d  */
//    /* Code decompiled incorrectly, please refer to instructions dump. */
//    private String loadAttachment(java.io.InputStream r9, int r10) throws IOException {
//        /*
//            r8 = this;
//            r5 = 0
//            r4 = 0
//            byte[] r0 = new byte[r10]
//        L_0x0004:
//            int r6 = r10 - r5
//            if (r6 <= 0) goto L_0x0011
//            int r6 = r10 - r5
//            int r4 = r9.read(r0, r5, r6)
//            r6 = -1
//            if (r4 != r6) goto L_0x0016
//        L_0x0011:
//            if (r4 != 0) goto L_0x0018
//            java.lang.String r6 = ""
//        L_0x0015:
//            return r6
//        L_0x0016:
//            int r5 = r5 + r4
//            goto L_0x0004
//        L_0x0018:
//            java.io.ByteArrayOutputStream r1 = new java.io.ByteArrayOutputStream
//            r1.<init>()
//            r2 = 0
//            java.util.zip.GZIPOutputStream r3 = new java.util.zip.GZIPOutputStream     // Catch:{ all -> 0x003a }
//            r3.<init>(r1)     // Catch:{ all -> 0x003a }
//            r6 = 0
//            int r7 = r0.length     // Catch:{ all -> 0x0041 }
//            r3.write(r0, r6, r7)     // Catch:{ all -> 0x0041 }
//            r3.finish()     // Catch:{ all -> 0x0041 }
//            byte[] r6 = r1.toByteArray()     // Catch:{ all -> 0x0041 }
//            r7 = 0
//            java.lang.String r6 = android.util.Base64.encodeToString(r6, r7)     // Catch:{ all -> 0x0041 }
//            if (r3 == 0) goto L_0x0015
//            r3.close()
//            goto L_0x0015
//        L_0x003a:
//            r6 = move-exception
//        L_0x003b:
//            if (r2 == 0) goto L_0x0040
//            r2.close()
//        L_0x0040:
//            throw r6
//        L_0x0041:
//            r6 = move-exception
//            r2 = r3
//            goto L_0x003b
//        */
//        throw new UnsupportedOperationException("Method not decompiled: com.parse.ErrorReporter.loadAttachment(java.io.InputStream, int):java.lang.String");
//    }
//
//    private static File fileForName(FileProvider fileProvider, String path, String fileName) {
//        return new File(fileProvider.getFile(path), fileName);
//    }
//
//    private void deleteFile(String path, String fileName) {
//        if (!fileForName(this.mFileProvider, path, fileName).delete()) {
//            Log.w(ACRA.LOG_TAG, "Could not delete error report : " + fileName);
//        }
//    }
//
//    public ReportsSenderWorker checkReportsOnApplicationStart() {
//        String[] filesList = getCrashReportFilesList(ACRA_DIRNAME, REPORTFILE_EXTENSION);
//        String[] nativeCrashFileList = getCrashReportFilesList(DUMP_DIR, DUMPFILE_EXTENSION);
//        if ((filesList == null || filesList.length <= 0) && (nativeCrashFileList == null || nativeCrashFileList.length <= 0)) {
//            return null;
//        }
//        Log.v(ACRA.LOG_TAG, "About to start ReportSenderWorker from #checkReportOnApplicationStart");
//        if (nativeCrashFileList != null && nativeCrashFileList.length > 0) {
//            this.mHasNativeCrashDumpOnInit = true;
//        }
//        return checkReportsOfType(ALL_REPORT_TYPES);
//    }
//
//    public ReportsSenderWorker checkReportsOfType(CrashReportType... types) {
//        ReportsSenderWorker worker = new ReportsSenderWorker(types);
//        worker.start();
//        return worker;
//    }
//
//    public boolean isNativeCrashedOnPreviousRun() {
//        return this.mHasNativeCrashDumpOnInit;
//    }
//
//    public void addReportSender(ReportSender sender) {
//        this.mReportSenders.add(sender);
//    }
//
//    public void removeAllReportSenders() {
//        this.mReportSenders.clear();
//    }
//
//    public void setMaxReportSize(long size) {
//        this.mMaxReportSize = size;
//    }
//
//    public void setReportSender(ReportSender sender) {
//        removeAllReportSenders();
//        addReportSender(sender);
//    }
//
//    public void registerActivity(String activityName) {
//        if (activityName != null) {
//            this.activityLogger.append(activityName);
//        }
//    }
//
//    private ReportField[] getReportFieldsForException(Throwable e) {
//        return e instanceof OutOfMemoryError ? ACRA.MINIMAL_REPORT_FIELDS : ACRA.ALL_CRASH_REPORT_FIELDS;
//    }
//
//    /* access modifiers changed from: package-private */
//    public Throwable getMostSignificantCause(Throwable e) {
//        if (e instanceof NonCrashException) {
//            return e;
//        }
//        Throwable cause = e;
//        while (cause.getCause() != null) {
//            cause = cause.getCause();
//        }
//        return cause;
//    }
//
//    private boolean shouldContinueProcessingException(Throwable t) {
//        boolean z = true;
//        synchronized (this.mShouldContinueProcessingExceptionLock) {
//            if (this.mCurrentlyProcessingOOM) {
//                z = false;
//            } else if (t instanceof OutOfMemoryError) {
//                this.mCurrentlyProcessingOOM = true;
//            }
//        }
//        return z;
//    }
//}
