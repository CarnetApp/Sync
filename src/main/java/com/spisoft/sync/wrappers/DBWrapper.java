package com.spisoft.sync.wrappers;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Created by alexandre on 16/03/17.
 */

public interface DBWrapper {
    Cursor executeQuery(Uri uri, String[] strings, String s, String[] strings1, String s1);

    Uri insert(Uri uri, ContentValues contentValues);

    int delete(Uri uri, String s, String[] strings);

    int update(Uri uri, ContentValues contentValues, String s, String[] strings);

    String getType(Uri uri);
}
