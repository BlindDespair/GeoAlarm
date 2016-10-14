package com.example.geoalarm;

import android.content.SharedPreferences;
import android.media.RingtoneManager;

import java.io.Serializable;

/**
 * Created by Иван on 23.11.2015.
 */
public class Alarm implements Serializable {
    public enum Distances {
        CLOSEST,
        CLOSER,
        CLOSE,
        MEDIUM,
        FAR,
        FARTHER,
        FARTHEST;

        public int toInt() {
            switch (this.ordinal()) {
                case 0:
                    return 150;
                case 1:
                    return 250;
                case 2:
                    return 500;
                case 3:
                    return 1000;
                case 4:
                    return 2000;
                case 5:
                    return 5000;
                case 6:
                    return 10000;
            }
            return 5000;
        }
    }
    public Reminder reminder = new Reminder();
    private boolean alarmStatus = false;
    private String alarmTonePath = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
    private Boolean vibrate = true;
    private Distances distance = Distances.MEDIUM;

    public Alarm(SharedPreferences alarmPreferences) {
        getValuesFromPreferences(alarmPreferences);

    }

    public void getValuesFromPreferences(SharedPreferences alarmPreferences) {
        if (alarmPreferences.contains("ALARM_TONE")) {
            alarmTonePath = alarmPreferences.getString("ALARM_TONE", "");
        }
        if (alarmPreferences.contains("ALARM_VIBRATE")) {
            vibrate = alarmPreferences.getBoolean("ALARM_VIBRATE", true);
        }
        if (alarmPreferences.contains("ALARM_DISTANCE")) {
            distance = intToDistances(alarmPreferences.getInt("ALARM_DISTANCE", 1000));
        }
        if(alarmPreferences.contains("ALARM_REMINDER_STATUS")){
            if(reminder.getReminderStatus() != alarmPreferences.getBoolean("ALARM_REMINDER_STATUS", false)){
                reminder.changeReminderStatus();
            }
        }
        if(alarmPreferences.contains("REMINDER_TEXT")){
            reminder.setReminderText(alarmPreferences.getString("REMINDER_TEXT", ""));
        }
    }

    public void changeAlarmStatus() {
        if (alarmStatus) {
            alarmStatus = false;
        } else {
            alarmStatus = true;
        }
    }

    public boolean getAlarmStatus() {
        return alarmStatus;
    }

    public String getAlarmTonePath() {
        return alarmTonePath;
    }

    /**
     * @param alarmTonePath the alarmTonePath to set
     */
    public void setAlarmTonePath(String alarmTonePath) {
        this.alarmTonePath = alarmTonePath;
    }

    /**
     * @return the vibrate
     */
    public Boolean getVibrate() {
        return vibrate;
    }

    /**
     * @param vibrate the vibrate to set
     */
    public void setVibrate(Boolean vibrate) {
        this.vibrate = vibrate;
    }

    public Distances getDistance() {
        return distance;
    }

    public void setDistance(Distances distance) {
        this.distance = distance;
    }

    public Distances intToDistances(int distance) {
        switch (distance) {
            case 150:
                return Distances.CLOSEST;
            case 250:
                return Distances.CLOSER;
            case 500:
                return Distances.CLOSE;
            case 1000:
                return Distances.MEDIUM;
            case 2000:
                return Distances.FAR;
            case 5000:
                return Distances.FARTHER;
            case 10000:
                return Distances.FARTHEST;
        }
        return Distances.FARTHER;
    }
    public class Reminder implements Serializable{
        private boolean reminder_status;
        private String reminder_text;
        private int timer;
        Reminder(){
            reminder_status = false;
            reminder_text = "";
            timer = 0;
        }

        public boolean getReminderStatus(){
            return reminder_status;
        }
        public String getReminderText() {
            return reminder_text;
        }

        public int getTimer() {
            return timer;
        }

        public void changeReminderStatus(){
            reminder_status = !reminder_status;
        }

        public void setReminderText(String reminder_text) {
            this.reminder_text = reminder_text;
        }

        public void setTimer(int hours, int mins, int secs) {
            this.timer = hours * 3600 + mins * 60 + secs;
        }
    }
}
