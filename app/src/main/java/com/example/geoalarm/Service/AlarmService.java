package com.example.geoalarm.Service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.example.geoalarm.Alarm;
import com.example.geoalarm.App;
import com.example.geoalarm.MainActivity;
import com.example.geoalarm.R;
import com.google.android.gms.maps.model.LatLng;

public class AlarmService extends Service {
    public static final String TAG = "AlarmServiceTag";
    public static LocationManager manager;
    public static boolean isGPSEnabled = false;
    private Alarm alarm;
    // flag for network status
    boolean isNetworkEnabled = false;
    private Intent serviceIntent;
    private LatLng markerPosition;
    private Notification geoAlarmNotification;
    private PendingIntent pendingIntent;
    private Notification.Builder builder;
    // flag for GPS status
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    } // method ends

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Bundle bundle = intent.getExtras();
        alarm = (Alarm) bundle.getSerializable("alarm");
        serviceIntent = intent;
        SharedPreferences alarmPreferences = getSharedPreferences("APP_PREFERENCES",MODE_PRIVATE);
        markerPosition = new LatLng(alarmPreferences.getFloat("MARKER_LATITUDE", 0),alarmPreferences.getFloat("MARKER_LONGITUDE", 0));
        Intent notificationIntent = new Intent(this, MainActivity.class);

        pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Geoalarm")
                .setContentIntent(pendingIntent);
        geoAlarmNotification = builder.build();
        startForeground(1337, geoAlarmNotification);
        return START_NOT_STICKY;
    } // method ends

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        //android.os.Debug.waitForDebugger();
        Log.d("service","Service is created//////////// ");
        // start asyntask to get locations
        new GetLocations().execute();
    }// on create ends

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        stopForeground(true);
        Log.d("service","Service is  Destroyed  //////////// ");
        if (manager != null && isGPSEnabled) {
            manager.removeUpdates(myListener);
            Log.d("service","Service is  Destroyed under if //////////// ");
        }

    } // method ends

    public class GetLocations extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            // getting GPS status
            isGPSEnabled = manager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);
            // getting network status
            isNetworkEnabled = manager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (isGPSEnabled) {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                        0, myListener);
            }
        }


    }// asyntask class ends

    public LocationListener myListener = new LocationListener() {

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onLocationChanged(Location location) {
            if(alarm.getAlarmStatus()){
                float[] distance = new float[1];
                Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                        markerPosition.latitude, markerPosition.longitude, distance);
                if (distance[0] > 1000) {
                    float distanceInKilometres = (distance[0] - alarm.getDistance().toInt()) / 1000;
                    builder.setContentText(getString(R.string.alarm_triggers_in) + " " + Float.toString((float) Math.round(distanceInKilometres * 100) / 100) + " " + getString(R.string.kilometers));
                } else {
                    builder.setContentText(getString(R.string.alarm_triggers_in) + " " + Integer.toString(Math.round(distance[0]) - alarm.getDistance().toInt()) + " " + getString(R.string.meters));
                }
                geoAlarmNotification = builder.build();
                startForeground(1337, geoAlarmNotification);

                if(distance[0] <= alarm.getDistance().toInt()){
                    Intent myIntent = new Intent(App.getContext(), AlarmManagerBroadcastReceiver.class);
                    myIntent.putExtra("alarm", alarm);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(App.getContext(), 0, myIntent,PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager alarmManager = (AlarmManager)App.getContext().getSystemService(Context.ALARM_SERVICE);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
                    stopService(serviceIntent);
                }
            }
        }

        public void onProviderDisabled(String arg0) {
            // TODO Auto-generated method stub
            // Toast.makeText(mContext, "Gps is disable",
            // Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String arg0) {
            // TODO Auto-generated method stub
            // Toast.makeText(mContext, "Gps is on", Toast.LENGTH_SHORT).show();
        }

        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
            // TODO Auto-generated method stub
            // Toast.makeText(appcontext, "Gps  status is chnged ",
            // Toast.LENGTH_SHORT).show();
        }
    };
}
