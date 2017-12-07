package com.spisoft.sync.account;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.spisoft.sync.Configuration;
import com.spisoft.sync.Log;
import com.spisoft.sync.R;

public class AccountListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private View mAddButton;
    private ListView mListView;
    private AccountAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);
        mAddButton = findViewById(R.id.addButton);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(AccountListActivity.this, AccountTypeActivity.class));
            }
        });
        mListView = findViewById(R.id.account_list);
        mListView.setOnItemClickListener(this);
        refreshCursor();

    }
    private void refreshCursor(){
        Cursor cursor = DBAccountHelper.getInstance(this).getCursor();
        if(cursor == null || cursor.getCount() == 0){

        }
        else {
            mAdapter = new AccountAdapter(this,cursor, 0);
            mListView.setAdapter(mAdapter);
        }

    }

    public void onResume(){
        super.onResume();
        if(mAdapter!=null)
            refreshCursor();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mAdapter.getCursor().moveToPosition(i);
        Configuration.sOnAccountSelectedListener.onAccountSelected((int)l,  mAdapter.getCursor().getInt(mAdapter.getCursor().getColumnIndex(DBAccountHelper.KEY_ACCOUNT_TYPE)));
    }
}
