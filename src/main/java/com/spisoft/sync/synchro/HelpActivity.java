package com.spisoft.sync.synchro;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.spisoft.sync.R;


public class HelpActivity extends AppCompatActivity {

    private static final String SHOULD_START_ACTIVITY = "should_start_gdrive_act";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_help);
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(SHOULD_START_ACTIVITY, false).commit();
    }
    public void addAccount(View v){
        //startActivity(new Intent(this,AuthorizeActivity.class));
        finish();
    }
    public void exit(View v){
        finish();
    }
    public static boolean shouldStartActivity(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SHOULD_START_ACTIVITY, true);
    }

}
