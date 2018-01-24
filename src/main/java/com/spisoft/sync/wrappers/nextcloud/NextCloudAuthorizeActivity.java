package com.spisoft.sync.wrappers.nextcloud;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.spisoft.sync.Log;
import com.spisoft.sync.R;

public class NextCloudAuthorizeActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    public static final String EXTRA_ACCOUNT_ID="account_id";
    private View mConnectButton;
    private EditText mPasswordInput;
    private EditText mRemoteInput;
    private EditText mUsernameInput;
    private View mCancelButton;
    private int mAccountId;
    private Spinner mServerSpinner;
    private View mRemoteInputContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_cloud_authorize);
        mAccountId = getIntent().getIntExtra(EXTRA_ACCOUNT_ID, -1);
        mConnectButton = findViewById(R.id.connect_button);
        mServerSpinner = findViewById(R.id.server_spinner);
        mCancelButton = findViewById(R.id.cancel_button);
        mRemoteInput = findViewById(R.id.input_remote);
        mRemoteInputContainer = findViewById(R.id.input_remote_layout);
        mUsernameInput = findViewById(R.id.input_username);
        mPasswordInput = findViewById(R.id.input_password);
        mConnectButton.setOnClickListener(this);
        mServerSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onClick(View view) {
        if(view == mConnectButton) {
            if (!mRemoteInput.getText().toString().isEmpty() && !mUsernameInput.getText().toString().isEmpty() && !mPasswordInput.getText().toString().isEmpty()) {
                NextCloudCredentialsHelper.Credentials cred = NextCloudCredentialsHelper.getInstance(this).addOrReplaceAccount(new NextCloudCredentialsHelper.Credentials(-1, mAccountId, mRemoteInput.getText().toString(),
                        mUsernameInput.getText().toString(), mPasswordInput.getText().toString()));
                Log.d("accounddebug", "added "+cred.id);
                setResult(RESULT_OK);
                finish();
            }
        }
        else{
            setResult(RESULT_CANCELED);
            finish();
        }
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String [] entries = getResources().getStringArray(R.array.nextcloud_array);
        String [] values = getResources().getStringArray(R.array.nextcloud_values);
        String [] full =  getResources().getStringArray(R.array.nextcloud_full_instances);
        boolean isFull = false;
        for(String f : full){
            if(entries[i].equals(f)) {
                isFull = true;
                break;
            }
        }
        if(i<values.length){
            mRemoteInputContainer.setVisibility(View.GONE);
            mRemoteInput.setText(values[i]);
        }
        else{
            mRemoteInputContainer.setVisibility(View.VISIBLE);
            mRemoteInput.setText("");
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
