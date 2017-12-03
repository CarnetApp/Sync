package com.spisoft.sync.wrappers.nextcloud;

import android.content.Context;

/**
 * Created by phoenamandre on 14/05/17.
 */

public class NextCloudSyncedFoldersDBHelper {

    private static NextCloudSyncedFoldersDBHelper sNextCloudSyncedFoldersDBHelper;

    public NextCloudSyncedFoldersDBHelper(Context ct) {
    }

    public static NextCloudSyncedFoldersDBHelper getInstance(Context ct){
        if(sNextCloudSyncedFoldersDBHelper == null)
            sNextCloudSyncedFoldersDBHelper = new NextCloudSyncedFoldersDBHelper(ct);
        return sNextCloudSyncedFoldersDBHelper;
    }

    public String getRemoteSyncedPathForLocal(String mRelativeRootPath) {
        return "Documents";
    }
}
