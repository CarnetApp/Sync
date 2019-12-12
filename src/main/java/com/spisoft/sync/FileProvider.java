package com.spisoft.sync;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.Nullable;
import android.util.Log;

import com.spisoft.sync.account.DBAccountHelper;
import com.spisoft.sync.wrappers.Wrapper;
import com.spisoft.sync.wrappers.WrapperFactory;

/**
 * Created by alexandre on 16/03/17.
 */

public class FileProvider extends ContentProvider {
    private static final UriMatcher URI_MATCHER =
            new UriMatcher(UriMatcher.NO_MATCH);
    public static final String AUTHORITY = "spisync.fileprovider";
    private static final int FILES = 200;
    private static final int ACCOUNT_ID = 201;
    private static final String TAG = "FileProvider";

    static {
        URI_MATCHER.addURI(AUTHORITY, "file/*", FILES);
    }
    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        Wrapper wrapper = getWrapper(uri);
        if(wrapper!=null)
            return wrapper.getDBWrapper().executeQuery(uri,  strings,  s,  strings1, s1);
        return null;
    }

    private Wrapper getWrapper(Uri uri){
        int table = URI_MATCHER.match(uri);
        switch (table) {
            case FILES:
                Log.d(TAG,"selecting file table");
                int accountId = Integer.parseInt(uri.getPathSegments().get(1));
                Log.d(TAG,"selecting account "+accountId);
                DBAccountHelper.Account account =DBAccountHelper.getInstance(getContext()).getAccount(accountId);
                if(account!=null){
                    Log.d(TAG,"account type "+account.accountType);

                    Wrapper wrapper = WrapperFactory.getWrapper(getContext(), account.accountType, account.accountID);

                    return wrapper;
                }else
                    Log.d(TAG,"no account found");
                break;
        }
        return null;
    }
    @Nullable
    @Override
    public String getType(Uri uri) {
        Wrapper wrapper = getWrapper(uri);
        if(wrapper!=null)
            return wrapper.getDBWrapper().getType(uri);
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Wrapper wrapper = getWrapper(uri);
        if(wrapper!=null)
            return  wrapper.getDBWrapper().insert(uri, contentValues);
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        Wrapper wrapper = getWrapper(uri);
        if(wrapper!=null)
            return  wrapper.getDBWrapper().delete(uri, s, strings);
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        Wrapper wrapper = getWrapper(uri);
        if(wrapper!=null)
            return  wrapper.getDBWrapper().update(uri, contentValues, s, strings);
        return 0;
    }
}
