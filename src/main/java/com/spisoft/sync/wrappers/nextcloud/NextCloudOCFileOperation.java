package com.spisoft.sync.wrappers.nextcloud;

import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;
import com.spisoft.sync.wrappers.FileItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by phoenamandre on 14/05/17.
 */

public class NextCloudOCFileOperation implements NextCloudFileOperation {

    private static final String TAG = "NextCloudOCFileOperation";
    private final OwnCloudClient mClient;
    private final NextCloudWrapper mNextCloudWrapper;

    public NextCloudOCFileOperation(NextCloudWrapper wrapper) {
        mNextCloudWrapper = wrapper;
        mClient = wrapper.getClient();
    }


    @Override
    public boolean download(String remotePath, String to) {
        Log.d(TAG, "download " +remotePath+" to "+to);
        File dest = new File(to);
        File parent = dest.getParentFile();
        parent.mkdirs();
        File tmp = new File(parent, ".donotsync.tmp"+System.currentTimeMillis());
        MyDownloadRemoteFileOperation readRemoteFileOperation = new MyDownloadRemoteFileOperation(remotePath, tmp.getAbsolutePath());
        try {
            readRemoteFileOperation.logHeader(mClient);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OperationCancelledException e) {
            e.printStackTrace();
        }
        RemoteOperationResult result = readRemoteFileOperation.execute(mClient);
        if(tmp.exists()) {
            Log.d(TAG, "tmp.exists");
            if (result.isSuccess()) {
                Log.d(TAG, "result success");
                if(tmp.length()>0){
                    dest.delete();
                    boolean success = tmp.renameTo(dest);
                    Log.d(TAG, "renaming... "+success);
                    if(!success)
                        tmp.delete();
                    return success;
                }
            }
            tmp.delete();
        }
        return false;
    }

    @Override
    public boolean upload(String fromFile, String remotePath) {
        UploadRemoteFileOperation uploadOperation = new UploadRemoteFileOperation(fromFile, remotePath, null);
        RemoteOperationResult result = uploadOperation.execute(mClient);
        return result.isSuccess();
    }

    @Override
    public boolean mkdir(String remotePath) {
        CreateRemoteFolderOperation remoteFolderOperation = new CreateRemoteFolderOperation(remotePath, true);
        return remoteFolderOperation.execute(mClient).isSuccess();
    }

    @Override
    public boolean delete(String remotePath) {
        RemoveRemoteFileOperation uploadOperation = new RemoveRemoteFileOperation(remotePath);
        RemoteOperationResult result = uploadOperation.execute(mClient);
        return result.isSuccess();
    }

    @Override
    public RemoteFile getFileInfo(String remotePath) {
        ReadRemoteFileOperation readRemoteFileOperation = new ReadRemoteFileOperation(remotePath);
        RemoteOperationResult result = readRemoteFileOperation.execute(mClient);
        if (result.isSuccess()) {
            com.spisoft.sync.Log.d(TAG, "read success ");
            return (RemoteFile) result.getData().get(0);
        }
        return null;
    }
}
