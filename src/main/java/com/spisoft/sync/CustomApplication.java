package com.spisoft.sync;

import android.app.Application;

import com.spisoft.sync.utils.Utils;

/**
 * Created by phoenamandre on 03/06/17.
 */

public class CustomApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.context = this;
    }
}
