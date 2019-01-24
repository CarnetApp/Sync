package com.spisoft.sync.wrappers.nextcloud;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


import com.spisoft.sync.account.DBAccountHelper;
import com.spisoft.sync.database.SyncDatabase;

import java.io.Serializable;

/**
 * Created by alexandre on 27/04/16.
 */
public class NextCloudCredentialsHelper {



    private static NextCloudCredentialsHelper sNextCloudCredentialsHelper;
    private final Context mContext;
    private static final String TABLE_NAME = "NextCloudCredentials";
    public static final String KEY_INTERNAL_ID = "_id";
    public static final String KEY_ACCOUNT_ID = "account_id";
    public static final String KEY_REMOTE = "remote";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";

    private static final String[] COLUMNS = {
            KEY_INTERNAL_ID,
            KEY_ACCOUNT_ID,
            KEY_REMOTE,
            KEY_USERNAME,
            KEY_PASSWORD
    };
    public static final String CREATE_DATABASE = "create table " + TABLE_NAME + "( "
            + KEY_INTERNAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_ACCOUNT_ID + " INTEGER, "
            + KEY_REMOTE + " TEXT, "
            + KEY_USERNAME + " TEXT, "
            + KEY_PASSWORD + " TEXT,"
            +" FOREIGN KEY("+KEY_ACCOUNT_ID+") REFERENCES "+ DBAccountHelper.TABLE_NAME+"("+DBAccountHelper.KEY_ACCOUNT_ID+"));";
    public NextCloudCredentialsHelper(Context context){
        mContext = context.getApplicationContext();
    }

    public static NextCloudCredentialsHelper getInstance(Context context) {
        if(sNextCloudCredentialsHelper==null)
            sNextCloudCredentialsHelper = new NextCloudCredentialsHelper(context);
        return sNextCloudCredentialsHelper;
    }

    public Credentials addOrReplaceAccount(Credentials credentials) {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(KEY_ACCOUNT_ID, credentials.accountID);
            if(credentials.id >=0)
                initialValues.put(KEY_INTERNAL_ID, credentials.id);
            initialValues.put(KEY_REMOTE, credentials.remote);
            initialValues.put(KEY_USERNAME, credentials.username);
            initialValues.put(KEY_PASSWORD, credentials.password);
            long id = sqLiteDatabase.insertWithOnConflict(TABLE_NAME, null, initialValues,SQLiteDatabase.CONFLICT_REPLACE);
            credentials.id = (int) id;
            database.close();
        }
        return credentials;
    }

    public void delete(Credentials account){
        delete(account.accountID);
    }
    public void delete(long accountID){
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            sqLiteDatabase.delete(TABLE_NAME, KEY_ACCOUNT_ID + "=?", new String[]{accountID + ""});
            database.close();
        }
    }

    public void deleteAll() {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            sqLiteDatabase.delete(TABLE_NAME, null, null);
            database.close();
        }
    }

    public Credentials getCredentials(int accountId) {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            Cursor cursor = sqLiteDatabase.query(TABLE_NAME, COLUMNS, KEY_ACCOUNT_ID+" = ?", new String[]{accountId+""}, null, null, null);
            if(cursor.getCount()>0){
                database.close();
                cursor.moveToFirst();
                return new Credentials(cursor.getInt(cursor.getColumnIndex(KEY_INTERNAL_ID)),accountId,cursor.getString(cursor.getColumnIndex(KEY_REMOTE)),cursor.getString(cursor.getColumnIndex(KEY_USERNAME)),cursor.getString(cursor.getColumnIndex(KEY_PASSWORD)));
            }
            database.close();
            return null;
        }
    }

    public Cursor getCursor() {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            Cursor cursor = sqLiteDatabase.query(TABLE_NAME, COLUMNS, null, null, null, null, null);
            database.close();
            return cursor;
        }
    }

    public static class Credentials implements Serializable{
        public Credentials(int id, int accountID, String remote, String username, String password) {
            this.id = id;
            this.accountID = accountID;
            this.remote = remote;
            this.username = username;
            this.password = password;
        }
        public Credentials(){

        }
        public int id;
        public int accountID;
        public String remote;
        public String username;
        public String password;
    }

}
