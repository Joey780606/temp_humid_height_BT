package pcp.com.bttemperature;

import pcp.com.bttemperature.utilities.CelaerActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.Locale;

//public class MainActivity extends AppCompatActivity {
public class MainActivity extends CelaerActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ListView mListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayShowTitleEnabled(false);  // 91 important
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

    }
}