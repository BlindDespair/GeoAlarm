package com.example.geoalarm.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;

import com.example.geoalarm.Alarm;
import com.example.geoalarm.Alert.AlarmAlertActivity;

/**
 * Created by Иван on 06.03.2016.
 */
public class AlarmManagerBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        WakeLocker.acquire(context);

        Bundle bundle = intent.getExtras();
        final Alarm alarm = (Alarm) bundle.getSerializable("alarm");

        Intent geoAlarmAlertActivityIntent;

        geoAlarmAlertActivityIntent = new Intent(context, AlarmAlertActivity.class);

        geoAlarmAlertActivityIntent.putExtra("alarm", alarm);

        geoAlarmAlertActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(geoAlarmAlertActivityIntent);
    }

}
