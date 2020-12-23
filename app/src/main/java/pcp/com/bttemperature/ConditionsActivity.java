package pcp.com.bttemperature;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.RectRegion;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import org.joda.time.DateTimeZone;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import pcp.com.bttemperature.ambientDevice.AmbientDevice;
import pcp.com.bttemperature.ambientDevice.AmbientDeviceManager;
import pcp.com.bttemperature.ble.AmbientDeviceService;
import pcp.com.bttemperature.controls.LightGraph;
import pcp.com.bttemperature.controls.SelectiveViewPager;
import pcp.com.bttemperature.database.ConditionDatabaseHelper;
import pcp.com.bttemperature.database.ConditionObject;
import pcp.com.bttemperature.database.SQLiteCursorLoader;
import pcp.com.bttemperature.utilities.CelaerActivity;
import pcp.com.bttemperature.utilities.MyApplication;

public class ConditionsActivity extends CelaerActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final int LOADER_ALL = 0;
    public static final int LOADER_BETWEEN = 3;
    public static final int LOADER_GREATER_THAN = 1;
    public static final int LOADER_LESS_THAN = 2;
    private static final String TAG = ConditionsActivity.class.getSimpleName();
    private static ConditionsPagerAdapter mAdapter;
    private static float mAverageHumidity;
    private static float mAverageTemp;
    private static AmbientDevice mCurrentAmbientDevice;
    private static boolean mLoading = true;
    private static float mMaxHumidity;
    private static float mMaxTemp;
    private static float mMinHumidity;
    private static float mMinTemp;
    private static int mOffset = 0;
    private static int mSelectedPageIndex;
    private static int mSelectedZoom = 1;
    private static SelectiveViewPager mViewPager;
    private static XYSeries mXYSeriesHumidity;
    private static XYSeries mXYSeriesTemp;
    private static float maxY;
    private static float maxYHumidity;
    private static float minY;
    private static float minYHumidity;
    private static long timestamp1;
    private static long timestamp2;
    private static ToggleButton toggle0;
    private static ToggleButton toggle1;
    private static ToggleButton toggle2;
    private static ToggleButton toggle3;
    private static ToggleButton toggle4;
    private static ToggleButton toggle5;
    private static ArrayList<Number> valuesHumidity;
    private static ArrayList<Number> valuesHumidityMaster;
    private static ArrayList<Number> valuesLight;
    private static ArrayList<Number> valuesLightMaster;
    private static ArrayList<Number> valuesTemp;
    private static ArrayList<Number> valuesTempMaster;
    private static ArrayList<Number> xAxisHumidity;
    private static ArrayList<Number> xAxisLight;
    private static ArrayList<Number> xAxisMaster;
    private static ArrayList<Number> xAxisTemp;
    private Handler handler;
    private AmbientDeviceService mAmbientDeviceService;
    private int mConditionsReceived;
    private View mCursorView;
    private TextView mCursorViewDate;
    private TextView mCursorViewHumidity;
    private TextView mCursorViewLight;
    private TextView mCursorViewTemp;
    private int mDataPacketsReceived;
    private String mDateLabelString = "";
    private boolean mDisplayCursor;
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        /* class com.celaer.android.ambient.ConditionsActivity.C02368 */

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AmbientDeviceService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(ConditionsActivity.TAG, "Broadcast received: CONNECTED");
                ConditionsActivity.this.getWindow().addFlags(128);
                ConditionsActivity.this.mProgressDialog.setTitle((CharSequence) null);
                ConditionsActivity.this.mProgressDialog.setMessage(ConditionsActivity.this.getString(R.string.connected));
                ConditionsActivity.this.handler.postDelayed(ConditionsActivity.this.f11r, 4000);
            } else if (AmbientDeviceService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(ConditionsActivity.TAG, "Broadcast received: DISCONNECTED");
                ConditionsActivity.this.getWindow().clearFlags(128);
                ConditionsActivity.this.mProgressDialog.dismiss();
                try {
                    ConditionsActivity.this.unregisterReceiver(ConditionsActivity.this.mGattUpdateReceiver);
                    ConditionsActivity.this.unbindService(ConditionsActivity.this.mServiceConnection);
                } catch (IllegalArgumentException e) {
                }
                ConditionsActivity.this.handler.removeCallbacks(ConditionsActivity.this.f11r);
                ConditionsActivity.this.mAmbientDeviceService = null;
                ConditionsActivity.this.mSyncInProgress = false;
            } else if (AmbientDeviceService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(ConditionsActivity.TAG, "Broadcast received: SERVICES DISCOVERED");
                ConditionsActivity.this.mAmbientDeviceService.subscribeToConditionsNotifications();
            } else if (AmbientDeviceService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(ConditionsActivity.TAG, "Broadcast received: DATA AVAILABLE");
                byte[] data = intent.getByteArrayExtra(AmbientDeviceService.EXTRA_DATA);
                if (((UUID) intent.getExtras().get(AmbientDeviceService.CHAR_UUID)).equals(AmbientDeviceService.BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                    byte b = data[0];
                    Log.d(ConditionsActivity.TAG, "received batteryLevel: " + ((int) b));
                    ConditionsActivity.mCurrentAmbientDevice.batteryLevel = b;
                }
            } else if (AmbientDeviceService.ACTION_SUBSCRIPTION_FINISHED.equals(action)) {
                Log.d(ConditionsActivity.TAG, "Broadcast received: SUBSCRIPTION FINISHED");
                ConditionsActivity.this.startConditionsDataSync();
            } else if (AmbientDeviceService.ACTION_WRITE_SUCCESSFUL.equals(action)) {
                Log.d(ConditionsActivity.TAG, "Broadcast received: WRITE SUCCESSFUL");
                UUID uuid = (UUID) intent.getExtras().get(AmbientDeviceService.CHAR_UUID);
            } else if (AmbientDeviceService.ACTION_CONDITIONS_DATA_RECEIVED.equals(action)) {
                ConditionsActivity.this.processConditionsData(intent.getByteArrayExtra(AmbientDeviceService.EXTRAS_CONDITIONS));
            }
        }
    };
    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        /* class com.celaer.android.ambient.ConditionsActivity.C02335 */

        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            int unused = ConditionsActivity.mSelectedZoom = Integer.parseInt(compoundButton.getTag().toString());
            ConditionsActivity.this.updateZoomButtons();
            int unused2 = ConditionsActivity.mOffset = 0;
            ConditionsActivity.this.updateTimestampsAndReload();
        }
    };
    private ViewPager.OnPageChangeListener mOnPageChangerListener = new ViewPager.OnPageChangeListener() {
        /* class com.celaer.android.ambient.ConditionsActivity.C02346 */

        @Override // android.support.p000v4.view.ViewPager.OnPageChangeListener
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override // android.support.p000v4.view.ViewPager.OnPageChangeListener
        public void onPageSelected(int position) {
            int unused = ConditionsActivity.mSelectedPageIndex = position;
        }

        @Override // android.support.p000v4.view.ViewPager.OnPageChangeListener
        public void onPageScrollStateChanged(int state) {
            if (state != 0) {
                return;
            }
            if (ConditionsActivity.mSelectedPageIndex == 0) {
                ConditionsActivity.access$1408();
                ConditionsActivity.this.updateTimestampsAndReload();
            } else if (ConditionsActivity.mSelectedPageIndex == 2) {
                if (ConditionsActivity.mOffset > 0) {
                    ConditionsActivity.access$1410();
                }
                ConditionsActivity.this.updateTimestampsAndReload();
            }
        }
    };
    private LinearLayout mOverlay;
    private boolean mPreviousConditionExists;
    private AlertDialog mProgressDialog;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        /* class com.celaer.android.ambient.ConditionsActivity.ServiceConnectionC02357 */

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            ConditionsActivity.this.mAmbientDeviceService = ((AmbientDeviceService.LocalBinder) service).getService();
            Log.e(ConditionsActivity.TAG, "Service Connection Successful");
            if (!ConditionsActivity.this.mAmbientDeviceService.initialize()) {
                Log.e(ConditionsActivity.TAG, "Unable to initialize Bluetooth");
                ConditionsActivity.this.finish();
            }
            if (!ConditionsActivity.this.mAmbientDeviceService.isConnected()) {
                ConditionsActivity.this.mAmbientDeviceService.connect(ConditionsActivity.mCurrentAmbientDevice.getAddress());
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            ConditionsActivity.this.mAmbientDeviceService = null;
        }
    };
    private String mString1;
    private String mString2;
    private String mString3;
    private String mString4;
    private String mString5;
    private String mString6;
    private String mString7;
    private boolean mSyncInProgress;

    /* renamed from: r */
    private Runnable f11r;

    static /* synthetic */ int access$1408() {
        int i = mOffset;
        mOffset = i + 1;
        return i;
    }

    static /* synthetic */ int access$1410() {
        int i = mOffset;
        mOffset = i - 1;
        return i;
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conditions);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        //getActionBar().setDisplayShowTitleEnabled(true);
        mCurrentAmbientDevice = AmbientDeviceManager.get(this).getAmbientDevice(getIntent().getStringExtra("DEVICE_ADDRESS"));
        //getActionBar().setTitle(mCurrentAmbientDevice.getName());
        valuesTemp = new ArrayList<>();
        valuesHumidity = new ArrayList<>();
        valuesLight = new ArrayList<>();
        valuesTempMaster = new ArrayList<>();
        valuesHumidityMaster = new ArrayList<>();
        valuesLightMaster = new ArrayList<>();
        xAxisTemp = new ArrayList<>();
        xAxisHumidity = new ArrayList<>();
        xAxisLight = new ArrayList<>();
        xAxisMaster = new ArrayList<>();
        maxY = 71.0f;
        minY = -41.0f;
        maxYHumidity = 101.0f;
        minYHumidity = 101.0f;
        mXYSeriesTemp = new XYSeries() {
            /* class com.celaer.android.ambient.ConditionsActivity.C02291 */

            @Override // com.androidplot.p005xy.XYSeries
            public int size() {
                return ConditionsActivity.valuesTemp.size();
            }

            @Override // com.androidplot.p005xy.XYSeries
            public Number getX(int index) {
                return (Number) ConditionsActivity.xAxisTemp.get(index);
            }

            @Override // com.androidplot.p005xy.XYSeries
            public Number getY(int index) {
                return (Number) ConditionsActivity.valuesTemp.get(index);
            }

            @Override // com.androidplot.Series
            public String getTitle() {
                return null;
            }
        };
        mXYSeriesHumidity = new XYSeries() {
            /* class com.celaer.android.ambient.ConditionsActivity.C02302 */

            @Override // com.androidplot.p005xy.XYSeries
            public int size() {
                return ConditionsActivity.valuesHumidity.size();
            }

            @Override // com.androidplot.p005xy.XYSeries
            public Number getX(int index) {
                return (Number) ConditionsActivity.xAxisHumidity.get(index);
            }

            @Override // com.androidplot.p005xy.XYSeries
            public Number getY(int index) {
                return (Number) ConditionsActivity.valuesHumidity.get(index);
            }

            @Override // com.androidplot.Series
            public String getTitle() {
                return null;
            }
        };
        toggle0 = (ToggleButton) findViewById(R.id.activity_conditions_zoom0);
        toggle1 = (ToggleButton) findViewById(R.id.activity_conditions_zoom1);
        toggle2 = (ToggleButton) findViewById(R.id.activity_conditions_zoom2);
        toggle3 = (ToggleButton) findViewById(R.id.activity_conditions_zoom3);
        toggle4 = (ToggleButton) findViewById(R.id.activity_conditions_zoom4);
        toggle5 = (ToggleButton) findViewById(R.id.activity_conditions_zoom5);
        updateZoomButtons();
        this.mOverlay = (LinearLayout) findViewById(R.id.activity_conditions_overlay);
        this.mOverlay.setVisibility(View.INVISIBLE);
        this.mCursorView = findViewById(R.id.activity_conditions_cursor);
        this.mCursorView.setVisibility(View.INVISIBLE);
        this.mCursorViewDate = (TextView) findViewById(R.id.activity_conditions_cursor_date);
        this.mCursorViewTemp = (TextView) findViewById(R.id.activity_conditions_cursor_temp);
        this.mCursorViewHumidity = (TextView) findViewById(R.id.activity_conditions_cursor_humidity);
        this.mCursorViewLight = (TextView) findViewById(R.id.activity_conditions_cursor_light);
        mViewPager = (SelectiveViewPager) findViewById(R.id.activity_conditions_viewPager);
        mAdapter = new ConditionsPagerAdapter();
        mViewPager.setAdapter(mAdapter);
        mSelectedPageIndex = 1;
        mViewPager.setCurrentItem(1);
        mViewPager.setOnPageChangeListener(this.mOnPageChangerListener);
        mLoading = true;
        this.mSyncInProgress = false;
        this.mDisplayCursor = false;
        updateTimestamps();
        this.handler = new Handler();
        this.f11r = new Runnable() {
            /* class com.celaer.android.ambient.ConditionsActivity.RunnableC02313 */

            public void run() {
                Log.i(ConditionsActivity.TAG, "Attempting to start service discovery" + ConditionsActivity.this.mAmbientDeviceService.discoverServices());
            }
        };
        getLoaderManager().initLoader(3, null, this);
    }

    /* access modifiers changed from: protected */
    @Override // com.celaer.android.ambient.utilities.CelaerActivity
    public void onResume() {
        if (((MyApplication) getApplication()).wasInBackground) {
            Log.d(TAG, "launching from BACKGROUND");
            Intent intent = new Intent();
            setResult(-1, intent);
            intent.putExtra(MainActivity.EXTRAS_DISCONNECTION_CODE, 0);
            finish();
        }
        super.onResume();
        mOffset = 0;
        mSelectedZoom = 1;
        updateZoomButtons();
        updateTimestampsAndReload();
    }

    /* access modifiers changed from: protected */
    @Override // com.celaer.android.ambient.utilities.CelaerActivity
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        this.handler.removeCallbacks(this.f11r);
        getWindow().clearFlags(128);
        if (this.mSyncInProgress) {
            if (this.mAmbientDeviceService != null) {
                this.mAmbientDeviceService.disconnect();
            }
            this.mSyncInProgress = false;
            try {
                unregisterReceiver(this.mGattUpdateReceiver);
                unbindService(this.mServiceConnection);
            } catch (IllegalArgumentException e) {
            }
            this.mAmbientDeviceService = null;
            this.mProgressDialog.dismiss();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.mSyncInProgress) {
            return true;
        }
        getMenuInflater().inflate(R.menu.menu_conditions, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != R.id.menu_conditions_action_sync) {
            if (item.getItemId() == 16908332 && !this.mSyncInProgress) {
                this.handler.removeCallbacks(this.f11r);
                try {
                    unregisterReceiver(this.mGattUpdateReceiver);
                    unbindService(this.mServiceConnection);
                } catch (IllegalArgumentException e) {
                }
                this.mAmbientDeviceService = null;
                Intent intent = new Intent();
                setResult(-1, intent);
                intent.putExtra(MainActivity.EXTRAS_DISCONNECTION_CODE, 0);
                finish();
            }
            return super.onOptionsItemSelected(item);
        } else if (!isBLEEnabled()) {
            showBLEDialog();
            return true;
        } else {
            this.mProgressDialog = new AlertDialog.Builder(this).setTitle((CharSequence) null).setMessage(R.string.connecting).setNegativeButton(R.string.cancel_download, new DialogInterface.OnClickListener() {
                /* class com.celaer.android.ambient.ConditionsActivity.DialogInterface$OnClickListenerC02324 */

                public void onClick(DialogInterface dialogInterface, int i) {
                    ConditionsActivity.this.mAmbientDeviceService.disconnect();
                    ConditionsActivity.this.handler.removeCallbacks(ConditionsActivity.this.f11r);
                    try {
                        ConditionsActivity.this.unregisterReceiver(ConditionsActivity.this.mGattUpdateReceiver);
                        ConditionsActivity.this.unbindService(ConditionsActivity.this.mServiceConnection);
                    } catch (IllegalArgumentException e) {
                    }
                    ConditionsActivity.this.mAmbientDeviceService = null;
                    ConditionsActivity.this.mSyncInProgress = false;
                }
            }).setCancelable(false).show();
            Intent gattServiceIntent = new Intent(this, AmbientDeviceService.class);
            bindService(gattServiceIntent, this.mServiceConnection, Context.BIND_AUTO_CREATE);
            startService(gattServiceIntent);
            registerReceiver(this.mGattUpdateReceiver, makeGattUpdateIntentFilter());
            this.mSyncInProgress = true;
            this.mConditionsReceived = 0;
            this.mDataPacketsReceived = 0;
            return true;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4 && !this.mSyncInProgress) {
            this.handler.removeCallbacks(this.f11r);
            try {
                unregisterReceiver(this.mGattUpdateReceiver);
                unbindService(this.mServiceConnection);
            } catch (IllegalArgumentException e) {
            }
            this.mAmbientDeviceService = null;
            Intent intent = new Intent();
            setResult(-1, intent);
            intent.putExtra(MainActivity.EXTRAS_DISCONNECTION_CODE, 0);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateZoomButtons() {
        toggle0.setOnCheckedChangeListener(null);
        toggle1.setOnCheckedChangeListener(null);
        toggle2.setOnCheckedChangeListener(null);
        toggle3.setOnCheckedChangeListener(null);
        toggle4.setOnCheckedChangeListener(null);
        toggle5.setOnCheckedChangeListener(null);
        toggle0.setChecked(false);
        toggle1.setChecked(false);
        toggle2.setChecked(false);
        toggle3.setChecked(false);
        toggle4.setChecked(false);
        toggle5.setChecked(false);
        switch (mSelectedZoom) {
            case 0:
                toggle0.setChecked(true);
                break;
            case 1:
                toggle1.setChecked(true);
                break;
            case 2:
                toggle2.setChecked(true);
                break;
            case 3:
                toggle3.setChecked(true);
                break;
            case 4:
                toggle4.setChecked(true);
                break;
            case 5:
                toggle5.setChecked(true);
                break;
        }
        toggle0.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
        toggle1.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
        toggle2.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
        toggle3.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
        toggle4.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
        toggle5.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
    }

    private static class ConditionListAllCursorLoader extends SQLiteCursorLoader {
        public ConditionListAllCursorLoader(Context context) {
            super(context);
        }

        /* access modifiers changed from: protected */
        @Override // com.celaer.android.ambient.database.SQLiteCursorLoader
        public Cursor loadCursor() {
            return AmbientDeviceManager.get(getContext()).queryConditionsAll(ConditionsActivity.mCurrentAmbientDevice.getAddress());
        }
    }

    private static class ConditionListGreaterCursorLoader extends SQLiteCursorLoader {
        public ConditionListGreaterCursorLoader(Context context) {
            super(context);
        }

        /* access modifiers changed from: protected */
        @Override // com.celaer.android.ambient.database.SQLiteCursorLoader
        public Cursor loadCursor() {
            return AmbientDeviceManager.get(getContext()).queryConditionsAfterTimestamp(ConditionsActivity.mCurrentAmbientDevice.getAddress(), ConditionsActivity.timestamp1);
        }
    }

    private static class ConditionListLessCursorLoader extends SQLiteCursorLoader {
        public ConditionListLessCursorLoader(Context context) {
            super(context);
        }

        /* access modifiers changed from: protected */
        @Override // com.celaer.android.ambient.database.SQLiteCursorLoader
        public Cursor loadCursor() {
            return AmbientDeviceManager.get(getContext()).queryConditionsBeforeTimestamp(ConditionsActivity.mCurrentAmbientDevice.getAddress(), ConditionsActivity.timestamp1);
        }
    }

    private static class ConditionListBetweenCursorLoader extends SQLiteCursorLoader {
        public ConditionListBetweenCursorLoader(Context context) {
            super(context);
        }

        /* access modifiers changed from: protected */
        @Override // com.celaer.android.ambient.database.SQLiteCursorLoader
        public Cursor loadCursor() {
            boolean unused = ConditionsActivity.mLoading = true;
            return AmbientDeviceManager.get(getContext()).queryConditionsBetweenTimestamps(ConditionsActivity.mCurrentAmbientDevice.getAddress(), ConditionsActivity.timestamp1, ConditionsActivity.timestamp2);
        }
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (i == 0) {
            return new ConditionListAllCursorLoader(this);
        }
        if (i == 1) {
            return new ConditionListGreaterCursorLoader(this);
        }
        if (i == 2) {
            return new ConditionListLessCursorLoader(this);
        }
        if (i == 3) {
            return new ConditionListBetweenCursorLoader(this);
        }
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:52:0x01f2  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x0222  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x023b  */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x025b  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x029b  */
    /* JADX WARNING: Removed duplicated region for block: B:95:0x02ef  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onLoadFinished(android.content.Loader<android.database.Cursor> paramLoader, android.database.Cursor paramCursor) {
        /*
        // Method dump skipped, instructions count: 1558
        */
        //throw new UnsupportedOperationException("Method not decompiled: com.celaer.android.ambient.ConditionsActivity.onLoadFinished(android.content.Loader, android.database.Cursor):void");
        if (paramCursor != null) {
            byte b;
            long l10;
            Log.d(TAG, "conditions retrieved: " + paramCursor.getCount());
            paramCursor.moveToFirst();
            int m = paramCursor.getColumnIndex("timestamp");
            int n = paramCursor.getColumnIndex("tempC");
            int i1 = paramCursor.getColumnIndex("humidity");
            int i2 = paramCursor.getColumnIndex("light");
            DateFormat.getTimeInstance();
            valuesTemp.clear();
            valuesHumidity.clear();
            valuesLight.clear();
            valuesTempMaster.clear();
            valuesHumidityMaster.clear();
            valuesLightMaster.clear();
            xAxisTemp.clear();
            xAxisHumidity.clear();
            xAxisLight.clear();
            xAxisMaster.clear();
            maxY = 200.0F;
            minY = -41.0F;
            maxYHumidity = 101.0F;
            minYHumidity = 0.0F;
            mAverageTemp = 0.0F;
            mAverageHumidity = 0.0F;
            long l8 = 0L;
            int k = 0;
            int j = 0;
            int i = 0;
            long l4 = 0L;
            long l3 = 0L;
            long l2 = 0L;
            long l7 = 0L;
            long l6 = 0L;
            long l5 = 0L;
            long l1 = 0L;
            long l9 = 0L;
            switch (mSelectedZoom) {
                default:
                    l10 = 0L;
                    b = 0;
                    while (b < paramCursor.getCount()) {
                        long l11 = paramCursor.getLong(m);
                        float f1 = paramCursor.getFloat(n);
                        float f2 = paramCursor.getFloat(i1);
                        long l15 = paramCursor.getLong(i2);
                        xAxisMaster.add(Long.valueOf(l11));
                        if (b == 0) {
                            if (l11 == timestamp1) {
                                if (f1 < 70.1D) {
                                    float f = f1;
                                    if (!mCurrentAmbientDevice.tempUnitsC)
                                        f = (float)MainActivity.convertCtoF(f1);
                                    valuesTemp.add(Float.valueOf(f));
                                    xAxisTemp.add(Long.valueOf(l11));
                                    if (maxY > 199.0F) {
                                        maxY = f;
                                        minY = f;
                                    } else if (f > maxY) {
                                        maxY = f;
                                    } else if (f < minY) {
                                        minY = f;
                                    }
                                }
                                if (f2 < 101.0F) {
                                    valuesHumidity.add(Float.valueOf(f2));
                                    xAxisHumidity.add(Long.valueOf(l11));
                                    if (maxYHumidity > 100.0F) {
                                        maxYHumidity = f2;
                                        minYHumidity = f2;
                                    } else if (f2 > maxYHumidity) {
                                        maxYHumidity = f2;
                                    } else if (f2 < minYHumidity) {
                                        minYHumidity = f2;
                                    }
                                }
                                if (l15 < 65535L) {
                                    valuesLight.add(Long.valueOf(l15));
                                    xAxisLight.add(Long.valueOf(l11));
                                }
                                l9 = 0L;
                                l10 = (1L + 0L) * l1 + timestamp1;
                                paramCursor.moveToNext();
                                long l = l8;
                                l8 = l4;
                                l11 = l2;
                                l4 = l3;
                                continue;
                            }
                            long l17 = (l11 - timestamp1) / l1;
                            long l16 = l17;
                            if ((l11 - timestamp1) % l1 == 0L)
                                l16 = l17 - 1L;
                            l17 = (1L + l16) * l1 + timestamp1;
                            long l18 = l8;
                            int i8 = k;
                            int i6 = i;
                            int i7 = j;
                            continue;
                        }
                        long l12 = l9;
                        int i4 = j;
                        int i3 = i;
                        int i5 = k;
                        long l13 = l10;
                        long l14 = l8;
                        if (l11 > l10) {
                            int i6 = k;
                            if (k==1) { //Joey fix
                                mAverageTemp /= k;
                                l12 = l7;
                                if (valuesTemp.size() == 0)
                                    l12 = l4;
                                valuesTemp.add(Float.valueOf(mAverageTemp));
                                xAxisTemp.add(Long.valueOf(l12));
                                if (maxY > 199.0F) {
                                    maxY = mAverageTemp;
                                    minY = mAverageTemp;
                                } else if (mAverageTemp > maxY) {
                                    maxY = mAverageTemp;
                                } else if (mAverageTemp < minY) {
                                    minY = mAverageTemp;
                                }
                                mAverageTemp = 0.0F;
                                i6 = 0;
                            }
                            k = j;
                            if (j == 1) {   //Joey fix
                                mAverageHumidity /= j;
                                if (mCurrentAmbientDevice.getDeviceType() == 0)
                                    mAverageHumidity = Math.round(mAverageHumidity);
                                l12 = l6;
                                if (valuesHumidity.size() == 0)
                                    l12 = l3;
                                valuesHumidity.add(Float.valueOf(mAverageHumidity));
                                xAxisHumidity.add(Long.valueOf(l12));
                                if (maxYHumidity > 100.0F) {
                                    maxYHumidity = mAverageHumidity;
                                    minYHumidity = mAverageHumidity;
                                } else if (mAverageHumidity > maxYHumidity) {
                                    maxYHumidity = mAverageHumidity;
                                } else if (mAverageHumidity < minYHumidity) {
                                    minYHumidity = mAverageHumidity;
                                }
                                mAverageHumidity = 0.0F;
                                k = 0;
                            }
                            long l17 = l9;
                            j = i;
                            long l18 = l10;
                            long l16 = l8;
                            if (i == 1) {   //Joey fix
                                l8 = Math.round((float)(l8 / i));
                                l12 = l5;
                                if (valuesLight.size() == 0)
                                    l12 = l2;
                                valuesLight.add(Long.valueOf(l8));
                                xAxisLight.add(Long.valueOf(l12));
                                l16 = 0L;
                                j = 0;
                                l18 = l10;
                                l17 = l9;
                            }
                            while (true) {
                                l12 = l17;
                                i4 = k;
                                i3 = j;
                                i5 = i6;
                                l13 = l18;
                                l14 = l16;
                                l17++;
                                l18 = (1L + l17) * l1 + timestamp1;
                            }
                        }
                        continue;
                    }
                    break;
                case 0:
                    l1 = 60L;
                case 1:
                    l1 = 900L;
                case 2:
                    l1 = 3600L;
                case 3:
                    l1 = 21600L;
                case 4:
                    l1 = 43200L;
                case 5:
                    l1 = 345600L;
            }
            if (valuesTemp.size() != 0) {
                mMaxTemp = maxY;
                mMinTemp = minY;
                float f1 = maxY - minY;
                if (mCurrentAmbientDevice.tempUnitsC) {
                    f1 = Math.abs(f1 / 4.0F);
                } else {
                    f1 = Math.abs(f1 / 8.0F);
                }
                float f2 = f1;
                if (f1 < 1.0F)
                    f2 = 1.0F;
                minY = (float)Math.floor((minY - f2));
                maxY = (float)Math.ceil((maxY + f2));
                if (minY < 0.0F) {
                    i = (int)minY;
                    minY -= (5 - i * -1 % 5);
                } else {
                    i = (int)minY;
                    minY -= Math.abs(i % 5);
                }
                if (maxY < 0.0F) {
                    i = (int)maxY;
                    maxY += (i * -1 % 5);
                } else {
                    i = (int)maxY;
                    maxY += (5 - Math.abs(i % 5));
                }
            }
            if (valuesHumidity.size() != 0) {
                mMaxHumidity = maxYHumidity;
                mMinHumidity = minYHumidity;
                float f2 = (maxYHumidity - minYHumidity) / 4.0F;
                float f1 = f2;
                if (f2 < 1.0F)
                    f1 = 1.0F;
                minYHumidity = (float)Math.floor((minYHumidity - f1));
                maxYHumidity = (float)Math.ceil((maxYHumidity + f1));
                i = (int)minYHumidity;
                minYHumidity -= (i % 5);
                i = (int)maxYHumidity;
                maxYHumidity += (5 - i % 5);
                if (minYHumidity < 0.0F)
                    minYHumidity = 0.0F;
                if (maxYHumidity > 100.0F)
                    maxYHumidity = 100.0F;
            }
        }
        mLoading = false;
        mAdapter.notifyDataSetChanged();
        this.mDisplayCursor = false;
        mViewPager.setOnTouchListener(null);
        paramCursor.close();
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void updateTimestamps() {
        SimpleDateFormat sdf;
        SimpleDateFormat sdf2;
        Calendar cal1 = Calendar.getInstance();
        Calendar calNow = Calendar.getInstance();
        calNow.setTime(new Date());
        cal1.clear();
        if (mSelectedZoom == 0) {
            cal1.set(calNow.get(1), calNow.get(2), calNow.get(5), calNow.get(11), 0);
            long lowerTimestamp = (cal1.getTime().getTime() / 1000) - (((long) mOffset) * 3600);
            timestamp1 = lowerTimestamp;
            timestamp2 = lowerTimestamp + 3600;
            Date date1 = new Date(timestamp1 * 1000);
            Date date2 = new Date(timestamp2 * 1000);
            DateFormat dateFormat = DateFormat.getDateInstance(1, Locale.getDefault());
            DateFormat timeFormat = DateFormat.getTimeInstance(3, Locale.getDefault());
            this.mDateLabelString = dateFormat.format(date1) + ", " + timeFormat.format(date1) + " - " + timeFormat.format(date2);
            cal1.setTimeInMillis(timestamp1 * 1000);
            if (mCurrentAmbientDevice.timeFormat24) {
                sdf2 = new SimpleDateFormat("H:mm");
            } else {
                sdf2 = new SimpleDateFormat("h:mm");
            }
            this.mString1 = sdf2.format(cal1.getTime());
            cal1.set(12, 15);
            this.mString2 = sdf2.format(cal1.getTime());
            cal1.set(12, 30);
            this.mString3 = sdf2.format(cal1.getTime());
            cal1.set(12, 45);
            this.mString4 = sdf2.format(cal1.getTime());
        } else if (mSelectedZoom == 1) {
            cal1.set(calNow.get(1), calNow.get(2), calNow.get(5));
            long lowerTimestamp2 = (cal1.getTime().getTime() / 1000) - (((long) mOffset) * 86400);
            timestamp1 = lowerTimestamp2;
            timestamp2 = lowerTimestamp2 + 86400;
            if (mOffset == 0) {
                this.mDateLabelString = getString(R.string.today);
            } else if (mOffset == 1) {
                this.mDateLabelString = getString(R.string.yesterday);
            } else {
                Date nowDate = new Date(timestamp1 * 1000);
                this.mDateLabelString = new SimpleDateFormat("EEEE").format(nowDate) + ", " + DateFormat.getDateInstance(1, Locale.getDefault()).format(nowDate);
            }
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(2015, 0, 1, 4, 0, 0);
            if (mCurrentAmbientDevice.timeFormat24) {
                sdf = new SimpleDateFormat("H");
            } else {
                sdf = new SimpleDateFormat("h");
            }
            this.mString1 = "";
            this.mString2 = sdf.format(cal.getTime());
            cal.set(11, 8);
            this.mString3 = sdf.format(cal.getTime());
            cal.set(11, 12);
            this.mString4 = sdf.format(cal.getTime());
            cal.set(11, 16);
            this.mString5 = sdf.format(cal.getTime());
            cal.set(11, 20);
            this.mString6 = sdf.format(cal.getTime());
        } else if (mSelectedZoom == 2) {
            cal1.set(calNow.get(1), calNow.get(2), calNow.get(5));
            long lowerTimestamp3 = ((cal1.getTime().getTime() / 1000) - 518400) - (((long) mOffset) * 604800);
            timestamp1 = lowerTimestamp3;
            timestamp2 = lowerTimestamp3 + 604800;
            Date date12 = new Date(timestamp1 * 1000);
            Date date22 = new Date((timestamp2 * 1000) - 1);
            DateFormat dateFormat2 = DateFormat.getDateInstance(1, Locale.getDefault());
            this.mDateLabelString = dateFormat2.format(date12) + " - " + dateFormat2.format(date22);
            cal1.setTimeInMillis(timestamp1 * 1000);
            SimpleDateFormat sdf3 = new SimpleDateFormat("d");
            this.mString1 = sdf3.format(cal1.getTime());
            cal1.add(5, 1);
            this.mString2 = sdf3.format(cal1.getTime());
            cal1.add(5, 1);
            this.mString3 = sdf3.format(cal1.getTime());
            cal1.add(5, 1);
            this.mString4 = sdf3.format(cal1.getTime());
            cal1.add(5, 1);
            this.mString5 = sdf3.format(cal1.getTime());
            cal1.add(5, 1);
            this.mString6 = sdf3.format(cal1.getTime());
            cal1.add(5, 1);
            this.mString7 = sdf3.format(cal1.getTime());
        } else if (mSelectedZoom == 3) {
            cal1.set(calNow.get(1), calNow.get(2), calNow.get(5));
            long lowerTimestamp4 = ((cal1.getTime().getTime() / 1000) - 2332800) - (((long) mOffset) * 2419200);
            timestamp1 = lowerTimestamp4;
            timestamp2 = lowerTimestamp4 + 2419200;
            Date date13 = new Date(timestamp1 * 1000);
            Date date23 = new Date((timestamp2 * 1000) - 1);
            DateFormat dateFormat3 = DateFormat.getDateInstance(1, Locale.getDefault());
            this.mDateLabelString = dateFormat3.format(date13) + " - " + dateFormat3.format(date23);
            cal1.setTimeInMillis(timestamp1 * 1000);
            SimpleDateFormat sdf4 = new SimpleDateFormat("d");
            this.mString1 = sdf4.format(cal1.getTime());
            cal1.add(5, 7);
            this.mString2 = sdf4.format(cal1.getTime());
            cal1.add(5, 7);
            this.mString3 = sdf4.format(cal1.getTime());
            cal1.add(5, 7);
            this.mString4 = sdf4.format(cal1.getTime());
        } else if (mSelectedZoom == 4) {
            cal1.set(calNow.get(1), calNow.get(2), 1);
            cal1.add(2, -2 - (mOffset * 3));
            timestamp1 = cal1.getTime().getTime() / 1000;
            cal1.add(2, 3);
            timestamp2 = cal1.getTime().getTime() / 1000;
            Date date14 = new Date(timestamp1 * 1000);
            Date date24 = new Date((timestamp2 * 1000) - 1);
            SimpleDateFormat sdf5 = new SimpleDateFormat("MMMM y");
            this.mDateLabelString = sdf5.format(date14) + " - " + sdf5.format(date24);
            cal1.setTimeInMillis(timestamp1 * 1000);
            SimpleDateFormat sdf1 = new SimpleDateFormat("MMM");
            this.mString1 = sdf1.format(cal1.getTime());
            cal1.add(2, 1);
            this.mString2 = sdf1.format(cal1.getTime());
            cal1.add(2, 1);
            this.mString3 = sdf1.format(cal1.getTime());
        } else if (mSelectedZoom == 5) {
            cal1.set(calNow.get(1), calNow.get(2), 1);
            cal1.add(2, -11 - (mOffset * 12));
            timestamp1 = cal1.getTime().getTime() / 1000;
            cal1.add(2, 12);
            timestamp2 = cal1.getTime().getTime() / 1000;
            Date date15 = new Date(timestamp1 * 1000);
            Date date25 = new Date((timestamp2 * 1000) - 1);
            SimpleDateFormat sdf6 = new SimpleDateFormat("MMMM y");
            this.mDateLabelString = sdf6.format(date15) + " - " + sdf6.format(date25);
            cal1.setTimeInMillis(timestamp1 * 1000);
            SimpleDateFormat sdf32 = new SimpleDateFormat("MMM");
            this.mString1 = sdf32.format(cal1.getTime());
            cal1.add(2, 3);
            this.mString2 = sdf32.format(cal1.getTime());
            cal1.add(2, 3);
            this.mString3 = sdf32.format(cal1.getTime());
            cal1.add(2, 3);
            this.mString4 = sdf32.format(cal1.getTime());
        }
        this.mPreviousConditionExists = AmbientDeviceManager.get(this).conditionExistsBefore(mCurrentAmbientDevice.getAddress(), timestamp1);
        DateTimeZone.getDefault().getOffset(System.currentTimeMillis());
        Log.d(TAG, "mOffset: " + mOffset + " timestamp1: " + timestamp1 + " timestamp2: " + timestamp2);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateTimestampsAndReload() {
        updateTimestamps();
        getLoaderManager().restartLoader(3, null, this);
        valuesTemp.clear();
        valuesHumidity.clear();
        valuesLight.clear();
        xAxisTemp.clear();
        xAxisHumidity.clear();
        xAxisLight.clear();
        xAxisMaster.clear();
        mAdapter.notifyDataSetChanged();
        this.mDisplayCursor = false;
        mViewPager.setOnTouchListener(null);
        mViewPager.setCurrentItem(1, false);
    }

    /* access modifiers changed from: private */
    public class ConditionsPagerAdapter extends PagerAdapter {
        private View.OnLongClickListener mLongLickListener;
        private View.OnTouchListener mOnTouchListener;

        private ConditionsPagerAdapter() {
            this.mOnTouchListener = new View.OnTouchListener() {
                /* class com.celaer.android.ambient.ConditionsActivity.ConditionsPagerAdapter.View$OnTouchListenerC02371 */

                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getAction() == 0) {
                        int width = ConditionsActivity.this.getResources().getDisplayMetrics().widthPixels;
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ConditionsActivity.this.mOverlay.getLayoutParams();
                        RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) ConditionsActivity.this.mCursorView.getLayoutParams();
                        int index = ConditionsActivity.this.findNearestIndexForXProportion(((double) ((int) motionEvent.getRawX())) / ((double) width));
                        ConditionsActivity.this.updateCursorLabels(index);
                        int xFinal = (int) Math.round((((double) (((Number) ConditionsActivity.xAxisMaster.get(index)).longValue() - ConditionsActivity.timestamp1)) / ((double) (ConditionsActivity.timestamp2 - ConditionsActivity.timestamp1))) * ((double) width));
                        if (xFinal < width / 2) {
                            params.leftMargin = xFinal;
                        } else {
                            params.leftMargin = xFinal - params.width;
                        }
                        params2.leftMargin = xFinal;
                        ConditionsActivity.this.mOverlay.setLayoutParams(params);
                        ConditionsActivity.this.mCursorView.setLayoutParams(params2);
                        return false;
                    } else if (motionEvent.getAction() == 1) {
                        ConditionsActivity.mViewPager.setPaging(true);
                        ConditionsActivity.this.mOverlay.setVisibility(View.INVISIBLE);
                        ConditionsActivity.this.mCursorView.setVisibility(View.INVISIBLE);
                        return false;
                    } else if (motionEvent.getAction() != 2) {
                        return false;
                    } else {
                        int width2 = ConditionsActivity.this.getResources().getDisplayMetrics().widthPixels;
                        RelativeLayout.LayoutParams params3 = (RelativeLayout.LayoutParams) ConditionsActivity.this.mOverlay.getLayoutParams();
                        RelativeLayout.LayoutParams params22 = (RelativeLayout.LayoutParams) ConditionsActivity.this.mCursorView.getLayoutParams();
                        int index2 = ConditionsActivity.this.findNearestIndexForXProportion(((double) ((int) motionEvent.getRawX())) / ((double) width2));
                        ConditionsActivity.this.updateCursorLabels(index2);
                        int xFinal2 = (int) Math.round((((double) (((Number) ConditionsActivity.xAxisMaster.get(index2)).longValue() - ConditionsActivity.timestamp1)) / ((double) (ConditionsActivity.timestamp2 - ConditionsActivity.timestamp1))) * ((double) width2));
                        if (xFinal2 < width2 / 2) {
                            params3.leftMargin = xFinal2;
                        } else {
                            params3.leftMargin = xFinal2 - params3.width;
                        }
                        params22.leftMargin = xFinal2;
                        ConditionsActivity.this.mOverlay.setLayoutParams(params3);
                        ConditionsActivity.this.mCursorView.setLayoutParams(params22);
                        return false;
                    }
                }
            };
            this.mLongLickListener = new View.OnLongClickListener() {
                /* class com.celaer.android.ambient.ConditionsActivity.ConditionsPagerAdapter.View$OnLongClickListenerC02382 */

                public boolean onLongClick(View view) {
                    ConditionsActivity.mViewPager.setPaging(false);
                    ConditionsActivity.this.mOverlay.setVisibility(View.VISIBLE);
                    ConditionsActivity.this.mCursorView.setVisibility(View.VISIBLE);
                    return true;
                }
            };
        }

        @Override // android.support.p000v4.view.PagerAdapter
        public int getCount() {
            if (ConditionsActivity.mOffset == 0) {
                return 2;
            }
            return 3;
        }

        @Override // android.support.p000v4.view.PagerAdapter
        public Object instantiateItem(ViewGroup container, int position) {
            View view = ((LayoutInflater) container.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.activity_conditions_page, (ViewGroup) null);
            TextView tv = (TextView) view.findViewById(R.id.activity_conditions_page_dateTextView);
            Button buttonLeft = (Button) view.findViewById(R.id.activity_conditions_page_leftButton);
            Button buttonRight = (Button) view.findViewById(R.id.activity_conditions_page_rightButton);
            XYPlot plot = (XYPlot) view.findViewById(R.id.activity_conditions_page_chart1);
            XYPlot plot2 = (XYPlot) view.findViewById(R.id.activity_conditions_page_chart2);
            TextView noDataTemp = (TextView) view.findViewById(R.id.activity_conditions_page_tempNoData);
            TextView noDataHumidity = (TextView) view.findViewById(R.id.activity_conditions_page_humidityNoData);
            LinearLayout mLayout3 = (LinearLayout) view.findViewById(R.id.activity_conditions_page_labels_3);
            LinearLayout mLayout4 = (LinearLayout) view.findViewById(R.id.activity_conditions_page_labels_4);
            LinearLayout mLayout6 = (LinearLayout) view.findViewById(R.id.activity_conditions_page_labels_6);
            LinearLayout mLayout7 = (LinearLayout) view.findViewById(R.id.activity_conditions_page_labels_7);
            LightGraph mLightGraph = (LightGraph) view.findViewById(R.id.activity_conditions_page_lightGraph);
            mLayout3.setVisibility(View.GONE);
            mLayout4.setVisibility(View.GONE);
            mLayout6.setVisibility(View.GONE);
            mLayout7.setVisibility(View.GONE);
            if (ConditionsActivity.mSelectedZoom == 0) {
                mLayout3.setVisibility(View.GONE);
                mLayout4.setVisibility(View.VISIBLE);
                mLayout6.setVisibility(View.GONE);
                mLayout7.setVisibility(View.GONE);
                ((TextView) view.findViewById(R.id.activity_conditions_page_4_1)).setText(ConditionsActivity.this.mString1);
                ((TextView) view.findViewById(R.id.activity_conditions_page_4_2)).setText(ConditionsActivity.this.mString2);
                ((TextView) view.findViewById(R.id.activity_conditions_page_4_3)).setText(ConditionsActivity.this.mString3);
                ((TextView) view.findViewById(R.id.activity_conditions_page_4_4)).setText(ConditionsActivity.this.mString4);
            } else if (ConditionsActivity.mSelectedZoom == 1) {
                mLayout3.setVisibility(View.GONE);
                mLayout4.setVisibility(View.GONE);
                mLayout6.setVisibility(View.VISIBLE);
                mLayout7.setVisibility(View.GONE);
                ((TextView) view.findViewById(R.id.activity_conditions_page_6_1)).setText(ConditionsActivity.this.mString1);
                ((TextView) view.findViewById(R.id.activity_conditions_page_6_2)).setText(ConditionsActivity.this.mString2);
                ((TextView) view.findViewById(R.id.activity_conditions_page_6_3)).setText(ConditionsActivity.this.mString3);
                ((TextView) view.findViewById(R.id.activity_conditions_page_6_4)).setText(ConditionsActivity.this.mString4);
                ((TextView) view.findViewById(R.id.activity_conditions_page_6_5)).setText(ConditionsActivity.this.mString5);
                ((TextView) view.findViewById(R.id.activity_conditions_page_6_6)).setText(ConditionsActivity.this.mString6);
            } else if (ConditionsActivity.mSelectedZoom == 2) {
                mLayout3.setVisibility(View.GONE);
                mLayout4.setVisibility(View.GONE);
                mLayout6.setVisibility(View.GONE);
                mLayout7.setVisibility(View.VISIBLE);
                ((TextView) view.findViewById(R.id.activity_conditions_page_7_1)).setText(ConditionsActivity.this.mString1);
                ((TextView) view.findViewById(R.id.activity_conditions_page_7_2)).setText(ConditionsActivity.this.mString2);
                ((TextView) view.findViewById(R.id.activity_conditions_page_7_3)).setText(ConditionsActivity.this.mString3);
                ((TextView) view.findViewById(R.id.activity_conditions_page_7_4)).setText(ConditionsActivity.this.mString4);
                ((TextView) view.findViewById(R.id.activity_conditions_page_7_5)).setText(ConditionsActivity.this.mString5);
                ((TextView) view.findViewById(R.id.activity_conditions_page_7_6)).setText(ConditionsActivity.this.mString6);
                ((TextView) view.findViewById(R.id.activity_conditions_page_7_7)).setText(ConditionsActivity.this.mString7);
            } else if (ConditionsActivity.mSelectedZoom == 3) {
                mLayout3.setVisibility(View.GONE);
                mLayout4.setVisibility(View.VISIBLE);
                mLayout6.setVisibility(View.GONE);
                mLayout7.setVisibility(View.GONE);
                ((TextView) view.findViewById(R.id.activity_conditions_page_4_1)).setText(ConditionsActivity.this.mString1);
                ((TextView) view.findViewById(R.id.activity_conditions_page_4_2)).setText(ConditionsActivity.this.mString2);
                ((TextView) view.findViewById(R.id.activity_conditions_page_4_3)).setText(ConditionsActivity.this.mString3);
                ((TextView) view.findViewById(R.id.activity_conditions_page_4_4)).setText(ConditionsActivity.this.mString4);
            } else if (ConditionsActivity.mSelectedZoom == 4) {
                mLayout3.setVisibility(View.VISIBLE);
                mLayout4.setVisibility(View.GONE);
                mLayout6.setVisibility(View.GONE);
                mLayout7.setVisibility(View.GONE);
                ((TextView) view.findViewById(R.id.activity_conditions_page_3_1)).setText(ConditionsActivity.this.mString1);
                ((TextView) view.findViewById(R.id.activity_conditions_page_3_2)).setText(ConditionsActivity.this.mString2);
                ((TextView) view.findViewById(R.id.activity_conditions_page_3_3)).setText(ConditionsActivity.this.mString3);
            } else if (ConditionsActivity.mSelectedZoom == 5) {
                mLayout3.setVisibility(View.GONE);
                mLayout4.setVisibility(View.VISIBLE);
                mLayout6.setVisibility(View.GONE);
                mLayout7.setVisibility(View.GONE);
                ((TextView) view.findViewById(R.id.activity_conditions_page_4_1)).setText(ConditionsActivity.this.mString1);
                ((TextView) view.findViewById(R.id.activity_conditions_page_4_2)).setText(ConditionsActivity.this.mString2);
                ((TextView) view.findViewById(R.id.activity_conditions_page_4_3)).setText(ConditionsActivity.this.mString3);
                ((TextView) view.findViewById(R.id.activity_conditions_page_4_4)).setText(ConditionsActivity.this.mString4);
            }
            if (position == 1) {
                if (ConditionsActivity.mOffset == 0) {
                    buttonRight.setVisibility(View.INVISIBLE);
                } else {
                    buttonRight.setVisibility(View.VISIBLE);
                }
                buttonLeft.setVisibility(View.VISIBLE);
                if (ConditionsActivity.this.mPreviousConditionExists) {
                    buttonLeft.setVisibility(View.VISIBLE);
                } else {
                    buttonLeft.setVisibility(View.INVISIBLE);
                }
                tv.setText(ConditionsActivity.this.mDateLabelString);
                LineAndPointFormatter series1Format = new LineAndPointFormatter();
                series1Format.setPointLabelFormatter(new PointLabelFormatter());
                series1Format.configure(ConditionsActivity.this.getApplicationContext(), R.xml.line_point_formatter_temperature);
                series1Format.setPointLabelFormatter(null);
                plot.addSeries(ConditionsActivity.mXYSeriesTemp, series1Format);
                plot.setDomainBoundaries(Long.valueOf(ConditionsActivity.timestamp1), Long.valueOf(ConditionsActivity.timestamp2), BoundaryMode.FIXED);
                plot.setRangeBoundaries(Float.valueOf(ConditionsActivity.minY), Float.valueOf(ConditionsActivity.maxY), BoundaryMode.FIXED);
                plot.setPlotMargins(0.0f, 0.0f, 0.0f, 0.0f);
                plot.setPlotPadding(0.0f, 0.0f, 0.0f, 0.0f);

                // 舊的
                //plot.getGraphWidget().setGridPaddingTop(25.0f);
                //plot.getGraphWidget().setGridPaddingBottom(25.0f);

                // 下面是新的
                RectRegion bounds = plot.getBounds();

                plot.setRangeBoundaries(
                        bounds.getMinY().doubleValue() - 1, BoundaryMode.FIXED,
                        bounds.getMaxY().doubleValue() + 1, BoundaryMode.FIXED);

                plot.setDomainBoundaries(
                        bounds.getMinX().doubleValue() - 1, BoundaryMode.FIXED,
                        bounds.getMaxX().doubleValue() + 1, BoundaryMode.FIXED);

                //plot.setDomainLabelWidget(null);  //暫時找不到
                plot.setBackgroundPaint(null);

                // plot.getGraphWidget().setBackgroundPaint(null);
                plot.setBackgroundPaint(null);

                //plot.getGraphWidget().getGridBackgroundPaint().setColor(-1);
                plot.getGraph().getGridBackgroundPaint().setColor(-1);

                plot.setBorderPaint(null);
                //plot.getGraphWidget().setDomainLabelWidth(0.0f); //Not found new method
                //plot.getGraphWidget().setRangeLabelWidth(0.0f);
                //plot.getGraphWidget().setDomainLabelPaint(null);
                //plot.getGraphWidget().setDomainOriginLabelPaint(null);
                //plot.getGraphWidget().setRangeLabelHorizontalOffset(-140.0f);
                //plot.getGraphWidget().getRangeLabelPaint().setColor(ConditionsActivity.this.getResources().getColor(R.color.gray50));
                //plot.getGraphWidget().getRangeLabelPaint().setTextSize(45.0f);

                //plot.setRangeStep(XYStepMode.SUBDIVIDE, 2.0d); // old
                plot.setRangeStep(StepMode.SUBDIVIDE, 2.0d);

                //plot.setRangeValueFormat(new DecimalFormat("#.0°"));
                //plot.getGraphWidget().setRangeOriginLabelPaint(null);

                //plot.getGraphWidget().setDomainOriginLinePaint(null); //old
                plot.getGraph().setDomainOriginLinePaint(null);

                //plot.getGraphWidget().setRangeOriginLinePaint(null);  //old
                plot.getGraph().setRangeOriginLinePaint(null);

                //plot.getLayoutManager().remove(plot.getTitleWidget());

                //plot.getGraphWidget().getDomainGridLinePaint().setColor(0);
                plot.getGraph().getDomainGridLinePaint().setColor(0);

                //plot.getGraphWidget().getRangeGridLinePaint().setColor(0);
                plot.getGraph().getRangeGridLinePaint().setColor(0);

                //plot.getGraphWidget().getRangeSubGridLinePaint().setColor(0);
                plot.getGraph().getRangeSubGridLinePaint().setColor(0);

                //plot.getLayoutManager().remove(plot.getLegendWidget());
                //plot.getLayoutManager().remove(plot.getDomainLabelWidget());

                LineAndPointFormatter humidityFormat = new LineAndPointFormatter();
                humidityFormat.setPointLabelFormatter(new PointLabelFormatter());
                humidityFormat.configure(ConditionsActivity.this.getApplicationContext(), R.xml.line_point_formatter_humidity);
                humidityFormat.setPointLabelFormatter(null);

                plot2.addSeries(ConditionsActivity.mXYSeriesHumidity, humidityFormat);
                plot2.setDomainBoundaries(Long.valueOf(ConditionsActivity.timestamp1), Long.valueOf(ConditionsActivity.timestamp2), BoundaryMode.FIXED);
                plot2.setRangeBoundaries(Float.valueOf(ConditionsActivity.minYHumidity), Float.valueOf(ConditionsActivity.maxYHumidity), BoundaryMode.FIXED);
                plot2.setPlotMargins(0.0f, 0.0f, 0.0f, 0.0f);
                plot2.setPlotPadding(0.0f, 0.0f, 0.0f, 0.0f);

                //plot2.getGraphWidget().setGridPaddingTop(25.0f);
                //plot2.getGraphWidget().setGridPaddingBottom(25.0f);
                // 下面是新的
                bounds = plot2.getBounds();

                plot2.setRangeBoundaries(
                        bounds.getMinY().doubleValue() - 1, BoundaryMode.FIXED,
                        bounds.getMaxY().doubleValue() + 1, BoundaryMode.FIXED);

                plot2.setDomainBoundaries(
                        bounds.getMinX().doubleValue() - 1, BoundaryMode.FIXED,
                        bounds.getMaxX().doubleValue() + 1, BoundaryMode.FIXED);

                //plot2.setDomainLabelWidget(null);
                plot2.setBackgroundPaint(null);

                //plot2.getGraphWidget().setBackgroundPaint(null);

                //plot2.getGraphWidget().getGridBackgroundPaint().setColor(-1);
                plot2.getGraph().getGridBackgroundPaint().setColor(-1);

                plot2.setBorderPaint(null);
                //plot2.getGraphWidget().setDomainLabelWidth(0.0f);
                //plot2.getGraphWidget().setRangeLabelWidth(0.0f);
                //plot2.getGraphWidget().setDomainLabelPaint(null);
                //plot2.getGraphWidget().setDomainOriginLabelPaint(null);
                //if (ConditionsActivity.mCurrentAmbientDevice.getDeviceType() == 0) {
                //    plot2.getGraphWidget().setRangeLabelHorizontalOffset(-90.0f);
                //} else if (ConditionsActivity.mCurrentAmbientDevice.getDeviceType() == 1) {
                //    plot2.getGraphWidget().setRangeLabelHorizontalOffset(-125.0f);
                //}
                //plot2.getGraphWidget().getRangeLabelPaint().setColor(ConditionsActivity.this.getResources().getColor(C0272R.color.gray50));
                //plot2.getGraphWidget().getRangeLabelPaint().setTextSize(45.0f);
                //plot2.setRangeStep(XYStepMode.SUBDIVIDE, 2.0d);
                //if (ConditionsActivity.mCurrentAmbientDevice.getDeviceType() == 0) {
                //    plot2.setRangeValueFormat(new DecimalFormat("#"));
                //} else if (ConditionsActivity.mCurrentAmbientDevice.getDeviceType() == 1) {
                //    plot2.setRangeValueFormat(new DecimalFormat("#.0"));
                //}
                //plot2.getGraphWidget().setRangeOriginLabelPaint(null);

                //plot2.getGraphWidget().setDomainOriginLinePaint(null);
                plot2.getGraph().setDomainOriginLinePaint(null);

                //plot2.getGraphWidget().setRangeOriginLinePaint(null);
                plot2.getGraph().setRangeOriginLinePaint(null);

                //plot2.getLayoutManager().remove(plot2.getTitleWidget());

                //plot2.getGraphWidget().getDomainGridLinePaint().setColor(0);
                plot2.getGraph().getDomainGridLinePaint().setColor(0);

                //plot2.getGraphWidget().getRangeGridLinePaint().setColor(0);
                plot2.getGraph().getRangeGridLinePaint().setColor(0);

                //plot2.getGraphWidget().getRangeSubGridLinePaint().setColor(0);
                plot2.getGraph().getRangeSubGridLinePaint().setColor(0);

                //plot2.getLayoutManager().remove(plot2.getLegendWidget());
                //plot2.getLayoutManager().remove(plot2.getDomainLabelWidget());
                if (ConditionsActivity.valuesTemp.size() == 0) {
                    plot.setVisibility(View.GONE);
                    if (!ConditionsActivity.mLoading) {
                        noDataTemp.setVisibility(View.VISIBLE);
                    }
                } else {
                    plot.redraw();
                    plot.setVisibility(View.VISIBLE);
                }
                if (ConditionsActivity.valuesHumidity.size() == 0) {
                    plot2.setVisibility(View.GONE);
                    if (!ConditionsActivity.mLoading) {
                        noDataHumidity.setVisibility(View.VISIBLE);
                    }
                } else {
                    plot2.redraw();
                    plot2.setVisibility(View.VISIBLE);
                }
                if (ConditionsActivity.xAxisMaster.size() == 0) {
                    view.setOnLongClickListener(null);
                    view.setOnTouchListener(null);
                } else {
                    view.setOnLongClickListener(this.mLongLickListener);
                    view.setOnTouchListener(this.mOnTouchListener);
                }
                mLightGraph.drawLightBar(ConditionsActivity.valuesLight, ConditionsActivity.xAxisLight, ConditionsActivity.timestamp1, ConditionsActivity.timestamp2);
            } else {
                plot.setVisibility(View.GONE);
                plot2.setVisibility(View.GONE);
                noDataHumidity.setVisibility(View.GONE);
                noDataTemp.setVisibility(View.GONE);
                buttonLeft.setVisibility(View.INVISIBLE);
                buttonRight.setVisibility(View.INVISIBLE);
                tv.setText(R.string.loading_data);
                mLightGraph.resetLightBar();
                view.setOnClickListener(null);
                view.setOnLongClickListener(null);
            }
            ConditionsActivity.this.mOverlay.setVisibility(View.INVISIBLE);
            ConditionsActivity.this.mCursorView.setVisibility(View.INVISIBLE);
            container.addView(view);
            return view;
        }

        @Override // android.support.p000v4.view.PagerAdapter
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override // android.support.p000v4.view.PagerAdapter
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager) container).removeView((View) object);
        }

        @Override // android.support.p000v4.view.PagerAdapter
        public int getItemPosition(Object object) {
            return -2;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private int findNearestIndexForXProportion(double xPro) {
        long currentTimestamp = Math.round(((double) (timestamp2 - timestamp1)) * xPro) + timestamp1;
        int lesserPosition = 0;
        int greaterPosition = 0;
        int i = 0;
        while (true) {
            if (i >= xAxisMaster.size()) {
                break;
            } else if (xAxisMaster.get(i).longValue() > currentTimestamp) {
                greaterPosition = i;
                break;
            } else {
                lesserPosition = i;
                i++;
            }
        }
        if (greaterPosition == 0 && lesserPosition != 0) {
            return xAxisMaster.size() - 1;
        }
        return currentTimestamp - xAxisMaster.get(lesserPosition).longValue() < xAxisMaster.get(greaterPosition).longValue() - currentTimestamp ? lesserPosition : greaterPosition;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateCursorLabels(int position) {
        long masterTimestamp = xAxisMaster.get(position).longValue();
        this.mCursorViewDate.setText(DateFormat.getDateTimeInstance(3, 3, Locale.getDefault()).format(new Date(1000 * masterTimestamp)));
        ConditionObject condition = AmbientDeviceManager.get(this).conditionEqual(mCurrentAmbientDevice.getAddress(), masterTimestamp);
        double tempValue = condition.getTempC();
        if (tempValue >= 70.1d) {
            this.mCursorViewTemp.setText(getString(R.string.temp) + ": " + getString(R.string.n_a));
        } else if (mCurrentAmbientDevice.tempUnitsC) {
            this.mCursorViewTemp.setText(getString(R.string.temp) + String.format(": %.1f°C", Double.valueOf(tempValue)));
        } else {
            this.mCursorViewTemp.setText(getString(R.string.temp) + String.format(": %.1f°F", Double.valueOf(convertCtoF(tempValue))));
        }
        double humValue = condition.getHumidity();
        if (humValue >= 100.99d || humValue < 0.0d) {
            this.mCursorViewHumidity.setText(getString(R.string.hum) + ": " + getString(R.string.n_a));
        } else if (mCurrentAmbientDevice.getDeviceType() == 0) {
            this.mCursorViewHumidity.setText(getString(R.string.hum) + String.format(": %.0f", Double.valueOf((double) Math.round(humValue))) + "%");
        } else if (mCurrentAmbientDevice.getDeviceType() == 1) {
            this.mCursorViewHumidity.setText(getString(R.string.hum) + String.format(": %.1f", Double.valueOf(humValue)) + "%");
        }
        long lightValue = condition.getLight();
        if (lightValue >= 65535 || lightValue < 0) {
            this.mCursorViewLight.setText(getString(R.string.light) + ": " + getString(R.string.n_a));
        } else {
            this.mCursorViewLight.setText(getString(R.string.light) + ": " + lightValue + " lx");
        }
    }

    public static int byteToUnsignedInt(byte b) {
        return b & 255;
    }

//    private void uploadInvalidData(String address, long timestamp, double tempC, double humidity, long light, String deviceVersion) {
//        ParseObject newDevice = new ParseObject("LoggingError");
//        newDevice.put("deviceAddress", address);
//        newDevice.put(ConditionDatabaseHelper.COLUMN_CONDITIONS_TIMESTAMP, Long.valueOf(timestamp));
//        newDevice.put(ConditionDatabaseHelper.COLUMN_CONDITIONS_TEMPC, Double.valueOf(tempC));
//        newDevice.put(ConditionDatabaseHelper.COLUMN_CONDITIONS_HUMIDITY, Double.valueOf(humidity));
//        newDevice.put(ConditionDatabaseHelper.COLUMN_CONDITIONS_LIGHT, Long.valueOf(light));
//        newDevice.put("deviceVersion", deviceVersion);
//        newDevice.put("platform", "android");
//        try {
//            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
//            String version = pInfo.versionName;
//            newDevice.put("appVersion", version + "_" + pInfo.versionCode);
//        } catch (PackageManager.NameNotFoundException e) {
//        }
//        newDevice.saveEventually();
//    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void processConditionsData(byte[] data) {
        AmbientDeviceManager manager = AmbientDeviceManager.get(this);
        String currentAddress = mCurrentAmbientDevice.getAddress();
        int[] dataInt = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            dataInt[i] = byteToUnsignedInt(data[i]);
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2016, 0, 3, 0, 0, 0);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2017, 0, 3, 0, 0, 0);
        long timestampDSTlower = cal1.getTime().getTime() / 1000;
        long timestampDSTupper = cal2.getTime().getTime() / 1000;
        String[] versions = mCurrentAmbientDevice.getFirmwareVersion().split("\\.");
        double temp1 = (((double) ((dataInt[4] * 10) + (dataInt[5] & 15))) - 400.0d) / 10.0d;
        long timestamp12 = ((long) ((dataInt[0] << 24) | (dataInt[1] << 16) | (dataInt[2] << 8) | dataInt[3])) + 1388534400;
        if (Integer.valueOf(versions[0]).intValue() == 0 && Integer.valueOf(versions[1]).intValue() <= 5 && timestamp12 >= timestampDSTlower && timestamp12 < timestampDSTupper) {
            timestamp12 -= 172800;
        }
        double humidity1 = ((double) dataInt[6]) + (((double) (dataInt[5] >> 4)) / 10.0d);
        long light1 = (long) ((dataInt[7] << 8) | dataInt[8]);
        double temp2 = (((double) ((dataInt[13] * 10) + (dataInt[14] & 15))) - 400.0d) / 10.0d;
        long timestamp22 = ((long) ((dataInt[9] << 24) | (dataInt[10] << 16) | (dataInt[11] << 8) | dataInt[12])) + 1388534400;
        if (Integer.valueOf(versions[0]).intValue() == 0 && Integer.valueOf(versions[1]).intValue() <= 5 && timestamp22 >= timestampDSTlower && timestamp22 < timestampDSTupper) {
            timestamp22 -= 172800;
        }
        double humidity2 = ((double) dataInt[15]) + (((double) (dataInt[14] >> 4)) / 10.0d);
        long light2 = (long) ((dataInt[16] << 8) | dataInt[17]);
        if (timestamp12 == 1388534400 && humidity1 == 0.0d) {
            Log.d(TAG, "downloading complete");
            getWindow().clearFlags(128);
            mCurrentAmbientDevice.updateTimestampSync();
            manager.saveAmbientDevices();
            this.mAmbientDeviceService.disconnect();
            this.mSyncInProgress = false;
            try {
                unregisterReceiver(this.mGattUpdateReceiver);
                unbindService(this.mServiceConnection);
            } catch (IllegalArgumentException e) {
            }
            this.mAmbientDeviceService = null;
            this.mProgressDialog.dismiss();
            updateTimestampsAndReload();
            Log.d(TAG, "conditionsReceived: " + this.mConditionsReceived);
            Log.d(TAG, "packetsReceived: " + this.mDataPacketsReceived);
            return;
        }
        if (timestamp22 != 1388534400 || humidity2 != 0.0d) {
            if (timestamp22 % 60 == 0) {
                if (!manager.conditionExistsEqual(currentAddress, timestamp22)) {
                    manager.insertCondition(currentAddress, timestamp22, temp2, humidity2, light2);
                }
                this.mConditionsReceived++;
            }
            if (timestamp12 % 60 == 0) {
                if (!manager.conditionExistsEqual(currentAddress, timestamp12)) {
                    manager.insertCondition(currentAddress, timestamp12, temp1, humidity1, light1);
                }
                this.mConditionsReceived++;
            }
        } else if (timestamp12 % 60 == 0) {
            if (!manager.conditionExistsEqual(currentAddress, timestamp12)) {
                manager.insertCondition(currentAddress, timestamp12, temp1, humidity1, light1);
            }
            this.mConditionsReceived++;
        }
        this.mDataPacketsReceived++;
        if (this.mDataPacketsReceived % 6 == 0) {
            this.mProgressDialog.setMessage(getString(R.string.downloading) + " " + this.mConditionsReceived);
            this.mAmbientDeviceService.requestConditionsDataMore();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startConditionsDataSync() {
        long lastTimestamp = AmbientDeviceManager.get(this).getLastTimestampForConditionsLog(mCurrentAmbientDevice.getAddress());
        Log.d(TAG, "lastTimestamp: " + lastTimestamp);
        if (lastTimestamp == 0) {
            if (mCurrentAmbientDevice.getTimestampBase() == 0) {
                if (mCurrentAmbientDevice.getTimestampBase() / 1000 >= 1388534400) {
                    this.mAmbientDeviceService.requestConditionsDataAfter((mCurrentAmbientDevice.getTimestampBase() / 1000) - 1388534400);
                } else {
                    this.mAmbientDeviceService.requestConditionsDataAll();
                }
            } else if (mCurrentAmbientDevice.getTimestampBase() / 1000 >= 1388534400) {
                this.mAmbientDeviceService.requestConditionsDataAfter((mCurrentAmbientDevice.getTimestampBase() / 1000) - 1388534400);
            }
        } else if (mCurrentAmbientDevice.getTimestampBase() / 1000 > lastTimestamp) {
            this.mAmbientDeviceService.requestConditionsDataAfter((mCurrentAmbientDevice.getTimestampBase() / 1000) - 1388534400);
        } else {
            Calendar cal1 = Calendar.getInstance();
            cal1.set(2016, 0, 1, 0, 0, 0);
            Calendar cal2 = Calendar.getInstance();
            cal2.set(2017, 0, 1, 0, 0, 0);
            long timestampDSTlower = cal1.getTime().getTime() / 1000;
            long timestampDSTupper = cal2.getTime().getTime() / 1000;
            String[] versions = mCurrentAmbientDevice.getFirmwareVersion().split("\\.");
            long timestampRequest = lastTimestamp - 1388534400;
            if (Integer.valueOf(versions[0]).intValue() == 0 && Integer.valueOf(versions[1]).intValue() <= 5 && lastTimestamp >= timestampDSTlower && lastTimestamp < timestampDSTupper) {
                timestampRequest += 172800;
            }
            this.mAmbientDeviceService.requestConditionsDataAfter(timestampRequest);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AmbientDeviceService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(AmbientDeviceService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(AmbientDeviceService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(AmbientDeviceService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(AmbientDeviceService.ACTION_SUBSCRIPTION_FINISHED);
        intentFilter.addAction(AmbientDeviceService.ACTION_WRITE_SUCCESSFUL);
        intentFilter.addAction(AmbientDeviceService.ACTION_ERROR);
        intentFilter.addAction(AmbientDeviceService.ACTION_CONDITIONS_CURRENT_RECEIVED);
        intentFilter.addAction(AmbientDeviceService.ACTION_CONDITIONS_DATA_RECEIVED);
        intentFilter.addAction(AmbientDeviceService.ACTION_AMBIENT_SETTINGS_RECEIVED);
        return intentFilter;
    }

    public static double convertCtoF(double temperature) {
        return (1.8d * temperature) + 32.0d;
    }

    /* access modifiers changed from: protected */
    public boolean isBLEEnabled() {
        BluetoothAdapter adapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    /* access modifiers changed from: protected */
    public void showBLEDialog() {
        startActivityForResult(new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE"), 0);
    }
}
