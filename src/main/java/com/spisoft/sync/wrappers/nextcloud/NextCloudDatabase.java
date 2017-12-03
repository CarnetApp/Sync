package com.spisoft.sync.wrappers.nextcloud;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by alexandre on 27/04/16.
 */
public class NextCloudDatabase {
    public static final String DATABASE_NAME = "DBGDrive";
    public static final int DATABASE_VERSION = 1;
    public static NextCloudDatabase sGDriveDatabase=null;
    private final Context mContext;
    public static Object lock = new Object();
    private DatabaseHelper mDatabaseHelper;

    public NextCloudDatabase(Context context) {
        mContext = context;
    }

    public SQLiteDatabase open(){
        if(mDatabaseHelper == null)
            mDatabaseHelper = new DatabaseHelper(mContext);
        return mDatabaseHelper.getWritableDatabase();
    }

    public void close(){
        mDatabaseHelper.close();
    }
    public static NextCloudDatabase getInstance(Context context){
        if(sGDriveDatabase==null)
            sGDriveDatabase = new NextCloudDatabase(context);
        return sGDriveDatabase;
    }



    private class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, NextCloudDatabase.DATABASE_NAME, null, NextCloudDatabase.DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // This method is only called once when the database is created for the first time
            db.execSQL(NextCloudFileHelper.CREATE_DATABASE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
}
