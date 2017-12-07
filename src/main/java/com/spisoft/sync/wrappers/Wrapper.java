package com.spisoft.sync.wrappers;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.spisoft.sync.database.SyncedFolderDBHelper;
import com.spisoft.sync.synchro.SyncWrapper;

import java.sql.PreparedStatement;

/**
 * Created by alexandre on 15/03/17.
 */

public abstract class Wrapper {
    protected  int mAccountId;
    protected  Context mContext;

    public Wrapper(Context context, Integer  accountID){
       init(context, accountID);
    }

    public Wrapper(){
    }

    public void init(Context context, Integer accountId){
        mAccountId = accountId;
        mContext = context;
    }

    public boolean isInitialized(){
        return mContext!=null;
    }

    public boolean isMyAccount(Integer type){ return false; }

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

    public void initDB(SQLiteDatabase db) {
    }

    public static interface ResultListener{
        public void onResult(int resultCode, Object data);
    }

    //public static String getFriendlyName(Context context);

    //public static int getAccountType();

    //public static Drawable getIcon(Context context);
}
