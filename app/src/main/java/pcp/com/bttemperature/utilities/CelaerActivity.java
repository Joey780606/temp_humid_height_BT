package pcp.com.bttemperature.utilities;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class CelaerActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();
        ((MyApplication) getApplication()).stopActivityTransitionTimer();  //Need to convert to (MyApplication)
        Log.d("CelaerActivity", "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        ((MyApplication) getApplication()).startActivityTransitionTimer();
    }
}
