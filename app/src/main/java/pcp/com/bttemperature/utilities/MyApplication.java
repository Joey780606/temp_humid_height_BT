package pcp.com.bttemperature.utilities;

import android.app.Application;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

class MyApplication extends Application {
    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    public boolean wasInBackground = true;

    public void startActivityTransitionTimer() {
        this.mActivityTransitionTimer = new Timer();
        this.mActivityTransitionTimerTask = new TimerTask() {

            @Override
            public void run() {
                MyApplication.this.wasInBackground = true;
                Log.d("MyApplication", "wentToBackground");
            }
        };
        this.mActivityTransitionTimer.schedule(this.mActivityTransitionTimerTask, 2000);
    }

    public void stopActivityTransitionTimer() {
        if(this.mActivityTransitionTimerTask != null) {
            this.mActivityTransitionTimerTask.cancel();
        }
        if(this.mActivityTransitionTimer != null) {
            this.mActivityTransitionTimer.cancel();
        }
        this.wasInBackground = false;
    }
}
