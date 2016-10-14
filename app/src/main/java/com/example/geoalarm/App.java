package com.example.geoalarm;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

/**
 * Created by Иван on 29.11.2015.
 */
public class App extends Application {
    private static Context context;


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext() {
            return context;
    }

    public static boolean isAlarmServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static String cutReminderText(String str, int maxLength, int cutToLength){
        if(str.length() > maxLength){
            return str.substring(0,cutToLength) + "...";
        }
        return str;
    }

}
