package com.spisoft.sync.wrappers.googledrive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class AuthorizeActivity extends AppCompatActivity implements DriveSyncWrapper.OnConnectiongListener {

    private DriveSyncWrapper mDriveWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // setContentView(R.layout.activity_authorize);
        mDriveWrapper = new DriveSyncWrapper(this, -1);
        mDriveWrapper.authorize(this, this);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        mDriveWrapper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConnected() {
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onConnectionFailed() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}
