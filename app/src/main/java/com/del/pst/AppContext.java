package com.del.pst;

import android.app.Application;

import androidx.room.Room;

import com.del.pst.dao.AppDatabase;

public class AppContext extends Application {

    public static final String DB_NAME = "pst";

    private static AppContext instance;
    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        database = Room.databaseBuilder(this, AppDatabase.class, DB_NAME).
                allowMainThreadQueries().
                build();
    }

    public static AppContext getInstance() {
        return instance;
    }

    public AppDatabase getDatabase() {
        return database;
    }
}
