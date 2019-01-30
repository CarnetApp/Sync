package com.spisoft.sync.wrappers.nextcloud;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.gson.GsonBuilder;
import com.nextcloud.android.sso.AccountImporter;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.helper.VersionCheckHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;
import com.nextcloud.android.sso.ui.UiExceptionManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.spisoft.sync.R;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class    NextCloudAuthorizeFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    public static final String EXTRA_ACCOUNT_ID="account_id";
    private static final int NEXTCLOUD_APP_REQUIRED_VERSION = 30040291;
    private View mConnectButton;
    private EditText mPasswordInput;
    private EditText mRemoteInput;
    private EditText mUsernameInput;
    private View mCancelButton;
    private int mAccountId = -1;
    private Spinner mServerSpinner;
    private View mRemoteInputContainer;
    private OnConnectedListener mOnConnectedListener;
    private View mNewAccountButton;
    private View mLoadingView;
    private CheckCredentialsTask mCheckCredentialsTask;
    private View mErrorTV;
    private View mChooseConnectionView;
    private View mFormView;
    private View mSwitchToFormButton;
    private View mConnectWithNCAppButton;
    private NextCloudCredentialsHelper.Credentials mCredentials;

    private void openAccountChooser() {
        try {
            AccountImporter.pickNewAccount(this);
        } catch (NextcloudFilesAppNotInstalledException e) {
            UiExceptionManager.showDialogForException(getActivity(), e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        AccountImporter.onActivityResult(requestCode, resultCode, data, this, new AccountImporter.IAccountAccessGranted() {
            @Override
            public void accountAccessGranted(SingleSignOnAccount account) {
                // As this library supports multiple accounts we created some helper methods if you only want to use one.
                // The following line stores the selected account as the "default" account which can be queried by using
                // the SingleAccountHelper.getCurrentSingleSignOnAccount(context) method
                SingleAccountHelper.setCurrentAccount(getActivity(), account.name);

                // Get the "default" account
                SingleSignOnAccount ssoAccount = null;
                try {
                    ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(getActivity());
                    NextcloudAPI nextcloudAPI = new NextcloudAPI(getActivity(), ssoAccount, new GsonBuilder().create(), callback);
                } catch (NextcloudFilesAppAccountNotFoundException e) {
                    e.printStackTrace();
                } catch (NoCurrentAccountSelectedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    NextcloudAPI.ApiConnectedListener callback = new NextcloudAPI.ApiConnectedListener() {
        @Override
        public void onConnected() {
            NextCloudAuthorizeFragment.this.onConnected(true);
        }

        @Override
        public void onError(Exception ex) {
            // TODO handle errors
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_next_cloud_authorize, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(getArguments() != null)
            mAccountId = getArguments().getInt(EXTRA_ACCOUNT_ID, -1);
        else if (getActivity() != null){
            mAccountId = getActivity().getIntent().getIntExtra(NextCloudAuthorizeActivity.EXTRA_ACCOUNT_ID, -1);
        }
        mErrorTV = view.findViewById(R.id.error);
        mConnectButton = view.findViewById(R.id.connect_button);
        mServerSpinner = view.findViewById(R.id.server_spinner);
        mServerSpinner.setVisibility(View.VISIBLE);
        mCancelButton = view.findViewById(R.id.cancel_button);
        mNewAccountButton = view.findViewById(R.id.new_account);
        mRemoteInput = view.findViewById(R.id.input_remote);
        mRemoteInputContainer = view.findViewById(R.id.input_remote_layout);
        mUsernameInput = view.findViewById(R.id.input_username);
        mPasswordInput = view.findViewById(R.id.input_password);
        mConnectButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        mNewAccountButton.setOnClickListener(this);
        mServerSpinner.setOnItemSelectedListener(this);
        mLoadingView = view.findViewById(R.id.loading);
        mFormView = view.findViewById(R.id.credential_form);
        mChooseConnectionView = view.findViewById(R.id.login_choose);
        mSwitchToFormButton = view.findViewById(R.id.switch_to_form_button);
        mSwitchToFormButton.setOnClickListener(this);
        mConnectWithNCAppButton = view.findViewById(R.id.connect_with_nc_app_button);
        mConnectWithNCAppButton.setOnClickListener(this);

        if(mAccountId >= 0){
            mCredentials = NextCloudCredentialsHelper.getInstance(getActivity()).getCredentials(mAccountId);
            if(mCredentials!=null) {
                mRemoteInput.setText(mCredentials.remote);
                mRemoteInput.setEnabled(false);
                mUsernameInput.setText(mCredentials.username);
                mUsernameInput.setEnabled(false);
                mPasswordInput.setText(mCredentials.password);
                switchToForm();
            }
        }
        //openAccountChooser();
    }

    public void switchToForm(){
        mFormView.setVisibility(View.VISIBLE);
        mChooseConnectionView.setVisibility(View.GONE);
    }

    public void setInstance(String instance) {
        switchToForm();
        mRemoteInput.setText(instance);
    }

    public interface OnConnectedListener {
        void onConnected(boolean singleSignOn, String remote, String username, String password);
    }
    public void setOnConnectClickListener(OnConnectedListener onConnectedListener){
        mOnConnectedListener = onConnectedListener;
    }
    @Override
    public void onClick(View view) {
        if(view == mConnectButton) {
            if (!mRemoteInput.getText().toString().isEmpty() && !mUsernameInput.getText().toString().isEmpty() && !mPasswordInput.getText().toString().isEmpty()) {
                if(!mRemoteInput.getText().toString().startsWith("http"))
                    mRemoteInput.setText("https://"+mRemoteInput.getText().toString());
                mLoadingView.setVisibility(View.VISIBLE);
                mCheckCredentialsTask = new CheckCredentialsTask();
                mCheckCredentialsTask.execute();
            }
        }
        else if(view == mNewAccountButton){
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,Uri.parse((mRemoteInput.getText().toString().startsWith("http")?"":"https://")+mRemoteInput.getText().toString()+"/index.php/apps/registration/"));
            startActivity(browserIntent);
        } else if(view == mSwitchToFormButton){
            switchToForm();
        } else if(view == mConnectWithNCAppButton){
            int eMessage = -1;
            try {
                if(VersionCheckHelper.getNextcloudFilesVersionCode(getActivity())< NEXTCLOUD_APP_REQUIRED_VERSION){
                    eMessage = R.string.nextcloud_app_version_needed;
                }
            } catch (PackageManager.NameNotFoundException e) {
                eMessage = R.string.nextcloud_app_needed;
            }
            if(eMessage == -1)
                openAccountChooser();
            else{
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(eMessage);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.nextcloud.client")));
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
            }
        }
        else{
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
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
    private void onConnected(boolean singleSignOn){
        if(mOnConnectedListener != null){
            mOnConnectedListener.onConnected(singleSignOn, mRemoteInput.getText().toString(),
                    mUsernameInput.getText().toString(), mPasswordInput.getText().toString());

        }else {
            String password =  mPasswordInput.getText().toString();
            NextCloudCredentialsHelper.Credentials cred;
            if(mCredentials!=null) {
                mCredentials.password = password;
                cred = mCredentials;
            }
            else {
                cred = new NextCloudCredentialsHelper.Credentials(-1, mAccountId, mRemoteInput.getText().toString(),
                        mUsernameInput.getText().toString(), password);
            }
            cred = NextCloudCredentialsHelper.getInstance(getActivity()).addOrReplaceAccount(cred);
            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        }
    }

    public class CheckCredentialsTask extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(
                    Uri.parse(mRemoteInput.getText().toString()),
                    getContext(),
                    // Activity or Service context
                    true);
            client.setCredentials(
                    OwnCloudCredentialsFactory.newBasicCredentials(mUsernameInput.getText().toString(), mPasswordInput.getText().toString())
            );
            ReadRemoteFolderOperation refreshOperation = new ReadRemoteFolderOperation(FileUtils.PATH_SEPARATOR);
            RemoteOperationResult remoteOperationResult = refreshOperation.execute(client);
            if(!remoteOperationResult.isSuccess()){
                if(remoteOperationResult.getException() instanceof CertificateCombinedException){
                    try {
                        NetworkUtils.addCertToKnownServersStore(((CertificateCombinedException)remoteOperationResult.getException()).getServerCertificate(), getContext());
                        refreshOperation = new ReadRemoteFolderOperation(FileUtils.PATH_SEPARATOR);
                        remoteOperationResult = refreshOperation.execute(client);
                    } catch (KeyStoreException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (CertificateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return remoteOperationResult.isSuccess();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mLoadingView.setVisibility(View.GONE);
            if(result){
                onConnected(false);
            }
            else{
                mErrorTV.setVisibility(View.VISIBLE);
            }
        }


    }
}
