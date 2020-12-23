package pcp.com.bttemperature;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import pcp.com.bttemperature.ambientDevice.AmbientDevice;
import pcp.com.bttemperature.ambientDevice.AmbientDeviceManager;
import pcp.com.bttemperature.ble.parser.AmbientDeviceAdvObject;
import pcp.com.bttemperature.database.CSVWriter;
import pcp.com.bttemperature.database.ConditionDatabaseHelper;
import pcp.com.bttemperature.database.SQLiteCursorLoader;
import pcp.com.bttemperature.parse.ParseFileUtils;
import pcp.com.bttemperature.tools.PDFTools;
import pcp.com.bttemperature.utilities.CelaerActivity;
import pcp.com.bttemperature.utilities.MyApplication;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SensorMasterSettingsActivity extends CelaerActivity implements LoaderManager.LoaderCallbacks<Cursor>, BluetoothAdapter.LeScanCallback {
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final String GOOGLE_DRIVE_PDF_READER_PREFIX = "http://drive.google.com/viewer?url=";
    private static final String HTML_MIME_TYPE = "text/html";
    private static final long IDLE_TIMEOUT = 180000;
    public static final int LOADER_ALL = 0;
    private static final String PDF_MIME_TYPE = "application/pdf";
    public static final int REQUEST_CODE_SENSOR_SETTINGS = 4;
    private static final String TAG = SensorMasterSettingsActivity.class.getSimpleName();
    private static AmbientDevice mCurrentAmbientDevice;
    private Handler handler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private TextView mConnectionStatusTextView;
    private RelativeLayout mDetailedLayout;
    private View.OnClickListener mDetailedListener = new View.OnClickListener() {
        /* class com.celaer.android.ambient.SensorMasterSettingsActivity.View$OnClickListenerC02772 */

        public void onClick(View view) {
            if (SensorMasterSettingsActivity.this.isBLEEnabled()) {
                //Intent intentTransition = new Intent(SensorMasterSettingsActivity.this, SensorSettingsActivity.class);
                //intentTransition.putExtra("DEVICE_ADDRESS", SensorMasterSettingsActivity.mCurrentAmbientDevice.getAddress());
                //SensorMasterSettingsActivity.this.startActivityForResult(intentTransition, 4);
                return;
            }
            SensorMasterSettingsActivity.this.showBLEDialog();
        }
    };
    private TextView mDeviceVersionNumber;
    private ImageView mDisclosureImage;
    private View.OnClickListener mExportListener = new View.OnClickListener() {
        /* class com.celaer.android.ambient.SensorMasterSettingsActivity.View$OnClickListenerC02805 */

        public void onClick(View view) {
            Log.d(SensorMasterSettingsActivity.TAG, "export click");
            SensorMasterSettingsActivity.this.mInstructionsPressed = true;
            SensorMasterSettingsActivity.this.getLoaderManager().initLoader(0, null, SensorMasterSettingsActivity.this);
        }
    };
    private TextView mExportTextView;
    private TextView mHexNameTextView;
    private RelativeLayout mInstructionsLayout;
    private View.OnClickListener mInstructionsListener = new View.OnClickListener() {
        /* class com.celaer.android.ambient.SensorMasterSettingsActivity.View$OnClickListenerC02783 */

        public void onClick(View view) {
            String filename;
            SensorMasterSettingsActivity.this.mInstructionsPressed = true;
            if (Locale.getDefault().getLanguage().equalsIgnoreCase("ja")) {
                filename = "a570-manual-latest-ja.pdf";
            } else {
                filename = "a570-manual-latest.pdf";
            }
            File tempFile = new File(Environment.getExternalStorageDirectory() + File.separator + "Ambient" + File.separator, filename);
            if (tempFile.exists()) {
                Log.d(SensorMasterSettingsActivity.TAG, "tempFileExists:" + tempFile);
                if (PDFTools.isPDFSupported(SensorMasterSettingsActivity.this)) {
                    SensorMasterSettingsActivity.this.openPDF(tempFile);
                } else {
                    SensorMasterSettingsActivity.this.askToOpenPDFThroughGoogleDrive(SensorMasterSettingsActivity.this, tempFile.toString());
                }
            }
        }
    };
    private boolean mInstructionsPressed = false;
    private EditText mNameEditText;
    private Runnable rRefreshStatus;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_master_settings);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        mCurrentAmbientDevice = AmbientDeviceManager.get(this).getAmbientDevice(getIntent().getStringExtra("DEVICE_ADDRESS"));
        this.mHexNameTextView = (TextView) findViewById(R.id.activity_sensor_master_settings_hexNameTextView);
        this.mNameEditText = (EditText) findViewById(R.id.activity_sensor_master_settings_nameTextView);
        this.mDetailedLayout = (RelativeLayout) findViewById(R.id.activity_sensor_master_settings_detailedLayout);
        this.mExportTextView = (TextView) findViewById(R.id.activity_sensor_master_settings_exportTextView);
        this.mDeviceVersionNumber = (TextView) findViewById(R.id.activity_sensor_master_settings_versionTextView);
        this.mConnectionStatusTextView = (TextView) findViewById(R.id.activity_sensor_master_settings_connectionStatus);
        this.mDisclosureImage = (ImageView) findViewById(R.id.activity_sensor_master_settings_disclosureImage);
        this.mInstructionsLayout = (RelativeLayout) findViewById(R.id.activity_sensor_master_settings_instructionsLayout);
        this.handler = new Handler();
        this.rRefreshStatus = new Runnable() {
            /* class com.celaer.android.ambient.SensorMasterSettingsActivity.RunnableC02761 */

            public void run() {
                if (new Date().getTime() - SensorMasterSettingsActivity.mCurrentAmbientDevice.getTimestampBroadcast() < 5000) {
                    SensorMasterSettingsActivity.this.mConnectionStatusTextView.setText(SensorMasterSettingsActivity.this.getString(R.string.ready_to_connect));
                    SensorMasterSettingsActivity.this.mConnectionStatusTextView.setTextColor(SensorMasterSettingsActivity.this.getResources().getColor(R.color.greenReady));
                } else {
                    SensorMasterSettingsActivity.this.mConnectionStatusTextView.setText(SensorMasterSettingsActivity.this.getString(R.string.please_move_closer_to_the_sensor));
                    SensorMasterSettingsActivity.this.mConnectionStatusTextView.setTextColor(SensorMasterSettingsActivity.this.getResources().getColor(R.color.gray50));
                }
                SensorMasterSettingsActivity.this.handler.postDelayed(SensorMasterSettingsActivity.this.rRefreshStatus, 1000);
            }
        };
        this.mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = this.mBluetoothManager.getAdapter();
        CopyInstructionManual();
    }

    /* access modifiers changed from: protected */
    @Override // com.celaer.android.ambient.utilities.CelaerActivity
    public void onResume() {
        if (((MyApplication) getApplication()).wasInBackground && !this.mInstructionsPressed) {
            Log.d(TAG, "launching from BACKGROUND");
            Intent intent = new Intent();
            setResult(-1, intent);
            intent.putExtra(MainActivity.EXTRAS_DISCONNECTION_CODE, 0);
            finish();
        }
        super.onResume();
        this.mHexNameTextView.setText("(" + mCurrentAmbientDevice.getHexName() + ")");
        this.mNameEditText.setText(mCurrentAmbientDevice.getName());
        this.mDeviceVersionNumber.setText(mCurrentAmbientDevice.getFirmwareVersion());
        this.mDetailedLayout.setOnClickListener(this.mDetailedListener);
        this.mInstructionsLayout.setOnClickListener(this.mInstructionsListener);
        this.mExportTextView.setOnClickListener(this.mExportListener);
        this.mConnectionStatusTextView.setTextColor(getResources().getColor(R.color.gray50));
        this.mDisclosureImage.setVisibility(View.VISIBLE);
        this.mBluetoothAdapter.startLeScan(this);
        this.rRefreshStatus.run();
    }

    /* access modifiers changed from: protected */
    @Override // com.celaer.android.ambient.utilities.CelaerActivity
    public void onPause() {
        super.onPause();
        String tempString = this.mNameEditText.getText().toString().trim();
        if (tempString.equals("")) {
            tempString = getString(R.string.sensor) + " " + mCurrentAmbientDevice.getHexName();
        }
        this.handler.removeCallbacks(this.rRefreshStatus);
        this.mNameEditText.setText(tempString);
        mCurrentAmbientDevice.setName(tempString);
        this.mBluetoothAdapter.stopLeScan(this);
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode);
        if (requestCode == 4) {
            mCurrentAmbientDevice = AmbientDeviceManager.get(this).getAmbientDevice(data.getStringExtra("DEVICE_ADDRESS"));
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            Intent intent = new Intent();
            setResult(-1, intent);
            intent.putExtra(MainActivity.EXTRAS_DISCONNECTION_CODE, 0);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

//    public boolean onMenuItemSelected(int featureId, MenuItem item) { //Joey modified
//        if (item.getItemId() == 16908332) {
//            Intent intent = new Intent();
//            setResult(-1, intent);
//            intent.putExtra(MainActivity.EXTRAS_DISCONNECTION_CODE, 0);
//            finish();
//        }
//        return super.onMenuItemSelected(featureId, item);
//    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void openPDF(File pdfFile) {
        Log.d(TAG, "openPDF: " + Uri.fromFile(pdfFile));
        Intent i = new Intent("android.intent.action.VIEW");
        i.setDataAndType(Uri.fromFile(pdfFile), PDF_MIME_TYPE);
        startActivity(i);
    }

    public void askToOpenPDFThroughGoogleDrive(final Context context, final String pdfUrl) {
        new AlertDialog.Builder(context).setTitle(R.string.pdf_show_online_dialog_title).setMessage(R.string.pdf_show_online_dialog_question).setNegativeButton(R.string.no, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            /* class com.celaer.android.ambient.SensorMasterSettingsActivity.DialogInterface$OnClickListenerC02794 */

            public void onClick(DialogInterface dialog, int which) {
                SensorMasterSettingsActivity.this.openPDFThroughGoogleDrive(context, pdfUrl);
            }
        }).show();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void openPDFThroughGoogleDrive(Context context, String pdfUrl) {
        Intent i = new Intent("android.intent.action.VIEW");
        i.setDataAndType(Uri.parse(GOOGLE_DRIVE_PDF_READER_PREFIX + pdfUrl), HTML_MIME_TYPE);
        startActivity(i);
    }

    private void CopyInstructionManual() {
        String nativeFileName;
        Exception e;
        Log.d(TAG, "CopyInstructionManual");
        File exportDir = new File(Environment.getExternalStorageDirectory() + File.separator + "Ambient");
        Log.d(TAG, exportDir.toString());
        if (!exportDir.exists()) {
            Log.d(TAG, "directory does not exist, mkdir");
            exportDir.mkdirs();
        }
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e2) {
            Log.e("tag", e2.getMessage());
        }
        for (int i = 0; i < files.length; i++) {
            String fStr = files[i];
            Log.d(TAG, "file: " + fStr);
            if (Locale.getDefault().getLanguage().equalsIgnoreCase("ja")) {
                nativeFileName = "a570-manual-latest-ja.pdf";
            } else {
                nativeFileName = "a570-manual-latest.pdf";
            }
            if (fStr.equalsIgnoreCase(nativeFileName)) {
                File tempFile = new File(Environment.getExternalStorageDirectory() + File.separator + "Ambient" + File.separator, nativeFileName);
                if (tempFile.exists()) {
                    Log.d(TAG, "older FileExists:" + tempFile);
                    tempFile.delete();
                }
                try {
                    InputStream in = assetManager.open(files[i]);
                    OutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory() + File.separator + "Ambient" + File.separator + files[i]);
                    try {
                        copyFile(in, out);
                        in.close();
                        out.flush();
                        out.close();
                        return;
                    } catch (Exception e3) {
                        e = e3;
                    }
                } catch (Exception e4) {
                    e = e4;
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int read = in.read(buffer);
            if (read != -1) {
                out.write(buffer, 0, read);
            } else {
                return;
            }
        }
    }

    private static class ConditionListAllCursorLoader extends SQLiteCursorLoader {
        public ConditionListAllCursorLoader(Context context) {
            super(context);
        }

        /* access modifiers changed from: protected */
        @Override // com.celaer.android.ambient.database.SQLiteCursorLoader
        public Cursor loadCursor() {
            return AmbientDeviceManager.get(getContext()).queryConditionsAll(SensorMasterSettingsActivity.mCurrentAmbientDevice.getAddress());
        }
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (i == 0) {
            return new ConditionListAllCursorLoader(this);
        }
        return null;
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            Log.d(TAG, "fetchedResults: " + cursor.getCount());
            if (cursor.getCount() > 0) {
                exportCsvFromCursor(cursor);
            } else {
                new AlertDialog.Builder(this).setTitle(R.string.error).setMessage(R.string.noDataExists).setPositiveButton(R.string.yes, (DialogInterface.OnClickListener) null).show();
            }
        }
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void exportCsvFromCursor(Cursor cursor) {
        File exportDir = new File(Environment.getExternalStorageDirectory() + File.separator + "AmbientExport");
        Log.d(TAG, exportDir.toString());
        File file = null;
        //if (((double) (new File(getApplicationContext().getFilesDir().getAbsoluteFile().toString()).getFreeSpace() / ParseFileUtils.ONE_MB)) >= 0.1d) {
        if (((double) (new File(getApplicationContext().getFilesDir().getAbsoluteFile().toString()).getFreeSpace() / ParseFileUtils.ONE_MB)) >= 0.1d) {
            exportDir.toString();
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            } else {
                File[] filenames = exportDir.listFiles();
                int length = filenames.length;
                for (int i = 0; i < length; i++) {
                    filenames[i].delete();
                }
            }
            File file2 = new File(exportDir, mCurrentAmbientDevice.getName() + "-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".csv");
            try {
                file2.createNewFile();
                CSVWriter csvWriter = new CSVWriter(new FileWriter(file2));
                if (mCurrentAmbientDevice.tempUnitsC) {
                    csvWriter.writeNext(new String[]{"Year", "Month", "Day", "Hour", "Minute", "Second", "Temperature (C)", "Humidity (%)", "Light (Lux)"});
                } else {
                    csvWriter.writeNext(new String[]{"Year", "Month", "Day", "Hour", "Minute", "Second", "Temperature (F)", "Humidity (%)", "Light (Lux)"});
                }
                int timestampIndex = cursor.getColumnIndex(ConditionDatabaseHelper.COLUMN_CONDITIONS_TIMESTAMP);
                int tempCIndex = cursor.getColumnIndex(ConditionDatabaseHelper.COLUMN_CONDITIONS_TEMPC);
                int humidityIndex = cursor.getColumnIndex(ConditionDatabaseHelper.COLUMN_CONDITIONS_HUMIDITY);
                int lightIndex = cursor.getColumnIndex(ConditionDatabaseHelper.COLUMN_CONDITIONS_LIGHT);
                cursor.moveToFirst();
                Calendar cal = Calendar.getInstance();
                for (int i2 = 0; i2 < cursor.getCount(); i2++) {
                    long timestamp = cursor.getLong(timestampIndex);
                    float tempC = cursor.getFloat(tempCIndex);
                    float humidity = cursor.getFloat(humidityIndex);
                    long light = cursor.getLong(lightIndex);
                    cal.setTimeInMillis(1000 * timestamp);
                    String tempTempC = "n/a";
                    String tempHumidity = "n/a";
                    String tempLight = "n/a";
                    if (((double) tempC) <= 70.01d) {
                        if (mCurrentAmbientDevice.tempUnitsC) {
                            tempTempC = String.format("%.1f", Float.valueOf(tempC));
                        } else {
                            tempTempC = String.format("%.1f", Double.valueOf(convertCtoF((double) tempC)));
                        }
                    }
                    if (((double) humidity) <= 100.01d) {
                        if (mCurrentAmbientDevice.getDeviceType() == 0) {
                            tempHumidity = String.format("%d", Integer.valueOf(Math.round(humidity)));
                        } else if (mCurrentAmbientDevice.getDeviceType() == 1) {
                            tempHumidity = String.format("%.1f", Float.valueOf(humidity));
                        }
                    }
                    if (light < 65535) {
                        tempLight = "" + light;
                    }
                    csvWriter.writeNext(new String[]{String.format("%d", Integer.valueOf(cal.get(1))), String.format("%d", Integer.valueOf(cal.get(2) + 1)), String.format("%d", Integer.valueOf(cal.get(5))), String.format("%d", Integer.valueOf(cal.get(11))), String.format("%d", Integer.valueOf(cal.get(12))), String.format("%d", Integer.valueOf(cal.get(13))), tempTempC, tempHumidity, tempLight});
                    cursor.moveToNext();
                }
                csvWriter.close();
                file = file2;
            } catch (IOException e) {
                e = e;
                file = file2;
                Log.e(TAG, e.getMessage(), e);
                Uri u1 = Uri.fromFile(file);
                Date date = new Date();
                Intent sendIntent = new Intent("android.intent.action.SEND");
                sendIntent.putExtra("android.intent.extra.SUBJECT", "[" + getString(R.string.app_name) + "] " + mCurrentAmbientDevice.getName() + " " + getString(R.string.export_at) + " " + date.toString());
                sendIntent.putExtra("android.intent.extra.STREAM", u1);
                sendIntent.putExtra("android.intent.extra.TEXT", getString(R.string.ambient_monitor) + ": " + mCurrentAmbientDevice.getName() + " (" + mCurrentAmbientDevice.getHexName() + ")\n" + getString(R.string.export_date) + ": " + date.toString());
                sendIntent.setType(HTML_MIME_TYPE);
                startActivity(sendIntent);
            }
            Uri u122 = Uri.fromFile(file);
            Date date22 = new Date();
            Intent sendIntent22 = new Intent("android.intent.action.SEND");
            sendIntent22.putExtra("android.intent.extra.SUBJECT", "[" + getString(R.string.app_name) + "] " + mCurrentAmbientDevice.getName() + " " + getString(R.string.export_at) + " " + date22.toString());
            sendIntent22.putExtra("android.intent.extra.STREAM", u122);
            sendIntent22.putExtra("android.intent.extra.TEXT", getString(R.string.ambient_monitor) + ": " + mCurrentAmbientDevice.getName() + " (" + mCurrentAmbientDevice.getHexName() + ")\n" + getString(R.string.export_date) + ": " + date22.toString());
            sendIntent22.setType(HTML_MIME_TYPE);
            startActivity(sendIntent22);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateDevice(AmbientDeviceAdvObject object) {
        AmbientDevice existingDevice = AmbientDeviceManager.get(this).getAmbientDevice(object.getAddress());
        if (existingDevice != null) {
            Log.d(TAG, "updating device: " + object.getAddress());
            existingDevice.processBroadcast(object);
            AmbientDeviceManager.get(this).saveAmbientDevices();
        }
    }

    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        final AmbientDeviceAdvObject newObject = new AmbientDeviceAdvObject(device, scanRecord);
        if (newObject.getUUID().equals(MainActivity.AMBIENT_SENSOR_UUID) && newObject.isBroadcastMode()) {
            runOnUiThread(new Runnable() {
                /* class com.celaer.android.ambient.SensorMasterSettingsActivity.RunnableC02816 */

                public void run() {
                    SensorMasterSettingsActivity.this.updateDevice(newObject);
                }
            });
        }
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
