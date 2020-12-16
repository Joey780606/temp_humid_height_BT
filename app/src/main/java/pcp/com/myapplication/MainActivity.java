package pcp.com.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import pcp.com.myapplication.utilities.CelaerActivity;

import android.os.Bundle;

//public class MainActivity extends AppCompatActivity {
public class MainActivity extends CelaerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}