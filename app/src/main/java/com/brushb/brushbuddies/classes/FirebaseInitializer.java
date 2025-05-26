package com.brushb.brushbuddies.classes;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class FirebaseInitializer extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}