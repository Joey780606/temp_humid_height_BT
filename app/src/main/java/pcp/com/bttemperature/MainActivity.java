package pcp.com.bttemperature;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import pcp.com.bttemperature.ambientDevice.AmbientDevice;
import pcp.com.bttemperature.ambientDevice.AmbientDeviceManager;
import pcp.com.bttemperature.ble.AmbientDeviceService;
import pcp.com.bttemperature.ble.parser.AmbientDeviceAdvObject;
import pcp.com.bttemperature.database.ConditionDatabaseHelper;
import pcp.com.bttemperature.database.SQLiteCursorLoader;
import pcp.com.bttemperature.utilities.CelaerActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.androidplot.xy.XYSeries;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import com.androidplot.xy.XYPlot;

//public class MainActivity extends AppCompatActivity {
public class MainActivity extends CelaerActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    // 91 important, my variable
    ScanCallback scanCallback;
    private BluetoothLeScanner bluetoothLeScanner;
    private int permissionFINE_LOCATION;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100;
    private int mCheckBLEPermission = 0;    // Check permission
    private boolean keepScan = false;

    public static final String AMBIENT_SENSOR_UUID = "FEB1";
    public static final String EXTRAS_DISCONNECTION_CODE = "DISCONNECTION_CODE";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static boolean mDidClickItem;
    private static boolean mLoading = true;

    private static AmbientDevice mUpdateAmbientDevice;

    private AmbientDeviceSortAdapter mAdapter;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private boolean mBroadcastScan;

    private AmbientDevice mCurrentAmbientDevice;

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        /* class com.celaer.android.ambient.DeviceListActivity.C024616 */

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AmbientDeviceService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(MainActivity.TAG, "Broadcast received: CONNECTED");
                MainActivity.this.mProgressDialog.setTitle((CharSequence) null);
                MainActivity.this.mProgressDialog.setMessage(MainActivity.this.getString(R.string.connected));
                new Handler().postDelayed(new Runnable() {
                    /* class com.celaer.android.ambient.DeviceListActivity.C024616.RunnableC02471 */

                    public void run() {
                        Log.i(MainActivity.TAG, "Attempting to start service discovery" + MainActivity.this.mAmbientDeviceService.discoverServices());
                    }
                }, 3000);
            } else if (AmbientDeviceService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(MainActivity.TAG, "Broadcast received: DISCONNECTED");
                if (!MainActivity.this.mPairingComplete) {
                    MainActivity.this.mProgressDialog.dismiss();
                    new AlertDialog.Builder(MainActivity.this).setTitle(R.string.error).setMessage(R.string.pairing_error).setPositiveButton(R.string.error, (DialogInterface.OnClickListener) null).show();
                    //MainActivity.this.mBluetoothAdapter.stopLeScan(MainActivity.this);
                    joey_btScanDevices(false);
                    MainActivity.this.mBroadcastScan = true;
                    //MainActivity.this.mBluetoothAdapter.startLeScan(MainActivity.this);
                    joey_btScanDevices(true);
                }
                try {
                    MainActivity.this.unregisterReceiver(MainActivity.this.mGattUpdateReceiver);
                    MainActivity.this.unbindService(MainActivity.this.mServiceConnection);
                } catch (IllegalArgumentException e) {
                }
                MainActivity.this.stopService(new Intent(MainActivity.this, AmbientDeviceService.class));
            } else if (AmbientDeviceService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(MainActivity.TAG, "Broadcast received: SERVICES DISCOVERED");
                new Handler().postDelayed(new Runnable() {
                    /* class com.celaer.android.ambient.DeviceListActivity.C024616.RunnableC02482 */

                    public void run() {
                        Log.d(MainActivity.TAG, "Starting Subscription");
                        MainActivity.this.mAmbientDeviceService.subscribeToNotifications();
                    }
                }, 500);
            } else if (AmbientDeviceService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(MainActivity.TAG, "Broadcast received: DATA AVAILABLE");
                UUID charUUID = (UUID) intent.getExtras().get(AmbientDeviceService.CHAR_UUID);
                byte[] data = intent.getByteArrayExtra(AmbientDeviceService.EXTRA_DATA);
                if (charUUID.equals(AmbientDeviceService.BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                    byte b = data[0];
                    Log.d(MainActivity.TAG, "received batteryLevel: " + ((int) b));
                    MainActivity.this.mCurrentAmbientDevice.batteryLevel = b;
                    MainActivity.this.writeSuccessful(charUUID);
                }
            } else if (AmbientDeviceService.ACTION_SUBSCRIPTION_FINISHED.equals(action)) {
                Log.d(MainActivity.TAG, "Broadcast received: SUBSCRIPTION FINISHED");
                MainActivity.this.startClockSync();
            } else if (AmbientDeviceService.ACTION_WRITE_SUCCESSFUL.equals(action)) {
                Log.d(MainActivity.TAG, "Broadcast received: WRITE SUCCESSFUL");
                MainActivity.this.writeSuccessful((UUID) intent.getExtras().get(AmbientDeviceService.CHAR_UUID));
            } else if (AmbientDeviceService.ACTION_AMBIENT_SETTINGS_RECEIVED.equals(action)) {
                MainActivity.this.processSettingsByteArray(intent.getByteArrayExtra(AmbientDeviceService.EXTRAS_CONDITIONS));
            }
        }
    };
    private static float maxY;
    private static float maxYHumidity;
    private static float minY;
    private static float minYHumidity;
    private static long timestamp1;
    private static long timestamp2;
    private static ArrayList<Number> valuesHumidity;
    private static ArrayList<Number> valuesTemp;
    private static ArrayList<Number> xAxisHumidity;
    private static ArrayList<Number> xAxisTemp;
    private Handler handler;
    private AmbientDeviceService mAmbientDeviceService;


    private boolean mGraphInvalid = true;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private ListView mListView;
    private AmbientDeviceAdvObject mObjectToConnect;
    private int mOffset = 0;
    private boolean mPairingComplete;
    private ProgressDialog mProgressDialog;
    private boolean mRestoreSensor = false;
    private Runnable mScanRefresh;
    private int mSelectedPageIndex = 1;
    private int mSelectedZoom = 1;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        /* class com.celaer.android.ambient.DeviceListActivity.ServiceConnectionC024515 */

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            MainActivity.this.mAmbientDeviceService = ((AmbientDeviceService.LocalBinder) service).getService();
            Log.e(MainActivity.TAG, "Service Connection Successful");
            if (!MainActivity.this.mAmbientDeviceService.initialize()) {
                //Log.v(TAG, "Joey 201222 start pair 000");
                Log.e(MainActivity.TAG, "Unable to initialize Bluetooth");
                MainActivity.this.finish();
            }
            //Log.v(TAG, "Joey 201222 start pair 001");
            MainActivity.this.mAmbientDeviceService.connect(MainActivity.this.mCurrentAmbientDevice.getAddress());
        }

        public void onServiceDisconnected(ComponentName componentName) {
            MainActivity.this.mAmbientDeviceService = null;
        }
    };

    private ArrayList<AmbientDeviceAdvObject> mUnpairedObjects;
    private int mUpdatePointer = -1;
    private XYSeries mXYSeriesTemp;
    private ListView pairingListView;
    private PopupWindow popWindow;

    /* renamed from: r */
    private Runnable f12r;
    private Runnable rRestartScan;
    private Runnable rStartNextSync;

    public static final long MAX_REPORT_AGE = 86400000;

    private View.OnClickListener mConditionsListener = new View.OnClickListener() {
        /* class com.celaer.android.ambient.DeviceListActivity.View$OnClickListenerC024111 */

        public void onClick(View view) {
            View parentRow = (View) view.getParent().getParent().getParent().getParent();
            AmbientDevice device = AmbientDeviceManager.get(MainActivity.this).getAmbientDevice(((ListView) parentRow.getParent()).getPositionForView(parentRow));
            Intent intentTransition = new Intent(MainActivity.this, ConditionsActivity.class);
            intentTransition.putExtra("DEVICE_ADDRESS", device.getAddress());
            MainActivity.this.startActivityForResult(intentTransition, 3);
        }
    };

    private View.OnClickListener mSettingsButtonListener = new View.OnClickListener() {
        /* class com.celaer.android.ambient.DeviceListActivity.View$OnClickListenerC024010 */

        public void onClick(View view) {
            View parentRow = (View) view.getParent().getParent().getParent().getParent();
            AmbientDevice device = AmbientDeviceManager.get(MainActivity.this).getAmbientDevice(((ListView) parentRow.getParent()).getPositionForView(parentRow));
            Intent intentTransition = new Intent(MainActivity.this, SensorMasterSettingsActivity.class);
            intentTransition.putExtra("DEVICE_ADDRESS", device.getAddress());
            MainActivity.this.startActivityForResult(intentTransition, 4);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            //startActivity(new Intent(this, MasterSettingsActivity.class));
            return true;
        } else if (id != R.id.action_pair) {
            return super.onOptionsItemSelected(item);
        } else {
            onShowPopup(findViewById(R.id.action_pair));
            return true;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void pairSensor() {
        AmbientDeviceManager tempManager = AmbientDeviceManager.get(this);
        if (this.mObjectToConnect.getDeviceType() == 0) {
            this.mCurrentAmbientDevice = new AmbientDevice(this.mObjectToConnect, getString(R.string.sensor), tempManager.getAmbientDeviceCount());
        } else if (this.mObjectToConnect.getDeviceType() == 1) {
            this.mCurrentAmbientDevice = new AmbientDevice(this.mObjectToConnect, getString(R.string.precision), tempManager.getAmbientDeviceCount());
        }
        if (this.mObjectToConnect.createTimestampBase) {
            this.mCurrentAmbientDevice.setTimestampBase();
        }
        this.mPairingComplete = false;
        Intent gattServiceIntent = new Intent(this, AmbientDeviceService.class);
        bindService(gattServiceIntent, this.mServiceConnection, Context.BIND_AUTO_CREATE);
        startService(gattServiceIntent);
        registerReceiver(this.mGattUpdateReceiver, makeGattUpdateIntentFilter());
        //Log.v(TAG, "Joey 201222 start pair");
        this.mAdapter.notifyDataSetChanged();
        this.mProgressDialog = ProgressDialog.show(this, null, getString(R.string.connecting), true, true, new DialogInterface.OnCancelListener() {
            /* class com.celaer.android.ambient.DeviceListActivity.DialogInterface$OnCancelListenerC02536 */

            public void onCancel(DialogInterface dialog) {
                Log.d(MainActivity.TAG, "connection canceled");
                MainActivity.this.mAmbientDeviceService.disconnect();
                joey_btScanDevices(false);
                //MainActivity.this.mBluetoothAdapter.stopLeScan(DeviceListActivity.this);
                MainActivity.this.mBroadcastScan = true;
                joey_btScanDevices(true);
                //MainActivity.this.mBluetoothAdapter.startLeScan(DeviceListActivity.this);
            }
        });
    }

    public void onShowPopup(View v) {
        View inflatedView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.popup_scanner, (ViewGroup) null, false);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        float f = (float) size.y;
        mDidClickItem = false;
        try {
            unregisterReceiver(this.mGattUpdateReceiver);
            unbindService(this.mServiceConnection);
        } catch (IllegalArgumentException e) {
        }
        stopService(new Intent(this, AmbientDeviceService.class));
        this.popWindow = new PopupWindow(inflatedView, size.x - 150, size.y - 500, true);
        this.popWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_rect));
        this.popWindow.setFocusable(true);
        this.popWindow.setOutsideTouchable(true);
        this.popWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            /* class com.celaer.android.ambient.DeviceListActivity.C02547 */

            public void onDismiss() {
                MainActivity.this.handler.removeCallbacks(MainActivity.this.mScanRefresh);
                joey_btScanDevices(false);
                //MainActivity.this.mBluetoothAdapter.stopLeScan(MainActivity.this);
                MainActivity.this.mBroadcastScan = true;
                MainActivity.this.pairingListView = null;
                if (!MainActivity.mDidClickItem) {
                    Log.d(MainActivity.TAG, "restarting scan");
                    joey_btScanDevices(false);
                    //MainActivity.this.mBluetoothAdapter.stopLeScan(MainActivity.this);
                    MainActivity.this.mBroadcastScan = true;
                    joey_btScanDevices(true);
                    //MainActivity.this.mBluetoothAdapter.startLeScan(MainActivity.this);
                }
            }
        });
        ((Button) inflatedView.findViewById(R.id.popup_scanner_cancelButton)).setOnClickListener(new View.OnClickListener() {
            /* class com.celaer.android.ambient.DeviceListActivity.View$OnClickListenerC02558 */

            public void onClick(View v) {
                MainActivity.this.popWindow.dismiss();
            }
        });
        //if(this.mUnpairedObjects == null)
            this.mUnpairedObjects = new ArrayList<>();
        this.pairingListView = (ListView) inflatedView.findViewById(R.id.popup_scanner_listView);
        this.pairingListView.setEmptyView(inflatedView.findViewById(R.id.popup_scanner_list_emptyView));
        this.pairingListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            /* class com.celaer.android.ambient.DeviceListActivity.C02569 */

            @Override // android.widget.AdapterView.OnItemClickListener
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainActivity.this.mObjectToConnect = (AmbientDeviceAdvObject) parent.getItemAtPosition(position);
                Log.d(MainActivity.TAG, "connectionState: " + MainActivity.this.mBluetoothManager.getConnectionState(MainActivity.this.mObjectToConnect.getDevice(), 7));
                boolean unused = MainActivity.mDidClickItem = true;
                MainActivity.this.popWindow.dismiss();
                MainActivity.this.handler.removeCallbacks(MainActivity.this.mScanRefresh);
                MainActivity.this.mRestoreSensor = false;
                if (AmbientDeviceManager.get(MainActivity.this).getAmbientDevice(MainActivity.this.mObjectToConnect.getAddress()) != null) {
                    new AlertDialog.Builder(MainActivity.this).setTitle(R.string.previously_paired_device).setMessage(R.string.previously_paired_message).setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                        /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02644 */

                        public void onClick(DialogInterface dialogInterface, int i) {
                            AmbientDeviceManager tempManager = AmbientDeviceManager.get(MainActivity.this);
                            MainActivity.this.mCurrentAmbientDevice = tempManager.getAmbientDevice(MainActivity.this.mObjectToConnect.getAddress());
                            MainActivity.this.mCurrentAmbientDevice.setFirmwareVersion(MainActivity.this.mObjectToConnect.getFirmwareVersion());
                            MainActivity.this.mRestoreSensor = true;
                            Intent gattServiceIntent = new Intent(MainActivity.this, AmbientDeviceService.class);
                            MainActivity.this.bindService(gattServiceIntent, MainActivity.this.mServiceConnection, Context.BIND_AUTO_CREATE);
                            MainActivity.this.startService(gattServiceIntent);
                            MainActivity.this.registerReceiver(MainActivity.this.mGattUpdateReceiver, MainActivity.makeGattUpdateIntentFilter());
                            MainActivity.this.mAdapter.notifyDataSetChanged();
                            MainActivity.this.mProgressDialog = ProgressDialog.show(MainActivity.this, null, MainActivity.this.getString(R.string.connecting), true, true, new DialogInterface.OnCancelListener() {
                                /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02644.DialogInterface$OnCancelListenerC02651 */

                                public void onCancel(DialogInterface dialog) {
                                    Log.d(MainActivity.TAG, "connection canceled");
                                    MainActivity.this.mAmbientDeviceService.disconnect();
                                    joey_btScanDevices(false);
                                    //MainActivity.this.mBluetoothAdapter.stopLeScan(MainActivity.this);
                                    MainActivity.this.mBroadcastScan = true;
                                    joey_btScanDevices(true);
                                    //MainActivity.this.mBluetoothAdapter.startLeScan(MainActivity.this);
                                }
                            });
                        }
                    }).setNegativeButton(R.string.erase, new DialogInterface.OnClickListener() {
                        /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02623 */

                        public void onClick(DialogInterface dialog, int which) {
                            AmbientDeviceManager tempManager = AmbientDeviceManager.get(MainActivity.this);
                            tempManager.deleteAmbientDevice(tempManager.getAmbientDevice(MainActivity.this.mObjectToConnect.getAddress()));
                            if (MainActivity.this.mObjectToConnect.getDeviceType() == 0) {
                                MainActivity.this.mCurrentAmbientDevice = new AmbientDevice(MainActivity.this.mObjectToConnect, MainActivity.this.getString(R.string.sensor), AmbientDeviceManager.get(MainActivity.this).getAmbientDeviceCount());
                            } else if (MainActivity.this.mObjectToConnect.getDeviceType() == 1) {
                                MainActivity.this.mCurrentAmbientDevice = new AmbientDevice(MainActivity.this.mObjectToConnect, MainActivity.this.getString(R.string.precision), AmbientDeviceManager.get(MainActivity.this).getAmbientDeviceCount());
                            }
                            MainActivity.this.mPairingComplete = false;
                            Intent gattServiceIntent = new Intent(MainActivity.this, AmbientDeviceService.class);
                            MainActivity.this.bindService(gattServiceIntent, MainActivity.this.mServiceConnection, Context.BIND_AUTO_CREATE);
                            MainActivity.this.startService(gattServiceIntent);
                            MainActivity.this.registerReceiver(MainActivity.this.mGattUpdateReceiver, MainActivity.makeGattUpdateIntentFilter());
                            MainActivity.this.mAdapter.notifyDataSetChanged();
                            MainActivity.this.mProgressDialog = ProgressDialog.show(MainActivity.this, null, MainActivity.this.getString(R.string.connecting), true, true, new DialogInterface.OnCancelListener() {
                                /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02623.DialogInterface$OnCancelListenerC02631 */

                                public void onCancel(DialogInterface dialog) {
                                    Log.d(MainActivity.TAG, "connection canceled");
                                    MainActivity.this.mAmbientDeviceService.disconnect();
                                    joey_btScanDevices(false);
                                    //MainActivity.this.mBluetoothAdapter.stopLeScan(MainActivity.this);
                                    MainActivity.this.mBroadcastScan = true;
                                    joey_btScanDevices(true);
                                    //MainActivity.this.mBluetoothAdapter.startLeScan(MainActivity.this);
                                }
                            });
                        }
                    }).show();
                } else if (MainActivity.this.mObjectToConnect.getDataExists()) {
                    new AlertDialog.Builder(MainActivity.this).setTitle(R.string.previous_log_data_exists).setMessage(R.string.previous_log_message).setPositiveButton(R.string.keep, new DialogInterface.OnClickListener() {
                        /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02612 */

                        public void onClick(DialogInterface dialogInterface, int i) {
                            MainActivity.this.pairSensor();
                        }
                    }).setNegativeButton(R.string.erase, new DialogInterface.OnClickListener() {
                        /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02571 */

                        public void onClick(DialogInterface dialog, int which) {
                            new AlertDialog.Builder(MainActivity.this).setTitle(R.string.erase_or_ignore_data).setMessage(R.string.erase_or_ignore_message).setPositiveButton(R.string.keep, new DialogInterface.OnClickListener() {
                                /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02571.DialogInterface$OnClickListenerC02603 */

                                public void onClick(DialogInterface dialogInterface, int i) {
                                    MainActivity.this.pairSensor();
                                }
                            }).setNeutralButton(R.string.ignore, new DialogInterface.OnClickListener() {
                                /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02571.DialogInterface$OnClickListenerC02592 */

                                public void onClick(DialogInterface dialog, int which) {
                                    MainActivity.this.mObjectToConnect.createTimestampBase = true;
                                    MainActivity.this.pairSensor();
                                }
                            }).setNegativeButton(R.string.erase, new DialogInterface.OnClickListener() {
                                /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02571.DialogInterface$OnClickListenerC02581 */

                                public void onClick(DialogInterface dialog, int which) {
                                    MainActivity.this.mObjectToConnect.deleteTempDataAfterPairing = true;
                                    MainActivity.this.pairSensor();
                                }
                            }).show();
                        }
                    }).show();
                } else {
                    MainActivity.this.pairSensor();
                }
            }
        });
        this.mLeDeviceListAdapter = new LeDeviceListAdapter(this, this.mUnpairedObjects);
        this.mLeDeviceListAdapter.setNotifyOnChange(true);
        this.pairingListView.setAdapter((ListAdapter) this.mLeDeviceListAdapter);
        this.popWindow.showAtLocation(v, 17, 0, 100);
        //this.mLeDeviceListAdapter.notifyDataSetChanged();

        joey_btScanDevices(false);
        //this.mBluetoothAdapter.stopLeScan(this);
        this.mBroadcastScan = false;
        joey_btScanDevices(true);
        //this.mBluetoothAdapter.startLeScan(this);
        this.mScanRefresh.run();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getActionBar().setDisplayShowTitleEnabled(false);  // 91 important
        setContentView(R.layout.activity_device_list);

        SharedPreferences preferences = getSharedPreferences("pcp.com.bttemperature.preferences", 0);   // 91 important, SharePreferences use
        SharedPreferences.Editor editor = preferences.edit();
        if (preferences.getString("countryUnits", "none").equalsIgnoreCase("none")) {  // 91 important
            if (Locale.getDefault().getCountry().equalsIgnoreCase("us")) {
                Log.d(TAG, "setting default F");
                editor.putString("countryUnits", "f");
            } else if (Locale.getDefault().getCountry().equalsIgnoreCase("jp")) {
                Log.d(TAG, "setting default JP");
                editor.putString("countryUnits", "jp");
            } else {
                Log.d(TAG, "setting default C");
                editor.putString("countryUnits", "c");
            }
        }
        editor.commit();

        this.mListView = (ListView) findViewById(R.id.activity_device_list_listView);
        this.mAdapter = new AmbientDeviceSortAdapter();
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        this.mListView.setEmptyView(findViewById(R.id.activity_device_list_emptyView));

        fCheckPermission();
        this.mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = this.mBluetoothManager.getAdapter();
        this.bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        valuesTemp = new ArrayList<>();
        valuesHumidity = new ArrayList<>();
        xAxisTemp = new ArrayList<>();
        xAxisHumidity = new ArrayList<>();
        maxY = 71.0f;   //應是溫度最高單位
        minY = -41.0f;
        maxYHumidity = 101.0f;
        minYHumidity = 101.0f;
        timestamp1 = 0;
        timestamp2 = 0;
        this.mSelectedPageIndex = 1;
        this.mOffset = 0;
        this.handler = new Handler();
        this.f12r = new Runnable() {
            /* class com.celaer.android.ambient.DeviceListActivity.RunnableC02391 */

            public void run() {
                MainActivity.this.mAdapter.notifyDataSetChanged();
                MainActivity.this.handler.postDelayed(MainActivity.this.f12r, 1000);
            }
        };
        this.rStartNextSync = new Runnable() {
            /* class com.celaer.android.ambient.DeviceListActivity.RunnableC02492 */

            public void run() {
                MainActivity.this.startNextSync();
            }
        };
        this.rRestartScan = new Runnable() {
            /* class com.celaer.android.ambient.DeviceListActivity.RunnableC02503 */

            public void run() {
                MainActivity.this.restartScan();
                MainActivity.this.handler.postDelayed(MainActivity.this.rRestartScan, 30000);
            }
        };
        this.mScanRefresh = new Runnable() {
            /* class com.celaer.android.ambient.DeviceListActivity.RunnableC02514 */

            public void run() {
                if (MainActivity.this.mUnpairedObjects.size() == 0) {
                    //MainActivity.this.mBluetoothAdapter.stopLeScan(MainActivity.this);
                    //MainActivity.this.mBluetoothAdapter.startLeScan(MainActivity.this);
                    joey_btScanDevices(false);
                    joey_btScanDevices(true);
                    MainActivity.this.handler.postDelayed(MainActivity.this.mScanRefresh, 1000);
                    return;
                }
                //MainActivity.this.mBluetoothAdapter.stopLeScan(MainActivity.this);
                //MainActivity.this.mBluetoothAdapter.startLeScan(MainActivity.this);
                joey_btScanDevices(false);
                joey_btScanDevices(true);
                MainActivity.this.handler.postDelayed(MainActivity.this.mScanRefresh, 10000);
            }
        };
        this.mXYSeriesTemp = new XYSeries() {
            /* class com.celaer.android.ambient.DeviceListActivity.C02525 */

            @Override // com.androidplot.p005xy.XYSeries
            public int size() {
                return MainActivity.valuesTemp.size();
            }

            @Override // com.androidplot.p005xy.XYSeries
            public Number getX(int index) {
                return (Number) MainActivity.xAxisTemp.get(index);
            }

            @Override // com.androidplot.p005xy.XYSeries
            public Number getY(int index) {
                return (Number) MainActivity.valuesTemp.get(index);
            }

            @Override // com.androidplot.Series
            public String getTitle() {
                return null;
            }
        };
    }

    /* access modifiers changed from: protected */
    @Override // com.celaer.android.ambient.utilities.CelaerActivity
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        AmbientDeviceManager manager = AmbientDeviceManager.get(this);
        int count = manager.getAmbientDeviceCount();
        Log.d(TAG, "retrieved " + count + " devices");
        boolean errorFound = false;
        int i = 0;
        while (true) {
            if (i >= count) {
                break;
            }
            AmbientDevice device = manager.getAmbientDevice(i);
            if (device == null) {
                Log.d(TAG, "null device: " + i);
                errorFound = true;
                break;
            }
            Log.d(TAG, "device: " + device.getHexName() + " sortIndex: " + device.sortIndex);
            i++;
        }
        if (errorFound) {
            ArrayList<String> addressList = new ArrayList<>();
            ArrayList<AmbientDevice> devices = manager.getAmbientDevices();
            ArrayList<AmbientDevice> objectsToDelete = new ArrayList<>();
            for (int i2 = 0; i2 < devices.size(); i2++) {
                String deviceAddress = devices.get(i2).getAddress();
                if (addressList.contains(deviceAddress)) {
                    Log.d(TAG, "duplicate device found");
                    objectsToDelete.add(devices.get(i2));
                } else {
                    addressList.add(deviceAddress);
                }
            }
            if (objectsToDelete.size() != 0) {
                Log.d(TAG, "objectsToDelete not zero");
                Iterator<AmbientDevice> it = objectsToDelete.iterator();
                while (it.hasNext()) {
                    AmbientDevice device2 = it.next();
                    Log.d(TAG, "deleting: " + device2.getAddress());
                    manager.deleteAmbientDevice(device2);
                }
            }
        }
        SharedPreferences preferences = getSharedPreferences("com.celaer.android.ambient.preferences", 0);
        preferences.edit();
        String dstFix = preferences.getString("DST_FIX", "never");
        if (dstFix.equalsIgnoreCase("never")) {
            Log.d(TAG, "starting DST fix");
            this.mUpdatePointer = 0;
            startNextUpdate();
        } else if (dstFix.equalsIgnoreCase("yes")) {
            Log.d(TAG, "DST fix already implemented");
        }
        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        }
        //this.mBluetoothAdapter.stopLeScan(this);
        joey_btScanDevices(false);
        this.mBroadcastScan = true;
        this.rRestartScan.run();
        this.mAdapter.notifyDataSetChanged();
        this.handler.postDelayed(this.f12r, 1000);
        stopService(new Intent(this, AmbientDeviceService.class));
    }

    /* access modifiers changed from: protected */
    @Override // com.celaer.android.ambient.utilities.CelaerActivity
    public void onPause() {
        super.onPause();
        this.handler.removeCallbacks(this.f12r);
        this.handler.removeCallbacks(this.rStartNextSync);
        this.handler.removeCallbacks(this.rRestartScan);
        //this.mBluetoothAdapter.stopLeScan(this);
        joey_btScanDevices(false);
        try {
            unregisterReceiver(this.mGattUpdateReceiver);
            unbindService(this.mServiceConnection);
        } catch (IllegalArgumentException e) {
        }
        this.mAmbientDeviceService = null;
        AmbientDeviceManager.get(this).saveAmbientDevices();
    }

    private boolean startNextUpdate() {
        while (this.mUpdatePointer < AmbientDeviceManager.get(this).getAmbientDevices().size()) {
            AmbientDevice device = AmbientDeviceManager.get(this).getAmbientDevice(this.mUpdatePointer);
            this.mUpdatePointer++;
            String firmwareVersion = device.getFirmwareVersion();
            Log.d(TAG, "firmwareVersion: " + firmwareVersion);
            String[] versions = firmwareVersion.split("\\.");
            if (Integer.valueOf(versions[0]).intValue() == 0 && Integer.valueOf(versions[1]).intValue() <= 5) {
                Log.e(TAG, "found device to update: " + device.getAddress());
                Calendar cal1 = Calendar.getInstance();
                cal1.set(2016, 0, 3, 0, 0, 0);
                Calendar cal2 = Calendar.getInstance();
                cal2.set(2017, 0, 3, 0, 0, 0);
                timestamp1 = cal1.getTime().getTime() / 1000;
                timestamp2 = cal2.getTime().getTime() / 1000;
                mUpdateAmbientDevice = device;
                Log.d(TAG, "starting loader");
                getLoaderManager().restartLoader(3, null, this);
                return true;
            }
        }
        SharedPreferences.Editor editor = getSharedPreferences("com.celaer.android.ambient.preferences", 0).edit();
        editor.putString("DST_FIX", "yes");
        editor.commit();
        Log.d(TAG, "fix finished");
        return false;
    }

    private static class ConditionListBetweenCursorLoader extends SQLiteCursorLoader {
        public ConditionListBetweenCursorLoader(Context context) {
            super(context);
        }

        /* access modifiers changed from: protected */
        @Override // com.celaer.android.ambient.database.SQLiteCursorLoader
        public Cursor loadCursor() {
            Log.d(MainActivity.TAG, "loadCursor: " + MainActivity.mUpdateAmbientDevice.getAddress() + " " + MainActivity.timestamp1 + ":" + MainActivity.timestamp2);
            boolean unused = MainActivity.mLoading = true;
            return AmbientDeviceManager.get(getContext()).queryConditionsBetweenTimestamps(MainActivity.mUpdateAmbientDevice.getAddress(), MainActivity.timestamp1, MainActivity.timestamp2);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == 3) {
            return new ConditionListBetweenCursorLoader(this);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            Log.d(TAG, "conditions retrieved: " + cursor.getCount());
            cursor.moveToFirst();
            int timestampIndex = cursor.getColumnIndex(ConditionDatabaseHelper.COLUMN_CONDITIONS_TIMESTAMP);
            for (int i = 0; i < cursor.getCount(); i++) {
                AmbientDeviceManager.get(this).updateConditionTimestamp(cursor.getLong(cursor.getColumnIndex("_id")), cursor.getLong(timestampIndex) - 172800);
                cursor.moveToNext();
            }
        }
        cursor.close();
        Log.d(TAG, "onLoadFinished");
        startNextUpdate();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


    public class AmbientDeviceSortAdapter extends BaseAdapter {
        public AmbientDeviceSortAdapter() {
        }

        public int getViewTypeCount() {
            return 1;
        }

        public int getCount() {
            return AmbientDeviceManager.get(MainActivity.this).getAmbientDeviceCount();
        }

        public int getItemViewType(int position) {
            return 0;
        }

        public boolean isEnabled(int position) {
            return false;
        }

        public long getItemId(int i) {
            return (long) i;
        }

        public Object getItem(int i) {
            return AmbientDeviceManager.get(MainActivity.this).getAmbientDevice(i);
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            String tempString;
            String tempString2;
            String tempString3;
            String maxString;
            String minString;
            String tempString4;
            AmbientDevice device = (AmbientDevice) getItem(i);
            if (view == null) {
                viewHolder = new ViewHolder();
                view = MainActivity.this.getLayoutInflater().inflate(R.layout.list_item_ambient, viewGroup, false);
                viewHolder.name = (TextView) view.findViewById(R.id.list_item_ambient_nameTextView);
                viewHolder.tempUnits = (TextView) view.findViewById(R.id.list_item_ambient_tempUnits);
                viewHolder.tempNow = (TextView) view.findViewById(R.id.list_item_ambient_tempNow);
                viewHolder.tempMaxMin = (TextView) view.findViewById(R.id.list_item_ambient_tempMaxMin);
                viewHolder.humidityNow = (TextView) view.findViewById(R.id.list_item_ambient_humidityNow);
                viewHolder.humidityMaxMin = (TextView) view.findViewById(R.id.list_item_ambient_humidityMaxMin);
                viewHolder.lightNow = (TextView) view.findViewById(R.id.list_item_ambient_lightNow);
                viewHolder.lightMaxMin = (TextView) view.findViewById(R.id.list_item_ambient_lightMaxMin);
                viewHolder.colorBar1 = view.findViewById(R.id.list_item_ambient_colorBar1);
                viewHolder.colorBar2 = view.findViewById(R.id.list_item_ambient_colorBar2);
                viewHolder.settingsButton = (ImageButton) view.findViewById(R.id.list_item_ambient_settingsButton);
                viewHolder.syncLastTextView = (TextView) view.findViewById(R.id.list_item_ambient_lastSync);
                viewHolder.viewLogLayout = (LinearLayout) view.findViewById(R.id.list_item_ambient_viewLogLayout);
                viewHolder.batteryLevel = (TextView) view.findViewById(R.id.list_item_ambient_batteryText);
                viewHolder.batteryImage = (ImageView) view.findViewById(R.id.list_item_ambient_batteryImage);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            viewHolder.name.setText(device.getName());
            if (device.tempUnitsC) {
                viewHolder.tempUnits.setText("°C");
            } else {
                viewHolder.tempUnits.setText("°F");
            }
            if (device.tempUnitsC) {
                if (device.currentTempC > 71.1d) {
                    tempString = "<-30";
                } else if (device.currentTempC > 70.0d) {
                    tempString = ">70";
                } else {
                    tempString = String.format("%.1f", Double.valueOf(device.currentTempC));
                }
            } else if (device.currentTempC > 71.1d) {
                tempString = "<" + String.format("%.1f", Double.valueOf(MainActivity.convertCtoF(-30.0d)));
            } else if (device.currentTempC > 70.0d) {
                tempString = ">" + String.format("%.1f", Double.valueOf(MainActivity.convertCtoF(70.0d)));
            } else {
                tempString = String.format("%.1f", Double.valueOf(MainActivity.convertCtoF(device.currentTempC)));
            }
            viewHolder.tempNow.setText(tempString);
            if (device.currentHumidity < 5.0d) {
                tempString = "<5";
            } else if (device.currentHumidity > 99.99d) {
                if (device.currentTempC < 0.0d || device.currentTempC > 60.0d) {
                    tempString = "--";
                } else {
                    tempString = ">99";
                }
            } else if (device.getDeviceType() == 0) {
                tempString = String.format("%.0f", Double.valueOf(device.currentHumidity));
            } else if (device.getDeviceType() == 1) {
                tempString = String.format("%.1f", Double.valueOf(device.currentHumidity));
            }
            viewHolder.humidityNow.setText(tempString);
            if (device.currentLightLevel >= 4000) {
                if (device.currentLightLevel < 65535) {
                    tempString2 = String.format("%.1fk", Double.valueOf(((double) device.currentLightLevel) / 1000.0d));
                } else {
                    tempString2 = ">30k";
                }
            } else if (device.currentLightLevel >= 0) {
                tempString2 = String.format("%d", Long.valueOf(device.currentLightLevel));
            } else {
                tempString2 = "--";
            }
            viewHolder.lightNow.setText(tempString2);
            if (device.getMaxTempC() > 70.0d) {
                tempString3 = "";
            } else if (device.getMaxTempC() - device.getMinTempC() < 0.01d) {
                tempString3 = "";
            } else if (device.tempUnitsC) {
                tempString3 = String.format("%.1f - %.1f", Double.valueOf(device.getMinTempC()), Double.valueOf(device.getMaxTempC()));
            } else {
                tempString3 = String.format("%.1f - %.1f", Double.valueOf(MainActivity.convertCtoF(device.getMinTempC())), Double.valueOf(MainActivity.convertCtoF(device.getMaxTempC())));
            }
            viewHolder.tempMaxMin.setText(tempString3);
            if (device.getMaxHumidity() > 100.0d) {
                tempString3 = "";
            } else if (device.getMaxHumidity() - device.getMinHumidity() < 0.01d) {
                tempString3 = "";
            } else if (device.getDeviceType() == 0) {
                tempString3 = String.format("%.0f - %.0f", Double.valueOf(device.getMinHumidity()), Double.valueOf(device.getMaxHumidity()));
            } else if (device.getDeviceType() == 1) {
                tempString3 = String.format("%.1f - %.1f", Double.valueOf(device.getMinHumidity()), Double.valueOf(device.getMaxHumidity()));
            }
            viewHolder.humidityMaxMin.setText(tempString3);
            if (device.getMaxLightLevel() == 65535) {
                tempString4 = "";
            } else if (device.getMaxLightLevel() - device.getMinLightLevel() == 0) {
                tempString4 = "";
            } else {
                if (device.getMaxLightLevel() >= 1000) {
                    maxString = String.format("%.1fk", Double.valueOf(((double) device.getMaxLightLevel()) / 1000.0d));
                } else {
                    maxString = String.format("%d", Long.valueOf(device.getMaxLightLevel()));
                }
                if (device.getMinLightLevel() >= 1000) {
                    minString = String.format("%.1fk", Double.valueOf(((double) device.getMinLightLevel()) / 1000.0d));
                } else {
                    minString = String.format("%d", Long.valueOf(device.getMinLightLevel()));
                }
                tempString4 = minString + " - " + maxString;
            }
            viewHolder.lightMaxMin.setText(tempString4);
            String colorString = "#" + String.format("%02X%02X%02X", Integer.valueOf(device.ledNormalColorR), Integer.valueOf(device.ledNormalColorG), Integer.valueOf(device.ledNormalColorB));
            viewHolder.colorBar1.setBackgroundColor(Color.parseColor(colorString));
            viewHolder.colorBar2.setBackgroundColor(Color.parseColor(colorString));
            long deviceLastUpdated = device.getTimestampBroadcast();
            long nowTimestamp = new Date().getTime();
            if (nowTimestamp - deviceLastUpdated < 60000) {
                viewHolder.syncLastTextView.setText("<1 " + MainActivity.this.getString(R.string.min));
            } else if (nowTimestamp - deviceLastUpdated < 3600000) {
                int minToShow = (int) Math.floor((double) ((nowTimestamp - deviceLastUpdated) / 60000));
                viewHolder.syncLastTextView.setText(String.format("%d", Integer.valueOf(minToShow)) + " " + MainActivity.this.getString(R.string.min));
            } else if (nowTimestamp - deviceLastUpdated < MAX_REPORT_AGE) {
                int hoursToShow = (int) Math.floor((double) ((nowTimestamp - deviceLastUpdated) / 3600000));
                viewHolder.syncLastTextView.setText(String.format("%d", Integer.valueOf(hoursToShow)) + " " + MainActivity.this.getString(R.string.hour));
            } else if (nowTimestamp - deviceLastUpdated < 8553600000L) {
                int daysToShow = (int) Math.floor((double) ((nowTimestamp - deviceLastUpdated) / 356400000));
                if (daysToShow == 1) {
                    viewHolder.syncLastTextView.setText(String.format("%d", Integer.valueOf(daysToShow)) + " " + MainActivity.this.getString(R.string.day));
                } else {
                    viewHolder.syncLastTextView.setText(String.format("%d", Integer.valueOf(daysToShow)) + " " + MainActivity.this.getString(R.string.days));
                }
            } else {
                viewHolder.syncLastTextView.setText(">99 " + MainActivity.this.getString(R.string.days));
            }
            viewHolder.batteryLevel.setText(device.batteryLevel + "%");
            if (device.batteryLevel > 60) {
                viewHolder.batteryImage.setBackgroundResource(R.drawable.ic_battery_green);
            } else if (device.batteryLevel > 10) {
                viewHolder.batteryImage.setBackgroundResource(R.drawable.ic_battery_orange);
            } else {
                viewHolder.batteryImage.setBackgroundResource(R.drawable.ic_battery_red);
            }
            viewHolder.settingsButton.setOnClickListener(MainActivity.this.mSettingsButtonListener);
            viewHolder.viewLogLayout.setOnClickListener(MainActivity.this.mConditionsListener);
            viewHolder.row = i;
            return view;
        }

        private class ViewHolder {
            ImageView batteryImage;
            TextView batteryLevel;
            View colorBar1;
            View colorBar2;
            LinearLayout expandedLayout;
            XYPlot graph1;
            LinearLayout graphLayout;
            TextView humidityMaxMin;
            TextView humidityNow;
            TextView lightMaxMin;
            TextView lightNow;
            TextView name;
            int row;
            ImageButton settingsButton;
            ImageButton syncButton;
            TextView syncLastTextView;
            TextView syncTextView;
            TextView tempMaxMin;
            TextView tempNow;
            TextView tempUnits;
            ToggleButton toggle0;
            ToggleButton toggle1;
            ToggleButton toggle2;
            ToggleButton toggle3;
            ToggleButton toggle4;
            ToggleButton toggle5;
            LinearLayout viewLogLayout;
            ViewPager viewPager;

            private ViewHolder() {
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startNextSync() {
        if (this.mCurrentAmbientDevice.mSyncBattery) {
            this.mCurrentAmbientDevice.mSyncState = AmbientDevice.SyncState.BLE_OBJECT_SYNC_STATE_Battery;
            this.mProgressDialog.setTitle((CharSequence) null);
            this.mProgressDialog.setMessage(getString(R.string.syncing));
            this.mAmbientDeviceService.readBatteryLevel();
        }
        if (this.mCurrentAmbientDevice.mSyncTime) {
            this.mCurrentAmbientDevice.mSyncState = AmbientDevice.SyncState.BLE_OBJECT_SYNC_STATE_Time;
            this.mProgressDialog.setMessage(getString(R.string.syncing));
            this.mAmbientDeviceService.writeClockTime(this.mObjectToConnect.deleteTempDataAfterPairing);
        } else if (this.mCurrentAmbientDevice.mSyncDST) {
            this.mCurrentAmbientDevice.mSyncState = AmbientDevice.SyncState.BLE_OBJECT_SYNC_STATE_DST;
            this.mAmbientDeviceService.writeDst(this.mCurrentAmbientDevice.dstEnabled);
        } else if (this.mCurrentAmbientDevice.mSyncReadSettings) {
            this.mProgressDialog.setMessage(getString(R.string.syncing));
            this.mCurrentAmbientDevice.mSyncState = AmbientDevice.SyncState.BLE_OBJECT_SYNC_STATE_ReadSettings;
            this.mAmbientDeviceService.readSettings();
        } else if (this.mCurrentAmbientDevice.mSyncLCDSettings) {
            this.mCurrentAmbientDevice.mSyncState = AmbientDevice.SyncState.BLE_OBJECT_SYNC_STATE_LCDSettings;
            this.mAmbientDeviceService.writeLCDSettings(this.mCurrentAmbientDevice);
        } else {
            this.mCurrentAmbientDevice.mSyncState = AmbientDevice.SyncState.BLE_OBJECT_SYNC_STATE_Idle;
            syncSuccessful();
        }
    }

    private void syncSuccessful() {
        this.mProgressDialog.dismiss();
        this.mAmbientDeviceService.disconnect();
        if (!this.mRestoreSensor) {
            AmbientDeviceManager.get(this).addAmbientDevice(this.mCurrentAmbientDevice);
        }
        AmbientDeviceManager.get(this).saveAmbientDevices();
        this.mPairingComplete = true;
        this.mAdapter.notifyDataSetChanged();
        this.mBroadcastScan = true;
        //this.mBluetoothAdapter.startLeScan(this);
        joey_btScanDevices(true);
        Intent gattServiceIntent = new Intent(this, AmbientDeviceService.class);
        try {
            unregisterReceiver(this.mGattUpdateReceiver);
            unbindService(this.mServiceConnection);
            stopService(gattServiceIntent);
        } catch (IllegalArgumentException e) {
        }
    }

    public static double convertCtoF(double temperature) {
        return (1.8d * temperature) + 32.0d;
    }

    private void processSettingsByteArray(byte[] setByte) {
        int[] setInt = new int[setByte.length];
        for (int i = 0; i < setByte.length; i++) {
            setInt[i] = byteToUnsignedInt(setByte[i]);
        }
        if (setInt[0] == 126) {
            if ((setInt[1] & 1) == 1) {
                this.mCurrentAmbientDevice.ledEnabled = true;
            } else {
                this.mCurrentAmbientDevice.ledEnabled = false;
            }
            if (((setInt[1] >> 1) & 1) == 1) {
                this.mCurrentAmbientDevice.ledAutoBrightnessEnabled = true;
            } else {
                this.mCurrentAmbientDevice.ledAutoBrightnessEnabled = false;
            }
            this.mCurrentAmbientDevice.ledBrightnessLevel = (setInt[1] >> 2) & 15;
            if (((setInt[1] >> 6) & 1) == 1) {
                this.mCurrentAmbientDevice.autoOnOffEnabled = true;
            } else {
                this.mCurrentAmbientDevice.autoOnOffEnabled = false;
            }
            this.mCurrentAmbientDevice.ledNormalColorR = setInt[2];
            this.mCurrentAmbientDevice.ledNormalColorG = setInt[3];
            this.mCurrentAmbientDevice.ledNormalColorB = setInt[4];
            this.mCurrentAmbientDevice.autoStartHr = setInt[8];
            this.mCurrentAmbientDevice.autoStartMin = setInt[9];
            this.mCurrentAmbientDevice.autoStopHr = setInt[10];
            this.mCurrentAmbientDevice.autoStopMin = setInt[11];
            Log.d(TAG, "settings1 received");
        } else if (setInt[0] == 127) {
            if ((setInt[1] & 1) == 1) {
                this.mCurrentAmbientDevice.lcdEnabled = true;
            } else {
                this.mCurrentAmbientDevice.lcdEnabled = false;
            }
            if (getSharedPreferences("com.celaer.android.ambient.preferences", 0).getString("countryUnits", "none").equalsIgnoreCase("f")) {
                this.mCurrentAmbientDevice.tempUnitsC = false;
            } else {
                this.mCurrentAmbientDevice.tempUnitsC = true;
            }
            if (((setInt[1] >> 2) & 1) == 1) {
                this.mCurrentAmbientDevice.timeFormat24 = true;
            } else {
                this.mCurrentAmbientDevice.timeFormat24 = false;
            }
            if (((setInt[1] >> 3) & 1) == 1) {
                this.mCurrentAmbientDevice.lcdScroll = true;
            } else {
                this.mCurrentAmbientDevice.lcdScroll = false;
            }
            this.mCurrentAmbientDevice.lcdDisplayIndex = setInt[1] >> 5;
            if ((setInt[2] & 1) == 1) {
                this.mCurrentAmbientDevice.measureTempHumidityEnabled = true;
            } else {
                this.mCurrentAmbientDevice.measureTempHumidityEnabled = false;
            }
            if (((setInt[2] >> 2) & 1) == 1) {
                this.mCurrentAmbientDevice.measureLightEnabled = true;
            } else {
                this.mCurrentAmbientDevice.measureLightEnabled = false;
            }
            this.mCurrentAmbientDevice.measureTempHumiditySec = (setInt[3] << 8) | setInt[4];
            this.mCurrentAmbientDevice.measureLightSec = (setInt[5] << 8) | setInt[6];
            if ((setInt[7] & 1) == 1) {
                this.mCurrentAmbientDevice.loggingEnabled = true;
            } else {
                this.mCurrentAmbientDevice.loggingEnabled = false;
            }
            this.mCurrentAmbientDevice.loggingSec = (setInt[8] << 8) | setInt[9];
            this.mCurrentAmbientDevice.broadcastSec = (setInt[10] << 8) | setInt[11];
            Log.d(TAG, "settings2 received");
            if (this.mCurrentAmbientDevice.mSyncReadSettings) {
                this.mCurrentAmbientDevice.mSyncReadSettings = false;
                startNextSync();
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void restartScan() {
        if (this.mBroadcastScan) {
            Log.d(TAG, "restartScan");
            //this.mBluetoothAdapter.stopLeScan(this);
            //this.mBluetoothAdapter.startLeScan(this);
            joey_btScanDevices(false);
            joey_btScanDevices(true);
        }
    }

    private void writeSuccessful(UUID charUUID) {
        if (charUUID.equals(AmbientDeviceService.BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
            this.mCurrentAmbientDevice.mSyncBattery = false;
            Log.d(TAG, "Battery Sync Successful");
        } else if (charUUID.equals(AmbientDeviceService.TIME_CHARACTERISTIC_UUID)) {
            this.mCurrentAmbientDevice.mSyncTime = false;
            Log.d(TAG, "Time Sync Successful");
        } else if (charUUID.equals(AmbientDeviceService.NEXT_DST_CHARACTERISTIC_UUID)) {
            this.mCurrentAmbientDevice.mSyncDST = false;
            Log.d(TAG, "DST Sync Successful");
        } else if (charUUID.equals(AmbientDeviceService.AMBIENT_SETTINGS_CHARACTERISTIC_UUID)) {
            if (this.mCurrentAmbientDevice.mSyncReadSettings) {
                return;
            }
            if (this.mCurrentAmbientDevice.mSyncLCDSettings) {
                this.mCurrentAmbientDevice.mSyncLCDSettings = false;
                Log.d(TAG, "LCD Settings Sync Successful");
            }
        } else if (charUUID.equals(AmbientDeviceService.CONDITIONS_DATA_CHARACTERISTIC_UUID)) {
            return;
        }
        this.handler.postDelayed(this.rStartNextSync, 500);
    }

    private void startClockSync() {
        this.mCurrentAmbientDevice.mSyncBattery = true;
        this.mCurrentAmbientDevice.mSyncTime = true;
        this.mCurrentAmbientDevice.mSyncDST = true;
        this.mCurrentAmbientDevice.mSyncReadSettings = true;
        this.mCurrentAmbientDevice.mSyncLCDSettings = true;
        this.handler.postDelayed(this.rStartNextSync, 1000);
    }

    public static int byteToUnsignedInt(byte b) {
        return b & 255;
    }

    // 91 important, New BT scan
    public void joey_btScanDevices(boolean action) {

        if(action == true)
            if(!keepScan)
                keepScan = true;
            else
                return;
        else {
            if(keepScan)
                return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Log.v(TAG, "Joey test 000:" + (this.mBluetoothAdapter != null) + " , " + action);
            if (this.mBluetoothAdapter != null) {
                fCheckBle(action);
            }
        }
    }

    @RequiresApi(21)
    void fCheckBle(boolean bOnOff) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(bOnOff) {
                scanCallback = null;
                scanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult scanResult) {
                        //Log.v(TAG, "Joey test 000---------------------------");
                        final AmbientDeviceAdvObject newObject = new AmbientDeviceAdvObject(scanResult.getDevice(), scanResult.getScanRecord().getBytes());
                        //scanResult.
                        //Log.v(TAG, "Joey test 001:" + MainActivity.this.mBroadcastScan + " , " + newObject.getUUID() + " , " + newObject.isBroadcastMode() + " , " + newObject.getFirmwareVersionComponents()[0] + " , " + newObject.getFirmwareVersionComponents()[1] + " , " + scanResult.getDevice().getAddress());
                        if (MainActivity.this.mBroadcastScan) {
                            //Log.v(TAG, "Joey test 0021");
                            if (newObject.getUUID().equals(AMBIENT_SENSOR_UUID) && newObject.isBroadcastMode()) {
                                runOnUiThread(new Runnable() {
                                    /* class com.celaer.android.ambient.DeviceListActivity.RunnableC024212 */

                                    public void run() {
                                        //Log.v(TAG, "Joey test 0022");
                                        MainActivity.this.updateDevice(newObject);
                                    }
                                });
                            }
                        } else if (newObject.getUUID().equals(AMBIENT_SENSOR_UUID) && !newObject.isBroadcastMode()) {
                            //Log.v(TAG, "Joey test 0031");
                            int[] version = newObject.getFirmwareVersionComponents();
                            if ((version[0] == 0 && version[1] >= 2) || version[0] > 0) {
                                runOnUiThread(new Runnable() {
                                    /* class com.celaer.android.ambient.DeviceListActivity.RunnableC024313 */

                                    public void run() {
                                        //Log.v(TAG, "Joey test 0032");
                                        MainActivity.this.deviceFound(newObject);
                                    }
                                });
                            }
                        }
                        //Log.v(TAG, "Joey test 000===================");
                    }

                    @Override
                    public void onScanFailed(int i) {

                    }
                };

                bluetoothLeScanner.startScan(scanCallback);
            } else {
                if(scanCallback != null) {
                    MainActivity.this.bluetoothLeScanner.stopScan(scanCallback);
                }
            }
        }
    }

    private void updateDevice(AmbientDeviceAdvObject object) {
        AmbientDevice existingDevice = AmbientDeviceManager.get(this).getAmbientDevice(object.getAddress());
        if (existingDevice != null) {
            Log.d(TAG, "updating device: " + object.getAddress());
            existingDevice.processBroadcast(object);
            AmbientDeviceManager.get(this).saveAmbientDevices();
        }
    }

    private void deviceFound(AmbientDeviceAdvObject newObject) {
        boolean deviceFound = false;
        Iterator<AmbientDeviceAdvObject> it = this.mUnpairedObjects.iterator();
        while (true) {
            if (it.hasNext()) {
                if (it.next().getAddress().equals(newObject.getAddress())) {
                    deviceFound = true;
                    break;
                }
            } else {
                break;
            }
        }
        if (!deviceFound) {
            //Log.d(TAG, "Joey test 0050 foundObject: " + newObject);
            this.mUnpairedObjects.add(newObject);
            this.mLeDeviceListAdapter.notifyDataSetChanged();
            //Log.d(TAG, "Joey test 0051 : " + this.mLeDeviceListAdapter.getCount());
        }
    }

    /* access modifiers changed from: private */
    public class LeDeviceListAdapter extends ArrayAdapter<AmbientDeviceAdvObject> {
        private LayoutInflater mInflator;

        public LeDeviceListAdapter(Context context, ArrayList<AmbientDeviceAdvObject> ambientDevices) {
            super(context, 0, ambientDevices);
            this.mInflator = MainActivity.this.getLayoutInflater();
            //Log.v(TAG, "Joey test 005 constructor");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            boolean isNull = false;
            AmbientDeviceAdvObject currentAmbientAdvObject = (AmbientDeviceAdvObject) getItem(position);
            if (convertView == null) {
                isNull = true;
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.popup_scanner, parent, false);
                viewHolder.name = (TextView) convertView.findViewById(R.id.popup_scanner_list_textView);
                //viewHolder.name = (TextView) convertView.findViewById(R.id.popup_scanner_listView);
                convertView.setTag(viewHolder);

                Joey_pair_device(currentAmbientAdvObject);
            } else {
                isNull = false;
                viewHolder = (ViewHolder) convertView.getTag();
                //viewHolder.name = (TextView) convertView.findViewById(R.id.popup_scanner_list_emptyView);
            }
            viewHolder.name.setText(MainActivity.this.getString(R.string.sensor) + " " + currentAmbientAdvObject.getHexName());
            Log.v(TAG, "Joey test 005:" + MainActivity.this.getString(R.string.sensor) + " " + currentAmbientAdvObject.getHexName() + " , " +  position + " , " + isNull + " , " + currentAmbientAdvObject);
            viewHolder.row = position;


            //MainActivity.this.popWindow.showAtLocation(convertView, 17, 0, 100);
            //notifyDataSetChanged();
            return convertView;
        }

        @Override
        public int getCount() {
            return super.getCount();
        }

        private class ViewHolder {
            TextView name;
            int row;

            private ViewHolder() {
            }
        }
    }

    /* access modifiers changed from: private */
    public static IntentFilter makeGattUpdateIntentFilter() {
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

    private void fCheckPermission() {
        int iWork = -1;
        permissionFINE_LOCATION = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        //permissionACCESS_BACKGROUND_LOCATION = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);

        Log.v(TAG, "permissionCheck check:" + permissionFINE_LOCATION);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {  // Need < 6.0
            iWork = 1;
        } else if (permissionFINE_LOCATION != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else if (permissionFINE_LOCATION == PackageManager.PERMISSION_GRANTED) {
            iWork = 1;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        int ivalue = -1;
        mCheckBLEPermission = 0;
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                for (int i = 0; i < permissions.length; i++) {
                    Log.v(TAG, "PermissionResult:" + i + " , " + permissions[i] + " , " + grantResults[i]);
                    if (ivalue == -1) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED && permissions[i].equals("android.permission.ACCESS_FINE_LOCATION")) {
                            ivalue = 1;
                            break;
                        }
                    }
                }
                // If request is cancelled, the result arrays are empty.
                if (permissions.length > 0 && ivalue == 1) {
                    mCheckBLEPermission = 1;
                }
                break;
            }

            default:
                break;
        }
    }

    private void ensureBLESupported() {
        if (!getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
            Toast.makeText(this, (int) R.string.ble_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }
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

    private void Joey_pair_device(AmbientDeviceAdvObject currentAmbientAdvObject) {
        MainActivity.this.mObjectToConnect = currentAmbientAdvObject;
        Log.d(MainActivity.TAG, "connectionState: " + MainActivity.this.mBluetoothManager.getConnectionState(MainActivity.this.mObjectToConnect.getDevice(), 7));
        boolean unused = MainActivity.mDidClickItem = true;
        MainActivity.this.popWindow.dismiss();
        MainActivity.this.handler.removeCallbacks(MainActivity.this.mScanRefresh);
        MainActivity.this.mRestoreSensor = false;
        if (AmbientDeviceManager.get(MainActivity.this).getAmbientDevice(MainActivity.this.mObjectToConnect.getAddress()) != null) {
            new AlertDialog.Builder(MainActivity.this).setTitle(R.string.previously_paired_device).setMessage(R.string.previously_paired_message).setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02644 */

                public void onClick(DialogInterface dialogInterface, int i) {
                    AmbientDeviceManager tempManager = AmbientDeviceManager.get(MainActivity.this);
                    MainActivity.this.mCurrentAmbientDevice = tempManager.getAmbientDevice(MainActivity.this.mObjectToConnect.getAddress());
                    MainActivity.this.mCurrentAmbientDevice.setFirmwareVersion(MainActivity.this.mObjectToConnect.getFirmwareVersion());
                    MainActivity.this.mRestoreSensor = true;
                    Intent gattServiceIntent = new Intent(MainActivity.this, AmbientDeviceService.class);
                    MainActivity.this.bindService(gattServiceIntent, MainActivity.this.mServiceConnection, Context.BIND_AUTO_CREATE);
                    MainActivity.this.startService(gattServiceIntent);
                    MainActivity.this.registerReceiver(MainActivity.this.mGattUpdateReceiver, MainActivity.makeGattUpdateIntentFilter());
                    MainActivity.this.mAdapter.notifyDataSetChanged();
                    MainActivity.this.mProgressDialog = ProgressDialog.show(MainActivity.this, null, MainActivity.this.getString(R.string.connecting), true, true, new DialogInterface.OnCancelListener() {
                        /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02644.DialogInterface$OnCancelListenerC02651 */

                        public void onCancel(DialogInterface dialog) {
                            Log.d(MainActivity.TAG, "connection canceled");
                            MainActivity.this.mAmbientDeviceService.disconnect();
                            joey_btScanDevices(false);
                            //MainActivity.this.mBluetoothAdapter.stopLeScan(MainActivity.this);
                            MainActivity.this.mBroadcastScan = true;
                            joey_btScanDevices(true);
                            //MainActivity.this.mBluetoothAdapter.startLeScan(MainActivity.this);
                        }
                    });
                }
            }).setNegativeButton(R.string.erase, new DialogInterface.OnClickListener() {
                /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02623 */

                public void onClick(DialogInterface dialog, int which) {
                    AmbientDeviceManager tempManager = AmbientDeviceManager.get(MainActivity.this);
                    tempManager.deleteAmbientDevice(tempManager.getAmbientDevice(MainActivity.this.mObjectToConnect.getAddress()));
                    if (MainActivity.this.mObjectToConnect.getDeviceType() == 0) {
                        MainActivity.this.mCurrentAmbientDevice = new AmbientDevice(MainActivity.this.mObjectToConnect, MainActivity.this.getString(R.string.sensor), AmbientDeviceManager.get(MainActivity.this).getAmbientDeviceCount());
                    } else if (MainActivity.this.mObjectToConnect.getDeviceType() == 1) {
                        MainActivity.this.mCurrentAmbientDevice = new AmbientDevice(MainActivity.this.mObjectToConnect, MainActivity.this.getString(R.string.precision), AmbientDeviceManager.get(MainActivity.this).getAmbientDeviceCount());
                    }
                    MainActivity.this.mPairingComplete = false;
                    Intent gattServiceIntent = new Intent(MainActivity.this, AmbientDeviceService.class);
                    MainActivity.this.bindService(gattServiceIntent, MainActivity.this.mServiceConnection, Context.BIND_AUTO_CREATE);
                    MainActivity.this.startService(gattServiceIntent);
                    MainActivity.this.registerReceiver(MainActivity.this.mGattUpdateReceiver, MainActivity.makeGattUpdateIntentFilter());
                    MainActivity.this.mAdapter.notifyDataSetChanged();
                    MainActivity.this.mProgressDialog = ProgressDialog.show(MainActivity.this, null, MainActivity.this.getString(R.string.connecting), true, true, new DialogInterface.OnCancelListener() {
                        /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02623.DialogInterface$OnCancelListenerC02631 */

                        public void onCancel(DialogInterface dialog) {
                            Log.d(MainActivity.TAG, "connection canceled");
                            MainActivity.this.mAmbientDeviceService.disconnect();
                            joey_btScanDevices(false);
                            //MainActivity.this.mBluetoothAdapter.stopLeScan(MainActivity.this);
                            MainActivity.this.mBroadcastScan = true;
                            joey_btScanDevices(true);
                            //MainActivity.this.mBluetoothAdapter.startLeScan(MainActivity.this);
                        }
                    });
                }
            }).show();
        } else if (MainActivity.this.mObjectToConnect.getDataExists()) {
            new AlertDialog.Builder(MainActivity.this).setTitle(R.string.previous_log_data_exists).setMessage(R.string.previous_log_message).setPositiveButton(R.string.keep, new DialogInterface.OnClickListener() {
                /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02612 */

                public void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.pairSensor();
                }
            }).setNegativeButton(R.string.erase, new DialogInterface.OnClickListener() {
                /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02571 */

                public void onClick(DialogInterface dialog, int which) {
                    new AlertDialog.Builder(MainActivity.this).setTitle(R.string.erase_or_ignore_data).setMessage(R.string.erase_or_ignore_message).setPositiveButton(R.string.keep, new DialogInterface.OnClickListener() {
                        /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02571.DialogInterface$OnClickListenerC02603 */

                        public void onClick(DialogInterface dialogInterface, int i) {
                            MainActivity.this.pairSensor();
                        }
                    }).setNeutralButton(R.string.ignore, new DialogInterface.OnClickListener() {
                        /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02571.DialogInterface$OnClickListenerC02592 */

                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.mObjectToConnect.createTimestampBase = true;
                            MainActivity.this.pairSensor();
                        }
                    }).setNegativeButton(R.string.erase, new DialogInterface.OnClickListener() {
                        /* class com.celaer.android.ambient.DeviceListActivity.C02569.DialogInterface$OnClickListenerC02571.DialogInterface$OnClickListenerC02581 */

                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.mObjectToConnect.deleteTempDataAfterPairing = true;
                            MainActivity.this.pairSensor();
                        }
                    }).show();
                }
            }).show();
        } else {
            MainActivity.this.pairSensor();
        }
    }
}