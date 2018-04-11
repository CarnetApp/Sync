package com.spisoft.sync.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import com.spisoft.sync.account.DBAccountHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by phoenamandre on 14/05/17.
 */

public class SyncedFolderDBHelper {

    private static SyncedFolderDBHelper sSyncedFolderDBHelper;
    public static final String KEY_ACCOUNT_ID = "account_id";
    public static final String KEY_PATH = "path";

    public final static String TABLE_NAME = "folder_sync";
    private static final String[] COLUMNS = {
            KEY_ACCOUNT_ID,
            KEY_PATH
    };
    public static final String CREATE_DATABASE = "create table " + TABLE_NAME + "( "
            + KEY_ACCOUNT_ID + " INTEGER,"
            + KEY_PATH + " text not null,"
            +" FOREIGN KEY("+KEY_ACCOUNT_ID+") REFERENCES "+DBAccountHelper.TABLE_NAME+"("+DBAccountHelper.KEY_ACCOUNT_ID+"), "
            + " PRIMARY KEY ("+ KEY_PATH +", "+ KEY_ACCOUNT_ID +"));";
    private final Context mContext;

    public SyncedFolderDBHelper(Context context) {
        mContext = context;
    }

    public static SyncedFolderDBHelper getInstance(Context context) {
        if(sSyncedFolderDBHelper==null)
            sSyncedFolderDBHelper = new SyncedFolderDBHelper(context);
        return sSyncedFolderDBHelper;
    }

    /**
     *  if syncPath is "all", syncFolder should be equal to rootfolder
     *   if syncPath parent of a rootFolder, syncPath = rootFolder
     *   if syncPath child of rootFolder, no change
     *
     * @return List<Pair<folderToSync, rootFolder>>
     */
    public List<Pair<String,String>> getLocalSyncedFolders(String syncedPath){
        List<Pair<String,String>> syncedFolders = new ArrayList<>();
        if(syncedPath.equals("all")){
            Cursor cursor = getCursor(null, null);
            if(cursor.getCount()>0){
                while (cursor.moveToNext()){
                    String path = cursor.getString(cursor.getColumnIndex(KEY_PATH));
                    syncedFolders.add(new Pair<String, String>(path,path));
                }
            }
        }
        return syncedFolders;
    }

    public Cursor getCursor(String selection, String[]args) {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            Cursor cursor = sqLiteDatabase.query(TABLE_NAME, COLUMNS, selection, args, null, null, null);
            return cursor;
        }
    }

    /**
     * returns all accounts syncing this folder or a parent
     * @param localPath
     * @return
     */
    public List<DBAccountHelper.Account> getRemoteAccountForSyncedFolder(String localPath){
        List<DBAccountHelper.Account> remoteAccounts = new ArrayList<>();
        Cursor cursor = getCursor(KEY_PATH+" = ?", new String[]{localPath});
        if(cursor.getCount()>0){
            while (cursor.moveToNext()){
                int accountId = cursor.getInt(cursor.getColumnIndex(KEY_ACCOUNT_ID));
                DBAccountHelper.Account account = DBAccountHelper.getInstance(mContext).getAccount(accountId);
                if(account!=null)
                    remoteAccounts.add(account);
            }
        }
        return remoteAccounts;
    }

    public void addPathToSync(int accountId, String path){
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(KEY_ACCOUNT_ID, accountId);
            initialValues.put(KEY_PATH, path);
            sqLiteDatabase.insertWithOnConflict(TABLE_NAME, null, initialValues,SQLiteDatabase.CONFLICT_IGNORE);
            database.close();
        }
    }

    public void removePathtoSync(int accountId, String localPath) {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            sqLiteDatabase.delete(TABLE_NAME,KEY_PATH + " = ? AND "+KEY_ACCOUNT_ID+" = ? ",new String[]{localPath, accountId+""});
            sqLiteDatabase.close();
        }
    }
}
