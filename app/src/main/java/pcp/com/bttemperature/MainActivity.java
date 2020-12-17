package pcp.com.bttemperature;

import androidx.viewpager.widget.ViewPager;
import de.ewmksoft.xyplot.core.XYPlot;
import pcp.com.bttemperature.ambientDevice.AmbientDevice;
import pcp.com.bttemperature.ambientDevice.AmbientDeviceManager;
import pcp.com.bttemperature.utilities.CelaerActivity;

import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Date;
import java.util.Locale;

//public class MainActivity extends AppCompatActivity {
public class MainActivity extends CelaerActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ListView mListView;

    private AmbientDeviceSortAdapter mAdapter;

    public static final long MAX_REPORT_AGE = 86400000;

    private View.OnClickListener mConditionsListener = new View.OnClickListener() {
        /* class com.celaer.android.ambient.DeviceListActivity.View$OnClickListenerC024111 */

        public void onClick(View view) {
            View parentRow = (View) view.getParent().getParent().getParent().getParent();
            AmbientDevice device = AmbientDeviceManager.get(MainActivity.this).getAmbientDevice(((ListView) parentRow.getParent()).getPositionForView(parentRow));
//            Intent intentTransition = new Intent(DeviceListActivity.this, ConditionsActivity.class);
//            intentTransition.putExtra("DEVICE_ADDRESS", device.getAddress());
//            DeviceListActivity.this.startActivityForResult(intentTransition, 3);
        }
    };

    private View.OnClickListener mSettingsButtonListener = new View.OnClickListener() {
        /* class com.celaer.android.ambient.DeviceListActivity.View$OnClickListenerC024010 */

        public void onClick(View view) {
            View parentRow = (View) view.getParent().getParent().getParent().getParent();
            AmbientDevice device = AmbientDeviceManager.get(MainActivity.this).getAmbientDevice(((ListView) parentRow.getParent()).getPositionForView(parentRow));
//            Intent intentTransition = new Intent(MainActivity.this, SensorMasterSettingsActivity.class);
//            intentTransition.putExtra("DEVICE_ADDRESS", device.getAddress());
//            MainActivity.this.startActivityForResult(intentTransition, 4);
        }
    };

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

//        this.mBluetoothManager = (BluetoothManager) getSystemService("bluetooth");
//        this.mBluetoothAdapter = this.mBluetoothManager.getAdapter();
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

    public static double convertCtoF(double temperature) {
        return (1.8d * temperature) + 32.0d;
    }
}