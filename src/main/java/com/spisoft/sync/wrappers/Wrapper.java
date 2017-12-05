package com.spisoft.sync.wrappers;

import android.app.Activity;
import android.content.Context;

import com.spisoft.sync.synchro.SyncWrapper;

import java.sql.PreparedStatement;

/**
 * Created by alexandre on 15/03/17.
 */

public interface Wrapper {
   // public Wrapper(Context context, Long  accountID);
    //public static boolean isMyAccount(Integer type);

    void listFiles();

    AsyncLister getAsyncLister(String path);

    DBWrapper getDBWrapper();

    SyncWrapper getSyncWrapper(Context context);

    void startAuthorizeActivityForResult(Activity activity, int requestCode);

    String getRemoteSyncDir(String rootPath);

    public static interface ResultListener{
        public void onResult(int resultCode, Object data);
    }

    //public static String getFriendlyName(Context context);

    //public static int getAccountType();

    //public static Drawable getIcon(Context context);
}
