package com.spisoft.sync.account;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.spisoft.sync.R;

/**
 * Created by phoenamandre on 03/12/17.
 */

public class AccountAdapter extends CursorAdapter {
    public AccountAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.account_item, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((TextView)view.findViewById(R.id.friendly_name)).setText(cursor.getString(cursor.getColumnIndex(DBAccountHelper.KEY_FRIENDLY_NAME)));
    }
}
