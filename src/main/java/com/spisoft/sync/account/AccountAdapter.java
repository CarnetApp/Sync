package com.spisoft.sync.account;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.spisoft.sync.R;
import com.spisoft.sync.wrappers.Wrapper;
import com.spisoft.sync.wrappers.WrapperFactory;

import java.util.HashMap;

/**
 * Created by phoenamandre on 03/12/17.
 */

public class AccountAdapter extends CursorAdapter {
    private final HashMap<Integer, Drawable> mWrapperDrawables;

    public AccountAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mWrapperDrawables = new HashMap<Integer, Drawable>();
        for(Wrapper wrapper : WrapperFactory.getWrapperList(context)){
            mWrapperDrawables.put(wrapper.getAccountType(), wrapper.getIcon());
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.account_item, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((ImageView)view.findViewById(R.id.imageView)).setImageDrawable(mWrapperDrawables.get(cursor.getInt(cursor.getColumnIndex(DBAccountHelper.KEY_ACCOUNT_TYPE))));
        ((TextView)view.findViewById(R.id.friendly_name)).setText(cursor.getString(cursor.getColumnIndex(DBAccountHelper.KEY_FRIENDLY_NAME)));
    }
}
