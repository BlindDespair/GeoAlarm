package com.example.geoalarm;

import android.app.Application;
import android.content.Context;

/**
 * Created by Иван on 29.11.2015.
 */
public class App extends Application {
    private static Context context;

    public static Context getContext() {
        return context;
    }

    public static void setContext(Context mContext) {
        context = mContext;
    }

}
