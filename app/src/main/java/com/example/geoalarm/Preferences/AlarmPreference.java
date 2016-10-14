package com.example.geoalarm.Preferences;

/**
 * Created by Иван on 04.03.2016.
 */
public class AlarmPreference {
    public enum Key{
        ALARM_TONE,
        ALARM_VIBRATE,
        ALARM_DISTANCE
    }
    public enum Type{
        BOOLEAN,
        LIST
    }


    private Key key;
    private Type type;
    private String title;
    private String summary;
    private Object value;
    private String[] options;

    public AlarmPreference(Key key, Object value, Type type) {
        this(key,null,null,null, value, type);
    }

    public AlarmPreference(Key key,String title, String summary, String[] options, Object value,  Type type) {
        setTitle(title);
        setSummary(summary);
        setOptions(options);
        setKey(key);
        setValue(value);
        setType(type);
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
