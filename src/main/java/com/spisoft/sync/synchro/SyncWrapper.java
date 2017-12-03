package com.spisoft.sync.synchro;

import android.content.Context;

import java.io.File;
import java.io.StringReader;

/**
 * Created by alexandre on 25/04/16.
 */
public abstract class SyncWrapper {
    public static final int ERROR = -1;
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_FAILURE = 1;
    public static final int STATUS_PENDING = 2;
    protected final Context mContext;
    protected final long mAccountID;

    public abstract int loadDistantFiles();
    public SyncWrapper(Context ct, long accountID){
        mAccountID = accountID;
        mContext = ct;
    }

    public abstract int onFile(File file, String md5);

    public abstract int endOfSync();

    public abstract int connect();

    public abstract int loadRootFolder();

    public abstract void setCurrentlySyncedDir(String rootPath);

    public abstract int onFolder(File file, boolean secondPathWithFolderEmpty);

    public abstract void setLocalRootFolder(String rootFolder);

    /*need to be created in static */
    //public abstract boolean isMyAccount(int accountType);
}
