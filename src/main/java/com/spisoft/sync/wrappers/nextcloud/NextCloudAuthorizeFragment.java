package com.spisoft.sync.wrappers.nextcloud;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.spisoft.sync.Log;
import com.spisoft.sync.R;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class NextCloudAuthorizeFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    public static final String EXTRA_ACCOUNT_ID="account_id";
    private View mConnectButton;
    private EditText mPasswordInput;
    private EditText mRemoteInput;
    private EditText mUsernameInput;
    private View mCancelButton;
    private int mAccountId;
    private Spinner mServerSpinner;
    private View mRemoteInputContainer;
    private OnConnectClickListener mOnConnectClickListener;
    private View mNewAccountButton;
    private View mLoadingView;
    private CheckCredentialsTask mCheckCredentialsTask;
    private View mErrorTV;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_next_cloud_authorize, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(getArguments() != null)
        mAccountId = getArguments().getInt(EXTRA_ACCOUNT_ID, -1);
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
    }

    public void setInstance(String instance) {
        mRemoteInput.setText(instance);
    }

    public interface OnConnectClickListener{
        void onConnectClick(String remote, String username, String password);
    }
    public void setOnConnectClickListener(OnConnectClickListener onConnectClickListener){
        mOnConnectClickListener = onConnectClickListener;
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
                if(mOnConnectClickListener != null){
                    mOnConnectClickListener.onConnectClick(mRemoteInput.getText().toString(),
                            mUsernameInput.getText().toString(), mPasswordInput.getText().toString());

                }else {
                    NextCloudCredentialsHelper.Credentials cred = NextCloudCredentialsHelper.getInstance(getActivity()).addOrReplaceAccount(new NextCloudCredentialsHelper.Credentials(-1, mAccountId, mRemoteInput.getText().toString(),
                            mUsernameInput.getText().toString(), mPasswordInput.getText().toString()));
                    Log.d("accounddebug", "added " + cred.id);
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                }
            }
            else{
                mErrorTV.setVisibility(View.VISIBLE);
            }
        }


    }
}
