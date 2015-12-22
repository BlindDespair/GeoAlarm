package com.example.geoalarm;

/**
 * Created by Иван on 23.11.2015.
 */
public class Alarm {
    private boolean alarmStatus;
    Alarm(){
        alarmStatus = false;
    }
    public void changeAlarmStatus(){
        if(alarmStatus){
            alarmStatus = false;
        }
        else {
            alarmStatus = true;
        }
    }
    public boolean getAlarmStatus(){
        return alarmStatus;
    }
}
