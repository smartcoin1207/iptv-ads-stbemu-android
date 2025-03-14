package com.stbemugen.utils;

import android.app.Application;

import com.google.android.gms.ads.MobileAds;
import com.stbemugen.R;

import papaya.in.admobopenads.AppOpenManager;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        MobileAds.initialize(this);
        new AppOpenManager(this,getResources().getString(R.string.admob_app_open_id));
    }
}
