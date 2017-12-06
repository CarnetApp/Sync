package com.spisoft.sync.wrappers.nextcloud;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.spisoft.sync.Log;
import com.spisoft.sync.R;

public class NextCloudAuthorizeActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String EXTRA_ACCOUNT_ID="account_id";
    private View mConnectButton;
    private EditText mPasswordInput;
    private EditText mRemoteInput;
    private EditText mUsernameInput;
    private View mCancelButton;
    private int mAccountId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_cloud_authorize);
        mAccountId = getIntent().getIntExtra(EXTRA_ACCOUNT_ID, -1);
        mConnectButton = findViewById(R.id.connect_button);
        mCancelButton = findViewById(R.id.cancel_button);
        mRemoteInput = findViewById(R.id.input_remote);
        mUsernameInput = findViewById(R.id.input_username);
        mPasswordInput = findViewById(R.id.input_password);

        mConnectButton.setOnClickListener(this);
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
}
