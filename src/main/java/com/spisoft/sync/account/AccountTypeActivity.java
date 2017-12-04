package com.spisoft.sync.account;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.spisoft.sync.R;
import com.spisoft.sync.wrappers.Wrapper;
import com.spisoft.sync.wrappers.WrapperFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by phoenamandre on 03/12/17.
 */

public class AccountTypeActivity extends AppCompatActivity {

    private Button mAddButton;
    private LinearLayout mRootLayout;
    private LinearLayout mAccountTypeListLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_type);

        mRootLayout = findViewById(R.id.root);
        mAccountTypeListLayout = findViewById(R.id.account_type_list);
        refreshWrapperList();

    }
    private void refreshWrapperList(){
        try {
            for(Class wrapperClass : WrapperFactory.wrappers){
                Method m = null;

                m = wrapperClass.getMethod("getFriendlyName", Context.class);
                m.setAccessible(true);
                String friendlyName = (String) m.invoke(wrapperClass, this);
                m = wrapperClass.getMethod("getIcon", Context.class);
                m.setAccessible(true);
                Drawable img = (Drawable) m.invoke(wrapperClass, this);
                m = wrapperClass.getMethod("getAccountType");
                m.setAccessible(true);
                int accountType = (Integer) m.invoke(wrapperClass);
                createAccountTypeView(friendlyName, img, accountType);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void createAccountTypeView(String friendlyName, Drawable drawable, final int accountType){

        View accoutTypeView = LayoutInflater.from(this).inflate(R.layout.account_item, null);
        accoutTypeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AccountTypeActivity.this);
                builder.setTitle(R.string.name_for_account);
                final EditText editText = new EditText(AccountTypeActivity.this);
                editText.setHint(R.string.nextcloud_hint);
                builder.setView(editText);
                builder.setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        DBAccountHelper.Account account = DBAccountHelper.getInstance(AccountTypeActivity.this)
                                .addOrReplaceAccount(new DBAccountHelper.Account(-1, accountType, editText.getText().toString()));
                        Log.d("accountdebug","account created "+account.accountID);
                        WrapperFactory.getWrapper(AccountTypeActivity.this, accountType, account.accountID).startAuthorizeActivityForResult(AccountTypeActivity.this, 0);
                    }
                }).setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        ((TextView)accoutTypeView.findViewById(R.id.friendly_name)).setText(friendlyName);
        ((ImageView)accoutTypeView.findViewById(R.id.imageView)).setImageDrawable(drawable);
        mAccountTypeListLayout.addView(accoutTypeView);
    }
}

