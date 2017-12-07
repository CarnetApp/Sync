package com.spisoft.sync.wrappers;

import android.app.Activity;
import android.content.Context;

import com.spisoft.sync.database.SyncedFolderDBHelper;
import com.spisoft.sync.synchro.SyncWrapper;

import java.sql.PreparedStatement;

/**
 * Created by alexandre on 15/03/17.
 */

public abstract class Wrapper {
    protected final int mAccountId;
    protected final Context mContext;

    public Wrapper(Context context, Integer  accountID){
        mAccountId = accountID;
        mContext = context;
    }
    //public static boolean isMyAccount(Integer type);

    public abstract void listFiles();

    public abstract AsyncLister getAsyncLister(String path);

    public abstract DBWrapper getDBWrapper();

    public abstract SyncWrapper getSyncWrapper(Context context);

    public abstract void startAuthorizeActivityForResult(Activity activity, int requestCode);

    public abstract String getRemoteSyncDir(String rootPath);

    public final boolean addFolderSync(String local, String remote){
        if(!internalAddFolderSync(local, remote))
            return false;
        else{
            SyncedFolderDBHelper.getInstance(mContext).addPathToSync(mAccountId, local);
            return true;
        }
    }

    protected abstract boolean internalAddFolderSync(String local, String remote);

    public static interface ResultListener{
        public void onResult(int resultCode, Object data);
    }

    //public static String getFriendlyName(Context context);

    //public static int getAccountType();

    //public static Drawable getIcon(Context context);
}
