package com.example.geoalarm;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.example.geoalarm.Preferences.AlarmPreferencesActivity;
import com.example.geoalarm.Service.AlarmService;
import com.example.geoalarm.Service.AlarmServiceBroadcastReceiver;
import com.example.geoalarm.map.MapWrapperLayout;
import com.example.geoalarm.map.OnInfoWindowElemTouchListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity implements View.OnClickListener, View.OnFocusChangeListener{

    private ViewGroup infoWindowWithoutButton;
    private ViewGroup infoWindow;
    private TextView infoTitle, infoReminderText, tvTimer;
    private Button infoButton, infoReminderButton;
    private OnInfoWindowElemTouchListener infoButtonListener, infoReminderButtonListener;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Alarm alarm;
    private SharedPreferences alarmPreferences;
    private final MarkerOptions theMarker = new MarkerOptions();
    private boolean intentUsed = false;
    private Marker marker;
    private Circle circle;
    private Intent serviceIntent;
    private PendingIntent pendingIntent;
    private AlarmManager alarmManager;
    private CountDownTimer countDownTimer;
    private long timeWhenToStart;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_map);
        this.infoWindow = (ViewGroup)getLayoutInflater().inflate(R.layout.info_window, null);
        setUpMapIfNeeded();
        infoWindowWithoutButton = (ViewGroup)getLayoutInflater().inflate(R.layout.info_window_without_button, null);
        this.infoTitle = (TextView)infoWindow.findViewById(R.id.title);
        this.infoReminderText = (TextView) infoWindow.findViewById(R.id.reminder_text_output);
        this.infoButton = (Button)infoWindow.findViewById(R.id.infoButton);
        this.infoReminderButton = (Button) infoWindow.findViewById(R.id.infoReminderButton);
        findViewById(R.id.btnSettings).setOnClickListener(this);
        tvTimer = (TextView) findViewById(R.id.tv_timer);
        alarmPreferences = getSharedPreferences("APP_PREFERENCES",MODE_PRIVATE);
        alarm = new Alarm(alarmPreferences);
        serviceIntent = new Intent(App.getContext(), AlarmService.class);
        serviceIntent.addCategory(AlarmService.TAG);
        if(alarmPreferences.getBoolean("ALARM_STATUS", false)){
            alarm.changeAlarmStatus();
            marker = mMap.addMarker(theMarker
                    .position(new LatLng(alarmPreferences.getFloat("MARKER_LATITUDE", 0), alarmPreferences.getFloat("MARKER_LONGITUDE", 0)))
                    .title("")
                    .snippet(getString(R.string.marker_reminder_default))
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)));
            if(alarm.reminder.getReminderStatus() && !alarm.reminder.getReminderText().equals("")){
                marker.setSnippet(App.cutReminderText(alarm.reminder.getReminderText(), 55, 50));
            }
            circle = mMap.addCircle(new CircleOptions()
                    .center(theMarker.getPosition())
                    .radius(alarm.getDistance().toInt())
                    .fillColor(0x10000000)
                    .strokeWidth(0));
            infoButton.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic1));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(theMarker.getPosition(), 14));
        }
        this.infoButtonListener = new OnInfoWindowElemTouchListener(infoButton,
                ContextCompat.getDrawable(this, R.drawable.ic2),
                ContextCompat.getDrawable(this, R.drawable.ic3),
                ContextCompat.getDrawable(this, R.drawable.ic1),
                alarm)
        {
            @Override
            protected void onClickConfirmed(View v, final Marker marker) {
                // Here we can perform some action triggered after clicking the button
                alarm.changeAlarmStatus();
                SharedPreferences.Editor ed = alarmPreferences.edit();
                if(alarm.getAlarmStatus()) {
                    ed.putFloat("MARKER_LATITUDE", (float) theMarker.getPosition().latitude);
                    ed.putFloat("MARKER_LONGITUDE", (float) theMarker.getPosition().longitude);
                    Intent myIntent = new Intent(App.getContext(), AlarmServiceBroadcastReceiver.class);
                    myIntent.putExtra("alarm", alarm);
                    pendingIntent = PendingIntent.getBroadcast(App.getContext(), 0, myIntent,PendingIntent.FLAG_CANCEL_CURRENT);
                    alarmManager = (AlarmManager)App.getContext().getSystemService(Context.ALARM_SERVICE);
                    if(alarm.reminder.getReminderStatus()){
                        countDown(alarm.reminder.getTimer());
                        timeWhenToStart = System.currentTimeMillis() + alarm.reminder.getTimer() * 1000;
                        alarmManager.set(AlarmManager.RTC_WAKEUP, timeWhenToStart, pendingIntent);
                        ed.putLong("TIME_WHEN_TO_START", timeWhenToStart);
                    }
                    else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
                    }
                }
                else{
                    if(countDownTimer != null){
                        countDownTimer.cancel();
                        tvTimer.setText("");
                    }
                    if(pendingIntent != null){
                        alarmManager.cancel(pendingIntent);
                    }
                    if(App.isAlarmServiceRunning(AlarmService.class)){
                        stopService(serviceIntent);
                    }
                }
                ed.putBoolean("ALARM_STATUS", alarm.getAlarmStatus());
                ed.commit();
            }
        };
        this.infoButton.setOnTouchListener(infoButtonListener);
        this.infoReminderButtonListener = new OnInfoWindowElemTouchListener(infoReminderButton,
                ContextCompat.getDrawable(this, R.drawable.edit_button),
                ContextCompat.getDrawable(this, R.drawable.edit_button),
                ContextCompat.getDrawable(this, R.drawable.edit_button),
                alarm) {
            @Override
            protected void onClickConfirmed(View v, Marker marker) {
                showReminderEditorDialog();
            }
        };
        this.infoReminderButton.setOnTouchListener(infoReminderButtonListener);
        if(!alarm.getAlarmStatus()) {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = null;
            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) != null){
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            else if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null){
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            else{
                location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
            if(location != null){
                LatLng myPosition = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPosition, 14));
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onStart() {
        super.onStart();
        alarmPreferences = getSharedPreferences("APP_PREFERENCES",MODE_PRIVATE);
        if(alarm.getAlarmStatus() != alarmPreferences.getBoolean("ALARM_STATUS", false)){
            alarm.changeAlarmStatus();
        }
        if(!alarm.getAlarmStatus()){
            if(!intentUsed || alarmPreferences.getBoolean("ALARM_WORKED",false)){
                mMap.clear();
            }
            infoButton.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic2));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        alarmPreferences = getSharedPreferences("APP_PREFERENCES",MODE_PRIVATE);
        SharedPreferences.Editor ed = alarmPreferences.edit();
        ed.putBoolean("ALARM_WORKED", false);
        ed.commit();
        alarm.getValuesFromPreferences(alarmPreferences);
        if(circle != null && alarm.getDistance().toInt() != (int) circle.getRadius()){
            circle.setRadius(alarm.getDistance().toInt());
        }
        if(alarm.getAlarmStatus() && !App.isAlarmServiceRunning(AlarmService.class) && alarm.reminder.getReminderStatus() && alarmPreferences.contains("TIME_WHEN_TO_START")){
            countDown((int) (alarmPreferences.getLong("TIME_WHEN_TO_START", 0) - System.currentTimeMillis())/1000);
        }
        if(alarm.getAlarmStatus() && intentUsed){
            serviceIntent.putExtra("alarm", alarm);
        }
        intentUsed = false;
        setUpMapIfNeeded();

    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final MapWrapperLayout mapWrapperLayout = (MapWrapperLayout)findViewById(R.id.map_relative_layout);
        // MapWrapperLayout initialization
        // 20 - offset between the default InfoWindow bottom edge and it's content bottom edge
        mapWrapperLayout.init(mMap, 47 + getPixelsFromDp(this,20));
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                this.getInfoContents(marker);
                if (mMap.getMyLocation() != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    return infoWindow;
                }
                else{
                    return infoWindowWithoutButton;
                }
            }

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public View getInfoContents(Marker marker) {
                // Setting up the infoWindow with current marker's info
                if (mMap.getMyLocation() != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    infoReminderText.setText(marker.getSnippet());
                    infoTitle.setText(marker.getTitle());
                    //infoButtonListener.setMarker(marker);
                    infoButtonListener.setMarker(marker);
                    // We must call this to set the current marker and infoWindow references
                    // to the MapWrapperLayout
                    mapWrapperLayout.setMarkerWithInfoWindow(marker, infoWindow);
                    return infoWindow;
                }
                else{
                    ((TextView) infoWindowWithoutButton.findViewById(R.id.titleWithoutButton)).setText(marker.getTitle());
                    return infoWindowWithoutButton;
                }
            }
        });
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    showAlertDialog();
                }
                return false;
            }
        });
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onMapLongClick(LatLng latLng) {
                mMap.clear();
                if(alarm.getAlarmStatus()){
                    alarm.changeAlarmStatus();
                    infoButton.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic2));
                    if(countDownTimer != null){
                        countDownTimer.cancel();
                        tvTimer.setText("");
                    }
                    if(pendingIntent != null){
                        alarmManager.cancel(pendingIntent);
                    }
                    if(App.isAlarmServiceRunning(AlarmService.class)){
                        stopService(serviceIntent);
                    }
                }
                marker = mMap.addMarker(theMarker
                        .position(new LatLng(latLng.latitude, latLng.longitude))
                        .title("")
                        .snippet(getString(R.string.marker_reminder_default))
                        .draggable(false)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin)));
                if(alarm.reminder.getReminderStatus() && !alarm.reminder.getReminderText().equals("")){
                    marker.setSnippet(App.cutReminderText(alarm.reminder.getReminderText(), 55, 50));
                }
                circle = mMap.addCircle(new CircleOptions()
                        .center(theMarker.getPosition())
                        .radius(alarm.getDistance().toInt())
                        .fillColor(0x10000000)
                        .strokeWidth(0));
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    setMarkerTitle();
                } else {
                    showAlertDialog();
                    setMarkerTitle();
                }
                return false;
            }
        });

        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onMyLocationChange(Location location) {
                if (mMap.getMyLocation() != null && theMarker.getPosition() != null) {
                    setMarkerTitle();
                    if(marker.isInfoWindowShown()){
                        marker.showInfoWindow();
                    }
                }
            }
        });

    }



    @Override
    protected void onPause() {
        super.onPause();
        alarmPreferences = getSharedPreferences("APP_PREFERENCES", MODE_PRIVATE);
        if(!intentUsed && !alarmPreferences.getBoolean("ALARM_WORKED",false)) {
            SharedPreferences.Editor ed = alarmPreferences.edit();
            if(alarm.getAlarmStatus()){
                ed.putBoolean("ALARM_STATUS", alarm.getAlarmStatus());
                ed.putBoolean("ALARM_REMINDER_STATUS", alarm.reminder.getReminderStatus());
                ed.commit();
            }
            else{
                if(countDownTimer != null){
                    countDownTimer.cancel();
                    tvTimer.setText("");
                }
                if(pendingIntent != null){
                    alarmManager.cancel(pendingIntent);
                }
                if(App.isAlarmServiceRunning(AlarmService.class)){
                    stopService(serviceIntent);
                }
                ed.putBoolean("ALARM_STATUS", alarm.getAlarmStatus());
                ed.commit();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_alarm_preference, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void showAlertDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.not_able_to_get_location))
                .setMessage(getString(R.string.not_able_to_get_location_alert_message))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.not_able_to_get_location_alert_negative_button),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                .setPositiveButton(getString(R.string.not_able_to_get_location_alert_positive_button),
                        new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(
                                                Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                        )
                                );
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    protected void showReminderEditorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.reminder_editor, null);
        final EditText reminder_text = (EditText) dialogView.findViewById(R.id.reminder_text);
        final EditText et_hours = (EditText) dialogView.findViewById(R.id.et_hours);
        et_hours.setFilters(new InputFilter[] {new InputFilterMinMax(0,99)});
        final EditText et_mins = (EditText) dialogView.findViewById(R.id.et_mins);
        et_mins.setFilters(new InputFilter[] {new InputFilterMinMax(0,59)});
        final EditText et_secs = (EditText) dialogView.findViewById(R.id.et_secs);
        et_secs.setFilters(new InputFilter[] {new InputFilterMinMax(0,59)});
        et_hours.addTextChangedListener(new TimerTextWatcher(et_hours, et_mins));
        et_mins.addTextChangedListener(new TimerTextWatcher(et_mins, et_secs));
        et_secs.addTextChangedListener(new TimerTextWatcher(et_secs, null));
        et_hours.setOnFocusChangeListener(this);
        et_mins.setOnFocusChangeListener(this);
        et_secs.setOnFocusChangeListener(this);
        final Switch sw = (Switch) dialogView.findViewById(R.id.switch1);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(buttonView.isChecked()){
                    reminder_text.setEnabled(true);
                    reminder_text.requestFocus();
                    et_hours.setEnabled(true);
                    et_mins.setEnabled(true);
                    et_secs.setEnabled(true);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(reminder_text, InputMethodManager.SHOW_IMPLICIT);
                }
                else{
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(dialogView.getWindowToken(),0);
                    reminder_text.clearFocus();
                    et_hours.clearFocus();
                    et_mins.clearFocus();
                    et_secs.clearFocus();
                    reminder_text.setEnabled(false);
                    et_hours.setEnabled(false);
                    et_mins.setEnabled(false);
                    et_secs.setEnabled(false);
                }
            }
        });
        if(alarm.reminder.getReminderStatus()) {
            sw.setChecked(true);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(reminder_text, InputMethodManager.SHOW_IMPLICIT);
        }
        reminder_text.setText(alarm.reminder.getReminderText());
        et_hours.setText(Integer.toString(alarm.reminder.getTimer()/3600));
        et_mins.setText(Integer.toString((alarm.reminder.getTimer()%3600)/60));
        et_secs.setText(Integer.toString(alarm.reminder.getTimer()%3600%60));
        editTimeText(et_hours);
        editTimeText(et_mins);
        editTimeText(et_secs);
        builder.setView(dialogView).setPositiveButton(getString(R.string.save_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor ed = alarmPreferences.edit();
                if (sw.isChecked()) {
                    View[] v = {et_hours, et_mins, et_secs};
                    if (!alarm.reminder.getReminderStatus()) {
                        alarm.reminder.changeReminderStatus();
                    }
                    alarm.reminder.setReminderText(reminder_text.getText().toString());
                    if (!alarm.reminder.getReminderText().equals("")) {
                        marker.setSnippet(App.cutReminderText(alarm.reminder.getReminderText(), 55, 50));
                        marker.showInfoWindow();
                    }
                    else{
                        marker.setSnippet(getString(R.string.marker_reminder_default));
                        marker.showInfoWindow();
                    }
                    ed.putString("REMINDER_TEXT", alarm.reminder.getReminderText());
                    ed.commit();
                    for (View view : v) {
                        if (((EditText) view).getText().toString().equals("")) {
                            ((EditText) view).setText("00");
                        }
                    }
                    alarm.reminder.setTimer(Integer.parseInt(et_hours.getText().toString()), Integer.parseInt(et_mins.getText().toString()), Integer.parseInt(et_secs.getText().toString()));
                    if (alarm.getAlarmStatus() && !App.isAlarmServiceRunning(AlarmService.class)) {
                        countDownTimer.cancel();
                        countDown(alarm.reminder.getTimer());
                        Intent myIntent = new Intent(App.getContext(), AlarmServiceBroadcastReceiver.class);
                        myIntent.putExtra("alarm", alarm);
                        pendingIntent = PendingIntent.getBroadcast(App.getContext(), 0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                        alarmManager = (AlarmManager) App.getContext().getSystemService(Context.ALARM_SERVICE);
                        timeWhenToStart = System.currentTimeMillis() + alarm.reminder.getTimer() * 1000;
                        alarmManager.set(AlarmManager.RTC_WAKEUP, timeWhenToStart, pendingIntent);
                        ed.putLong("TIME_WHEN_TO_START", timeWhenToStart);
                    }
                } else {
                    if (alarm.reminder.getReminderStatus()) {
                        alarm.reminder.changeReminderStatus();
                    }
                    marker.setSnippet(getString(R.string.marker_reminder_default));
                    marker.showInfoWindow();
                    if (alarm.getAlarmStatus()) {
                        Intent myIntent = new Intent(App.getContext(), AlarmServiceBroadcastReceiver.class);
                        myIntent.putExtra("alarm", alarm);
                        pendingIntent = PendingIntent.getBroadcast(App.getContext(), 0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                        alarmManager = (AlarmManager) App.getContext().getSystemService(Context.ALARM_SERVICE);
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
                    }
                }
                ed.putBoolean("ALARM_REMINDER_STATUS", alarm.reminder.getReminderStatus());
                ed.commit();
            }
        }).setNegativeButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if(alarm.reminder.getReminderStatus()){
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        WindowManager.LayoutParams wmlp = alertDialog.getWindow().getAttributes();
        wmlp.gravity = Gravity.TOP;
        wmlp.y = 40;
        alertDialog.show();
    }

    public static int getPixelsFromDp(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }

    public float getDistance(Location location, MarkerOptions marker){
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                marker.getPosition().latitude, marker.getPosition().longitude, results);
        return results[0];
    }

    public void setMarkerTitle(){
        if (mMap.getMyLocation() != null) {
            float distance = getDistance(mMap.getMyLocation(), theMarker);
            if (distance > 1000) {
                distance = distance / 1000;
                marker.setTitle(getString(R.string.distance) + ": " + Float.toString((float) Math.round(distance * 100) / 100) + " " + getString(R.string.kilometers));
            } else {
                marker.setTitle(getString(R.string.distance) + ": " + Integer.toString(Math.round(distance)) + " " + getString(R.string.meters));
            }
        } else {
            marker.setTitle(getString(R.string.no_location_data));
        }
    }

    public void editTimeText(View v){
        if(((EditText) v).length() < 2){
            for(int i = 2 - ((EditText) v).length(); i >= ((EditText) v).length(); i--)
                ((EditText) v).setText("0" + ((EditText) v).getText());
        }
    }

    public void countDown(int timerTimeLeft){
        countDownTimer = new CountDownTimer(timerTimeLeft*1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                String hours, mins, secs;
                hours = Integer.toString((int) (millisUntilFinished/1000)/3600);
                mins = Integer.toString((int) ((millisUntilFinished/1000)%3600)/60);
                secs = Integer.toString((int) ((millisUntilFinished/1000)%3600)%60);
                if(hours.length() < 2){
                    hours = "0" + hours;
                }
                for(int i = 2 - mins.length(); i >= mins.length(); i--){
                    mins = "0" + mins;
                }
                if(secs.length() < 2){
                    secs = "0" + secs;
                }
                tvTimer.setText(getString(R.string.timer_alarm_delayed_for) + hours + ":" + mins + ":" + secs);
            }

            @Override
            public void onFinish() {
                tvTimer.setText("");
            }
        }.start();
    }

    @Override
    public void onClick(View v) {
        if(v == findViewById(R.id.btnSettings)){
            Intent AlarmPreferencesActivityIntent;
            AlarmPreferencesActivityIntent = new Intent(App.getContext(), AlarmPreferencesActivity.class);
            AlarmPreferencesActivityIntent.putExtra("alarm", alarm);
            AlarmPreferencesActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intentUsed = true;
            App.getContext().startActivity(AlarmPreferencesActivityIntent);
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(hasFocus){
            ((EditText) v).setText("");
        }else {
            editTimeText(v);
        }

    }
}
