package com.spisoft.sync.wrappers.nextcloud;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.spisoft.sync.database.SyncDatabase;

/**
 * Created by phoenamandre on 14/05/17.
 */

public class NextCloudSyncedFoldersDBHelper {

    private static NextCloudSyncedFoldersDBHelper sNextCloudSyncedFoldersDBHelper;
    private static final String TABLE_NAME = "NextCloudSyncFolder";
    public static final String KEY_ACCOUNT_ID = "account_id";
    public static final String KEY_LOCAL_PATH = "local_path";
    public static final String KEY_REMOTE_PATH = "remote_path";

    private static final String[] COLUMNS = {
            KEY_ACCOUNT_ID,
            KEY_REMOTE_PATH,
            KEY_LOCAL_PATH,
    };
    public static final java.lang.String CREATE_DATABASE = "create table " + TABLE_NAME + "( "
            + KEY_ACCOUNT_ID + " long,"
            + KEY_REMOTE_PATH + " text not null, "
            + KEY_LOCAL_PATH + " text not null, "
            + "PRIMARY KEY ("+ KEY_ACCOUNT_ID +", "+ KEY_LOCAL_PATH +"));";
    private final Context mContext;

    public NextCloudSyncedFoldersDBHelper(Context ct) {
        mContext = ct.getApplicationContext();
    }

    public static NextCloudSyncedFoldersDBHelper getInstance(Context ct){
        if(sNextCloudSyncedFoldersDBHelper == null)
            sNextCloudSyncedFoldersDBHelper = new NextCloudSyncedFoldersDBHelper(ct);
        return sNextCloudSyncedFoldersDBHelper;
    }

    public void addOrReplaceSyncedFolder(long accountID, String localPath, String remotePath) {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(KEY_ACCOUNT_ID, accountID);
            initialValues.put(KEY_LOCAL_PATH, localPath);
            initialValues.put(KEY_REMOTE_PATH, remotePath);
            sqLiteDatabase.insertWithOnConflict(TABLE_NAME, null, initialValues,SQLiteDatabase.CONFLICT_REPLACE);
            database.close();
        }
    }

    public String getRemoteSyncedPathForLocal(Long accountID, String localPath) {
        return "Documents";
    }
}
