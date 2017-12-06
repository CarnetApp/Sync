package com.spisoft.sync.wrappers.nextcloud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.spisoft.sync.R;
import com.spisoft.sync.synchro.SyncWrapper;
import com.spisoft.sync.utils.Utils;
import com.spisoft.sync.wrappers.AsyncLister;
import com.spisoft.sync.wrappers.DBWrapper;
import com.spisoft.sync.wrappers.Wrapper;

import java.util.ArrayList;

/**
 * Created by alexandre on 15/03/17.
 */

public class NextCloudWrapper implements Wrapper, OnRemoteOperationListener {
    private static final String TAG = "NextCloudWrapper";
    private final Handler mAsyncHandler;
    private final HandlerThread mHandlerThread;
    private final int mAccountId;
    private final Context mContext;
    private Object syncLock = new Object();
    private OwnCloudClient mClient;

    public NextCloudWrapper(Context context, Integer accountID){
        mAccountId = accountID;
        Log.d("accounddebug","open "+accountID);
        mContext = context;
        mHandlerThread = new HandlerThread("MyHandlerThread");
        mHandlerThread.start();
        mAsyncHandler = new Handler(mHandlerThread.getLooper());
        setCredentials(accountID);

    }

    private void setCredentials(int accountID) {
        //check whether we have credentials
        NextCloudCredentialsHelper.Credentials credentials = NextCloudCredentialsHelper.getInstance(mContext).getCredentials(accountID);
        if(credentials!=null) {
            mClient = OwnCloudClientFactory.createOwnCloudClient(
                    Uri.parse(credentials.remote),
                    mContext,
                    // Activity or Service context
                    true);
            mClient.setCredentials(
                   OwnCloudCredentialsFactory.newBasicCredentials(credentials.username, credentials.password)
            );

        }
    }


    public static boolean isMyAccount(Integer type) {
        return true;
    }


    @Override
    public void listFiles() {



    }

    @Override
    public AsyncLister getAsyncLister(String path) {
        return new NextCloudAsyncLister(mClient, path, mAccountId);
    }

    public NextCloudSyncLister getSyncLister() {
        return new NextCloudSyncLister(mClient);
    }

    @Override
    public DBWrapper getDBWrapper() {
        return null;
    }

    @Override
    public SyncWrapper getSyncWrapper(Context context) {
        return new NextCloudSyncWrapper(context, mAccountId, this);
    }

    @Override
    public void startAuthorizeActivityForResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, NextCloudAuthorizeActivity.class);
        intent.putExtra(NextCloudAuthorizeActivity.EXTRA_ACCOUNT_ID, mAccountId);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    public String getRemoteSyncDir(String rootPath) {
        return NextCloudSyncedFoldersDBHelper.getInstance(mContext).getRemoteSyncedPathForLocal(mAccountId,rootPath);
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation remoteOperation, RemoteOperationResult remoteOperationResult) {
        if(remoteOperation instanceof ReadRemoteFolderOperation){
            if(remoteOperationResult.isSuccess()){
                Log.d(TAG, "onRemoteOperationFinish success ");
                ArrayList<Object> files = remoteOperationResult.getData();
                for(Object obj : files){
                    RemoteFile file = (RemoteFile) obj;
                    Log.d(TAG,"file "+file.getRemotePath());

                }

            }
        }
    }

    public OwnCloudClient getClient() {
        return mClient;
    }

    public static abstract class NextCloudResultListener implements OnRemoteOperationListener{

        protected final int mRequestCode;

        public NextCloudResultListener (int requestCode){
            mRequestCode = requestCode;
        }

    }

    public static String getFriendlyName(Context context){
        return "NextCloud";
    }

    public static int getAccountType(){
        return 1;
    }

    public static Drawable getIcon(Context context){

        return context.getResources().getDrawable(R.drawable.nextcloud_small);
    }
}
