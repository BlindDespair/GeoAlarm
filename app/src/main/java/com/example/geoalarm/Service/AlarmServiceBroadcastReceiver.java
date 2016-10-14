package com.example.geoalarm.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.example.geoalarm.Alarm;
import com.example.geoalarm.App;

public class AlarmServiceBroadcastReceiver extends BroadcastReceiver {

    private Intent serviceIntent;

    public AlarmServiceBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        Alarm alarm = (Alarm) bundle.getSerializable("alarm");
        SharedPreferences alarmPreferences = App.getContext().getSharedPreferences("APP_PREFERENCES", Context.MODE_PRIVATE);
        alarm.getValuesFromPreferences(alarmPreferences);
        serviceIntent = new Intent(App.getContext(), AlarmService.class);
        serviceIntent.putExtra("alarm", alarm);
        serviceIntent.addCategory(AlarmService.TAG);
        App.getContext().startService(serviceIntent);
    }
}
