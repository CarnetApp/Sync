package com.spisoft.sync.wrappers.googledrive;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveResourceClient;
import com.spisoft.sync.R;
import com.spisoft.sync.synchro.SyncWrapper;
import com.spisoft.sync.wrappers.AsyncLister;
import com.spisoft.sync.wrappers.DBWrapper;
import com.spisoft.sync.wrappers.Wrapper;


/**
 * Created by phoenamandre on 04/03/18.
 */

public class GDriveWrapper extends Wrapper {
    public static final int ACCOUNT_TYPE = 2;

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
       // setCredentials(accountID);
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
    public boolean isMyAccount(Integer type) {
        return type == ACCOUNT_TYPE;
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
        return ACCOUNT_TYPE;
    }

    @Override
    public Drawable getIcon() {
        return mContext.getResources().getDrawable(R.drawable.drive);
    }

    @Override
    public String getFriendlyName() {
        return mContext.getString(R.string.google_drive);
    }

    @Override
    protected boolean internalAddFolderSync(String local, String remote) {
        return true;
    }

    @Override
    public boolean internalRemoveSyncDir(String localPath) {
        return true;
    }

    public int getAccountId() {
        return mAccountId;
    }
}
