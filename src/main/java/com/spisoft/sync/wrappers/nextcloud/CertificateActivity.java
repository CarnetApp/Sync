package com.spisoft.sync.wrappers.nextcloud;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.spisoft.sync.R;
import com.spisoft.sync.synchro.SynchroService;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class CertificateActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.sync_error);
        builder.setMessage(R.string.sync_ssl_message);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    NetworkUtils.addCertToKnownServersStore(NextCloudSyncWrapper.cert, getApplicationContext());
                    getApplicationContext().startService(new Intent(getApplicationContext(), SynchroService.class));
                    finish();
                } catch (KeyStoreException e1) {
                    e1.printStackTrace();
                } catch (NoSuchAlgorithmException e1) {
                    e1.printStackTrace();
                } catch (CertificateException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("refuse_certificate", true).apply();
                finish();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
