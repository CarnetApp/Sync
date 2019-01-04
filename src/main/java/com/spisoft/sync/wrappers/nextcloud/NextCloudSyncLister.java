package com.spisoft.sync.wrappers.nextcloud;

import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.spisoft.sync.wrappers.AsyncLister;
import com.spisoft.sync.wrappers.FileItem;
import com.spisoft.sync.wrappers.ResultCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by phoenamandre on 14/05/17.
 */

public class NextCloudSyncLister {

    private static final String TAG = "NextCloudSyncLister";
    private final OwnCloudClient mClient;

    public NextCloudSyncLister(OwnCloudClient client) {
        mClient = client;
    }

    /**
     * returns null when error
     * @param path
     */
    public List<RemoteFile> retrieveList(String path) throws Exception {
        Log.d(TAG, "retrieveList "+path);

        ReadRemoteFolderOperation refreshOperation = new ReadRemoteFolderOperation((path.equals("/"))? FileUtils.PATH_SEPARATOR:path);
        // root folder
        RemoteOperationResult remoteOperationResult =  refreshOperation.execute(mClient);
        if(remoteOperationResult.isSuccess()){
            List<FileItem> files= new ArrayList<>();
            List<FileItem> folders= new ArrayList<>();
            ArrayList<Object> objects = remoteOperationResult.getData();
            List<RemoteFile> remoteFiles = new ArrayList<>();
            for(Object obj : objects){
                RemoteFile file = (RemoteFile) obj;
                remoteFiles.add(file);
            }
            return remoteFiles;
        }else{
            throw remoteOperationResult.getException();
        }

    }
}
