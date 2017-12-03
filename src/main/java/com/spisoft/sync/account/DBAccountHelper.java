package com.spisoft.sync.account;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.CursorAdapter;

import com.spisoft.sync.database.SyncDatabase;

import java.io.Serializable;

/**
 * Created by alexandre on 27/04/16.
 */
public class  DBAccountHelper {



    private static DBAccountHelper sDBDriveFileHelper;
    private final Context mContext;
    private static final String TABLE_NAME = "Account";
    public static final String KEY_ACCOUNT_ID = "account_id";
    public static final String KEY_ACCOUNT_TYPE = "account_type";
    public static final String KEY_FRIENDLY_NAME = "friendly_name";

    private static final String[] COLUMNS = {
            KEY_ACCOUNT_ID,
            KEY_ACCOUNT_TYPE,
            KEY_FRIENDLY_NAME
    };
    public static final String CREATE_DATABASE = "create table " + TABLE_NAME + "( "
            + KEY_ACCOUNT_ID + " LONG PRIMARY KEY,"
            + KEY_FRIENDLY_NAME + " text not null, "
            + KEY_ACCOUNT_TYPE + " integer);";
    public DBAccountHelper(Context context){
        mContext = context.getApplicationContext();
    }

    public static DBAccountHelper getInstance(Context context) {
        if(sDBDriveFileHelper==null)
            sDBDriveFileHelper = new DBAccountHelper(context);
        return sDBDriveFileHelper;
    }

    public Account addOrReplaceAccount(Account account) {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(KEY_ACCOUNT_TYPE, account.accountType);
            initialValues.put(KEY_ACCOUNT_ID, account.accountID);
            initialValues.put(KEY_FRIENDLY_NAME, account.friendlyName);
            long id = sqLiteDatabase.insertWithOnConflict(TABLE_NAME, null, initialValues,SQLiteDatabase.CONFLICT_REPLACE);
            account.accountID = (int) id;
            database.close();
        }
        return account;
    }

    public void delete(Account account){
        delete(account.accountID);
    }
    public void delete(int accountID){
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

    public Account getAccount(int accountId) {
        return new Account(accountId,0, "none");
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

    public static class Account implements Serializable{
        public Account(int accountID, int accountType, String friendlyName) {
            this.accountID = accountID;
            this.accountType = accountType;
            this.friendlyName = friendlyName;
        }
        public Account(){

        }

        public int accountID;
        public int accountType;
        public String friendlyName;
    }

}
