package com.spisoft.sync;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.spisoft.sync.account.DBAccountHelper;
import com.spisoft.sync.browsing.BrowsingFragment;
import com.spisoft.sync.synchro.SynchroService;
import com.spisoft.sync.wrappers.WrapperFactory;
import com.spisoft.sync.wrappers.AsyncLister;
import com.spisoft.sync.wrappers.FileItem;

import java.util.List;

public class MainActivity extends AppCompatActivity {


    private Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

// Create client object to perform remote operations

        WrapperFactory.getWrapper(this, 0, 0).getAsyncLister("/").retrieveList(0, new AsyncLister.AsyncListerListener() {
            @Override
            public void onListingResult(int requestCode, int resultCode, List<FileItem> list) {

            }
        });
        startService(new Intent(this, SynchroService.class));
        setContentView(R.layout.activity_sync_browser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

   //     setFragment(BrowsingFragment.newInstance(new DBAccountHelper.Account(0,0, null),new FileItem("/", true,0,0)));
    }
    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_main,fragment)
                .addToBackStack(null).commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
