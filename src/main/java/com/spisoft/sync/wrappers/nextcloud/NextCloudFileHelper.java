package com.spisoft.sync.wrappers.nextcloud;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.spisoft.sync.account.DBAccountHelper;
import com.spisoft.sync.database.SyncDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexandre on 27/04/16.
 */
public class NextCloudFileHelper {


    private static NextCloudFileHelper sDBDriveFileHelper;
    private final Context mContext;
    private static final String TABLE_NAME = "DBNextCloudFile";
    private static final String KEY_REMOTE_PATH = "key_remote_path";
    private static final String KEY_ACCOUNT = "account_id";
    private static final String KEY_SYNC_MD5 = "key_sync_md5";
    private static final String KEY_SYNC_LASTMOD = "key_sync_lastmod";
    private static final String KEY_CURRENTLY_DOWNLOADED_ETAG = "key_sync_etag";
    private static final String KEY_VISIT_STATUS = "key_visit_status";
    private static final String KEY_REMOTE_ETAG = "key_online_etag";
    private static final String KEY_REMOTE_MIME_TYPE = "remote_mimetype";

    private static final String[] COLUMNS = {
            KEY_REMOTE_PATH,
            KEY_ACCOUNT,
            KEY_REMOTE_ETAG,
            KEY_SYNC_MD5,
            KEY_SYNC_LASTMOD,
            KEY_CURRENTLY_DOWNLOADED_ETAG,
            KEY_REMOTE_MIME_TYPE,
            KEY_VISIT_STATUS
    };
    public static final java.lang.String CREATE_DATABASE = "create table " + TABLE_NAME + "( "
            + KEY_REMOTE_PATH + " text not null, "
            + KEY_ACCOUNT + " INTEGER,"
            + KEY_SYNC_MD5 + " text, "
            + KEY_SYNC_LASTMOD + " long DEFAULT(-1), "
            + KEY_CURRENTLY_DOWNLOADED_ETAG + " text, "
            + KEY_REMOTE_ETAG + " text,"
            + KEY_REMOTE_MIME_TYPE + " text,"
            + KEY_VISIT_STATUS + " INTEGER DEFAULT(-1), "
            +" FOREIGN KEY("+KEY_ACCOUNT+") REFERENCES "+ DBAccountHelper.TABLE_NAME+"("+DBAccountHelper.KEY_ACCOUNT_ID+"), "
            +"PRIMARY KEY ("+ KEY_REMOTE_PATH +", "+ KEY_ACCOUNT +"));";
    public static final String UPDATE_DB_V1_TO_V2 = "ALTER TABLE "+TABLE_NAME+" ADD "+KEY_SYNC_LASTMOD+" long DEFAULT(-1)";

    public NextCloudFileHelper(Context context){
        mContext = context.getApplicationContext();
    }

    public static NextCloudFileHelper getInstance(Context context) {
        if(sDBDriveFileHelper==null)
            sDBDriveFileHelper = new NextCloudFileHelper(context);
        return sDBDriveFileHelper;
    }

    public DBNextCloudFile getDBDriveFile(int accountID, String remotePath) {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        DBNextCloudFile dbDriveFile = null;
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            Cursor cursor = sqLiteDatabase.query(TABLE_NAME, COLUMNS, KEY_ACCOUNT + "=? AND " + KEY_REMOTE_PATH + "= ?", new String[]{accountID + "", remotePath}, null, null, null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                dbDriveFile = new DBNextCloudFile();
                dbDriveFile.md5 = cursor.getString(cursor.getColumnIndex(KEY_SYNC_MD5));
                dbDriveFile.lastMod = cursor.getLong(cursor.getColumnIndex(KEY_SYNC_LASTMOD));
                dbDriveFile.relativePath = remotePath;
                dbDriveFile.accountID = accountID;
                dbDriveFile.remoteMimeType = cursor.getString(cursor.getColumnIndex(KEY_REMOTE_MIME_TYPE));
                dbDriveFile.currentlyDownloadedOnlineEtag = cursor.getString(cursor.getColumnIndex(KEY_CURRENTLY_DOWNLOADED_ETAG));
                dbDriveFile.onlineEtag = cursor.getString(cursor.getColumnIndex(KEY_REMOTE_ETAG));
                dbDriveFile.visitStatus = cursor.getInt(cursor.getColumnIndex(KEY_VISIT_STATUS));
            }
            cursor.close();
            database.close();
        }
        return dbDriveFile;
    }

    public DBNextCloudFile addOrUpdateDBDriveFile(DBNextCloudFile dbDriveFile) {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(KEY_REMOTE_PATH, dbDriveFile.relativePath);
            initialValues.put(KEY_ACCOUNT, dbDriveFile.accountID);
            if(dbDriveFile.md5!=null)
                initialValues.put(KEY_SYNC_MD5, dbDriveFile.md5);
            initialValues.put(KEY_SYNC_LASTMOD, dbDriveFile.lastMod);
            if(dbDriveFile.remoteMimeType!=null)
            initialValues.put(KEY_REMOTE_MIME_TYPE, dbDriveFile.remoteMimeType);
            if( dbDriveFile.currentlyDownloadedOnlineEtag!=null)
            initialValues.put(KEY_CURRENTLY_DOWNLOADED_ETAG, dbDriveFile.currentlyDownloadedOnlineEtag);
            if( dbDriveFile.onlineEtag!=null)
            initialValues.put(KEY_REMOTE_ETAG, dbDriveFile.onlineEtag);
            if(dbDriveFile.visitStatus!=-1)
                initialValues.put(KEY_VISIT_STATUS, dbDriveFile.visitStatus);
            long id = sqLiteDatabase.insertWithOnConflict(TABLE_NAME, null, initialValues,SQLiteDatabase.CONFLICT_IGNORE);
            if(id == -1){
                sqLiteDatabase.update(TABLE_NAME, initialValues,KEY_REMOTE_PATH+" = ? AND "+KEY_ACCOUNT+" = ?", new String[]{dbDriveFile.relativePath, ""+dbDriveFile.accountID});
            }
            database.close();
        }
        return dbDriveFile;
    }

    public void delete(DBNextCloudFile dbDriveFile){
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            sqLiteDatabase.delete(TABLE_NAME, KEY_ACCOUNT + "=? AND " + KEY_REMOTE_PATH + "= ?", new String[]{dbDriveFile.accountID + "", dbDriveFile.relativePath});
            database.close();
        }
    }
    public void delete(long accountID){
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            sqLiteDatabase.delete(TABLE_NAME, KEY_ACCOUNT + "=?", new String[]{accountID + ""});
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

    /**
     *
     *
     * @param accountID
     * @param remotePath without slash at en or beginning
     * @return
     */
    public List<DBNextCloudFile> getChildrenTree(int accountID,String remotePath) {
        SyncDatabase database = SyncDatabase.getInstance(mContext);
        List<DBNextCloudFile> dbDriveFiles = new ArrayList<>();
        synchronized (database.lock) {
            SQLiteDatabase sqLiteDatabase = database.open();
            Cursor cursor = sqLiteDatabase.query(TABLE_NAME, COLUMNS, KEY_ACCOUNT + "=? AND " + KEY_REMOTE_PATH + " LIKE ?", new String[]{accountID + "", remotePath+"/%"}, null, null, null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do{
                    DBNextCloudFile dbDriveFile = new DBNextCloudFile();
                    dbDriveFile.md5 = cursor.getString(cursor.getColumnIndex(KEY_SYNC_MD5));
                    dbDriveFile.relativePath = cursor.getString(cursor.getColumnIndex(KEY_REMOTE_PATH));
                    dbDriveFile.accountID = accountID;
                    dbDriveFile.remoteMimeType = cursor.getString(cursor.getColumnIndex(KEY_REMOTE_MIME_TYPE));
                    dbDriveFile.lastMod = cursor.getLong(cursor.getColumnIndex(KEY_SYNC_LASTMOD));
                    dbDriveFile.currentlyDownloadedOnlineEtag = cursor.getString(cursor.getColumnIndex(KEY_CURRENTLY_DOWNLOADED_ETAG));
                    dbDriveFile.onlineEtag = cursor.getString(cursor.getColumnIndex(KEY_REMOTE_ETAG));
                    dbDriveFile.visitStatus = cursor.getInt(cursor.getColumnIndex(KEY_VISIT_STATUS));
                    dbDriveFiles.add(dbDriveFile);
                    Log.d("NextCloudWrapper", "adding "+dbDriveFile.relativePath);
                }while (cursor.moveToNext());

            }
            cursor.close();
            database.close();
        }
        return dbDriveFiles;
    }

    public static class DBNextCloudFile{


        public static class VisitStatus{
            public final static int STATUS_OK = 0;
            public final static int STATUS_FAILURE = 1;
        }
        public String remoteMimeType;
        public String currentlyDownloadedOnlineEtag;
        public DBNextCloudFile(String relativePath) {
            this.relativePath = relativePath;
        }
        public DBNextCloudFile(){}
        public String md5;
        public long lastMod = -1;
        public String relativePath;
        public String onlineEtag;
        public int accountID;
        public int visitStatus;
    }

}
