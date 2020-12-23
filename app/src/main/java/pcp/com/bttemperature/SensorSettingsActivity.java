package pcp.com.bttemperature;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import pcp.com.bttemperature.ambientDevice.AmbientDevice;
import pcp.com.bttemperature.ambientDevice.AmbientDeviceManager;
import pcp.com.bttemperature.ble.AmbientDeviceService;
import pcp.com.bttemperature.controls.ColorBar;
import pcp.com.bttemperature.utilities.CelaerActivity;
import pcp.com.bttemperature.utilities.MyApplication;

public class SensorSettingsActivity extends CelaerActivity {
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final long IDLE_TIMEOUT = 180000;
    private static final String TAG = SensorSettingsActivity.class.getSimpleName();
    private Handler handler;
    private AmbientSettingsAdapter mAdapter;
    private AlertDialog mAlertDialog;
    private AmbientDeviceService mAmbientDeviceService;
    private boolean mBackButtonPressed = false;
    private int mConditionsReceived;
    private AmbientDevice mCurrentAmbientDevice;
    private int mDataPacketsReceived;
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        /* class com.celaer.android.ambient.SensorSettingsActivity.C028310 */

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AmbientDeviceService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(SensorSettingsActivity.TAG, "Broadcast received: CONNECTED");
                SensorSettingsActivity.this.mProgressDialog.setMessage(SensorSettingsActivity.this.getString(R.string.connected));
                SensorSettingsActivity.this.handler.postDelayed(SensorSettingsActivity.this.f15r, 4000);
                SensorSettingsActivity.this.resetIdleTimeout();
            } else if (AmbientDeviceService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(SensorSettingsActivity.TAG, "Broadcast received: DISCONNECTED");
                SensorSettingsActivity.this.getWindow().clearFlags(128);
                SensorSettingsActivity.this.handler.removeCallbacks(SensorSettingsActivity.this.rIdleTimeout);
                SensorSettingsActivity.this.handler.removeCallbacks(SensorSettingsActivity.this.f15r);
                SensorSettingsActivity.this.handler.removeCallbacks(SensorSettingsActivity.this.rSubscribe);
                if (!SensorSettingsActivity.this.mBackButtonPressed) {
                    SensorSettingsActivity.this.mAmbientDeviceService.connect(SensorSettingsActivity.this.mCurrentAmbientDevice.getAddress());
                }
            } else if (AmbientDeviceService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(SensorSettingsActivity.TAG, "Broadcast received: SERVICES DISCOVERED");
                SensorSettingsActivity.this.mProgressDialog.setMessage(SensorSettingsActivity.this.getString(R.string.syncing));
                SensorSettingsActivity.this.handler.postDelayed(SensorSettingsActivity.this.rSubscribe, 1000);
            } else if (AmbientDeviceService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(SensorSettingsActivity.TAG, "Broadcast received: DATA AVAILABLE");
                byte[] data = intent.getByteArrayExtra(AmbientDeviceService.EXTRA_DATA);
                if (((UUID) intent.getExtras().get(AmbientDeviceService.CHAR_UUID)).equals(AmbientDeviceService.BATTERY_LEVEL_CHARACTERISTIC_UUID)) {
                    byte b = data[0];
                    Log.d(SensorSettingsActivity.TAG, "received batteryLevel: " + ((int) b));
                    SensorSettingsActivity.this.mCurrentAmbientDevice.batteryLevel = b;
                }
            } else if (AmbientDeviceService.ACTION_SUBSCRIPTION_FINISHED.equals(action)) {
                Log.d(SensorSettingsActivity.TAG, "Broadcast received: SUBSCRIPTION FINISHED");
                SensorSettingsActivity.this.startAllSettingsSync();
            } else if (AmbientDeviceService.ACTION_WRITE_SUCCESSFUL.equals(action)) {
                Log.d(SensorSettingsActivity.TAG, "Broadcast received: WRITE SUCCESSFUL");
                SensorSettingsActivity.this.writeSuccessful((UUID) intent.getExtras().get(AmbientDeviceService.CHAR_UUID));
            } else if (AmbientDeviceService.ACTION_CONDITIONS_DATA_RECEIVED.equals(action)) {
                byte[] conditionsByte = intent.getByteArrayExtra(AmbientDeviceService.EXTRAS_CONDITIONS);
                SensorSettingsActivity.this.resetIdleTimeout();
                SensorSettingsActivity.this.processConditionsData(conditionsByte);
            } else if (AmbientDeviceService.ACTION_AMBIENT_SETTINGS_RECEIVED.equals(action)) {
                SensorSettingsActivity.this.processSettingsByteArray(intent.getByteArrayExtra(AmbientDeviceService.EXTRAS_CONDITIONS));
            }
        }
    };
    private boolean mIntentionalDisconnect = false;
    private boolean mInterruptDownloading = false;
    private ListView mListView;
    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        /* class com.celaer.android.ambient.SensorSettingsActivity.C02864 */

        @Override // android.widget.AdapterView.OnItemClickListener
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            int incLED = 0;
            int incLEDBrightness = 0;
            int incLCDScroll = 0;
            if (SensorSettingsActivity.this.mCurrentAmbientDevice.ledEnabled) {
                incLED = 0 + 2;
                if (!SensorSettingsActivity.this.mCurrentAmbientDevice.ledAutoBrightnessEnabled) {
                    incLEDBrightness = 0 + 1;
                }
            }
            if (!SensorSettingsActivity.this.mCurrentAmbientDevice.lcdScroll) {
                incLCDScroll = 0 + 1;
            }
            int incLCDTotal = incLED + 2 + incLEDBrightness + 0;
            if (SensorSettingsActivity.this.getSharedPreferences("com.celaer.android.ambient.preferences", 0).getString("countryUnits", "none").equalsIgnoreCase("jp")) {
                incLCDTotal--;
            }
            Log.d(SensorSettingsActivity.TAG, "cell pressed: " + i);
            if (i != 0 && i != 1) {
                if (i < incLED + 2 + incLEDBrightness + 0) {
                    if (i == 2 || i == 3 || i == incLEDBrightness + 3 || i == incLEDBrightness + 3 + 1 || i == incLEDBrightness + 3 + 2 || i != incLEDBrightness + 3 + 3) {
                    }
                } else if (i < incLCDTotal + 4 + incLCDScroll) {
                    if (i == incLCDTotal || i == incLCDTotal + 1 || i == incLCDTotal + 2 || i == incLCDTotal + 3 || i == incLCDTotal + 4) {
                    }
                } else if (i < incLCDTotal + 4 + incLCDScroll + 2) {
                    if (i == incLCDTotal + 4 + incLCDScroll || i != incLCDTotal + 4 + incLCDScroll + 1) {
                    }
                } else if (i < incLCDTotal + 4 + incLCDScroll + 2 + 4) {
                    if (i == incLCDTotal + 4 + incLCDScroll + 2 || i == incLCDTotal + 4 + incLCDScroll + 2 + 1 || i == incLCDTotal + 4 + incLCDScroll + 2 + 2 || i == incLCDTotal + 4 + incLCDScroll + 2 + 3) {
                    }
                } else if (i < incLCDTotal + 4 + incLCDScroll + 2 + 4 + 2) {
                    if (i != incLCDTotal + 4 + incLCDScroll + 2 + 4 && i == incLCDTotal + 4 + incLCDScroll + 2 + 4 + 1) {
                        SensorSettingsActivity.this.mInterruptDownloading = false;
                        SensorSettingsActivity.this.mAlertDialog = new AlertDialog.Builder(SensorSettingsActivity.this).setTitle((CharSequence) null).setMessage(R.string.downloading).setNegativeButton(R.string.cancel_download, new DialogInterface.OnClickListener() {
                            /* class com.celaer.android.ambient.SensorSettingsActivity.C02864.DialogInterface$OnClickListenerC02871 */

                            public void onClick(DialogInterface dialogInterface, int i) {
                                SensorSettingsActivity.this.mInterruptDownloading = true;
                            }
                        }).setCancelable(false).show();
                        SensorSettingsActivity.this.mConditionsReceived = 0;
                        SensorSettingsActivity.this.mDataPacketsReceived = 0;
                        SensorSettingsActivity.this.getWindow().addFlags(128);
                        SensorSettingsActivity.this.mAmbientDeviceService.requestConditionsDataAll();
                    }
                } else if (i < incLCDTotal + 4 + incLCDScroll + 2 + 4 + 2 + 2 && i != incLCDTotal + 4 + incLCDScroll + 2 + 4 + 2 && i == incLCDTotal + 4 + incLCDScroll + 2 + 4 + 2 + 1) {
                    new AlertDialog.Builder(SensorSettingsActivity.this).setTitle(R.string.delete_from_where).setItems(new CharSequence[]{SensorSettingsActivity.this.getResources().getString(R.string.app_sensor), SensorSettingsActivity.this.getResources().getString(R.string.app_only), SensorSettingsActivity.this.getResources().getString(R.string.sensor_only)}, new DialogInterface.OnClickListener() {
                        /* class com.celaer.android.ambient.SensorSettingsActivity.C02864.DialogInterface$OnClickListenerC02893 */

                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == 0) {
                                AmbientDeviceManager.get(SensorSettingsActivity.this).deleteConditionsForAmbientDevice(SensorSettingsActivity.this.mCurrentAmbientDevice.getAddress());
                                SensorSettingsActivity.this.mAmbientDeviceService.deleteConditionsLog();
                            } else if (i == 1) {
                                AmbientDeviceManager.get(SensorSettingsActivity.this).deleteConditionsForAmbientDevice(SensorSettingsActivity.this.mCurrentAmbientDevice.getAddress());
                                SensorSettingsActivity.this.mCurrentAmbientDevice.setTimestampBase();
                            } else {
                                SensorSettingsActivity.this.mAmbientDeviceService.deleteConditionsLog();
                            }
                        }
                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        /* class com.celaer.android.ambient.SensorSettingsActivity.C02864.DialogInterface$OnClickListenerC02882 */

                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
                }
            }
        }
    };
    private ProgressDialog mProgressDialog;
    private boolean mSendLedDemo = false;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        /* class com.celaer.android.ambient.SensorSettingsActivity.ServiceConnectionC02949 */

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            SensorSettingsActivity.this.mAmbientDeviceService = ((AmbientDeviceService.LocalBinder) service).getService();
            Log.e(SensorSettingsActivity.TAG, "Service Connection Successful");
            if (!SensorSettingsActivity.this.mAmbientDeviceService.initialize()) {
                Log.e(SensorSettingsActivity.TAG, "Unable to initialize Bluetooth");
                SensorSettingsActivity.this.finish();
            }
            SensorSettingsActivity.this.mAmbientDeviceService.connect(SensorSettingsActivity.this.mCurrentAmbientDevice.getAddress());
            SensorSettingsActivity.this.mProgressDialog = ProgressDialog.show(SensorSettingsActivity.this, null, SensorSettingsActivity.this.getString(R.string.connecting), true, true, new DialogInterface.OnCancelListener() {
                /* class com.celaer.android.ambient.SensorSettingsActivity.ServiceConnectionC02949.DialogInterface$OnCancelListenerC02951 */

                public void onCancel(DialogInterface dialog) {
                    Log.d(SensorSettingsActivity.TAG, "connection canceled");
                    SensorSettingsActivity.this.handler.removeCallbacks(SensorSettingsActivity.this.f15r);
                    SensorSettingsActivity.this.handler.removeCallbacks(SensorSettingsActivity.this.rSubscribe);
                    SensorSettingsActivity.this.mAmbientDeviceService.disconnect();
                    Intent intent = new Intent();
                    SensorSettingsActivity.this.setResult(-1, intent);
                    intent.putExtra("DEVICE_ADDRESS", SensorSettingsActivity.this.mCurrentAmbientDevice.getAddress());
                    SensorSettingsActivity.this.finish();
                }
            });
        }

        public void onServiceDisconnected(ComponentName componentName) {
            SensorSettingsActivity.this.mAmbientDeviceService = null;
        }
    };

    /* renamed from: r */
    private Runnable f15r;
    private Runnable rIdleTimeout;
    private Runnable rSubscribe;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_settings);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        this.mCurrentAmbientDevice = AmbientDeviceManager.get(this).getAmbientDevice(getIntent().getStringExtra("DEVICE_ADDRESS"));
        this.mListView = (ListView) findViewById(R.id.activity_sensor_settings_listView);
        this.mAdapter = new AmbientSettingsAdapter();
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        this.mListView.setOnItemClickListener(this.mOnItemClickListener);
        this.handler = new Handler();
        this.f15r = new Runnable() {
            /* class com.celaer.android.ambient.SensorSettingsActivity.RunnableC02821 */

            public void run() {
                Log.i(SensorSettingsActivity.TAG, "Attempting to start service discovery" + SensorSettingsActivity.this.mAmbientDeviceService.discoverServices());
            }
        };
        this.rSubscribe = new Runnable() {
            /* class com.celaer.android.ambient.SensorSettingsActivity.RunnableC02842 */

            public void run() {
                SensorSettingsActivity.this.mAmbientDeviceService.subscribeToNotifications();
            }
        };
        this.rIdleTimeout = new Runnable() {
            /* class com.celaer.android.ambient.SensorSettingsActivity.RunnableC02853 */

            public void run() {
                SensorSettingsActivity.this.mIntentionalDisconnect = true;
                SensorSettingsActivity.this.handler.removeCallbacks(SensorSettingsActivity.this.f15r);
                SensorSettingsActivity.this.mAmbientDeviceService.disconnect();
                Intent intent = new Intent(SensorSettingsActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                SensorSettingsActivity.this.startActivity(intent);
            }
        };
    }

    /* access modifiers changed from: protected */
    @Override // com.celaer.android.ambient.utilities.CelaerActivity
    public void onResume() {
        if (((MyApplication) getApplication()).wasInBackground) {
            Log.d(TAG, "launching from BACKGROUND");
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        super.onResume();
        Intent gattServiceIntent = new Intent(this, AmbientDeviceService.class);
        bindService(gattServiceIntent, this.mServiceConnection, Context.BIND_AUTO_CREATE);
        startService(gattServiceIntent);
        registerReceiver(this.mGattUpdateReceiver, makeGattUpdateIntentFilter());
        resetIdleTimeout();
    }

    /* access modifiers changed from: protected */
    @Override // com.celaer.android.ambient.utilities.CelaerActivity
    public void onPause() {
        super.onPause();
        AmbientDeviceManager.get(this).saveAmbientDevices();
        try {
            unregisterReceiver(this.mGattUpdateReceiver);
            unbindService(this.mServiceConnection);
        } catch (IllegalArgumentException e) {
        }
        getWindow().clearFlags(128);
        this.mAmbientDeviceService = null;
        this.handler.removeCallbacks(this.rIdleTimeout);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            this.mBackButtonPressed = true;
            this.mAmbientDeviceService.disconnect();
            AmbientDeviceManager.get(this).saveAmbientDevices();
            Intent intent = new Intent();
            setResult(-1, intent);
            intent.putExtra("DEVICE_ADDRESS", this.mCurrentAmbientDevice.getAddress());
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

//
//    public final boolean onMenuItemSelected(int featureId, MenuItem item) {
//        if (item.getItemId() == 16908332) {
//            this.mBackButtonPressed = true;
//            this.mAmbientDeviceService.disconnect();
//            AmbientDeviceManager.get(this).saveAmbientDevices();
//            Intent intent = new Intent();
//            setResult(-1, intent);
//            intent.putExtra("DEVICE_ADDRESS", this.mCurrentAmbientDevice.getAddress());
//            finish();
//        }
//        return super.onMenuItemSelected(featureId, item);
//    }

    private int[] getSectionRowForI(int i) {
        int[] iArr = {0, 0};
        if (i == 0) {
            return new int[]{0, 0};
        }
        if (i == 1) {
            return new int[]{0, 1};
        }
        return iArr;
    }

    /* access modifiers changed from: private */
    public class AmbientSettingsAdapter extends BaseAdapter {
        public static final int TYPE_COLOR = 4;
        public static final int TYPE_DOUBLE = 2;
        public static final int TYPE_SECTION_HEADER = 0;
        public static final int TYPE_SEGMENTED = 3;
        public static final int TYPE_SEGMENTED_MEASURE = 5;
        public static final int TYPE_SEGMENTED_TWO_LINE = 6;
        public static final int TYPE_SWITCH = 1;
        private ColorBar.OnColorBarChangedListener mColorBarListener;
        private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener;
        private CompoundButton.OnCheckedChangeListener mSwitchListener;

        private AmbientSettingsAdapter() {
            this.mSwitchListener = new CompoundButton.OnCheckedChangeListener() {
                /* class com.celaer.android.ambient.SensorSettingsActivity.AmbientSettingsAdapter.C02961 */

                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    int switchNum = Integer.parseInt(compoundButton.getTag().toString());
                    if (switchNum == 0) {
                        SensorSettingsActivity.this.mCurrentAmbientDevice.ledEnabled = b;
                        SensorSettingsActivity.this.startLEDSync();
                    } else if (switchNum == 1) {
                        SensorSettingsActivity.this.mCurrentAmbientDevice.ledAutoBrightnessEnabled = b;
                        SensorSettingsActivity.this.startLEDSyncWithPreview();
                    } else if (switchNum == 2) {
                        SensorSettingsActivity.this.mCurrentAmbientDevice.autoOnOffEnabled = b;
                        SensorSettingsActivity.this.startLEDSync();
                    } else if (switchNum == 3) {
                        SensorSettingsActivity.this.mCurrentAmbientDevice.lcdScroll = b;
                        SensorSettingsActivity.this.startLCDSync();
                    } else if (switchNum == 4) {
                        SensorSettingsActivity.this.mCurrentAmbientDevice.dstEnabled = b;
                        SensorSettingsActivity.this.mAmbientDeviceService.writeDst(SensorSettingsActivity.this.mCurrentAmbientDevice.dstEnabled);
                    }
                    SensorSettingsActivity.this.mAdapter.notifyDataSetChanged();
                }
            };
            this.mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
                /* class com.celaer.android.ambient.SensorSettingsActivity.AmbientSettingsAdapter.C02972 */

                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    int buttonTag = Integer.parseInt(compoundButton.getTag().toString());
                    if (buttonTag / 10 == 1) {
                        if (buttonTag % 10 == 0) {
                            if (SensorSettingsActivity.this.mCurrentAmbientDevice.tempUnitsC) {
                                SensorSettingsActivity.this.mAdapter.notifyDataSetChanged();
                                return;
                            }
                            SensorSettingsActivity.this.mCurrentAmbientDevice.tempUnitsC = true;
                        } else if (!SensorSettingsActivity.this.mCurrentAmbientDevice.tempUnitsC) {
                            SensorSettingsActivity.this.mAdapter.notifyDataSetChanged();
                            return;
                        } else {
                            SensorSettingsActivity.this.mCurrentAmbientDevice.tempUnitsC = false;
                        }
                        SensorSettingsActivity.this.startLCDSync();
                    } else if (buttonTag / 10 == 2) {
                        if (buttonTag % 10 != 0) {
                            SensorSettingsActivity.this.mCurrentAmbientDevice.timeFormat24 = true;
                        } else if (!SensorSettingsActivity.this.mCurrentAmbientDevice.timeFormat24) {
                            SensorSettingsActivity.this.mAdapter.notifyDataSetChanged();
                            return;
                        } else {
                            SensorSettingsActivity.this.mCurrentAmbientDevice.timeFormat24 = false;
                        }
                        SensorSettingsActivity.this.startLCDSync();
                    } else if (buttonTag / 10 == 3) {
                        if (SensorSettingsActivity.this.mCurrentAmbientDevice.lcdDisplayIndex == buttonTag % 10) {
                            SensorSettingsActivity.this.mAdapter.notifyDataSetChanged();
                            return;
                        }
                        SensorSettingsActivity.this.mCurrentAmbientDevice.lcdDisplayIndex = buttonTag % 10;
                        SensorSettingsActivity.this.startLCDSync();
                    } else if (buttonTag / 10 == 4) {
                        if (SensorSettingsActivity.this.mCurrentAmbientDevice.ledBrightnessLevel == (buttonTag % 10) + 1) {
                            SensorSettingsActivity.this.mAdapter.notifyDataSetChanged();
                            return;
                        }
                        SensorSettingsActivity.this.mCurrentAmbientDevice.ledBrightnessLevel = (buttonTag % 10) + 1;
                        SensorSettingsActivity.this.startLEDSyncWithPreview();
                    } else if (buttonTag / 10 == 5) {
                        if (buttonTag % 10 == 0) {
                            SensorSettingsActivity.this.mCurrentAmbientDevice.measureTempHumiditySec = 10;
                            SensorSettingsActivity.this.mCurrentAmbientDevice.measureLightSec = 10;
                            SensorSettingsActivity.this.mCurrentAmbientDevice.loggingSec = 60;
                        } else if (buttonTag % 10 == 1) {
                            SensorSettingsActivity.this.mCurrentAmbientDevice.measureTempHumiditySec = 60;
                            SensorSettingsActivity.this.mCurrentAmbientDevice.measureLightSec = 60;
                            SensorSettingsActivity.this.mCurrentAmbientDevice.loggingSec = 900;
                        } else {
                            SensorSettingsActivity.this.mCurrentAmbientDevice.measureTempHumiditySec = 60;
                            SensorSettingsActivity.this.mCurrentAmbientDevice.measureLightSec = 60;
                            SensorSettingsActivity.this.mCurrentAmbientDevice.loggingSec = DateTimeConstants.SECONDS_PER_HOUR;
                        }
                        SensorSettingsActivity.this.startSensorSettingsSync();
                    }
                    SensorSettingsActivity.this.mAdapter.notifyDataSetChanged();
                }
            };
            this.mColorBarListener = new ColorBar.OnColorBarChangedListener() {
                /* class com.celaer.android.ambient.SensorSettingsActivity.AmbientSettingsAdapter.C02983 */

                @Override // com.celaer.android.ambient.controls.ColorBar.OnColorBarChangedListener
                public void onColorBarChanged(int red, int green, int blue) {
                    SensorSettingsActivity.this.mCurrentAmbientDevice.ledNormalColorR = red;
                    SensorSettingsActivity.this.mCurrentAmbientDevice.ledNormalColorG = green;
                    SensorSettingsActivity.this.mCurrentAmbientDevice.ledNormalColorB = blue;
                    SensorSettingsActivity.this.mAmbientDeviceService.writeLEDDemo(red, green, blue, 3);
                }

                @Override // com.celaer.android.ambient.controls.ColorBar.OnColorBarChangedListener
                public void onColorBarChangeFinished(int red, int green, int blue) {
                    SensorSettingsActivity.this.mCurrentAmbientDevice.ledNormalColorR = red;
                    SensorSettingsActivity.this.mCurrentAmbientDevice.ledNormalColorG = green;
                    SensorSettingsActivity.this.mCurrentAmbientDevice.ledNormalColorB = blue;
                    SensorSettingsActivity.this.mSendLedDemo = false;
                    SensorSettingsActivity.this.mAmbientDeviceService.writeLEDSettings(SensorSettingsActivity.this.mCurrentAmbientDevice);
                    AmbientDeviceManager.get(SensorSettingsActivity.this).saveAmbientDevices();
                }
            };
        }

        public int getViewTypeCount() {
            return 7;
        }

        public int getCount() {
            int count = 0 + 8 + 8;
            if (!SensorSettingsActivity.this.mCurrentAmbientDevice.lcdScroll) {
                count++;
            }
            if (SensorSettingsActivity.this.getSharedPreferences("com.celaer.android.ambient.preferences", 0).getString("countryUnits", "none").equalsIgnoreCase("jp")) {
                count--;
            }
            if (SensorSettingsActivity.this.mCurrentAmbientDevice.ledEnabled) {
                count += 3;
            }
            if (!SensorSettingsActivity.this.mCurrentAmbientDevice.ledAutoBrightnessEnabled) {
                return count + 1;
            }
            return count;
        }

        /* JADX WARNING: Removed duplicated region for block: B:105:0x0172 A[RETURN, SYNTHETIC] */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int getItemViewType(int param1Int) {
            int i = 0;
            byte b = 0;
            int k = 0;
            int j = b;
            if (SensorSettingsActivity.this.mCurrentAmbientDevice.ledEnabled) {
                int n = 0 + 2;
                i = n;
                j = b;
                if (!SensorSettingsActivity.this.mCurrentAmbientDevice.ledAutoBrightnessEnabled) {
                    j = 0 + 1;
                    i = n;
                }
            }
            if (!SensorSettingsActivity.this.mCurrentAmbientDevice.lcdScroll)
                k = 0 + 1;
            int m = i + 2 + j + 0;
            String str = SensorSettingsActivity.this.getSharedPreferences("com.celaer.android.ambient.preferences", 0).getString("countryUnits", "none");
            if (str.equalsIgnoreCase("jp")) {
                b = 0;
            } else {
                b = 1;
            }
            if (param1Int == 0)
                return 0;
            if (param1Int == 1)
                return 1;
            if (param1Int < i + 2 + j + 0) {
                if (param1Int == 2)
                    return 4;
                if (param1Int == 3)
                    return 1;
                if (param1Int == j + 3)
                    return 6;
                if (param1Int == j + 3 + 1)
                    return 1;
                if (param1Int == j + 3 + 2)
                    return 2;
                if (param1Int == j + 3 + 3)
                    return 2;
            } else if (param1Int < m + 3 + b + k) {
                if (str.equalsIgnoreCase("jp")) {
                    if (param1Int == m)
                        return 0;
                    if (param1Int == m + 1)
                        return 3;
                    if (param1Int == m + 2)
                        return 1;
                    if (param1Int == m + 3)
                        return 6;
                } else {
                    if (param1Int == m)
                        return 0;
                    if (param1Int == m + 1)
                        return 3;
                    if (param1Int == m + 2)
                        return 3;
                    if (param1Int == m + 3)
                        return 1;
                    if (param1Int == m + 4)
                        return 6;
                }
            } else if (param1Int < m + 3 + b + k + 2) {
                if (param1Int == m + 3 + b + k)
                    return 0;
                if (param1Int == m + 3 + b + k + 1)
                    return 5;
            } else if (param1Int < m + 3 + b + k + 2 + 4) {
                if (param1Int == m + 3 + b + k + 2)
                    return 0;
                if (param1Int == m + 3 + b + k + 2 + 1)
                    return 1;
                if (param1Int == m + 3 + b + k + 2 + 2)
                    return 2;
                if (param1Int == m + 3 + b + k + 2 + 3)
                    return 2;
            } else if (param1Int < m + 3 + b + k + 2 + 4 + 2) {
                if (param1Int == m + 3 + b + k + 2 + 4)
                    return 0;
                if (param1Int == m + 3 + b + k + 2 + 4 + 1)
                    return 2;
            } else if (param1Int < m + 3 + b + k + 2 + 4 + 2 + 2) {
                if (param1Int == m + 3 + b + k + 2 + 4 + 2)
                    return 0;
                if (param1Int == m + 3 + b + k + 2 + 4 + 2 + 1)
                    return 2;
            } else {
                return 0;
            }
            return 0;
            //throw new UnsupportedOperationException("Method not decompiled: com.celaer.android.ambient.SensorSettingsActivity.AmbientSettingsAdapter.getItemViewType(int):int");
        }

        public boolean isEnabled(int position) {
            int incLED = 0;
            int incLEDBrightness = 0;
            int incLCDScroll = 0;
            if (SensorSettingsActivity.this.mCurrentAmbientDevice.ledEnabled) {
                incLED = 0 + 2;
                if (!SensorSettingsActivity.this.mCurrentAmbientDevice.ledAutoBrightnessEnabled) {
                    incLEDBrightness = 0 + 1;
                }
            }
            if (!SensorSettingsActivity.this.mCurrentAmbientDevice.lcdScroll) {
                incLCDScroll = 0 + 1;
            }
            int incLCDTotal = incLED + 2 + incLEDBrightness + 0;
            if (SensorSettingsActivity.this.getSharedPreferences("com.celaer.android.ambient.preferences", 0).getString("countryUnits", "none").equalsIgnoreCase("jp")) {
                incLCDTotal--;
            }
            if (!(position == 0 || position == 1)) {
                if (position < incLED + 2 + incLEDBrightness + 0) {
                    if (!(position == 2 || position == 3 || position == incLEDBrightness + 3 || position == incLEDBrightness + 3 + 1 || (position != incLEDBrightness + 3 + 2 && position != incLEDBrightness + 3 + 3))) {
                        return true;
                    }
                } else if (position < incLCDTotal + 4 + incLCDScroll) {
                    if (position == incLCDTotal || position == incLCDTotal + 1 || position == incLCDTotal + 2 || position == incLCDTotal + 3 || position != incLCDTotal + 4) {
                    }
                } else if (position < incLCDTotal + 4 + incLCDScroll + 2) {
                    if (position == incLCDTotal + 4 + incLCDScroll || position == incLCDTotal + 4 + incLCDScroll + 1) {
                    }
                } else if (position < incLCDTotal + 4 + incLCDScroll + 2 + 4) {
                    if (position == incLCDTotal + 4 + incLCDScroll + 2 || position == incLCDTotal + 4 + incLCDScroll + 2 + 1 || position == incLCDTotal + 4 + incLCDScroll + 2 + 2 || position != incLCDTotal + 4 + incLCDScroll + 2 + 3) {
                    }
                } else if (position < incLCDTotal + 4 + incLCDScroll + 2 + 4 + 2) {
                    if (position != incLCDTotal + 4 + incLCDScroll + 2 + 4 && position == incLCDTotal + 4 + incLCDScroll + 2 + 4 + 1) {
                        return true;
                    }
                } else if (position < incLCDTotal + 4 + incLCDScroll + 2 + 4 + 2 + 2 && position != incLCDTotal + 4 + incLCDScroll + 2 + 4 + 2 && position == incLCDTotal + 4 + incLCDScroll + 2 + 4 + 2 + 1) {
                    return true;
                }
            }
            return false;
        }

        public long getItemId(int i) {
            return 0;
        }

        public Object getItem(int i) {
            return null;
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            int incLCDTempUnits;
            int type = getItemViewType(i);
            ViewHolderSwitch viewHolderSwitch = new ViewHolderSwitch();
            ViewHolderDouble viewHolderDouble = new ViewHolderDouble();
            ViewHolderHeader viewHolderHeader = new ViewHolderHeader();
            ViewHolderSegmented viewHolderSegmented = new ViewHolderSegmented();
            ViewHolderSegmentedTwoLine viewHolderSegmented2 = new ViewHolderSegmentedTwoLine();
            ViewHolderColor viewHolderColor = new ViewHolderColor();
            ViewHolderMeasurement viewHolderMeasurement = new ViewHolderMeasurement();
            if (view == null) {
                if (type == 0) {
                    view = SensorSettingsActivity.this.getLayoutInflater().inflate(R.layout.list_header_grouped, viewGroup, false);
                    viewHolderHeader.textView = (TextView) view.findViewById(R.id.list_header_textView);
                    view.setTag(viewHolderHeader);
                } else if (type == 2) {
                    view = SensorSettingsActivity.this.getLayoutInflater().inflate(R.layout.list_item_double_label, viewGroup, false);
                    viewHolderDouble.mainTextView = (TextView) view.findViewById(R.id.list_item_double_mainTextView);
                    viewHolderDouble.detailTextView = (TextView) view.findViewById(R.id.list_item_double_detailTextView);
                    viewHolderDouble.disclosureImage = (ImageView) view.findViewById(R.id.list_item_double_disclosureImageView);
                    view.setTag(viewHolderDouble);
                } else if (type == 1) {
                    view = SensorSettingsActivity.this.getLayoutInflater().inflate(R.layout.list_item_switch, viewGroup, false);
                    viewHolderSwitch.textView = (TextView) view.findViewById(R.id.list_item_switch_textView);
                    viewHolderSwitch.alarmSwitch = (Switch) view.findViewById(R.id.list_item_switch_switch);
                    view.setTag(viewHolderSwitch);
                } else if (type == 3) {
                    view = SensorSettingsActivity.this.getLayoutInflater().inflate(R.layout.list_item_segmented, viewGroup, false);
                    viewHolderSegmented.textView = (TextView) view.findViewById(R.id.list_item_segmented_textView);
                    viewHolderSegmented.toggle1 = (ToggleButton) view.findViewById(R.id.list_item_segmented_seg1);
                    viewHolderSegmented.toggle2 = (ToggleButton) view.findViewById(R.id.list_item_segmented_seg2);
                    viewHolderSegmented.toggle3 = (ToggleButton) view.findViewById(R.id.list_item_segmented_seg3);
                    view.setTag(viewHolderSegmented);
                } else if (type == 6) {
                    view = SensorSettingsActivity.this.getLayoutInflater().inflate(R.layout.list_item_segmented_two_line, viewGroup, false);
                    viewHolderSegmented2.textView = (TextView) view.findViewById(R.id.list_item_segmented2_textView);
                    viewHolderSegmented2.toggle1 = (ToggleButton) view.findViewById(R.id.list_item_segmented2_seg1);
                    viewHolderSegmented2.toggle2 = (ToggleButton) view.findViewById(R.id.list_item_segmented2_seg2);
                    viewHolderSegmented2.toggle3 = (ToggleButton) view.findViewById(R.id.list_item_segmented2_seg3);
                    viewHolderSegmented2.toggle4 = (ToggleButton) view.findViewById(R.id.list_item_segmented2_seg4);
                    viewHolderSegmented2.toggle5 = (ToggleButton) view.findViewById(R.id.list_item_segmented2_seg5);
                    view.setTag(viewHolderSegmented2);
                } else if (type == 4) {
                    view = SensorSettingsActivity.this.getLayoutInflater().inflate(R.layout.list_item_color, viewGroup, false);
                    viewHolderColor.textView = (TextView) view.findViewById(R.id.list_item_color_textView);
                    viewHolderColor.colorBar = (ColorBar) view.findViewById(R.id.list_item_color_colorBar);
                    viewHolderColor.colorBar.setOnColorBarChangedListener(this.mColorBarListener);
                    view.setTag(viewHolderColor);
                } else if (type == 5) {
                    view = SensorSettingsActivity.this.getLayoutInflater().inflate(R.layout.list_item_measurement, viewGroup, false);
                    viewHolderMeasurement.textView = (TextView) view.findViewById(R.id.list_item_measurement_textView);
                    viewHolderMeasurement.toggle1 = (ToggleButton) view.findViewById(R.id.list_item_measurement_seg1);
                    viewHolderMeasurement.toggle2 = (ToggleButton) view.findViewById(R.id.list_item_measurement_seg2);
                    viewHolderMeasurement.toggle3 = (ToggleButton) view.findViewById(R.id.list_item_measurement_seg3);
                    viewHolderMeasurement.measurementTextView = (TextView) view.findViewById(R.id.list_item_measurement_measPeriod);
                    viewHolderMeasurement.loggingTextView = (TextView) view.findViewById(R.id.list_item_measurement_loggingPeriod);
                    viewHolderMeasurement.capacityTextView = (TextView) view.findViewById(R.id.list_item_measurement_loggingCapacity);
                    view.setTag(viewHolderMeasurement);
                }
            } else if (type == 0) {
                viewHolderHeader = (ViewHolderHeader) view.getTag();
            } else if (type == 2) {
                viewHolderDouble = (ViewHolderDouble) view.getTag();
            } else if (type == 1) {
                viewHolderSwitch = (ViewHolderSwitch) view.getTag();
            } else if (type == 3) {
                viewHolderSegmented = (ViewHolderSegmented) view.getTag();
            } else if (type == 6) {
                viewHolderSegmented2 = (ViewHolderSegmentedTwoLine) view.getTag();
            } else if (type == 4) {
                viewHolderColor = (ViewHolderColor) view.getTag();
            } else if (type == 5) {
                viewHolderMeasurement = (ViewHolderMeasurement) view.getTag();
            }
            int incLED = 0;
            int incLEDBrightness = 0;
            int incLCDScroll = 0;
            if (SensorSettingsActivity.this.mCurrentAmbientDevice.ledEnabled) {
                incLED = 0 + 2;
                if (!SensorSettingsActivity.this.mCurrentAmbientDevice.ledAutoBrightnessEnabled) {
                    incLEDBrightness = 0 + 1;
                }
            }
            if (!SensorSettingsActivity.this.mCurrentAmbientDevice.lcdScroll) {
                incLCDScroll = 0 + 1;
            }
            int incLCDTotal = incLED + 2 + incLEDBrightness + 0;
            String tempSetting = SensorSettingsActivity.this.getSharedPreferences("com.celaer.android.ambient.preferences", 0).getString("countryUnits", "none");
            if (tempSetting.equalsIgnoreCase("jp")) {
                incLCDTempUnits = 0;
            } else {
                incLCDTempUnits = 1;
            }
            if (i == 0) {
                viewHolderHeader.textView.setText("");
            } else if (i == 1) {
                viewHolderSwitch.textView.setText(R.string.led_indicator);
                viewHolderSwitch.alarmSwitch.setOnCheckedChangeListener(null);
                viewHolderSwitch.alarmSwitch.setChecked(SensorSettingsActivity.this.mCurrentAmbientDevice.ledEnabled);
                viewHolderSwitch.alarmSwitch.setOnCheckedChangeListener(this.mSwitchListener);
                viewHolderSwitch.alarmSwitch.setTag(0);
                viewHolderSwitch.row = i;
            } else if (i < incLED + 2 + incLEDBrightness + 0) {
                if (i == 2) {
                    viewHolderColor.textView.setText(R.string.led_color);
                    viewHolderColor.colorBar.setSelectedColor(SensorSettingsActivity.this.mCurrentAmbientDevice.ledNormalColorR, SensorSettingsActivity.this.mCurrentAmbientDevice.ledNormalColorG, SensorSettingsActivity.this.mCurrentAmbientDevice.ledNormalColorB);
                    viewHolderColor.row = i;
                } else if (i == 3) {
                    viewHolderSwitch.textView.setText(R.string.auto_brightness);
                    viewHolderSwitch.alarmSwitch.setOnCheckedChangeListener(null);
                    viewHolderSwitch.alarmSwitch.setChecked(SensorSettingsActivity.this.mCurrentAmbientDevice.ledAutoBrightnessEnabled);
                    viewHolderSwitch.alarmSwitch.setOnCheckedChangeListener(this.mSwitchListener);
                    viewHolderSwitch.alarmSwitch.setTag(1);
                    viewHolderSwitch.row = i;
                } else if (i == incLEDBrightness + 3) {
                    viewHolderSegmented2.textView.setText(R.string.brightness);
                    viewHolderSegmented2.toggle1.setTextOn("1");
                    viewHolderSegmented2.toggle1.setTextOff("1");
                    viewHolderSegmented2.toggle2.setTextOn("2");
                    viewHolderSegmented2.toggle2.setTextOff("2");
                    viewHolderSegmented2.toggle3.setTextOn("3");
                    viewHolderSegmented2.toggle3.setTextOff("3");
                    viewHolderSegmented2.toggle4.setTextOn("4");
                    viewHolderSegmented2.toggle4.setTextOff("4");
                    viewHolderSegmented2.toggle5.setTextOn("5");
                    viewHolderSegmented2.toggle5.setTextOff("5");
                    viewHolderSegmented2.toggle1.setOnCheckedChangeListener(null);
                    viewHolderSegmented2.toggle2.setOnCheckedChangeListener(null);
                    viewHolderSegmented2.toggle3.setOnCheckedChangeListener(null);
                    viewHolderSegmented2.toggle4.setOnCheckedChangeListener(null);
                    viewHolderSegmented2.toggle5.setOnCheckedChangeListener(null);
                    viewHolderSegmented2.toggle1.setChecked(false);
                    viewHolderSegmented2.toggle2.setChecked(false);
                    viewHolderSegmented2.toggle3.setChecked(false);
                    viewHolderSegmented2.toggle4.setChecked(false);
                    viewHolderSegmented2.toggle5.setChecked(false);
                    switch (SensorSettingsActivity.this.mCurrentAmbientDevice.ledBrightnessLevel) {
                        case 1:
                            viewHolderSegmented2.toggle1.setChecked(true);
                            break;
                        case 2:
                            viewHolderSegmented2.toggle2.setChecked(true);
                            break;
                        case 3:
                            viewHolderSegmented2.toggle3.setChecked(true);
                            break;
                        case 4:
                            viewHolderSegmented2.toggle4.setChecked(true);
                            break;
                        case 5:
                            viewHolderSegmented2.toggle5.setChecked(true);
                            break;
                    }
                    viewHolderSegmented2.toggle1.setTag(40);
                    viewHolderSegmented2.toggle2.setTag(41);
                    viewHolderSegmented2.toggle3.setTag(42);
                    viewHolderSegmented2.toggle4.setTag(43);
                    viewHolderSegmented2.toggle5.setTag(44);
                    viewHolderSegmented2.toggle1.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented2.toggle2.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented2.toggle3.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented2.toggle4.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented2.toggle5.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented2.toggle4.setVisibility(View.VISIBLE);
                    viewHolderSegmented2.toggle5.setVisibility(View.VISIBLE);
                    viewHolderSegmented2.row = i;
                }
            } else if (i < incLCDTotal + 3 + incLCDTempUnits + incLCDScroll) {
                boolean jpTempUnits = false;
                if (tempSetting.equalsIgnoreCase("jp")) {
                    jpTempUnits = true;
                }
                if (i == incLCDTotal) {
                    viewHolderHeader.textView.setText("");
                } else if (!jpTempUnits && i == incLCDTotal + 1) {
                    viewHolderSegmented.textView.setText(R.string.temperature);
                    viewHolderSegmented.toggle1.setTextOn(SensorSettingsActivity.this.getString(R.string.celsius_abbrev));
                    viewHolderSegmented.toggle1.setTextOff(SensorSettingsActivity.this.getString(R.string.celsius_abbrev));
                    viewHolderSegmented.toggle2.setTextOn(SensorSettingsActivity.this.getString(R.string.fahrenheit_abbrev));
                    viewHolderSegmented.toggle2.setTextOff(SensorSettingsActivity.this.getString(R.string.fahrenheit_abbrev));
                    viewHolderSegmented.toggle1.setOnCheckedChangeListener(null);
                    viewHolderSegmented.toggle2.setOnCheckedChangeListener(null);
                    viewHolderSegmented.toggle1.setChecked(SensorSettingsActivity.this.mCurrentAmbientDevice.tempUnitsC);
                    viewHolderSegmented.toggle2.setChecked(!SensorSettingsActivity.this.mCurrentAmbientDevice.tempUnitsC);
                    viewHolderSegmented.toggle1.setTag(10);
                    viewHolderSegmented.toggle2.setTag(11);
                    viewHolderSegmented.toggle1.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented.toggle2.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented.toggle3.setVisibility(View.GONE);
                    viewHolderSegmented.row = i;
                } else if ((!jpTempUnits && i == incLCDTotal + 2) || (jpTempUnits && i == incLCDTotal + 1)) {
                    viewHolderSegmented.textView.setText(R.string.time_format);
                    viewHolderSegmented.toggle1.setTextOn("12");
                    viewHolderSegmented.toggle1.setTextOff("12");
                    viewHolderSegmented.toggle2.setTextOn("24");
                    viewHolderSegmented.toggle2.setTextOff("24");
                    viewHolderSegmented.toggle1.setOnCheckedChangeListener(null);
                    viewHolderSegmented.toggle2.setOnCheckedChangeListener(null);
                    viewHolderSegmented.toggle1.setChecked(!SensorSettingsActivity.this.mCurrentAmbientDevice.timeFormat24);
                    viewHolderSegmented.toggle2.setChecked(SensorSettingsActivity.this.mCurrentAmbientDevice.timeFormat24);
                    viewHolderSegmented.toggle1.setTag(20);
                    viewHolderSegmented.toggle2.setTag(21);
                    viewHolderSegmented.toggle1.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented.toggle2.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented.toggle3.setVisibility(View.GONE);
                    viewHolderSegmented.row = i;
                } else if ((!jpTempUnits && i == incLCDTotal + 3) || (jpTempUnits && i == incLCDTotal + 2)) {
                    viewHolderSwitch.textView.setText(R.string.scroll_display);
                    viewHolderSwitch.alarmSwitch.setOnCheckedChangeListener(null);
                    viewHolderSwitch.alarmSwitch.setChecked(SensorSettingsActivity.this.mCurrentAmbientDevice.lcdScroll);
                    viewHolderSwitch.alarmSwitch.setOnCheckedChangeListener(this.mSwitchListener);
                    viewHolderSwitch.alarmSwitch.setTag(3);
                    viewHolderSwitch.row = i;
                } else if ((!jpTempUnits && i == incLCDTotal + 4) || (jpTempUnits && i == incLCDTotal + 3)) {
                    viewHolderSegmented2.textView.setText(R.string.selected_screen);
                    viewHolderSegmented2.toggle1.setTextOn(SensorSettingsActivity.this.getString(R.string.clock));
                    viewHolderSegmented2.toggle1.setTextOff(SensorSettingsActivity.this.getString(R.string.clock));
                    viewHolderSegmented2.toggle2.setTextOn(SensorSettingsActivity.this.getString(R.string.temp));
                    viewHolderSegmented2.toggle2.setTextOff(SensorSettingsActivity.this.getString(R.string.temp));
                    viewHolderSegmented2.toggle3.setTextOn(SensorSettingsActivity.this.getString(R.string.hum));
                    viewHolderSegmented2.toggle3.setTextOff(SensorSettingsActivity.this.getString(R.string.hum));
                    viewHolderSegmented2.toggle4.setTextOn(SensorSettingsActivity.this.getString(R.string.light));
                    viewHolderSegmented2.toggle4.setTextOff(SensorSettingsActivity.this.getString(R.string.light));
                    viewHolderSegmented2.toggle1.setOnCheckedChangeListener(null);
                    viewHolderSegmented2.toggle2.setOnCheckedChangeListener(null);
                    viewHolderSegmented2.toggle3.setOnCheckedChangeListener(null);
                    viewHolderSegmented2.toggle4.setOnCheckedChangeListener(null);
                    viewHolderSegmented2.toggle5.setOnCheckedChangeListener(null);
                    viewHolderSegmented2.toggle1.setChecked(false);
                    viewHolderSegmented2.toggle2.setChecked(false);
                    viewHolderSegmented2.toggle3.setChecked(false);
                    viewHolderSegmented2.toggle4.setChecked(false);
                    switch (SensorSettingsActivity.this.mCurrentAmbientDevice.lcdDisplayIndex) {
                        case 0:
                            viewHolderSegmented2.toggle1.setChecked(true);
                            break;
                        case 1:
                            viewHolderSegmented2.toggle2.setChecked(true);
                            break;
                        case 2:
                            viewHolderSegmented2.toggle3.setChecked(true);
                            break;
                        case 3:
                            viewHolderSegmented2.toggle4.setChecked(true);
                            break;
                    }
                    viewHolderSegmented2.toggle1.setTag(30);
                    viewHolderSegmented2.toggle2.setTag(31);
                    viewHolderSegmented2.toggle3.setTag(32);
                    viewHolderSegmented2.toggle4.setTag(33);
                    viewHolderSegmented2.toggle1.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented2.toggle2.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented2.toggle3.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented2.toggle4.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderSegmented2.toggle4.setVisibility(View.VISIBLE);
                    viewHolderSegmented2.toggle5.setVisibility(View.GONE);
                    viewHolderSegmented2.row = i;
                }
            } else if (i < incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 2) {
                if (i == incLCDTotal + 3 + incLCDTempUnits + incLCDScroll) {
                    viewHolderHeader.textView.setText("");
                } else if (i == incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 1) {
                    viewHolderMeasurement.textView.setText(R.string.measurement_logging_speed);
                    viewHolderMeasurement.toggle1.setOnCheckedChangeListener(null);
                    viewHolderMeasurement.toggle2.setOnCheckedChangeListener(null);
                    viewHolderMeasurement.toggle3.setOnCheckedChangeListener(null);
                    viewHolderMeasurement.toggle1.setChecked(false);
                    viewHolderMeasurement.toggle2.setChecked(false);
                    viewHolderMeasurement.toggle3.setChecked(false);
                    if (SensorSettingsActivity.this.mCurrentAmbientDevice.loggingSec == 60) {
                        viewHolderMeasurement.toggle1.setChecked(true);
                        viewHolderMeasurement.measurementTextView.setText("10 " + SensorSettingsActivity.this.getString(R.string.sec));
                        viewHolderMeasurement.loggingTextView.setText("1 " + SensorSettingsActivity.this.getString(R.string.min));
                        viewHolderMeasurement.capacityTextView.setText("33 " + SensorSettingsActivity.this.getString(R.string.hours));
                    } else if (SensorSettingsActivity.this.mCurrentAmbientDevice.loggingSec == 900) {
                        viewHolderMeasurement.toggle2.setChecked(true);
                        viewHolderMeasurement.measurementTextView.setText("1 " + SensorSettingsActivity.this.getString(R.string.min));
                        viewHolderMeasurement.loggingTextView.setText("15 " + SensorSettingsActivity.this.getString(R.string.min));
                        viewHolderMeasurement.capacityTextView.setText("21 " + SensorSettingsActivity.this.getString(R.string.days));
                    } else {
                        viewHolderMeasurement.toggle3.setChecked(true);
                        viewHolderMeasurement.measurementTextView.setText("1 " + SensorSettingsActivity.this.getString(R.string.min));
                        viewHolderMeasurement.loggingTextView.setText("60 " + SensorSettingsActivity.this.getString(R.string.min));
                        viewHolderMeasurement.capacityTextView.setText("84 " + SensorSettingsActivity.this.getString(R.string.days));
                    }
                    viewHolderMeasurement.toggle1.setTag(50);
                    viewHolderMeasurement.toggle2.setTag(51);
                    viewHolderMeasurement.toggle3.setTag(52);
                    viewHolderMeasurement.toggle1.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderMeasurement.toggle2.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderMeasurement.toggle3.setOnCheckedChangeListener(this.mOnCheckedChangeListener);
                    viewHolderMeasurement.row = i;
                }
            } else if (i < incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 2 + 4) {
                if (i == incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 2) {
                    viewHolderHeader.textView.setText("");
                } else if (i == incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 2 + 1) {
                    viewHolderSwitch.textView.setText(R.string.auto_dst_update);
                    viewHolderSwitch.alarmSwitch.setOnCheckedChangeListener(null);
                    viewHolderSwitch.alarmSwitch.setChecked(SensorSettingsActivity.this.mCurrentAmbientDevice.dstEnabled);
                    viewHolderSwitch.alarmSwitch.setOnCheckedChangeListener(this.mSwitchListener);
                    viewHolderSwitch.alarmSwitch.setTag(View.INVISIBLE);
                    viewHolderSwitch.row = i;
                } else if (i == incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 2 + 2) {
                    DateTimeZone defaultZone = DateTimeZone.getDefault();
                    viewHolderDouble.mainTextView.setText(R.string.time_zone);
                    viewHolderDouble.detailTextView.setText(defaultZone.getID());
                    viewHolderDouble.disclosureImage.setVisibility(View.GONE);
                    viewHolderDouble.row = i;
                } else if (i == incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 2 + 3) {
                    if (TimeZone.getDefault().useDaylightTime()) {
                        DateTimeZone defaultZone2 = DateTimeZone.getDefault();
                        long current = System.currentTimeMillis();
                        long next = defaultZone2.nextTransition(current);
                        if (next == current) {
                            next = defaultZone2.nextTransition(1 + next);
                        }
                        Date date = new Date(next);
                        viewHolderDouble.detailTextView.setText(DateFormat.getDateFormat(SensorSettingsActivity.this).format(date) + ", " + DateFormat.getTimeFormat(SensorSettingsActivity.this).format(date));
                    } else {
                        viewHolderDouble.detailTextView.setText(R.string.no_dst);
                    }
                    viewHolderDouble.mainTextView.setText(R.string.next_change);
                    viewHolderDouble.disclosureImage.setVisibility(View.GONE);
                    viewHolderDouble.row = i;
                }
            } else if (i < incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 2 + 4 + 2) {
                if (i == incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 2 + 4) {
                    viewHolderHeader.textView.setText("");
                } else if (i == incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 2 + 4 + 1) {
                    viewHolderDouble.mainTextView.setText(R.string.full_download);
                    viewHolderDouble.detailTextView.setText("");
                    viewHolderDouble.disclosureImage.setVisibility(View.VISIBLE);
                    viewHolderDouble.row = i;
                }
            } else if (i >= incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 2 + 4 + 2 + 2) {
                viewHolderHeader.textView.setText("");
            } else if (i == incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 2 + 4 + 2) {
                viewHolderHeader.textView.setText("");
            } else if (i == incLCDTotal + 3 + incLCDTempUnits + incLCDScroll + 2 + 4 + 2 + 1) {
                viewHolderDouble.mainTextView.setText(R.string.delete_log_data);
                viewHolderDouble.detailTextView.setText("");
                viewHolderDouble.disclosureImage.setVisibility(View.VISIBLE);
                viewHolderDouble.row = i;
            }
            return view;
        }

        private class ViewHolderDouble {
            TextView detailTextView;
            ImageView disclosureImage;
            TextView mainTextView;
            int row;

            private ViewHolderDouble() {
            }
        }

        private class ViewHolderHeader {
            int row;
            TextView textView;

            private ViewHolderHeader() {
            }
        }

        private class ViewHolderSwitch {
            Switch alarmSwitch;
            int row;
            TextView textView;

            private ViewHolderSwitch() {
            }
        }

        private class ViewHolderSegmented {
            int row;
            TextView textView;
            ToggleButton toggle1;
            ToggleButton toggle2;
            ToggleButton toggle3;

            private ViewHolderSegmented() {
            }
        }

        private class ViewHolderSegmentedTwoLine {
            int row;
            TextView textView;
            ToggleButton toggle1;
            ToggleButton toggle2;
            ToggleButton toggle3;
            ToggleButton toggle4;
            ToggleButton toggle5;

            private ViewHolderSegmentedTwoLine() {
            }
        }

        private class ViewHolderColor {
            ColorBar colorBar;
            int row;
            TextView textView;

            private ViewHolderColor() {
            }
        }

        private class ViewHolderMeasurement {
            TextView capacityTextView;
            TextView loggingTextView;
            TextView measurementTextView;
            int row;
            TextView textView;
            ToggleButton toggle1;
            ToggleButton toggle2;
            ToggleButton toggle3;

            private ViewHolderMeasurement() {
            }
        }
    }

    public static int byteToUnsignedInt(byte b) {
        return b & 255;
    }

//    private void uploadInvalidData(String address, long timestamp, double tempC, double humidity, long light) {
//        ParseObject newDevice = new ParseObject("LoggingError");
//        newDevice.put("deviceAddress", address);
//        newDevice.put(ConditionDatabaseHelper.COLUMN_CONDITIONS_TIMESTAMP, Long.valueOf(timestamp));
//        newDevice.put(ConditionDatabaseHelper.COLUMN_CONDITIONS_TEMPC, Double.valueOf(tempC));
//        newDevice.put(ConditionDatabaseHelper.COLUMN_CONDITIONS_HUMIDITY, Double.valueOf(humidity));
//        newDevice.put(ConditionDatabaseHelper.COLUMN_CONDITIONS_LIGHT, Long.valueOf(light));
//        newDevice.put("platform", "android");
//        newDevice.saveEventually();
//    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void processConditionsData(byte[] data) {
        AmbientDeviceManager manager = AmbientDeviceManager.get(this);
        String currentAddress = this.mCurrentAmbientDevice.getAddress();
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
        String[] versions = this.mCurrentAmbientDevice.getFirmwareVersion().split("\\.");
        double temp1 = (((double) ((dataInt[4] * 10) + (dataInt[5] & 15))) - 400.0d) / 10.0d;
        long timestamp1 = ((long) ((dataInt[0] << 24) | (dataInt[1] << 16) | (dataInt[2] << 8) | dataInt[3])) + 1388534400;
        if (Integer.valueOf(versions[0]).intValue() == 0 && Integer.valueOf(versions[1]).intValue() <= 5 && timestamp1 >= timestampDSTlower && timestamp1 < timestampDSTupper) {
            timestamp1 -= 172800;
        }
        double humidity1 = ((double) dataInt[6]) + (((double) (dataInt[5] >> 4)) / 10.0d);
        long light1 = (long) ((dataInt[7] << 8) | dataInt[8]);
        double temp2 = (((double) ((dataInt[13] * 10) + (dataInt[14] & 15))) - 400.0d) / 10.0d;
        long timestamp2 = ((long) ((dataInt[9] << 24) | (dataInt[10] << 16) | (dataInt[11] << 8) | dataInt[12])) + 1388534400;
        if (Integer.valueOf(versions[0]).intValue() == 0 && Integer.valueOf(versions[1]).intValue() <= 5 && timestamp2 >= timestampDSTlower && timestamp2 < timestampDSTupper) {
            timestamp2 -= 172800;
        }
        double humidity2 = ((double) dataInt[15]) + (((double) (dataInt[14] >> 4)) / 10.0d);
        long light2 = (long) ((dataInt[16] << 8) | dataInt[17]);
        if (timestamp1 == 1388534400 && humidity1 == 0.0d) {
            Log.d(TAG, "downloading complete");
            getWindow().clearFlags(128);
            this.mCurrentAmbientDevice.updateTimestampSync();
            manager.saveAmbientDevices();
            this.mAlertDialog.dismiss();
            Log.d(TAG, "conditionsReceived: " + this.mConditionsReceived);
            Log.d(TAG, "packetsReceived: " + this.mDataPacketsReceived);
            return;
        }
        if (timestamp2 != 1388534400 || humidity2 != 0.0d) {
            if (timestamp2 % 60 == 0) {
                if (!manager.conditionExistsEqual(currentAddress, timestamp2)) {
                    manager.insertCondition(currentAddress, timestamp2, temp2, humidity2, light2);
                }
                this.mConditionsReceived++;
            }
            if (timestamp1 % 60 == 0) {
                if (!manager.conditionExistsEqual(currentAddress, timestamp1)) {
                    manager.insertCondition(currentAddress, timestamp1, temp1, humidity1, light1);
                }
                this.mConditionsReceived++;
            }
        } else if (timestamp1 % 60 == 0) {
            if (!manager.conditionExistsEqual(currentAddress, timestamp1)) {
                manager.insertCondition(currentAddress, timestamp1, temp1, humidity1, light1);
            }
            this.mConditionsReceived++;
        }
        this.mDataPacketsReceived++;
        if (this.mDataPacketsReceived % 6 == 0) {
            this.mAlertDialog.setMessage(getString(R.string.downloading) + " " + this.mConditionsReceived);
            if (this.mInterruptDownloading) {
                Log.d(TAG, "downloading complete");
                this.mCurrentAmbientDevice.updateTimestampSync();
                manager.saveAmbientDevices();
                return;
            }
            this.mAmbientDeviceService.requestConditionsDataMore();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
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
            this.mAdapter.notifyDataSetChanged();
            Log.d(TAG, "settings1 received");
        } else if (setInt[0] == 127) {
            if ((setInt[1] & 1) == 1) {
                this.mCurrentAmbientDevice.lcdEnabled = true;
            } else {
                this.mCurrentAmbientDevice.lcdEnabled = false;
            }
            if (((setInt[1] >> 1) & 1) == 1) {
                this.mCurrentAmbientDevice.tempUnitsC = true;
            } else {
                this.mCurrentAmbientDevice.tempUnitsC = false;
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
            this.mAdapter.notifyDataSetChanged();
            Log.d(TAG, "settings2 received");
            if (this.mCurrentAmbientDevice.mSyncReadSettings) {
                this.mCurrentAmbientDevice.mSyncReadSettings = false;
                startNextSync();
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startAllSettingsSync() {
        this.mCurrentAmbientDevice.mSyncTime = true;
        this.mCurrentAmbientDevice.mSyncDST = true;
        this.mCurrentAmbientDevice.mSyncReadSettings = true;
        startNextSync();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startLEDSync() {
        resetIdleTimeout();
        this.mProgressDialog = ProgressDialog.show(this, null, getString(R.string.syncing), true, true, new DialogInterface.OnCancelListener() {
            /* class com.celaer.android.ambient.SensorSettingsActivity.DialogInterface$OnCancelListenerC02905 */

            public void onCancel(DialogInterface dialog) {
            }
        });
        this.mSendLedDemo = false;
        this.mAmbientDeviceService.writeLEDSettings(this.mCurrentAmbientDevice);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startLEDSyncWithPreview() {
        resetIdleTimeout();
        this.mProgressDialog = ProgressDialog.show(this, null, getString(R.string.syncing), true, true, new DialogInterface.OnCancelListener() {
            /* class com.celaer.android.ambient.SensorSettingsActivity.DialogInterface$OnCancelListenerC02916 */

            public void onCancel(DialogInterface dialog) {
            }
        });
        this.mSendLedDemo = true;
        this.mAmbientDeviceService.writeLEDSettings(this.mCurrentAmbientDevice);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startLCDSync() {
        resetIdleTimeout();
        this.mProgressDialog = ProgressDialog.show(this, null, getString(R.string.syncing), true, true, new DialogInterface.OnCancelListener() {
            /* class com.celaer.android.ambient.SensorSettingsActivity.DialogInterface$OnCancelListenerC02927 */

            public void onCancel(DialogInterface dialog) {
            }
        });
        this.mAmbientDeviceService.writeLCDSettings(this.mCurrentAmbientDevice);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startSensorSettingsSync() {
        resetIdleTimeout();
        this.mProgressDialog = ProgressDialog.show(this, null, getString(R.string.syncing), true, true, new DialogInterface.OnCancelListener() {
            /* class com.celaer.android.ambient.SensorSettingsActivity.DialogInterface$OnCancelListenerC02938 */

            public void onCancel(DialogInterface dialog) {
            }
        });
        this.mAmbientDeviceService.writeSensorSettings(this.mCurrentAmbientDevice);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
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
        } else if (!charUUID.equals(AmbientDeviceService.CURRENT_CHARACTERISTIC_UUID) && !charUUID.equals(AmbientDeviceService.CONDITIONS_DATA_CHARACTERISTIC_UUID)) {
            if (charUUID.equals(AmbientDeviceService.AMBIENT_SETTINGS_CHARACTERISTIC_UUID)) {
                try {
                    this.mProgressDialog.dismiss();
                } catch (NullPointerException e) {
                }
                if (this.mSendLedDemo) {
                    this.mSendLedDemo = false;
                    this.mAmbientDeviceService.writeLEDDemo(this.mCurrentAmbientDevice.ledNormalColorR, this.mCurrentAmbientDevice.ledNormalColorG, this.mCurrentAmbientDevice.ledNormalColorB, 3);
                    return;
                }
                return;
            }
        } else {
            return;
        }
        startNextSync();
    }

    private void startNextSync() {
        resetIdleTimeout();
        if (this.mCurrentAmbientDevice.mSyncTime) {
            this.mCurrentAmbientDevice.mSyncState = AmbientDevice.SyncState.BLE_OBJECT_SYNC_STATE_Time;
            this.mAmbientDeviceService.writeClockTime(false);
        } else if (this.mCurrentAmbientDevice.mSyncDST) {
            this.mCurrentAmbientDevice.mSyncState = AmbientDevice.SyncState.BLE_OBJECT_SYNC_STATE_DST;
            this.mAmbientDeviceService.writeDst(this.mCurrentAmbientDevice.dstEnabled);
        } else if (this.mCurrentAmbientDevice.mSyncReadSettings) {
            this.mCurrentAmbientDevice.mSyncState = AmbientDevice.SyncState.BLE_OBJECT_SYNC_STATE_ReadSettings;
            this.mAmbientDeviceService.readSettings();
        } else {
            this.mCurrentAmbientDevice.mSyncState = AmbientDevice.SyncState.BLE_OBJECT_SYNC_STATE_Idle;
            syncSuccessful();
        }
    }

    private void syncSuccessful() {
        this.mProgressDialog.dismiss();
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

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void resetIdleTimeout() {
        this.handler.removeCallbacks(this.rIdleTimeout);
        this.handler.postDelayed(this.rIdleTimeout, IDLE_TIMEOUT);
    }
}
