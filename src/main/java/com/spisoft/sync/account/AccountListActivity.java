package com.spisoft.sync.account;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.widget.Toolbar;
import com.spisoft.sync.Configuration;
import com.spisoft.sync.Log;
import com.spisoft.sync.R;

public class AccountListActivity extends AppCompatActivity{
    private View mAddButton;
    private ListView mListView;
    private AccountAdapter mAdapter;
    private View mEmptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar2));
        setFragment(new AccountListFragment());
    }

    public void setFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment,fragment)
                .addToBackStack(null).commit();
    }

    public void onBackPressed(){
        super.onBackPressed();
        if(getSupportFragmentManager().getFragments().size() == 0)
            finish();
    }
}
