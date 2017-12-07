package com.spisoft.sync.browsing;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.spisoft.sync.Log;
import com.spisoft.sync.R;
import com.spisoft.sync.account.DBAccountHelper;
import com.spisoft.sync.wrappers.FileItem;

public class BrowserActivity extends AppCompatActivity {


    public static final String EXTRA_ACCOUNT_ID = "account_id";
    public static final String EXTRA_START_PATH = "start_path";
    public static final String EXTRA_AS_FILE_PICKER = "as_file_picker";
    public static final String EXTRA_DISPLAY_ONLY_MIMETYPE = "display_only_mimetype";
    private Fragment fragment;
    private int mAccountId;
    private DBAccountHelper.Account mAccount;
    private String mStartPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_sync_browser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        mAccountId = getIntent().getIntExtra(EXTRA_ACCOUNT_ID, -1);
        mStartPath = getIntent().getStringExtra(EXTRA_START_PATH);
        Log.d("accounddebug","brw get account "+mAccountId);
        mAccount = DBAccountHelper.getInstance(this).getAccount(mAccountId);
        Log.d("accounddebug","brw get account "+mAccount.friendlyName);

        setFragment(BrowsingFragment.newInstance(mAccount,new FileItem("/", true,0,0, "DIR"),getIntent().getBooleanExtra(EXTRA_AS_FILE_PICKER,false),getIntent().getStringExtra(EXTRA_DISPLAY_ONLY_MIMETYPE)));
    }
    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_main,fragment)
                .addToBackStack(null).commit();
    }

    public void onBackPressed(){
        if(getSupportFragmentManager().getBackStackEntryCount()==1){
            finish();
        }
        else
            super.onBackPressed();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
     //   getMenuInflater().inflate(R.menu.menu_main, menu);
        return false;
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
