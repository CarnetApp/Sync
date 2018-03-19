package com.spisoft.sync.wrappers.googledrive;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.spisoft.sync.account.DBAccountHelper;
import com.spisoft.sync.database.SyncDatabase;

/**
 * Created by phoenamandre on 14/05/17.
 */

public class DriveSyncedFoldersDBHelper {

    private static DriveSyncedFoldersDBHelper sDriveSyncedFoldersDBHelper;
    private static final String TABLE_NAME = "DriveSyncFolder";
    public static final String KEY_ACCOUNT_ID = "account_id";
    public static final String KEY_LOCAL_PATH = "local_path";
    public static final String KEY_REMOTE_PATH = "remote_path";

    private static final String[] COLUMNS = {
            KEY_ACCOUNT_ID,
            KEY_REMOTE_PATH,
            KEY_LOCAL_PATH,
    };
    public static final String CREATE_DATABASE = "create table " + TABLE_NAME + "( "
            + KEY_ACCOUNT_ID + " long,"
            + KEY_REMOTE_PATH + " text not null, "
            + KEY_LOCAL_PATH + " text not null, "
            +" FOREIGN KEY("+KEY_ACCOUNT_ID+") REFERENCES "+ DBAccountHelper.TABLE_NAME+"("+DBAccountHelper.KEY_ACCOUNT_ID+"), "
            + "PRIMARY KEY ("+ KEY_ACCOUNT_ID +", "+ KEY_LOCAL_PATH +"));";
    private final Context mContext;

    public DriveSyncedFoldersDBHelper(Context ct) {
        mContext = ct.getApplicationContext();
    }

    public static DriveSyncedFoldersDBHelper getInstance(Context ct){
        if(sDriveSyncedFoldersDBHelper == null)
            sDriveSyncedFoldersDBHelper = new DriveSyncedFoldersDBHelper(ct);
        return sDriveSyncedFoldersDBHelper;
    }

    public void addOrReplaceSyncedFolder(long accountID, String localPath, String remotePath) {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(KEY_ACCOUNT_ID, accountID);
            initialValues.put(KEY_REMOTE_PATH, remotePath);
            initialValues.put(KEY_LOCAL_PATH, localPath);
            sqLiteDatabase.insertWithOnConflict(TABLE_NAME, null, initialValues,SQLiteDatabase.CONFLICT_REPLACE);
            database.close();
        }
    }

    public Cursor getCursor(String selection, String[]args) {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            Cursor cursor = sqLiteDatabase.query(TABLE_NAME, COLUMNS, selection, args, null, null, null);
            return cursor;
        }
    }

    public String getRemoteSyncedPathForLocal(long accountID, String localPath) {
      /*      Cursor cursor = getCursor(KEY_ACCOUNT_ID +" = ? AND "+KEY_LOCAL_PATH+" = ? ", new String[]{accountID+"",localPath});
            if(cursor.getCount()>0) {
                cursor.moveToFirst();
                return cursor.getString(cursor.getColumnIndex(KEY_REMOTE_PATH));
            }*/
        return "QuickNote";
    }
}
