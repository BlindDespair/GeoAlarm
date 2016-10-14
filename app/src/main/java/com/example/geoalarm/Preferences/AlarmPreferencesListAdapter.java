package com.example.geoalarm.Preferences;


import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.example.geoalarm.Alarm;
import com.example.geoalarm.App;
import com.example.geoalarm.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Иван on 04.03.2016.
 */
public class AlarmPreferencesListAdapter extends BaseAdapter implements Serializable {
    private List<AlarmPreference> preferences = new ArrayList<AlarmPreference>();

    private Alarm alarm;
    private String[] alarmTones;
    private String[] alarmTonePaths;
    private final String[] alarmDistances = {"150 " + App.getContext().getString(R.string.meters), "250 "  + App.getContext().getString(R.string.meters), "500 " + App.getContext().getString(R.string.meters), "1 "  + App.getContext().getString(R.string.kilometer),"2 "  + App.getContext().getString(R.string.kilometers),"5 " + App.getContext().getString(R.string.kilometers),"10 " + App.getContext().getString(R.string.kilometers)};

    public AlarmPreferencesListAdapter(Alarm alarm) {
//		(new Runnable(){
//
//			@Override
//			public void run() {
        Log.d("AlarmPreferenceListAdapter", "Loading Ringtones...");

        RingtoneManager ringtoneMgr = new RingtoneManager(App.getContext());

        ringtoneMgr.setType(RingtoneManager.TYPE_ALARM);

        Cursor alarmsCursor = ringtoneMgr.getCursor();

        alarmTones = new String[alarmsCursor.getCount()+1];
        alarmTones[0] = "Silent";
        alarmTonePaths = new String[alarmsCursor.getCount()+1];
        alarmTonePaths[0] = "";

        if (alarmsCursor.moveToFirst()) {
            do {
                alarmTones[alarmsCursor.getPosition()+1] = ringtoneMgr.getRingtone(alarmsCursor.getPosition()).getTitle(App.getContext());
                alarmTonePaths[alarmsCursor.getPosition()+1] = ringtoneMgr.getRingtoneUri(alarmsCursor.getPosition()).toString();
            }while(alarmsCursor.moveToNext());
        }
        Log.d("AlarmPreferenceListAdapter", "Finished Loading " + alarmTones.length + " Ringtones.");
        alarmsCursor.close();
//
//			}
//
//		}).run();
//
        setGeoAlarm(alarm);
    }

    public void setGeoAlarm(Alarm alarm) {
        this.alarm = alarm;
        preferences.clear();

        preferences.add(new AlarmPreference(AlarmPreference.Key.ALARM_DISTANCE, App.getContext().getString(R.string.alarm_trigger_distance), Integer.toString(alarm.getDistance().toInt()) + " " + App.getContext().getString(R.string.meters), alarmDistances, alarm.getDistance(), AlarmPreference.Type.LIST));

        Uri alarmToneUri = Uri.parse(alarm.getAlarmTonePath());
        Ringtone alarmTone = RingtoneManager.getRingtone(App.getContext(), alarmToneUri);

        if(alarmTone instanceof Ringtone && !alarm.getAlarmTonePath().equalsIgnoreCase("")){
            preferences.add(new AlarmPreference(AlarmPreference.Key.ALARM_TONE, App.getContext().getString(R.string.ringtone), alarmTone.getTitle(App.getContext()),alarmTones, alarm.getAlarmTonePath(), AlarmPreference.Type.LIST));
        }else{
            preferences.add(new AlarmPreference(AlarmPreference.Key.ALARM_TONE, App.getContext().getString(R.string.ringtone), getAlarmTones()[0],alarmTones, null, AlarmPreference.Type.LIST));
        }

        preferences.add(new AlarmPreference(AlarmPreference.Key.ALARM_VIBRATE, App.getContext().getString(R.string.vibration), null, null, alarm.getVibrate(), AlarmPreference.Type.BOOLEAN));
    }

    @Override
    public int getCount() {
        return preferences.size();
    }

    @Override
    public Object getItem(int position) {
        return preferences.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AlarmPreference alarmPreference = (AlarmPreference) getItem(position);
        LayoutInflater layoutInflater = LayoutInflater.from(App.getContext());
        switch (alarmPreference.getType()) {
            case BOOLEAN:
                if(null == convertView || convertView.getId() != android.R.layout.simple_list_item_checked)
                    convertView = layoutInflater.inflate(android.R.layout.simple_list_item_checked, null);

                CheckedTextView checkedTextView = (CheckedTextView) convertView.findViewById(android.R.id.text1);
                checkedTextView.setText(alarmPreference.getTitle());
                checkedTextView.setChecked((Boolean) alarmPreference.getValue());
                break;
            case LIST:
            default:
                if(null == convertView || convertView.getId() != android.R.layout.simple_list_item_2)
                    convertView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null);

                TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
                text1.setTextSize(18);
                text1.setText(alarmPreference.getTitle());

                TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);
                text2.setText(alarmPreference.getSummary());
                break;
        }

        return convertView;
    }

    public Alarm getGeoAlarm(){
        for(AlarmPreference preference : preferences){
            switch(preference.getKey()){
                case ALARM_TONE:
                    alarm.setAlarmTonePath((String) preference.getValue());
                    break;
                case ALARM_VIBRATE:
                    alarm.setVibrate((Boolean) preference.getValue());
                    break;
                case ALARM_DISTANCE:
                    alarm.setDistance(Alarm.Distances.valueOf((String)preference.getValue()));
                    break;
            }
        }

        return alarm;
    }
    public String[] getAlarmTones() {
        return alarmTones;
    }

    public String[] getAlarmTonePaths() {
        return alarmTonePaths;
    }
}
