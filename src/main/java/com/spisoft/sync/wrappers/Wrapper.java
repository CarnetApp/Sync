package com.spisoft.sync.wrappers;

import android.app.Activity;
import android.content.Context;

import com.spisoft.sync.synchro.SyncWrapper;

import java.sql.PreparedStatement;

/**
 * Created by alexandre on 15/03/17.
 */

public interface Wrapper {
   // public Wrapper(Context context, Integer accountID);
    //public static boolean isMyAccount(Integer type);

    void listFiles();

    AsyncLister getAsyncLister(String path);

    DBWrapper getDBWrapper();

    SyncWrapper getSyncWrapper(Context context);

    void startAuthorizeActivityForResult(Activity activity);

    public static interface ResultListener{
        public void onResult(int resultCode, Object data);
    }

    //public static String getFriendlyName();

    //public static int getAccountType();
}
