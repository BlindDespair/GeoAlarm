package com.example.geoalarm.Preferences;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.example.geoalarm.Alarm;
import com.example.geoalarm.App;
import com.example.geoalarm.R;
import com.example.geoalarm.Service.AlarmService;


public class AlarmPreferencesActivity extends ActionBarActivity {
    private Alarm alarm;
    private ListAdapter listAdapter;
    private MediaPlayer mediaPlayer;
    private CountDownTimer alarmToneTimer;
    private ListView listView;
    private Intent serviceIntent;
    SharedPreferences alarmPreferences;
    SharedPreferences.Editor ed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_preferences);
        Bundle bundle = getIntent().getExtras();
        serviceIntent = new Intent(App.getContext(), AlarmService.class);
        serviceIntent.addCategory(AlarmService.TAG);
        alarmPreferences = getSharedPreferences("APP_PREFERENCES", MODE_PRIVATE);
        if (bundle != null && bundle.containsKey("alarm")) {
            setAlarm((Alarm) bundle.getSerializable("alarm"));
        } else {
            setAlarm(new Alarm(alarmPreferences));
        }
        if (bundle != null && bundle.containsKey("adapter")) {
            setListAdapter((AlarmPreferencesListAdapter) bundle.getSerializable("adapter"));
        } else {
            setListAdapter(new AlarmPreferencesListAdapter(getGeoAlarm()));
        }
        ed = alarmPreferences.edit();
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> l, View v, int position, long id) {
                final AlarmPreferencesListAdapter alarmPreferenceListAdapter = (AlarmPreferencesListAdapter) getListAdapter();
                final AlarmPreference alarmPreference = (AlarmPreference) alarmPreferenceListAdapter.getItem(position);
                AlertDialog.Builder alert;
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                switch (alarmPreference.getType()) {
                    case BOOLEAN:
                        CheckedTextView checkedTextView = (CheckedTextView) v;
                        boolean checked = !checkedTextView.isChecked();
                        ((CheckedTextView) v).setChecked(checked);
                        switch (alarmPreference.getKey()) {
                            case ALARM_VIBRATE:
                                alarm.setVibrate(checked);
                                if (checked) {
                                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                                    vibrator.vibrate(1000);
                                }
                                if(alarm.getAlarmStatus()) {
                                    restartService(alarm);
                                }
                                break;
                        }
                        ed.putBoolean("ALARM_VIBRATE", checked);
                        ed.commit();
                        alarmPreference.setValue(checked);
                        break;
                    case LIST:
                        alert = new AlertDialog.Builder(AlarmPreferencesActivity.this);

                        alert.setTitle(alarmPreference.getTitle());
                        // alert.setMessage(message);

                        CharSequence[] items = new CharSequence[alarmPreference.getOptions().length];
                        for (int i = 0; i < items.length; i++)
                            items[i] = alarmPreference.getOptions()[i];

                        alert.setItems(items, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (alarmPreference.getKey()) {
                                    case ALARM_DISTANCE:
                                        Alarm.Distances d = Alarm.Distances.values()[which];
                                        alarm.setDistance(d);
                                        ed.putInt("ALARM_DISTANCE", d.toInt());
                                        ed.commit();
                                        if(d.toInt() < 1000 && !alarmPreferences.getBoolean("ALERT_DIALOG_CHEKCBOX", false)){
                                            View checkBoxView = View.inflate(AlarmPreferencesActivity.this, R.layout.checkbox, null);
                                            CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
                                            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                                                @Override
                                                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                    ed.putBoolean("ALERT_DIALOG_CHEKCBOX", isChecked);
                                                    ed.commit();
                                                }
                                            });
                                            checkBox.setText(getString(R.string.warning_do_not_show_again));

                                            AlertDialog.Builder builder = new AlertDialog.Builder(AlarmPreferencesActivity.this);
                                            builder.setTitle(getString(R.string.warning));
                                            builder.setMessage(getString(R.string.warning_alert_message))
                                                    .setView(checkBoxView)
                                                    .setCancelable(false)
                                                    .setNegativeButton(getString(R.string.warning_alert_ok), new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            dialog.cancel();
                                                        }
                                                    }).show();
                                        }
                                        if(alarm.getAlarmStatus()) {
                                            restartService(alarm);
                                        }
                                        break;
                                    case ALARM_TONE:
                                        alarm.setAlarmTonePath(alarmPreferenceListAdapter.getAlarmTonePaths()[which]);
                                        ed.putString("ALARM_TONE", alarmPreferenceListAdapter.getAlarmTonePaths()[which]);
                                        ed.commit();
                                        if (alarm.getAlarmTonePath() != null) {
                                            if (mediaPlayer == null) {
                                                mediaPlayer = new MediaPlayer();
                                            } else {
                                                if (mediaPlayer.isPlaying())
                                                    mediaPlayer.stop();
                                                mediaPlayer.reset();
                                            }
                                            try {
                                                // mediaPlayer.setVolume(1.0f, 1.0f);
                                                mediaPlayer.setVolume(0.2f, 0.2f);
                                                mediaPlayer.setDataSource(AlarmPreferencesActivity.this, Uri.parse(alarm.getAlarmTonePath()));
                                                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                                                mediaPlayer.setLooping(false);
                                                mediaPlayer.prepare();
                                                mediaPlayer.start();

                                                // Force the mediaPlayer to stop after 3
                                                // seconds...
                                                if (alarmToneTimer != null)
                                                    alarmToneTimer.cancel();
                                                alarmToneTimer = new CountDownTimer(3000, 3000) {
                                                    @Override
                                                    public void onTick(long millisUntilFinished) {

                                                    }

                                                    @Override
                                                    public void onFinish() {
                                                        try {
                                                            if (mediaPlayer.isPlaying())
                                                                mediaPlayer.stop();
                                                        } catch (Exception e) {

                                                        }
                                                    }
                                                };
                                                alarmToneTimer.start();
                                            } catch (Exception e) {
                                                try {
                                                    if (mediaPlayer.isPlaying())
                                                        mediaPlayer.stop();
                                                } catch (Exception e2) {

                                                }
                                            }
                                        }
                                        if(alarm.getAlarmStatus()) {
                                            restartService(alarm);
                                        }
                                        break;
                                    default:
                                        break;
                                }
                                alarmPreferenceListAdapter.setGeoAlarm(getGeoAlarm());
                                alarmPreferenceListAdapter.notifyDataSetChanged();
                            }
                        });
                        alert.show();
                        break;
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("alarm", getGeoAlarm());
        outState.putSerializable("adapter", (AlarmPreferencesListAdapter) getListAdapter());
    }

    public Alarm getGeoAlarm() {
        return alarm;
    }

    public void setAlarm(Alarm alarm){
        this.alarm = alarm;
    }

    public ListAdapter getListAdapter() {
        return listAdapter;
    }

    public void setListAdapter(ListAdapter listAdapter) {
        this.listAdapter = listAdapter;
        getListView().setAdapter(listAdapter);

    }

    public void restartService(Alarm alarm){
        if(App.isAlarmServiceRunning(AlarmService.class)){
            stopService(serviceIntent);
            serviceIntent.putExtra("alarm", alarm);
            startService(serviceIntent);
        }
    }

    public ListView getListView() {
        if (listView == null)
            listView = (ListView) findViewById(R.id.settingsList);
        return listView;
    }
}
