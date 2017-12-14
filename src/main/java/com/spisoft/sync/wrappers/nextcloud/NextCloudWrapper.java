package com.spisoft.sync.wrappers.nextcloud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
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

public class NextCloudWrapper extends Wrapper implements OnRemoteOperationListener {
    private static final String TAG = "NextCloudWrapper";
    private final Handler mAsyncHandler;
    private final HandlerThread mHandlerThread;
    private Object syncLock = new Object();
    private OwnCloudClient mClient;
    public static final int ACCOUNT_TYPE = 1;
    public NextCloudWrapper(Context context, Integer accountID){
        super(context, accountID);
        Log.d("accounddebug","open "+accountID);
        mHandlerThread = new HandlerThread("MyHandlerThread");
        mHandlerThread.start();
        mAsyncHandler = new Handler(mHandlerThread.getLooper());


    }

    public void init(Context context, Integer accountID){
        super.init(context, accountID);
        setCredentials(accountID);
    }

    public NextCloudWrapper(Context context){
        super(context);
        mHandlerThread = new HandlerThread("MyHandlerThread");
        mHandlerThread.start();
        mAsyncHandler = new Handler(mHandlerThread.getLooper());
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


    public boolean isMyAccount(Integer type) {
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
    protected boolean internalAddFolderSync(String local, String remote) {
        NextCloudSyncedFoldersDBHelper.getInstance(mContext).addOrReplaceSyncedFolder(mAccountId, local, remote);
        return true;
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

    public String getFriendlyName(){
        return "NextCloud";
    }

    public int getAccountType(){
        return ACCOUNT_TYPE;
    }

    public Drawable getIcon(){

        return mContext.getResources().getDrawable(R.drawable.nextcloud_small);
    }

    public void initDB(SQLiteDatabase db) {
        db.execSQL(NextCloudFileHelper.CREATE_DATABASE);
        db.execSQL(NextCloudSyncedFoldersDBHelper.CREATE_DATABASE);
        db.execSQL(NextCloudCredentialsHelper.CREATE_DATABASE);
    }
}
