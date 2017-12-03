package com.spisoft.sync.database;

import android.content.Context;
import android.util.Pair;

import com.spisoft.sync.account.DBAccountHelper;
import com.spisoft.sync.utils.FileUtils;
import com.spisoft.sync.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by phoenamandre on 14/05/17.
 */

public class SyncedFolderDBHelper {

    private static SyncedFolderDBHelper sSyncedFolderDBHelper;

    public SyncedFolderDBHelper(Context context) {

    }

    public static SyncedFolderDBHelper getInstance(Context context) {
        if(sSyncedFolderDBHelper==null)
            sSyncedFolderDBHelper = new SyncedFolderDBHelper(context);
        return sSyncedFolderDBHelper;
    }

    /**
     *  if syncPath is "all", syncFolder should be equal to rootfolder
     *   if syncPath parent of a rootFolder, syncPath = rootFolder
     *   if syncPath child of rootFolder, no change
     *
     * @return List<Pair<folderToSync, rootFolder>>
     */
    public List<Pair<String,String>> getLocalSyncedFolders(String syncedPath){
        List<Pair<String,String>> syncedFolders = new ArrayList<>();
        if(Utils.isDebugPackage())
            syncedFolders.add(new Pair("/sdcard/NextcloudTest","/sdcard/NextcloudTest"));
        else
            syncedFolders.add(new Pair("/sdcard/Nextcloud/Documents","/sdcard/Nextcloud/Documents"));
        return syncedFolders;
    }

    /**
     * returns all accounts syncing this folder or a parent
     * @param localPath
     * @return
     */
    public List<DBAccountHelper.Account> getRemoteAccountForSyncedFolder(String localPath){
        List<DBAccountHelper.Account> remoteAccounts = new ArrayList<>();
        remoteAccounts.add(new DBAccountHelper.Account(0,0, "none"));
        return remoteAccounts;
    }
}
