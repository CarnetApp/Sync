package com.spisoft.sync.wrappers.nextcloud;

import com.owncloud.android.lib.resources.files.RemoteFile;

import java.util.List;

public abstract class NextCloudConnector {
    public static int STATUS_SUCCESS = NextCloudSyncWrapper.STATUS_SUCCESS;
    public static int STATUS_FAILURE = NextCloudSyncWrapper.STATUS_FAILURE;

    abstract int checkConnection();

    public void asyncListFiles(int requestCode, String path){

    }
    abstract List<RemoteFile> syncListFiles () throws Exception;

    public interface AsyncResultListener{
        public void onResult(int requestCode, int status, Object result);
    }



}
