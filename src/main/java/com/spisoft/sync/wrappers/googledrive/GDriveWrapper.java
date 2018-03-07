package com.spisoft.sync.wrappers.googledrive;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveResourceClient;
import com.spisoft.sync.synchro.SyncWrapper;
import com.spisoft.sync.wrappers.AsyncLister;
import com.spisoft.sync.wrappers.DBWrapper;
import com.spisoft.sync.wrappers.Wrapper;

/**
 * Created by phoenamandre on 04/03/18.
 */

public class GDriveWrapper extends Wrapper {
    private DriveResourceClient mDriveResourceClient;

    public GDriveWrapper(Context context, Integer accountID) {
        super(context, accountID);
    }

    public GDriveWrapper(Context context) {
        super(context);
    }

    public DriveResourceClient getDriveResourceClient(){
        return mDriveResourceClient;
    }

    @Override
    public void listFiles() {

    }
    public void init(Context context, Integer accountID){
        super.init(context, accountID);
        setCredentials(accountID);
    }

    private void setCredentials(Integer accountID) {
        mDriveResourceClient =
                Drive.getDriveResourceClient(mContext, GoogleSignIn.getLastSignedInAccount(mContext));
    }


    @Override
    public AsyncLister getAsyncLister(String folderId) {
        return new GDriveAsyncLister(mContext, mAccountId,folderId,  this);
    }

    @Override
    public DBWrapper getDBWrapper() {
        return null;
    }

    @Override
    public SyncWrapper getSyncWrapper(Context context) {
        return new DriveSyncWrapper(context,mAccountId);
    }

    @Override
    public void startAuthorizeActivityForResult(Activity activity, int requestCode) {

    }

    @Override
    public String getRemoteSyncDir(String rootPath) {
        return null;
    }

    @Override
    public int getAccountType() {
        return 0;
    }

    @Override
    public Drawable getIcon() {
        return null;
    }

    @Override
    public String getFriendlyName() {
        return null;
    }

    @Override
    protected boolean internalAddFolderSync(String local, String remote) {
        return false;
    }

    public int getAccountId() {
        return mAccountId;
    }
}
