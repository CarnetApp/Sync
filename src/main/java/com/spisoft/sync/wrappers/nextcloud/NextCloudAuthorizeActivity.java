package com.spisoft.sync.wrappers.nextcloud;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.spisoft.sync.R;

public class NextCloudAuthorizeActivity extends AppCompatActivity {
    public static final String EXTRA_ACCOUNT_ID="account_id";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_cloud_authorize);
    }
}
